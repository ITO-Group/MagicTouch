package com.example.zhoujianyu.magictouch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by ZhouJianyu on 2017/8/26.
 */

class MyRect{
    public float left;
    public float top;
    public float right;
    public float bottom;
    public int type;
    public int capacity;
    public MyRect(float l,float t,float r,float b,int c,int type){
        this.left = l;this.right = r;this.bottom = b;this.top = t;this.type = type;
        this.capacity = c;
    }
    @Override
    protected MyRect clone(){
        MyRect r = new MyRect(this.left,this.top,this.right,this.bottom,this.capacity,this.type);
        return r;
    }
}

public class EdgeTouchView extends View{
    private Paint mPaint;
    public final String TAG = "myData";
    public boolean isInTouch = false;
    public int touchTime = 0;
    public boolean isOutTouch = false;
    public ArrayList<MyRect> rects = new ArrayList<>();
    public ArrayList<MyRect> lastRects = new ArrayList<>();
    public void deepClone(){
        for(int i = 0;i<rects.size();i++){
            lastRects.add(rects.get(i).clone());
        }
    }

    public EdgeTouchView(Context context, AttributeSet attrs){
        super(context,attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }


    protected void onDraw(Canvas canvas){
        mPaint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
        for(int i = 0;i<rects.size();i++){
            if (rects.get(i).type == 1){
                mPaint.setColor(Color.RED);
                canvas.drawRect(rects.get(i).left, rects.get(i).top, rects.get(i).right, rects.get(i).bottom, mPaint);
                mPaint.setColor(Color.WHITE);
                mPaint.setTextSize(20);
                mPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(String.valueOf(rects.get(i).capacity),rects.get(i).left,rects.get(i).top,mPaint);
            }
            else if(rects.get(i).type==0){
                mPaint.setColor(Color.YELLOW);
                canvas.drawRect(rects.get(i).left, rects.get(i).top, rects.get(i).right, rects.get(i).bottom, mPaint);
                mPaint.setColor(Color.WHITE);
                mPaint.setTextSize(20);
                mPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(String.valueOf(rects.get(i).capacity),rects.get(i).left,rects.get(i).top,mPaint);
            }
        }
        deepClone();
        super.onDraw(canvas);
    }

    public void getDrawData(ArrayList<MyRect> rects){
        isOutTouch = true;
        this.rects.clear();

        this.rects = rects;
    }

}
