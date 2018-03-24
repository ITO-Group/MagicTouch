package com.example.zhoujianyu.magictouch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
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
    public int type;  //1->touch on,2->touch out
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
    public int[][] rawCapacityData;
    public ArrayList<MyRect> rects = new ArrayList<>();
    public ArrayList<MyRect> lastRects = new ArrayList<>();

    // define some outevent listener
    boolean listening = false;
    public Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            EdgeTouchView.this.invalidate();
        }
    };
    public OutClickListener outClickListener = null;
    public OutSlideListener outSlideListener = null;
    public TouchStatusAnalyzer touchStatusAnalyzer = new TouchStatusAnalyzer();

    public void deepClone(){
        for(int i = 0;i<rects.size();i++){
            lastRects.add(rects.get(i).clone());
        }
    }

    public EdgeTouchView(Context context, AttributeSet attrs){
        super(context,attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // listening out events
        this.listening = true;
        listeningCapacityChange();
    }

//    public boolean hasOutTouch(int[][]status){
//        for(int i = 0;i<status.length;i++){
//            for(int j = 0;j<status[i].length;j++){
//                if(status[i][j]==1)return true;
//            }
//        }
//        return false;
//    }

    public void listeningCapacityChange(){
        Thread mainListeningThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(listening){
                    try {
                        rawCapacityData = captureCapa();
                        // 对于blackboard,直接高亮point
                        // 数据准备
                        rects.clear();
                        getDrawingPixel(rawCapacityData);
                        EdgeTouchView.this.postInvalidate();
                        // 检测是否有outclick
                        // 检测是否有outslide

                    }catch (IOException e){
                        Log.e("gg","capacity capture failed");
                    }
                }
            }
        });
        mainListeningThread.start();
    }

    public void getDrawingPixel(int[][]status){
        for(int i = 0;i<Constant.ROW_NUM;i++){
            for(int j = 0;j<Constant.COL_NUM;j++){
                if(status[i][j]>Constant.OUT_TOUCH_THRESHOLD){
                    int x = Constant.CAPA_POS[i][j][0];
                    int y = Constant.CAPA_POS[i][j][1];
                    int capa = rawCapacityData[i][j];
                    int dx = Constant.PIXEL_WIDTH;
                    int dy = Constant.PIXEL_HEIGHT;
                    rects.add(new MyRect(x,y,x+dx,y+dy,capa,0));
                }
            }
        }
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

    public void setOutClickListener(@Nullable OutClickListener l){
        this.outClickListener = l;
    }

    public void setOutSlideListener(@Nullable OutSlideListener l){
        this.outSlideListener = l;
    }

    public interface OutClickListener{
        public boolean onClick(View v);
    }
    public interface OutSlideListener{
        public boolean onSlide(View v);
    }

    /***
     * utility function
     */
    public int[][] getCurrentStatusImage()throws IOException {
        /**
         * 首先获取原始的当前28*16 capacity 矩阵，然后返回该时刻的矩阵状态
         */
        this.rawCapacityData = captureCapa();
        int currentTouchPoints[][] = touchStatusAnalyzer.refineTouchPosition(this.rawCapacityData);  //当前outTouch 点集
        return currentTouchPoints;
    }

    public int[][] captureCapa()throws IOException{
        /***
         return a matrix that contains raw capacity sensing data
         ***/
        String line = "";
        ArrayList<String> rawData = new ArrayList<>();
        String command[] = {"aptouch_daemon_debug", "diffdata"};
        Process process = new ProcessBuilder(new String[] {"aptouch_daemon_debug", "diffdata"}).start();
        InputStream procInputStream = process.getInputStream();
        InputStreamReader reader = new InputStreamReader(procInputStream);
        BufferedReader bufferedreader = new BufferedReader(reader);
        while ((line = bufferedreader.readLine()) != null) {
            rawData.add(line);
        }
        int[][] curCapaData = new int[Constant.ROW_NUM][Constant.COL_NUM];
        for(int i = 0;i<rawData.size();i++){
            StringTokenizer t = new StringTokenizer(rawData.get(i));
            int j = 0;
            while(t.hasMoreTokens()){
                curCapaData[i][j++] = Integer.parseInt(t.nextToken());
            }
        }
        return curCapaData;
    }
}

