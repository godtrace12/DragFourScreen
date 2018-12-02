package com.example.daijun.dragfourscreen.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.example.daijun.dragfourscreen.R;
import com.example.daijun.dragfourscreen.widget.SelectedRectView;


/**
 * 【说明】：单个视频预览窗口
 *
 * @author daijun
 * @version 2.0
 * @date 2018/1/3 10:30
 */

public class SingleRealPlayView extends RelativeLayout implements SurfaceHolder.Callback{
    private static final String TAG = "SingleRealPlayView";
    /**
     * 视频播放的surfaceview
     */
    private SurfaceView svRealPlay;
    /**
     * 添加按钮
     */
    private ImageView btnAdd;
    /**
     * 选中框
     */
    private SelectedRectView vSelected;
    /**
     * 显示监控点名称
     */
    private TextView tvCamName;
    /**
     * 上次单击的时间 用于判断双击放大
     */
    private long mLastTime = 0;
    /**
     * 这次单击的时间 用于判断双击放大
     */
    private long mCurTime = 0;
    private Context context;
    /**
     * 用于区分预览surfaceview的单击、双击事件
     */
    static private final int MSG_SURFACEVIEW_CLICK = 1;
    static private final int MSG_SURFACEVIEW_DOUBLE_CLICK = 2;
    /**
     * 单预览窗口操作后回调给预览布局界面
     */
    private SingleRealPlayOperateCallback callback;
    /**
     * 预览surfaceview的holder
     */
    private SurfaceHolder surfaceHolder = null;

    /**
     * scroll滚动相关
     */
    private int lastX;
    private int lastY;
    private Scroller mScroller;
    /**
     * 指示是否正在进行放大
     */
    static boolean isOnZoom = false;
    /**
     * 正在放大的子view的索引
     */
    static private int ZOOM_INDEX_NONE = -1;
    static int zoomIndex = ZOOM_INDEX_NONE;

    public SingleRealPlayView(@NonNull Context context) {
        this(context, null);
    }

    public SingleRealPlayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SingleRealPlayView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        Log.e(TAG, "SingleRealPlayView: ");
        // 加载布局
        View view = LayoutInflater.from(context).inflate(R.layout.view_single_real_play, this, true);
        svRealPlay = (SurfaceView) view.findViewById(R.id.svRealPlay);
        btnAdd = (ImageView) view.findViewById(R.id.btnAdd);
        vSelected = (SelectedRectView) view.findViewById(R.id.vSelected);
        tvCamName = (TextView) view.findViewById(R.id.tvCamName);
        initViews();
    }

    private void initViews() {
        btnAdd.setOnClickListener(mainClickListener);
        svRealPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLastTime = mCurTime;
                mCurTime = System.currentTimeMillis();
                if (mCurTime - mLastTime < 300) {//双击事件
                    mCurTime = 0;
                    mLastTime = 0;
                    uiHandler.removeMessages(MSG_SURFACEVIEW_CLICK);
                    uiHandler.sendEmptyMessage(MSG_SURFACEVIEW_DOUBLE_CLICK);
                } else {//单击事件
                    uiHandler.sendEmptyMessageDelayed(MSG_SURFACEVIEW_CLICK, 310);
                }
            }
        });
        svRealPlay.getHolder().addCallback(this);
        // 初始化Scroller
        mScroller = new Scroller(context);
        svRealPlay.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
//                        Log.e(TAG, "onTouch: down");
                        lastX = (int) event.getX();
                        lastY = (int) event.getY();
                        if (isOnZoom == false) {
                            zoomIndex = getChildIndex();
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int offsetX = x - lastX;
                        int offsetY = y - lastY;
                        Log.e(TAG, "onTouch: move");
                        View viewGroupOri = (View) getParent();
                        int scrollX = viewGroupOri.getScrollX();
                        int scrollY = viewGroupOri.getScrollY();
                        Log.e(TAG, "onTouchEvent: scrollX=" + scrollX + " scrollY=" + scrollY + " offsetX=" + offsetX + " childWidth=" + getMeasuredWidth());
                        if (isOnZoom) {
                            ((View) getParent()).scrollBy(-offsetX, 0);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.e(TAG, "onTouch: up");
                        // 手指离开时，执行滑动过程
                        View viewGroup = (View) getParent();
                        //进行判断，应该往左翻到前一个预览界面、往右翻到后一个预览界面、还是回弹
                        int childWidth = getMeasuredWidth();
                        int curScrollX = viewGroup.getScrollX();
                        int curScrollY = viewGroup.getScrollY();
                        int curChildIndex = getChildIndex();
                        int indexDes = curChildIndex - zoomIndex;
                        // 方法2 可以在4个view中任意进行左右滑动
                        //当前view显示出来，父view所应滚动的距离
                        if (isOnZoom) {
                            int curViewPostionX = (curChildIndex - zoomIndex) * childWidth;
                            int parentChildCount = ((ViewGroup) getParent()).getChildCount();
                            if (curScrollX != 0 && (curScrollX < curViewPostionX - childWidth / 2) && curChildIndex != 0) {    //往左滑，切换到上一个画面
                                int disLeft = (curChildIndex - 1 - zoomIndex) * childWidth - curScrollX;
                                Log.e(TAG, "onTouch: curviewIndex=" + curChildIndex + " 滑动" + " disLeft=" + disLeft + " curScrollX=" + curScrollX + " curViewPostionX=" + curViewPostionX + " zoomIndex=" + zoomIndex);
                                mScroller.startScroll(viewGroup.getScrollX(), viewGroup.getScrollY(), disLeft, 0);
                                invalidate();
                            } else if (curScrollX != 0 && (curScrollX > curViewPostionX + childWidth / 2) && curChildIndex != parentChildCount - 1) { //往右，切换到下一个画面
                                int disRight = (curChildIndex + 1 - zoomIndex) * childWidth - curScrollX;
                                Log.e(TAG, "onTouch: curviewIndex=" + curChildIndex + " 滑动" + " disRight=" + disRight + " curScrollX=" + curScrollX + " curViewPostionX=" + curViewPostionX + " zoomIndex=" + zoomIndex);
                                mScroller.startScroll(viewGroup.getScrollX(), viewGroup.getScrollY(), disRight, 0);
                                invalidate();
                            } else {
                                int disOther = curViewPostionX - curScrollX;
                                Log.e(TAG, "onTouch: curviewIndex=" + curChildIndex + " 滑动" + " disOther=" + disOther + " curScrollX=" + curScrollX + " curViewPostionX=" + curViewPostionX + " zoomIndex=" + zoomIndex);
                                mScroller.startScroll(viewGroup.getScrollX(), viewGroup.getScrollY(), disOther, 0);
                                invalidate();
                            }
                        }

                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);     //onMeasure  onSizeChanged主要为了解决截图view随父view缩放的问题
/*        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View childView = getChildAt(i);
            if (childView.getId() == R.id.rlPlayerCapture) {
                Log.e(TAG, "onMeasure: capture child resize");
                int childWidth = sizeWidth/4;
                int childHeight = sizeHeight/4;
                Log.e(TAG, "onMeasure: childWidth="+childWidth+" childHeight="+childHeight);
                int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
                int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
                measureChild(childView,childWidthSpec,childHeightSpec);
                break;
            }
        }*/
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
/*        Log.e(TAG, "onSizeChanged: new w="+w+" h="+h+" oldW="+oldw+" oldh="+oldh);
//        invalidate();
        int viewWidth = w;
        int viewHeight = h;
        Log.e(TAG, "onSizeChanged: measure viewWidth="+viewWidth+" viewHeight="+viewHeight);
        RelativeLayout.LayoutParams captureLayoutParams = (LayoutParams) rlPlayerCapture.getLayoutParams();
        captureLayoutParams.width = w /4;
        captureLayoutParams.height = h/4;
        rlPlayerCapture.setLayoutParams(captureLayoutParams);
        rlPlayerCapture.requestLayout();
        invalidate();*/
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        // 判断Scroller是否执行完毕 isOnZoom用于判断是否处于放大状态，处于放大状态时用scoller进行滚动，处于正常时，双击后将父view(布局界面)
        //的滚动状态重置为(0,0)
        if (mScroller.computeScrollOffset() && isOnZoom) {
//            Log.e(TAG, "computeScroll: dj scrollX="+mScroller.getCurrX()+" scrollY="+mScroller.getCurrY());
            ((View) getParent()).scrollTo(
                    mScroller.getCurrX(),
                    mScroller.getCurrY());
            // 通过重绘来不断调用computeScroll
            invalidate();
        }
    }

    /**
     * 【说明】：设置单个预览界面操作后的回调
     *
     * @param
     * @return
     * @author daijun
     * @date 2018/1/3 15:45
     */
    public void setOpereateCallback(SingleRealPlayOperateCallback callback) {
        this.callback = callback;
    }

    public void setCameraName(String cameraName) {
        tvCamName.setText("cam" + cameraName);
    }

    public void setUiSelected(){
        //根据选择view时，播放的状态选择是细线框还是粗线框选中
        setUiNormalSelected();
    }

    public void setUiNormalSelected() {
        vSelected.setVisibility(VISIBLE);
        LayoutParams layoutParams = (LayoutParams) vSelected.getLayoutParams();
        layoutParams.setMargins(2, 2, 2, 2);
        vSelected.setLayoutParams(layoutParams);
    }

    public void setUiPlayingSelected() {
        vSelected.setVisibility(VISIBLE);
        LayoutParams layoutParams = (LayoutParams) vSelected.getLayoutParams();
        layoutParams.setMargins(0, 0, 0, 0);
        vSelected.setLayoutParams(layoutParams);
    }

    public void setUiUnSelected() {
        vSelected.setVisibility(GONE);
    }

    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SURFACEVIEW_CLICK:
                    Toast.makeText(getContext(), "预览控件单击！", Toast.LENGTH_SHORT).show();
                    callback.singleRealPlayClick(SingleRealPlayView.this);
                    break;
                case MSG_SURFACEVIEW_DOUBLE_CLICK:
                    Toast.makeText(getContext(), "预览控件双击！", Toast.LENGTH_SHORT).show();
                    View viewGroup = (View) getParent();
                    Log.e(TAG, "handleMessage: 预览界面双击 viewGroup scrollX=" + viewGroup.getScrollX() + " scrollY=" + viewGroup.getScrollY());
//                    mScroller.startScroll(viewGroup.getScrollX(),viewGroup.getScrollY(),-viewGroup.getScrollX(),-viewGroup.getScrollY());
//                    invalidate();
                    viewGroup.scrollTo(0, 0);
                    invalidate();
                    if (isOnZoom) {
                        isOnZoom = false;
                    } else {
                        isOnZoom = true;
                        //记录双击最大化时childView的索引
                        zoomIndex = getChildIndex();
                        Log.e(TAG, "handleMessage: zoomIndex=" + zoomIndex);
                    }
                    callback.singleRealPlayDoubleClick(SingleRealPlayView.this);
                    break;
            }
        }
    };


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceHolder = holder;
        //规避surfaceview重叠问题
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceHolder = null;

    }

    /**
     * 【说明】：获取childView中保存的索引
     *
     * @param
     * @return
     * @author daijun
     * @date 2018/1/10 13:44
     */
    private int getChildIndex() {
        int childIndex = (int) getTag();
        return childIndex;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.e(TAG, "onDetachedFromWindow: ");
        //静态变量值重置
        isOnZoom = false;
        zoomIndex = ZOOM_INDEX_NONE;
    }


    /**
     * 【说明】：规避surfaceview重叠问题
     *
     * @param
     * @return
     * @author daijun
     * @date 2018/1/31 21:00
     */
    public void setSurfaceviewOrderOnTop() {
        Log.e(TAG, "setSurfaceviewOrderOnTop: " + getTag());
        svRealPlay.setZOrderOnTop(true);
        svRealPlay.setZOrderMediaOverlay(true);
    }


    /** 界面主要的监听器 */
    private OnClickListener mainClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnAdd:
//                    callback.singleRealPlayAdd(SingleRealPlayView.this);
                    break;
            }
        }
    };


    /** 隐藏整个Loading控件 */
    private static final int STATUS_LOADING_ALL_GONE = 100;
    /** 显示Loading动画 */
    private static final int STATUS_LOADING_ANIM_VISIBLE = 101;
    /** 显示Loading提示语 */
    private static final int STATUS_LOADING_ERROR_VISIBLE = 102;
    /** 显示Loading图片 */
    private static final int STATUS_LOADING_ADD_VISIBLE = 103;


}
