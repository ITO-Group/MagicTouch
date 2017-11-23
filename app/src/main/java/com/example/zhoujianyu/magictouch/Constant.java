package com.example.zhoujianyu.magictouch;

/**
 * Created by ZhouJianyu on 2017/11/20.
 */

public interface Constant {

    //debug message
    String TAG = "myData";

    //时间
    int FLUSH_RATE = 10;
    int CLICK_INTERVAL = 50;

    //电容相关
    int ROW_NUM = 16;  //capa image row size
    int COL_NUM = 28;  //capa image col size

    int OUT_TOUCH_THRESHOLD = 200;
    int IN_TOUCH_THRESHOLD = 1200;

    int IN_TOUCH = 0;
    int OUT_TOUCH = 1;


}
