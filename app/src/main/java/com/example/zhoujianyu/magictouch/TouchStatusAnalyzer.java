package com.example.zhoujianyu.magictouch;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by ZhouJianyu on 2017/11/19.
 */

public class TouchStatusAnalyzer {
    private static final int rowNum = Constant.ROW_NUM;
    private static final int colNum = Constant.COL_NUM;
    private static final int inTouchThreshold = Constant.IN_TOUCH_THRESHOLD;
    private static final int outTouchThreshold = Constant.OUT_TOUCH_THRESHOLD;
    private int capacityHistoryData[][] = new int[rowNum][colNum];
    private int touchPoint[][] = new int[rowNum][colNum];
//    private int checkPoint[][] = new int[rowNum][colNum];
    private int arounds[][] = {{0,-1},{0,1},{-1,0},{1,0},{-1,-1},{-1,-1},{-1,1},{1,1}};

    public TouchStatusAnalyzer() {
        for(int i = 0;i<rowNum;i++){
            Arrays.fill(touchPoint[i],0);
        }
    }


    public int[][] refineTouchPosition(int[][] capacityData){
        /**
         * input: CapacityData->当前时刻获取的电容image(16*28)
         * output:
         * int currentStatus: 0->无out-of-touch,1->out-of-touch,2->touch on
         */
        for(int i = 0;i<rowNum;i++){
            Arrays.fill(touchPoint[i],0);
        }
        //找到所有touch out points
        for(int i = 0;i<capacityData.length;i++){
            for(int j = 0;j<capacityData[i].length;j++){
                if(capacityData[i][j]>Constant.OUT_TOUCH_THRESHOLD){
                    boolean isCenter = true;
                    boolean redNeighbor = false;
                    boolean isMaxPoint = true;
                    for(int p = 0;p<arounds.length;p++){
                        try{
                            if(capacityData[i+arounds[p][0]][j+arounds[p][1]]<Constant.OUT_TOUCH_THRESHOLD){isCenter = false;}
                            if(capacityData[i+arounds[p][0]][j+arounds[p][1]]>Constant.IN_TOUCH_THRESHOLD){redNeighbor = true;}
                            if(capacityData[i+arounds[p][0]][j+arounds[p][1]]>capacityData[i][j]){isMaxPoint = false;}
                        }catch(Exception e){

                        }
                    }
                    if(isCenter){
                        if(capacityData[i][j]>Constant.IN_TOUCH_THRESHOLD){touchPoint[i][j] = 2;}
                        else if(capacityData[i][j]<Constant.OUT_TOUCH_MAXIMUM){
                            //检测周围是否有red point
                            if(redNeighbor){touchPoint[i][j]=2;}
                            else{
                                // 针对touch out进行单点选择，每个指头的touch out必须只对应一个像素点
                                if(isMaxPoint){
                                    touchPoint[i][j]=1;
                                }
                            }

                        }
                    }
                }
            }
        }
        return this.touchPoint;
    }
}
