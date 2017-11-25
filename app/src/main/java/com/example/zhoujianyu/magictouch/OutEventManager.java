package com.example.zhoujianyu.magictouch;

import android.util.Log;

import com.android.volley.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
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
    int lastStatus = 0;
    int capaData[][] = new int[Constant.ROW_NUM][Constant.COL_NUM];

    TouchStatusAnalyzer touchStatusAnalyzer = new TouchStatusAnalyzer();
    ArrayList<ArrayList<int[]>>onTouchStatus = new ArrayList<>();  //size=2, 保存当前和上一时刻的touch points
    Timer timer = new Timer();
    OutClickListener outClickListener;

    public void updateStatus(ArrayList<ArrayList<int[]>> ss, ArrayList<int[]>s){
        /**
        input:
            ss: size 应为2，只保留当前和上一时刻的outTouch点的集合
            例如，当前status的outTouch点集为[{1,2},{2,3}]，表示在{1，2}和{2，3}这两个位置各有一个outTouch
            s: 当前outTouch点集
         */
        if(!ss.isEmpty()){
            ss.remove(0);
            ss.add(s);
        }
        //Log.e(MainActivity.TAG,Integer.valueOf(ss.size()).toString());

    }

    public void initializeCapa(){
        for(int i = 0;i<capaData.length;i++){
            for(int j = 0;j<capaData[i].length;j++){
                capaData[i][j] = 0;
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
        initializeCapa();
        initializeStatus();
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

    public void updateCapa(int[][]newCapa){
        assert(newCapa.length==capaData.length);
        for(int i = 0;i<capaData.length;i++){
            assert(newCapa[i].length==capaData.length);
            for(int j = 0;j<capaData[i].length;j++){
                capaData[i][j] = newCapa[i][j];
            }
        }
    }

    public int getCurrentStatusImage()throws IOException{
        int[][] newCapa = captureCapa();
        ArrayList<int[]>outTouchPointSet = new ArrayList<int[]>();
        int currentStatus = touchStatusAnalyzer.refineTouchPosition(newCapa,outTouchPointSet);  //当前outTouch 点集
        updateStatus(onTouchStatus,outTouchPointSet);
        updateCapa(newCapa);
        outTouchPointSet.clear();
        return currentStatus;
    }

    private boolean detectOutClick(){
        if(!detectingOutClick){
            detectingOutClick = true;
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try{
                        int currentStatus = getCurrentStatusImage();
                        //Log.e(Constant.TAG,Integer.valueOf(currentStatus).toString());
                        if(lastStatus==0&&currentStatus==0){ //00
                            //nothing
                        }
                        else if(lastStatus==1&&currentStatus==0){//10
                            //
                            Log.e(Constant.TAG,"down");
                        }
                        else if(lastStatus==0&&currentStatus==1){//01
                            Log.e(Constant.TAG,"up");
                        }
                        else{//11
                            Log.e(Constant.TAG,"maintain");
                        }
                        lastStatus = currentStatus;
                    }
                    catch(IOException e){
                        Log.e(MainActivity.TAG,"update capacity failed.");
                    }
                }
            },0, Constant.FLUSH_RATE);
        }
        return true;
    }
    private boolean detectOutSlide(){

        return true;
    }
    public boolean startDetectAll(){

        return true;
    }
    public boolean startDetectOutClick(){
        detectOutClick();
        return true;
    }
    public boolean startDetectOutSlide(){

        return true;
    }

    public void setOnOutClickListener(OutClickListener listener){
        this.outClickListener = listener;
    }

}
