package com.example.daijun.dragfourscreen.view;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.widget.ViewDragHelper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;


/**
 * 【说明】：onlayout中使用layout方式，tryCaptureView后导致childview索引变化，在随后onlayout方法中导致view位置错乱
 *   解决方法：在onFinishInflate()中子view通过setTag设置view的初始index，后续如果两个子view进行位置交换，则设置位置
 *              并且交换setTag中保存的view的index。
 *
 * @author daijun
 * @version 2.0
 * @date 2017/12/29 15:11
 */

public class RealPlayLayout extends ViewGroup implements SingleRealPlayOperateCallback {
    private static final String TAG = "RealPlayLayout";
    /** 布局的宽、高*/
    private int sizeWidth;
    private int sizeHeight;
    /** 子view的宽、高*/
    private int childWidth;
    private int childHeight;
    /** 拖动的子view的坐标 */
    private int mDragOriLeft;
    private int mDragOriTop;
    private ViewDragHelper mDragHelper;
    /** 画面的分割模式 */
    static private int ROW_NUM = 2;
    /** 要放大的view的索引 为-1时，默认没放大的view  为其他值时即代表要放大的view的索引*/
    private int zoomIndex = ZOOM_INDEX_NONE;
    static private int ZOOM_INDEX_NONE = -1;
    /** 删除监控画面时关联的用于指示显示删除操作的view */
    private View deleteFlagView = null;
    /** 布局界面相关操作回调 */
    private RealPlayCallback callback;
    /** 是否达到了可以删除预览子窗口的条件 */
    private boolean isCanDelete = false;
    /** 当前选中窗口的索引 */
    private int mCurSelChildIndex =0;


    public RealPlayLayout(Context context) {
        this(context,null);
    }

    public RealPlayLayout(Context context, AttributeSet attrs) {
        this(context,attrs,0);
    }

    public RealPlayLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //拖动相关初始化及处理
        initDragHelper();
        //根据选择的画面分割模式，添加相应个数的监控点
        for (int i=0;i<ROW_NUM*ROW_NUM;i++) {
            SingleRealPlayView playView = new SingleRealPlayView(getContext());
            playView.setOpereateCallback(this);
            addView(playView);
        }
    }

    private void initDragHelper(){
        mDragHelper = ViewDragHelper.create(this, new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                Log.e(TAG, "tryCaptureView: ");
                if (zoomIndex == ZOOM_INDEX_NONE) {
                    //当拖拽的view正在回弹时，不允许再进行拖拽
                    if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING) {
                        return false;
                    } else {
                        child.bringToFront();
                    }
//                child.requestLayout();
//                invalidate();
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void onViewCaptured(View capturedChild, int activePointerId) {
                super.onViewCaptured(capturedChild, activePointerId);
                //将拖动的子view里面的surfaceview顺序置为最上层，解决surfaceview重叠的问题
                ((SingleRealPlayView)capturedChild).setSurfaceviewOrderOnTop();
                mDragOriLeft = capturedChild.getLeft();
                mDragOriTop = capturedChild.getTop();
                Log.e(TAG, "onViewCaptured: left="+mDragOriLeft+" top="+mDragOriTop+" x="+capturedChild.getX()+" y="+capturedChild.getY());
                if (callback != null) {
                    callback.prepareDeleteRealPlay();
                }
                isCanDelete = false;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                return left;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                return top;
            }

            @Override
            public int getViewHorizontalDragRange(View child) {
                return getMeasuredWidth()-child.getMeasuredWidth();
            }

            @Override
            public int getViewVerticalDragRange(View child) {
                return getMeasuredHeight()-child.getMeasuredHeight();
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                //去除删除标志
                if (callback != null) {
                    callback.cancelOrFinishDelete();
                }
                if (isCanDelete) {      //判断是否处于删除状态
                    deleteSingleRealPlay(releasedChild);
                }else if (findExchangeItemView(releasedChild)) {
                    //判断什么情况下view弹回，什么情况下进行两个子view位置交换

                } else {
                    Log.e(TAG, "onViewReleased: go back");
                    mDragHelper.settleCapturedViewAt(mDragOriLeft, mDragOriTop);
                    invalidate();
                }
                isCanDelete = false;
            }

            @Override
            public void onViewDragStateChanged(int state) {
                super.onViewDragStateChanged(state);
                Log.e(TAG, "onViewDragStateChanged: state="+state);
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);
//                Log.e(TAG, "onViewPositionChanged: left="+left+" top="+top+" dx="+dx+" dy="+dy);
//                Log.e(TAG, "onViewPositionChanged: location on screen x="+location[0]+" y="+location[1]);
                //当是拖动状态时，判断被拖动的子View与删除标志view的位置关系
                if (deleteFlagView != null && mDragHelper.getViewDragState()== ViewDragHelper.STATE_DRAGGING) {
                    int[] location = new int[2];
                    changedView.getLocationOnScreen(location);
                    int[] locDeleteView = new int[2];
                    deleteFlagView.getLocationOnScreen(locDeleteView);
                    int delHeight = deleteFlagView.getHeight();
                    int delCenterHeight = locDeleteView[1]+delHeight/2;
                    int changeViewCenterHeight = location[1]+childHeight/2;
                    if (changeViewCenterHeight < delCenterHeight) {
                        Log.e(TAG, "onViewPositionChanged: 达到删除条件了,让删除view置为选中");
                        Log.e(TAG, "onViewPositionChanged: viewdrager 状态"+mDragHelper.getViewDragState());
                        if (callback != null) {    //达到删除条件
                            callback.satisfyDeleteRealPlayCondition();
                            isCanDelete = true;
                        }
                    } else {
                        if (callback != null) {
                            callback.prepareDeleteRealPlay();
                            isCanDelete = false;
                        }
                    }
                }

            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int count = getChildCount();
        //记录初始化时view顺序
        for (int i = 0; i < count; i++) {
            View childView = getChildAt(i);
            childView.setTag(i);
            ((SingleRealPlayView) childView).setCameraName(i+"");
        }
        Log.e(TAG, "onFinishInflate: childCount="+getChildCount());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /** 获得此ViewGroup上级容器为其推荐的宽和高，以及计算模式 */
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        //将子view的长宽都置为父view的一半
        childWidth = sizeWidth/ROW_NUM;
        childHeight = sizeHeight/ROW_NUM;
//        Log.e(TAG, "onMeasure: father width="+sizeWidth+" father height="+sizeHeight);
        int childWidthSppec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
        //判断是否有子view要进行放大,要放大的子view将长度置为父view的长宽
        if (zoomIndex == ZOOM_INDEX_NONE) {
            measureChildren(childWidthSppec, childHeightSpec);
        } else {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View childView = getChildAt(i);
                int index = (int) childView.getTag();
//                if (index == zoomIndex) {
                    int zoomChildWidthSpec = MeasureSpec.makeMeasureSpec(sizeWidth, MeasureSpec.EXACTLY);
                    int zoomChildHeightSpec = MeasureSpec.makeMeasureSpec(sizeHeight, MeasureSpec.EXACTLY);
                    measureChild(childView, zoomChildWidthSpec, zoomChildHeightSpec);
//                } else {
//                    measureChild(childView,childWidthSppec, childHeightSpec);
//                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        Log.e(TAG, "onLayout: ");
        int count = getChildCount();
        int childWidth = sizeWidth / ROW_NUM;
        int childHeight = sizeHeight / ROW_NUM;
        int left = 0;
        int top = 0;
        for (int i = 0; i < count; i++) {
            View childView = getChildAt(i);
//            Log.e(TAG, "onLayout: childView index="+i+" width="+childView.getWidth()+" height="+childView.getHeight());
            int oriIndex = (int) childView.getTag();
            //1、正常分割显示情况
            if (zoomIndex == ZOOM_INDEX_NONE) {
                left = (oriIndex % ROW_NUM) * childWidth;
                top = (oriIndex / ROW_NUM) * childHeight;
                childView.layout(left, top, left + childWidth, top + childHeight);
                if (childView.getVisibility() == INVISIBLE || childView.getVisibility() == GONE) {
                    childView.setVisibility(VISIBLE);
                }
            } else {    //2、选中的子view要放大(充满父view的情况)
                if (zoomIndex == oriIndex) {
//                    Log.e(TAG, "onLayout: 放大 index="+oriIndex);
                    childView.layout(0, 0, sizeWidth, sizeHeight);
                } else {
//                    Log.e(TAG, "onLayout: 放大 正常分割大小");
/*                    left = (oriIndex % ROW_NUM) * childWidth;
                    top = (oriIndex / ROW_NUM) * childHeight;
                    childView.layout(left, top, left + childWidth, top + childHeight);
                    childView.setVisibility(INVISIBLE);*/

                    //适应scroll page的适配，将子view排成一排
                    int indexDis = oriIndex - zoomIndex;
                    left = indexDis * sizeWidth;
                    top = 0;
                    childView.layout(left, top, left + sizeWidth, top + sizeHeight);
                }
            }
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mDragHelper != null && mDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        return true;
    }

    /**
     *【说明】：判断是否符合位置交换条件，判断出与哪个view交换，并交换2个item的位置
     * 分2个步骤，1、当前中心与哪个view的中心最近。2、当前移动的距离是否足够大，移动的距离再某个范围内
     *@author daijun
     *@date 2017/12/27 16:24
     *@param
     *@return
     */
    private boolean findExchangeItemView(View releasedChid) {
        //是否交换结果，false-没交换 true-发生了交换
        boolean result = false;
        int childCount = getChildCount();
        boolean isFound = false;
        View selView = null;
        float selDis = 0;
        for (int i=0;i<childCount;i++) {
            View childView = getChildAt(i);
            if (childView == releasedChid) {
                Log.e(TAG, "exchangeTwoItemPosition: 同一个view "+i);
            } else {
                Log.e(TAG, "exchangeTwoItemPosition: 不同view "+i);
                float childX = childView.getX();
                float childY = childView.getY();
                int childWidth = childView.getWidth();
                int childHeight = childView.getHeight();
                float desCenterX = childX + childWidth/2;
                float desCenterY = childY + childHeight/2;
                float oriCenterX = releasedChid.getX()+childWidth/2;
                float oriCenterY = releasedChid.getY()+childHeight/2;
                float distance = (float) Math.sqrt(Math.pow((desCenterX-oriCenterX),2)+ Math.pow((desCenterY-oriCenterY),2));
                float distanceRefer = (float) Math.sqrt(Math.pow((desCenterX-(mDragOriLeft+childWidth/2)),2)+ Math.pow((desCenterY-(mDragOriTop+childHeight/2)),2));
                //方法2 判断item位置时，水平、垂直方向差值都小于某个比值时，则认为找到了该item，否则认为没找到
                // 移动后与移动前距离比值
                float rate = distance / distanceRefer;
                if (rate <= 0.3) {
                    if (!isFound) {     //还没找到一个符合条件的item
                        isFound = true;
                        result = true;
                        selDis = distance;
                        selView = childView;
                    } else {    //找到了多于一个符合条件的item,对距离进行对比，选择距离更小的那一个
                        if (distance < selDis) {
                            selDis = distance;
                            selView = childView;
                        }
                    }
                }

            }
        }
        if (isFound && selView!=null) {
            exchangeTwoItemViewPosition(releasedChid,selView);
        }
        return result;
    }

    /**
     *【说明】：交换2个item的位置
     *@author daijun
     *@date 2017/12/28 10:45
     *@param
     *@return
     */
    private void exchangeTwoItemViewPosition(View oriView, final View desView){
        int desLeft = desView.getLeft();
        int desTop = desView.getTop();
        int desRight = desView.getRight();
        int desBottom = desView.getBottom();
        int oriLeft = oriView.getLeft();
        int oriTop = oriView.getTop();
        oriView.setLeft(desLeft);
        oriView.setTop(desTop);
        oriView.setRight(desRight);
        oriView.setBottom(desBottom);
        Log.e(TAG, "exchangeTwoItemViewPosition: desMarginleft="+desLeft+" desMarginTop="+desTop+" desX="+desView.getX()+" desY="+desView.getY()+" oriLeft="+mDragOriLeft+" oriTop="+mDragOriTop+" oriMarginLeft="+oriLeft+" oriMarginTop="+oriTop);
//        invalidate();
        //交换子view的索引index。
        int oriIndex = (int) oriView.getTag();
        int desIndex = (int) desView.getTag();
        desView.setTag(oriIndex);
        oriView.setTag(desIndex);

        //方法2 使用ValueAnimator改变marginLeft 和 marginTop
        int duration = 400;
        //左边
        ValueAnimator animDesX = ValueAnimator.ofInt(desLeft,mDragOriLeft);
        animDesX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
               int value = (int) animation.getAnimatedValue();
                desView.setLeft(value);
            }
        });
        //顶部
        ValueAnimator animDesY = ValueAnimator.ofInt(desTop,mDragOriTop);
        animDesY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                desView.setTop(value);
            }
        });
        //右边
        ValueAnimator animDesRight = ValueAnimator.ofInt(desRight,mDragOriLeft+childWidth);
        animDesRight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                desView.setRight(value);
            }
        });
        //底部
        ValueAnimator animDesBottom = ValueAnimator.ofInt(desBottom,mDragOriTop+childHeight);
        animDesBottom.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                desView.setBottom(value);
            }
        });
        AnimatorSet anisetDes = new AnimatorSet();
        anisetDes.setInterpolator(new AccelerateDecelerateInterpolator());
        anisetDes.setDuration(duration);
        anisetDes.playTogether(animDesX,animDesY,animDesRight,animDesBottom);
        anisetDes.start();

    }


    @Override
    public void singleRealPlayDoubleClick(View v) {
        int index = (int) v.getTag();
        //保存当前选中播放窗口的索引
        mCurSelChildIndex = index;
        if (zoomIndex == ZOOM_INDEX_NONE) { //当前没有被放大的界面，所以当前操作为进行放大
            zoomIndex = index;
            requestLayout();
            setAllRealPlayUnSelected(); //放大状态，隐藏选中框
            invalidate();
        } else {        //恢复正常大小
            zoomIndex = ZOOM_INDEX_NONE;
            requestLayout();
            setSingleRealPlaySelected(v);   //正常大小状态，显示选中框
            invalidate();
        }
        Log.e(TAG, "singleRealPlayViewDoubleClick: item Index="+index);
        LayoutParams layoutParams = v.getLayoutParams();
//        v
    }

    @Override
    public void singleRealPlayAdd(View v) {
        setSingleRealPlaySelected(v);
        //设置单个预览窗口的监控点信息，启动播放
        SingleRealPlayView curChildView = (SingleRealPlayView) v;
//        curChildView.setCameraInfo(mCameraInfo);
    }

    @Override
    public void singleRealPlayClick(View v){
        setSingleRealPlaySelected(v);
    }

    /**
    *【说明】：设置单个view被选中的状态及界面显示
    *@author daijun
    *@date 2018/2/5 11:10
    *@param
    *@return
    */
    private void setSingleRealPlaySelected(View v) {
        int curIndex = (int) v.getTag();
        //保存当前选中播放窗口的索引
        mCurSelChildIndex = curIndex;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            SingleRealPlayView childView = (SingleRealPlayView) getChildAt(i);
            int childIndex = (int) childView.getTag();
            if (childIndex == curIndex) {
                childView.setUiSelected();
            } else {
                childView.setUiUnSelected();
            }
        }
    }

    /**
    *【说明】：设置所有子播放界面都未选中
    *@author daijun
    *@date 2018/2/5 11:13
    *@param
    *@return
    */
    private void setAllRealPlayUnSelected(){
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            SingleRealPlayView childView = (SingleRealPlayView) getChildAt(i);
            childView.setUiUnSelected();
        }
    }

    /**
    *【说明】：设置删除标志视图，用于标志拖动删显示删除布局。
    *@author daijun
    *@date 2018/1/4 15:52
    *@param
    *@return
    */
    public void setDeleteView(View delView) {
        this.deleteFlagView = delView;
    }

    /**
     *【说明】：设置预览布局界面操作后的回调
     *@author daijun
     *@date 2018/1/3 15:45
     *@param
     *@return
     */
    public void setOpereateCallback(RealPlayCallback callback){
        this.callback = callback;
    }

    /**
    *【说明】：删除选定拖动的预览子窗口
    *@author daijun
    *@date 2018/1/4 18:28
    *@param
    *@return
    */
    public void deleteSingleRealPlay(View childView){
        int index = (int) childView.getTag();
        Toast.makeText(getContext(),"删除预览窗口 index="+index, Toast.LENGTH_SHORT).show();
        SingleRealPlayView selView = (SingleRealPlayView) childView;
    }

    /**
    *【说明】：临时方案，横屏状态下，滑动页面后，滑动完成后，先点击一下完成后的界面，再进行切换到竖屏,竖屏时index=2，转横屏后又转回来，还是index=2
    *@author daijun
    *@date 2018/1/11 15:19
    *@param
    *@return
    */
    public void resetContentScroll(){
        Log.e(TAG, "resetContentScroll: ");
        scrollTo(0,0);
    }

    /**
    *【说明】：获取当前选中的播放窗口
    *@author daijun
    *@date 2018/4/25 19:39
    *@param
    *@return
    */
    private SingleRealPlayView getCurSelRealPlayView(){
        int count = getChildCount();
        View childView = null;
        for (int i = 0; i < count; i++) {
            childView = getChildAt(i);
            int childIndex = (int) childView.getTag();
            if (childIndex == mCurSelChildIndex) {
                break;
            }
        }
        return (SingleRealPlayView) childView;
    }

    public interface RealPlayCallback{
        void prepareDeleteRealPlay();
        void satisfyDeleteRealPlayCondition();
        void cancelOrFinishDelete();
    }

}
