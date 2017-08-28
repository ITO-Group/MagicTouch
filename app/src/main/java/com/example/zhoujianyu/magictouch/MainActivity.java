package com.example.zhoujianyu.magictouch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private EdgeTouchView touchView;
    private final String TAG="myData";

    public final int INTOUCH = 0;
    public final int OUTOFTOUCH = 1;

    public int screenWidth = 0;
    public int screenHeight = 0;

    public int pixelWidth = 0;
    public int pixelHight = 0;

    public final int rowNum = 16;
    public final int colNum = 28;
    public int capacityMat[][] = new int[rowNum][colNum];
    public int capaPos[][][] = new int[rowNum][colNum][2];
    public ArrayList<int[][]> images = new ArrayList<>();
    public final int imageSize = 2;
    public double noiseGradient[][] = new double[rowNum][colNum];
    public long flushRate = 100;
//    Handler handler = new Handler(){
//        @Override
//        public void handleMessage(Message msg){
//            if(msg.what == INTOUCH){
//                touchView.isTouch = true;
//                touchView.postInvalidate();
//            }
//            else if(msg.what == OUTOFTOUCH){
//
//            }
//        }
//    };

    public int[][] getCopyCapaMat(){
        int result[][]= new int[rowNum][colNum];
        for(int i = 0;i<rowNum;i++){
            for(int j = 0;j<colNum;j++){
                result[i][j] = this.capacityMat[i][j];
            }
        }
        return result;
    }
    public void measureNoise(){
        images.clear();
        for(int i = 0;i<100;i++){
            try {
                refresh();
                images.add(getCopyCapaMat());
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int gmax = 0;
        for(int i = 0;i<rowNum;i++){
            for(int j = 0;j<colNum;j++){
                int max = 0;
                for(int k = 1;k<100;k++){
                    int delta = Math.abs(images.get(k)[i][j]-images.get(k-1)[i][j]);
                    if(delta>max){max = delta;}
                    if(max>gmax){gmax = max;}
                }
                noiseGradient[i][j] = max;
            }
        }
    }

    Runnable mRunnable = new Runnable(){
        @Override
        public void run(){
            measureNoise();
            while(true){
                try {
                    images.clear();
                    for(int i = 0;i<imageSize;i++){
                        refresh();
                        Thread.sleep(flushRate);
                        images.add(getCopyCapaMat());
                    }
                    touchView.getDrawData(getDrawPixel());
                    touchView.postInvalidate();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    };



//    private View.OnTouchListener touchListener = new View.OnTouchListener() {
//        @Override
//        public boolean onTouch(View view, MotionEvent motionEvent) {
//            EdgeTouchView ev = (EdgeTouchView)view;
//            //Log.e(TAG,String.valueOf(touchTime++));
//            ev.isTouch = true;
//            try {
//                refresh();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            ev.getDrawData(getDrawPixel());
//            ev.postInvalidate();
//            return true;
//        }
//    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    //mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    //mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    //mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        touchView = (EdgeTouchView) findViewById(R.id.touch);
        //touchView.setOnTouchListener(this.touchListener);
        getScreenSize();
        calPixelSize();
        getPixelPos();
        measureNoise();
        Thread thread = new Thread(mRunnable);
        thread.start();


        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }
    public void calPixelSize(){
        this.pixelWidth = (int)(this.screenWidth/this.rowNum);
        this.pixelHight = (int)(this.screenHeight/this.colNum);
    }

    public void getPixelPos(){
        int curX = 0;
        for(int i = 0;i<capaPos.length;i++){
            int curY = 0;
            for(int j = 0;j<capaPos[i].length;j++){
                capaPos[i][j][0] = curX;
                capaPos[i][j][1] = curY;
                curY += this.pixelHight;
            }
            curX += this.pixelWidth;
        }
    }

    public void getScreenSize(){
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
    }

    public void refresh()throws Exception{
        ArrayList<String> rawData = getRawCapacityData();
        for(int i = 0;i<rawData.size();i++){
            StringTokenizer t = new StringTokenizer(rawData.get(i));
            int j = 0;
            while(t.hasMoreTokens()){
                capacityMat[i][j++] = Integer.parseInt(t.nextToken());
            }
        }
    }

    public ArrayList<String> getRawCapacityData()throws Exception{
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
        return rawData;
    }
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus){
//        super.onWindowFocusChanged(hasFocus);
//        int loc[] = new int[2];
//        touchView.getLocationOnScreen(loc);
//        touchViewX = loc[0];
//        touchViewY = loc[1];
//    }

    public int getTouchType(int s[],int x,int y){
            int maxDelta = 0;
            for(int i = 1;i<s.length;i++){
                int delta = Math.abs(s[i]-s[i-1]);
                if(delta>maxDelta){
                    maxDelta = delta;
                }
            }
            if(maxDelta>50*noiseGradient[x][y]){
                Log.e(TAG,"in touch!!!");
                return 0;//intouch
            }
            else if(maxDelta > 10*noiseGradient[x][y]){
                Log.e(TAG,"out of touch!!!");
                return 1;//outoftouch
            }

        return -1;  //no touch
    }

    public ArrayList<MyRect> getDrawPixel(){
        ArrayList<MyRect> rects = new ArrayList<>();
        for(int i = 0;i<this.rowNum;i++){
            for(int j = 0;j<this.colNum;j++){
                int sequence[] = new int[this.imageSize];
                for(int k = 0;k<this.imageSize;k++){
                   sequence[k] = images.get(k)[i][j];
                }
                int type = getTouchType(sequence,i,j);
                if(type == 0){
                    rects.add(new MyRect(capaPos[i][j][0],capaPos[i][j][1],capaPos[i][j][0]+pixelWidth,capaPos[i][j][1]+pixelHight,capacityMat[i][j],0));
                }
                else if(type == 1){
                    rects.add(new MyRect(capaPos[i][j][0],capaPos[i][j][1],capaPos[i][j][0]+pixelWidth,capaPos[i][j][1]+pixelHight,capacityMat[i][j],1));
                }
            }
        }
        return rects;
    }
}
