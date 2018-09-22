package com.example.zhoujianyu.magictouch;

/**
 * Created by ZhouJianyu on 2017/11/20.
 */

public class Constant {

    //debug message
    public static final String TAG = "myData";

    //时间
    public static final int FLUSH_RATE = 100;
    public static final int CLICK_INTERVAL = 1000;
    public static final int POST_RATE = 1000;
    public static final int SLIDE_MAX_TIME = 500;
    public static final int CLICK_MIN_TIME = 1500;


    //电容相关
    public static final int ROW_NUM = 16;  //capa image row size
    public static final int COL_NUM = 28;  //capa image col size

    public static final int OUT_TOUCH_THRESHOLD = 200;
    public static final int OUT_TOUCH_MAXIMUM = 1400;
    public static final int IN_TOUCH_THRESHOLD = 1900;

    public static int PIXEL_WIDTH;
    public static int PIXEL_HEIGHT;

    public static int [][][]CAPA_POS=new int[ROW_NUM][COL_NUM][2];

    public static final int IN_TOUCH = 0;
    public static final int OUT_TOUCH = 1;

    int NO_TOUCH = 0;
    int TOUCH_OUT = 1;
    int TOUCH_ON = 2;
    public enum TOUCH_TYPE{NO_TOUCH,TOUCH_OUT,TOUCH_ON}

}
