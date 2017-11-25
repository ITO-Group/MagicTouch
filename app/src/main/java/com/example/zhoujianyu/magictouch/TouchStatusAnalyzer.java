package com.example.zhoujianyu.magictouch;

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
    private int floodMove[][] = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,-1}};

    public TouchStatusAnalyzer() {
        for(int i = 0;i<rowNum;i++){
            Arrays.fill(touchPoint[i],-1);
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
            return 1;
        if (a[i][j] < outTouchThreshold)
            return -1;
        return 0;
    }

    private void checkCapacityData(int a[][]) {
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < colNum; j++) {
                boolean top = true;
                if (checkType(a, i, j) == -1) {
                    continue;
                }
                for (int k = 0; k < floodMove.length; k++) {
                    int x = i + floodMove[k][0];
                    int y = j + floodMove[k][1];
                    if ((x >= 0 && x < rowNum) && (y >= 0 && y < colNum)) {
                        if (a[i][j] < a[x][y]) {
                            top = false;
                            break;
                        }
                    }
                }
                if (top) {
                    touchPoint[i][j] = checkType(a, i, j);
                }
            }
        }
    }

    private void generateList(ArrayList<int[]>outTouchPointSet) {
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < colNum; j++) {
                boolean hasZero = false;
                boolean hasOne = false;
                if (touchPoint[i][j] == -1) {
                    continue;
                }
                else if (checkPoint[i][j] == 0) {
                    hasZero = true;
                }
                else if (checkPoint[i][j] == 1) {
                    hasOne = true;
                }
                /*
                for (int x = i-1; x <= i+1; x++) {
                    if (x < 0 || x >= rowNum) {
                        continue;
                    }
                    for (int y = j-1; y <= j+1; y++) {
                        if (y >= 0 || y < colNum) {
                            if (checkPoint[x][y] == 0) {
                                hasZero = true;
                            }
                            if (checkPoint[x][y] == 1) {
                                hasOne = true;
                            }
                        }
                    }
                }
                */
                if (touchPoint[i][j] == 1 && hasZero) {
                    touchPoint[i][j] = 0;
                }
                else if (touchPoint[i][j] == 0 && hasOne) {
                    touchPoint[i][j] = 1;
                }
                if (touchPoint[i][j] == 0) {
                    outTouchPointSet.add(new int[]{i,j});
                }
            }
        }
    }

    public int refineTouchPosition(int[][] capacityData,ArrayList<int[]>outTouchPointSet){
        /**
         * input: CapacityData->当前时刻获取的电容image(16*28)
         * output:
         * outTouchPointSet: out-touch点集，其中每个点代表一个out of touch的中心,每一个点用一个size=2的数组表示，int[0]=rowId,int[1]=colId
         *                  传的是一个引用的空List，需要将结果写进去
         * int currentStatus: 0->无out-of-touch,1->out-of-touch
         */
        if (identicalMat(capacityData)) {
            return 0;
        }
        this.checkPoint = getCopyCapacityMat(this.touchPoint);
        checkCapacityData(capacityData);
        generateList(outTouchPointSet);
        this.capacityHistoryData = getCopyCapacityMat(capacityData);
        return (outTouchPointSet.size() > 0) ? 1 : 0;
    }

}
