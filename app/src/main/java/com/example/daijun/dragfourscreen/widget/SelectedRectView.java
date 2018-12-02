package com.example.daijun.dragfourscreen.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * 【说明】：拍照画面正中间的矩形框
 *
 * @author daijun
 * @version 2.0
 * @date 2016/11/4 13:38
 */
public class SelectedRectView extends View {
    private Paint mPaint;
    private int mColor;

    public SelectedRectView(Context context) {
        this(context, null);
    }

    public SelectedRectView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelectedRectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context){
        mColor = Color.BLUE;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);// 抗锯齿
        mPaint.setDither(true);// 防抖动
        mPaint.setColor(mColor);
        mPaint.setStrokeWidth(15);
        mPaint.setStyle(Paint.Style.STROKE);// 空心
        RectF rect = new RectF(0,0,this.getMeasuredWidth(),this.getMeasuredHeight());
        canvas.drawRect(rect, mPaint);
    }

}
