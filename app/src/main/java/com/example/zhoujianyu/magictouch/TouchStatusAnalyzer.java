package com.example.zhoujianyu.magictouch;

import java.util.ArrayList;

/**
 * Created by ZhouJianyu on 2017/11/19.
 */

public class TouchStatusAnalyzer {
    public static final int rowNum = 16;
    public static final int colNum = 28;
    public ArrayList<int[]> refineTouchPosition(int[][] lastCapaData,int[][] curCapaData){
        /**
         * input: lastCapaData->上一时刻保存的电容image(16*28)， curCapaData->当前时刻获取的电容image(16*28)
         * output: out-touch点集，其中每个点代表一个out of touch的中心,每一个点用一个size=2的数组表示，int[0]=rowId,int[1]=colId
         */
    }

}
