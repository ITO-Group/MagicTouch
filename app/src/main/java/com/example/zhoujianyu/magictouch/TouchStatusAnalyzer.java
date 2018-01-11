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
    private int checkPoint[][] = new int[rowNum][colNum];
    private int floodMove[][] = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};

    public TouchStatusAnalyzer() {
        for(int i = 0;i<rowNum;i++){
            Arrays.fill(touchPoint[i],0);
        }
    }

    private int[][] getCopyCapacityMat(int a[][]){
        int result[][]= new int[rowNum][colNum];
        for (int i = 0; i < rowNum; i++){
            System.arraycopy(a[i], 0, result[i], 0, colNum);
        }
        return result;
    }

    private boolean identicalMat(int a[][]) {
        for (int i = 0; i < rowNum; i++) {
            if (!Arrays.equals(a[i], capacityHistoryData[i])) {
                return false;
            }
        }
        return true;
    }

    private int checkType(int a[][], int i, int j) {
        if (a[i][j] > inTouchThreshold)
            return 2;
        if (a[i][j] < outTouchThreshold)
            return 0;
        return 1;
    }

    private void checkCapacityData(int a[][]) {
        //检测出所有touch中心点
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < colNum; j++) {
                boolean top = true;
                if (checkType(a, i, j) == 0) {
                    touchPoint[i][j] = 0;//filt掉所有噪声
                    continue;
                }
                for (int k = 0; k < floodMove.length; k++) {
                    int x = i + floodMove[k][0];
                    int y = j + floodMove[k][1];
                    if ((x >= 0 && x < rowNum) && (y >= 0 && y < colNum)) {
                        if (a[i][j] < a[x][y]) {
                            top = false;
                            touchPoint[i][j]=0; //filt掉所有非peak point
                            break;
                        }
                    }
                }
                if (top) {
                    touchPoint[i][j] = checkType(a, i, j);//peak point 只有两种：黄-》1，红-》2,噪声是0
                    if(touchPoint[i][j]==1){
                    }
                }
            }
        }
    }

    private void generateList(int[][]currentCapaData) {
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < colNum; j++) {
                boolean yellowed = false;
                boolean reded = false;
                if (touchPoint[i][j] == 0) {
                    continue;
                }
                else if (checkPoint[i][j] == 1) {
                    yellowed = true;
                }
                else if (checkPoint[i][j] == 2) {
                    reded = true;
                }
                if (touchPoint[i][j] == 2 && yellowed) {

                    touchPoint[i][j] = 1;//上一刻是黄，无论再怎么按压都是黄
                }
                else if (touchPoint[i][j] == 1 && reded) {
                   // Log.e("outclick","got it!!!!!!!!!!!!!");

                    touchPoint[i][j] = 2;//上一刻是红，再轻都是红
                }
            }
        }
    }

    public int[][] refineTouchPosition(int[][] capacityData){
        /**
         * input: CapacityData->当前时刻获取的电容image(16*28)
         * output:
         * int currentStatus: 0->无out-of-touch,1->out-of-touch
         */
        if (identicalMat(capacityData)) {
            return this.touchPoint;
        }
        this.checkPoint = getCopyCapacityMat(this.touchPoint); //获取上一次touchPoint状态的copy
        checkCapacityData(capacityData);//初步更新touchPoint, 给所有找到所有touch中心点
        generateList(capacityData);//此时touchPoint已更完
        this.capacityHistoryData = getCopyCapacityMat(capacityData);//用来下次调用与新的capacity做比较看是否完全相同
        return this.touchPoint;
    }
}
