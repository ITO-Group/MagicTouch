package com.example.zhoujianyu.magictouch;

import android.os.Handler;
import android.util.Log;

import com.android.volley.Request;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ZhouJianyu on 2017/11/18.
 */

public class OutEventManager {
    boolean detectingOutClick = false;
    boolean detectingOutSlide = false;
    boolean detectingOutPress = false;
    long lastTime = Calendar.getInstance().getTimeInMillis();
    long currentTime = Calendar.getInstance().getTimeInMillis();

    int[][] newCapa;
    int hisTouchStatus[][] = new int[Constant.ROW_NUM][Constant.COL_NUM];
    long [][]timeMatrix = new long[Constant.ROW_NUM][Constant.COL_NUM];  //记录每个cell黄色持续的时间

    ArrayList<int[]>outTouchPointSet = new ArrayList<int[]>();

    TouchStatusAnalyzer touchStatusAnalyzer = new TouchStatusAnalyzer();
    ArrayList<ArrayList<int[]>>onTouchStatus = new ArrayList<>();  //size=2, 保存当前和上一时刻的touch points

    public GoogleMap map;
    public Timer timer = new Timer();

    public interface OutClickListener{
        boolean onOutClick(GoogleMap map,int[]position);
    }

//    public void updateStatus(ArrayList<ArrayList<int[]>> ss, ArrayList<int[]>s){
//        /**
//        input:
//            ss: size 应为2，只保留当前和上一时刻的outTouch点的集合
//            例如，当前status的outTouch点集为[{1,2},{2,3}]，表示在{1，2}和{2，3}这两个位置各有一个outTouch
//            s: 当前outTouch点集
//         */
//        if(!ss.isEmpty()){
//            ss.remove(0);
//            ArrayList<int[]> cs = new ArrayList<>();
//            cs = (ArrayList<int[]>)s.clone();
//            ss.add(cs);
//        }
//        //Log.e(MainActivity.TAG,Integer.valueOf(ss.size()).toString());
//
//    }

    public void initializeTouchStatus(){
        for(int i = 0;i<hisTouchStatus.length;i++){
            for(int j = 0;j<hisTouchStatus[i].length;j++){
                hisTouchStatus[i][j] = 0;
            }
        }
    }

    public void initializeStatus(){
        ArrayList<int[]>s0 = new ArrayList<>();
        ArrayList<int[]>s1 = new ArrayList<>();
        onTouchStatus.add(s0);
        onTouchStatus.add(s1);
    }

    public OutEventManager(){
        initializeTouchStatus();
        initializeStatus();
        //初始化time矩阵
        for(int i = 0;i<Constant.ROW_NUM;i++){
            Arrays.fill(timeMatrix[i],0);
        }
    }

    public OutEventManager(GoogleMap map){
        initializeTouchStatus();
        initializeStatus();
        //初始化time矩阵
        for(int i = 0;i<Constant.ROW_NUM;i++){
            Arrays.fill(timeMatrix[i],0);
        }
        this.map = map;
    }

    public long getInterval(){
        lastTime = currentTime;
        currentTime = Calendar.getInstance().getTimeInMillis();
        return currentTime-lastTime;
    }

    public int[][] captureCapa()throws IOException{
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

    public void updateStauts(int[][]newTouchStatus){
        assert(newTouchStatus.length==hisTouchStatus.length);
        for(int i = 0;i<hisTouchStatus.length;i++){
            assert(newTouchStatus[i].length==hisTouchStatus.length);
            for(int j = 0;j<hisTouchStatus[i].length;j++){
                hisTouchStatus[i][j] = newTouchStatus[i][j];
            }
        }
    }

    public int[][] getCurrentStatusImage()throws IOException{
        /**
         * 首先获取原始的当前28*16 capacity 矩阵，然后返回该时刻的矩阵状态
         */
        this.newCapa = captureCapa();
        int currentTouchPoints[][] = touchStatusAnalyzer.refineTouchPosition(newCapa);  //当前outTouch 点集
        return currentTouchPoints;
    }

    private boolean detectOutSlide(){
        timer.scheduleAtFixedRate(new TimerTask() {
            int lastPos[] = {0,0};
            @Override
            public void run() {
                try{
                    int currentTouchPoint[][] = getCurrentStatusImage();
                }
                catch(IOException e){
                    Log.e(MainActivity.TAG,"update capacity failed.");
                }
            }
        },0, Constant.FLUSH_RATE);
        return true;
    }
    public boolean startDetectAll(){

        return true;
    }

    public void updateTouchStatus(int[][]newStatus){
        for(int i = 0;i< Constant.ROW_NUM;i++){
            for(int j = 0;j<Constant.COL_NUM;j++){
                hisTouchStatus[i][j]=newStatus[i][j];
            }
        }
    }

    public boolean startDetectOutClick(final Handler handler){
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try{
                    int currentPoint[][] = getCurrentStatusImage();
                    long interval = getInterval();
                    //Log.e("outclick",Long.valueOf(interval).toString());
                    /*
                    更新timer矩阵
                     */
                    for(int i = 0;i<Constant.ROW_NUM;i++){
                        for(int j = 0;j<Constant.COL_NUM;j++){
                            if(hisTouchStatus[i][j]==currentPoint[i][j]){
                                timeMatrix[i][j]+=interval;
                            }
                            else{
                                timeMatrix[i][j] = interval;
                                //只检测下降沿
                                if(hisTouchStatus[i][j]==1){
                                    if(timeMatrix[i][j]<=Constant.CLICK_INTERVAL){
                                        //Log.e("outclick","detected!!");
                                        Log.e("outclick","start zoom");
                                        //map.animateCamera(CameraUpdateFactory.zoomTo(5));
                                        Runnable runnable = new Runnable() {
                                            @Override
                                            public void run() {
                                                MapsActivity.mMap.animateCamera(CameraUpdateFactory.zoomTo(5));
                                            }
                                        };
                                        handler.post(runnable);
                                        Log.e("outclick","finish zoom");
                                    }
                                }
                            }
                        }
                    }
                    updateTouchStatus(currentPoint);
                }
                catch(IOException e){
                    Log.e(MainActivity.TAG,"update capacity failed.");
                }
            }
        },0, Constant.FLUSH_RATE);

        return true;
    }
    public boolean startDetectOutSlide(){
        detectOutSlide();
        return true;
    }


    public void test(ArrayList<MyRect> rects){
        try {
            int status[][] = getCurrentStatusImage();
            outTouchPointSet.clear();
            for(int i = 0;i<Constant.ROW_NUM;i++){
                for(int j = 0;j<Constant.COL_NUM;j++){
                    if(status[i][j]==1){
                        outTouchPointSet.add(new int[]{i,j,newCapa[i][j]});
                    }
                }
            }
            //Log.e("outclick",Integer.valueOf(outTouchPointSet.size()).toString());
            for(int i = 0;i<outTouchPointSet.size();i++){
                int rowId = outTouchPointSet.get(i)[0];
                int colId = outTouchPointSet.get(i)[1];
                int value = outTouchPointSet.get(i)[2];
                int x = Constant.CAPA_POS[rowId][colId][0];
                int y = Constant.CAPA_POS[rowId][colId][1];
                int dx = Constant.PIXEL_WIDTH;
                int dy = Constant.PIXEL_HEIGHT;
                rects.add(new MyRect(x,y,x+dx,y+dy,value,0));
            }
//            Log.e("outclick",Integer.valueOf(rects.size()).toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public void test2(ArrayList<MyRect> rects){
//        try{
//            int a = getCurrentStatusImage();
//            for(int i = 0;i<Constant.ROW_NUM;i++){
//                for(int j = 0;j<Constant.COL_NUM;j++){
//                    int x = Constant.CAPA_POS[i][j][0];
//                    int y = Constant.CAPA_POS[i][j][1];
//                    int dx = Constant.PIXEL_WIDTH;
//                    int dy = Constant.PIXEL_HEIGHT;
//                    int value = capaData[i][j];
//                    rects.add(new MyRect(x,y,x+dx,y+dy,value,1));
//                }
//            }
//        }
//        catch(Exception e){
//            String message = e.getMessage();
//            Log.e(Constant.TAG,"pause.....");
//        }
//    }
}

