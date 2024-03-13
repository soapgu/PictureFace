package com.soapgu.pictureface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SimpleFaceRectView extends View {

    private Paint paint;
    private Rect drawRect = new Rect();
    private double zoom;
    private List<Rect> rectList = new ArrayList<>();

    public SimpleFaceRectView(Context context) {
        super(context);
        init();
    }

    public SimpleFaceRectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SimpleFaceRectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SimpleFaceRectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init(){
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);  // 设置画笔样式为描边，用于画空心图形
        paint.setStrokeWidth(2);  // 设置描边宽度，可根据需求调整
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if( !rectList.isEmpty() ){
            rectList.forEach( rect -> {
                drawRect.left = (int)(rect.left * zoom);
                drawRect.top = (int)(rect.top * zoom);
                drawRect.right = (int)(rect.right * zoom);
                drawRect.bottom = (int)(rect.bottom * zoom);
                canvas.drawRect(drawRect,paint);
            } );
        }

    }

    public void updateFaceRect(int width, int height, List<Rect> rectList ){
        this.zoom = (double) getWidth() / (double)width;
        this.rectList = rectList;
        invalidate();
    }
}
