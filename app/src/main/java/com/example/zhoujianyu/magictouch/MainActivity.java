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
import android.widget.Button;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;



public class MainActivity extends AppCompatActivity {

    //private String googleMapApiKey = "AIzaSyC9zg_ArARptXAcls0jKJxIcO5iRDurRXs";
    private EdgeTouchView touchView;
    private DataSender dataSender;
    private OutEventManager outEventManager;
    private Button collectButton;
    private View.OnClickListener collectButtonClickListener;
    public static final String TAG = Constant.TAG;

    public int screenWidth = 0;
    public int screenHeight = 0;

    public int pixelWidth = 0;
    public int pixelHight = 0;

    public final int rowNum = Constant.ROW_NUM;
    public final int colNum = Constant.COL_NUM;
    public int capacityMat[][] = new int[rowNum][colNum];
    public int capaPos[][][] = new int[rowNum][colNum][2];
    public ArrayList<int[][]> images = new ArrayList<>();
    public final int imageSize = 1;
    public double noiseGradient[][] = new double[rowNum][colNum];
    public int flushRate = Constant.FLUSH_RATE;
    public int noiseMax = 0;
    public final int inTouchThreshold = Constant.IN_TOUCH_THRESHOLD;
    public final int outTouchThreshold = Constant.OUT_TOUCH_THRESHOLD;
    public final int postRate = Constant.POST_RATE;



    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
//                    mTextMessage.setText(R.string.title_home);
                    return true;
//                case R.id.navigation_dashboard:
//                    //启动mapsactivity
//                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
//                    startActivity(intent);
//                    return true;
                case R.id.navigation_notifications:
                    //mTextMessage.setText(R.string.title_notifications);
                    Intent intent2 = new Intent(MainActivity.this,BlackboardActivity.class);
                    startActivity(intent2);
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
//        touchView.enableDrawPixel();
        collectButton = (Button) findViewById(R.id.collect_button);
        outEventManager = new OutEventManager();
        RequestQueue queue = Volley.newRequestQueue(this);
        dataSender = new DataSender("http://173.250.139.42:3000/",queue);
        getScreenSize();
        calPixelSize();
        getPixelPos();
        Constant.CAPA_POS = capaPos;

//        new Timer().scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                dataSender.getData(capacityMat,rowNum,colNum);
//                dataSender.sendData(Request.Method.POST);
//            }
//        },0,postRate);

        //set up listener

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        BottomNavigationViewHelper.disableShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener); //navigation bar listener
        collectButtonClickListener = new View.OnClickListener() {
            boolean execute = true;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    while(execute){
                        try{
                            int[][] rawdata = outEventManager.captureCapa();
                            dataSender.getData(rawdata,rowNum,colNum);
                            dataSender.sendData(Request.Method.POST,"");
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread dataCollectThread = new Thread(runnable);
            @Override
            public void onClick(View v) {
                Button btn = (Button) v;
                Log.e("googleMap",btn.getText().toString());
                if(btn.getText().toString().equals("Start Collecting Data")){
                    btn.setText("collecting");
                    //launch data collector and data sender
                    execute = true;
                    dataCollectThread.start();
                }
                else{
                    btn.setText("Start Collecting Data");
                    dataSender.sendData(Request.Method.POST,"stop");
                    // end data collector and data sender
                    execute = false;
                }

            }
        };
        collectButton.setOnClickListener(collectButtonClickListener);


    }
    public void calPixelSize(){
        this.pixelWidth = (int)(this.screenWidth/this.rowNum);
        this.pixelHight = (int)(this.screenHeight/this.colNum);
        Constant.PIXEL_WIDTH = this.pixelWidth;
        Constant.PIXEL_HEIGHT = this.pixelHight;
    }

    public void getPixelPos(){
        int curX = 0;
        for(int i = 0;i<capaPos.length;i++){
            int curY = 0;
            for(int j = 0;j<capaPos[i].length;j++){
                capaPos[i][j][0] = curX;
                capaPos[i][j][1] = curY+1*this.pixelHight;  //消除显示偏移
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
        /**
         * description：获得原始String 类型的电容信号，并将其转化为int矩阵
         */
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

    public ArrayList<MyRect> getDrawPixel(){
        /**
         * input: 类成员变量capacityMat
         * output: 一个list,包含所有需要绘的点和每个点的类型（outTouch/inTouch）
         */
        ArrayList<MyRect> rects = new ArrayList<>();
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
