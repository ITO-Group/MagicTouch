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
import java.util.Arrays;
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
    public int touchTime = 0;
    public int[][] rawCapacityData;
    public int[][] historyImageStatus = new int[Constant.ROW_NUM][Constant.COL_NUM];
    public int[][] timerMatrix = new int[Constant.ROW_NUM][Constant.COL_NUM];
    public int[][] outEventMatrix = new int[Constant.ROW_NUM][Constant.COL_NUM]; //0代表无事件，1代表click事件，2代表slide事件
    private int arounds[][] = {{0,-1},{0,1},{-1,0},{1,0},{-1,-1},{-1,-1},{-1,1},{1,1}}; //当膜的结构变了之后可能需要改

    public ArrayList<MyRect> rects = new ArrayList<>();

    // define some outevent listener
    boolean listening = false;
    boolean isDrawPixel = false;
    boolean isListeningOutSlideEvent = false;
    boolean isListeningOutClickEvent = false;

    public OnOutClickListener onOutClickListener = null;
    public OnOutSlideListener onOutSlideListener = null;
    public TouchStatusAnalyzer touchStatusAnalyzer = new TouchStatusAnalyzer();

    public int[][] deepClone(int[][] array){
        int[][] copy = new int[Constant.ROW_NUM][Constant.COL_NUM];
        for(int i = 0;i<array.length;i++){
            for(int j = 0;j<array[i].length;j++){
                copy[i][j] = array[i][j];
            }
        }
        return copy;
    }

    public EdgeTouchView(Context context, AttributeSet attrs){
        super(context,attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // listening out events
        this.listening = true;
        listeningCapacityChange();

        //初始化历史imagestatus
        for(int i = 0;i<historyImageStatus.length;i++){
            Arrays.fill(historyImageStatus[i],0);
            Arrays.fill(timerMatrix[i],0);
            Arrays.fill(outEventMatrix[i],0);
        }
    }

    public void drawPixel(int [][]imageStatus){
        getDrawingPixel(imageStatus);  // 更新rects
        EdgeTouchView.this.postInvalidate();
    }

    /**
     * event listening related functions
     */
    public void listeningCapacityChange(){
        Thread mainListeningThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(listening){
                    try {
                         int[][] imageStatus = getCurrentStatusImage();
                        // 对于blackboard,直接高亮point
                        // 数据准备
                        if(isDrawPixel){drawPixel(imageStatus);}
                        // 检测是否有outclick
                        for(int i = 0;i<imageStatus.length;i++){
                            for(int j = 0;j<imageStatus[i].length;j++){
                                if(imageStatus[i][j] == 0){outEventMatrix[i][j]=0;}
                                else if(imageStatus[i][j]==1){outEventMatrix[i][j]=1;}
                                if(historyImageStatus[i][j]==imageStatus[i][j]) timerMatrix[i][j]+=1;
                                else {
                                    // status 发生变化
                                    if(imageStatus[i][j]==0&&timerMatrix[i][j]<Constant.SLIDE_MAX_STICKS){
                                        // 刚刚由1变成0
                                        try{
                                            if(imageStatus[i][j+1]==1){
                                                Log.e("gg","sliding down");
                                                outEventMatrix[i][j+1] = 2;
                                            }
                                            else if(imageStatus[i][j-1]==1){
                                                Log.e("gg","sliding up");
                                                outEventMatrix[i][j-1] = 2;
                                            }
                                            else if(outEventMatrix[i][j+1]==0&&outEventMatrix[i][j-1]==0){
                                                if(timerMatrix[i][j+1]>Constant.SLIDE_MAX_STICKS&&timerMatrix[i][j-1]>Constant.SLIDE_MAX_STICKS){
                                                    Log.e("gg","clicking");
                                                }
                                            }
                                        }catch(Exception e){}
                                    }
                                    timerMatrix[i][j] = 0;
                                }
                            }
                        }
                        historyImageStatus = deepClone(imageStatus);
                        // 检测是否有outslide

                    }catch (IOException e){
                        Log.e("gg","capacity capture failed");
                    }
                }
            }
        });
        mainListeningThread.start();
    }

    public void setOnOutClickListener(@Nullable OnOutClickListener l){
        enableListenToOutClickEvent();
        this.onOutClickListener = l;
    }
    public void setOnOutSlideListener(@Nullable OnOutSlideListener l){
        enableListenToOutSlideEvent();
        this.onOutSlideListener = l;
    }

    public interface OnOutClickListener{
        public boolean onOutClick(View v);
    }
    public interface OnOutSlideListener{
        public boolean onOutSlide(View v);
    }

    /**
     * Draw pixel related functions
     */
    public void enableDrawPixel(){isDrawPixel = true;}
    public void disableDrawPixel(){isDrawPixel = false;}
    public void enableListenToOutClickEvent(){isListeningOutClickEvent=true;}
    public void disableListenToOutClickEvent(){isListeningOutClickEvent=false;}
    public void enableListenToOutSlideEvent(){isListeningOutSlideEvent=true;}
    public void disableListenToOutSlideEvent(){isListeningOutSlideEvent=false;}

    public void getDrawingPixel(int[][]status){
        /**
         * get current image pixel matrix and update edgeview's property rects
         */
        for(int i = 0;i<Constant.ROW_NUM;i++){
            for(int j = 0;j<Constant.COL_NUM;j++){
                if(status[i][j]>0){
                    int x = Constant.CAPA_POS[i][j][0];
                    int y = Constant.CAPA_POS[i][j][1];
                    int capa = rawCapacityData[i][j];
                    int dx = Constant.PIXEL_WIDTH;
                    int dy = Constant.PIXEL_HEIGHT;
                    int type = 0;
                    if(status[i][j]==1)type = 0;
                    else type=1;
                    rects.add(new MyRect(x,y,x+dx,y+dy,capa,type));
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
        super.onDraw(canvas);
        rects.clear(); //清空rects
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

