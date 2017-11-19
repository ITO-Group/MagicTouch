package com.example.zhoujianyu.magictouch;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.MapFragment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private String googleMapApiKey = "AIzaSyC9zg_ArARptXAcls0jKJxIcO5iRDurRXs";
    private EdgeTouchView touchView;
    private DataSender dataSender;
    public static final String TAG="myData";

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
    public final int imageSize = 1;
    public double noiseGradient[][] = new double[rowNum][colNum];
    public long flushRate = 100;
    public int noiseMax = 0;
    public final int inTouchThreshold = 1200;
    public final int outTouchThreshold = 200;

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
                int a = 1;
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //method 1:根据image之间的差值
//        int gmax = 0;
//        for(int i = 0;i<rowNum;i++){
//            for(int j = 0;j<colNum;j++){
//                int max = 0;
//                for(int k = 1;k<100;k++){
//                    int delta = Math.abs(images.get(k)[i][j]-images.get(k-1)[i][j]);
//                    if(delta>max){max = delta;}
//                    if(max>gmax){gmax = max;}
//                }
//                noiseGradient[i][j] = max;
//            }
//        }
        //method2 : 根据峰值
        for(int i = 0;i<rowNum;i++){
            for(int j = 0;j<colNum;j++){
                for(int k =0;k<100;k++){
                    if(noiseMax<images.get(k)[i][j]){
                        noiseMax = images.get(k)[i][k];
                    }
                }
            }
        }
    }

    Runnable canvasRunnable = new Runnable(){
        @Override
        public void run(){
            //measureNoise();
            while(true){
                try {
                    images.clear();
                    for(int i = 0;i<imageSize;i++){
                        refresh();
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



    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            EdgeTouchView ev = (EdgeTouchView)view;
            ev.isInTouch = true;
            ev.postInvalidate();
            return true;
        }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
//                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_notifications:
                    //mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        touchView = (EdgeTouchView) findViewById(R.id.touch);
        RequestQueue queue = Volley.newRequestQueue(this);
        dataSender = new DataSender("http://10.0.0.67:3000/",queue);
        getScreenSize();
        calPixelSize();
        getPixelPos();

        Thread canvasThread = new Thread(canvasRunnable);
        canvasThread.start();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                dataSender.getData(capacityMat,rowNum,colNum);
                dataSender.sendData(Request.Method.POST);
            }
        },0,1000);

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
                capaPos[i][j][1] = curY-3*this.pixelHight;  //消除显示偏移
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


    public int getTouchType1(int s[],int x,int y){
        //method 1:根据差值
            int maxDelta = 0;
            for(int i = 1;i<s.length;i++){
                int delta = Math.abs(s[i]-s[i-1]);
                if(delta>maxDelta){
                    maxDelta = delta;
                }
            }
            if(maxDelta>=10*noiseGradient[x][y]){
                Log.e(TAG,"in touch!!!");
                return 0;//intouch
            }
            else if(maxDelta > 5*noiseGradient[x][y]){
                Log.e(TAG,"out of touch!!!");
                return 1;//outoftouch
            }
        return -1;  //no touch
    }

    public boolean canExtend(ArrayList<int[]>list,int state[][]){
        for(int i = 0;i<list.size();i++){
            int[] node = list.get(i);
            int x = node[0];int y = node[1];
            if(x-1>0){if(state[x-1][y]==0){return true;}}
            if(x+1<rowNum){if(state[x+1][y]==0){return true;}}
            if(y-1>0){if(state[x][y-1]==0){return true;}}
            if(y+1<colNum){if(state[x][y+1]==0){return true;}}
        }
        return false;
    }

    public ArrayList<MyRect> getDrawPixel(){
        ArrayList<MyRect> rects = new ArrayList<>();
        //根据跳变
//        for(int i = 0;i<this.rowNum;i++){
//            for(int j = 0;j<this.colNum;j++){
//                int sequence[] = new int[this.imageSize];
//                for(int k = 0;k<this.imageSize;k++){
//                   sequence[k] = images.get(k)[i][j];
//                }
//                int type = getTouchType(sequence,i,j);
//                if(type == 0){
//                    rects.add(new MyRect(capaPos[i][j][0],capaPos[i][j][1],capaPos[i][j][0]+pixelWidth,capaPos[i][j][1]+pixelHight,capacityMat[i][j],0));
//                }
//                else if(type == 1){
//                    rects.add(new MyRect(capaPos[i][j][0],capaPos[i][j][1],capaPos[i][j][0]+pixelWidth,capaPos[i][j][1]+pixelHight,capacityMat[i][j],1));
//                }
//            }
//        }
        //根据峰值
        int[][]state = new int[rowNum][colNum];
        for(int i = 0;i<this.rowNum;i++){
            for(int j = 0;j<this.colNum;j++){
                if(capacityMat[i][j]>inTouchThreshold){
                    state[i][j] = 1;
                    //rects.add(new MyRect(capaPos[i][j][0],capaPos[i][j][1],capaPos[i][j][0]+pixelWidth,capaPos[i][j][1]+pixelHight,capacityMat[i][j],0));
                }
            }
        }
        boolean extend = true;
        while(extend){
            extend = false;
            for(int i = 0;i<rowNum;i++){
                for(int j = 0;j<colNum;j++){
                    if(state[i][j]==1){
                        int up[] = new int[]{i,j};up[1]-=1;
                        int down[] = new int[]{i,j};down[1]+=1;
                        int left[] =new int[]{i,j};left[0]-=1;
                        int right[] = new int[]{i,j};right[0]+=1;
                        if(up[1]>=0&&capacityMat[up[0]][up[1]]>outTouchThreshold&&capacityMat[up[0]][up[1]]<inTouchThreshold&&state[up[0]][up[1]]!=1){state[up[0]][up[1]]=1;extend = true;}
                        if(down[1]<colNum&&capacityMat[down[0]][down[1]]>outTouchThreshold&&capacityMat[down[0]][down[1]]<inTouchThreshold&&state[down[0]][down[1]]!=1){state[down[0]][down[1]]=1;extend = true;}
                        if(left[0]>=0&&capacityMat[left[0]][left[1]]>outTouchThreshold&&capacityMat[left[0]][left[1]]<inTouchThreshold&&state[left[0]][left[1]]!=1){state[left[0]][left[1]]=1;extend = true;}
                        if(right[0]<rowNum&&capacityMat[right[0]][right[1]]>outTouchThreshold&&capacityMat[right[0]][right[1]]<inTouchThreshold&&state[right[0]][right[1]]!=1){state[right[0]][right[1]]=1;extend = true;}
                    }
                }
            }
        }

        for(int i = 0;i<rowNum;i++){
            for(int j = 0;j<colNum;j++){
                if(capacityMat[i][j]<inTouchThreshold&capacityMat[i][j]>outTouchThreshold&state[i][j]!=1){
                    rects.add(new MyRect(capaPos[i][j][0],capaPos[i][j][1],capaPos[i][j][0]+pixelWidth,capaPos[i][j][1]+pixelHight,capacityMat[i][j],0));
                }
                else if(state[i][j]==1){
                    rects.add(new MyRect(capaPos[i][j][0],capaPos[i][j][1],capaPos[i][j][0]+pixelWidth,capaPos[i][j][1]+pixelHight,capacityMat[i][j],1));
                }
            }
        }
        return rects;
    }
}
