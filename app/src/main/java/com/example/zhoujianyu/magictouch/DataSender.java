package com.example.zhoujianyu.magictouch;


import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ZhouJianyu on 2017/11/5.
 */
public class DataSender {
    private String url;
    private RequestQueue queue;
    private Response.Listener<String> listener;
    private Response.ErrorListener errorListener;
    private HashMap<String,String> data;
    public DataSender(String url,RequestQueue queue){
        this.url = url;
        this.queue = queue;
        listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

            }
        };
        errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        };
    }
    public void getData(int[][]data,int rowNum,int colNum){
        List<String> list = new ArrayList<String>();
        for(int i = 0;i<rowNum;i++){
            for(int j = 0;j<colNum;j++){
                list.add(new Integer(data[i][j]).toString());
            }
        }
        if(this.data==null){
            this.data = new HashMap<String,String>();
        }
//        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
//        Date date = new Date(System.currentTimeMillis());
//        String ct = formatter.format(date);
        String capa = TextUtils.join(",",list);
        this.data.put("pos","0");
        this.data.put("data",capa);
    }

    public void sendData(int method,final String suffix){
        StringRequest request = new StringRequest(method,this.url+suffix,listener,errorListener){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                if(suffix.equals("")){
                    return data;
                }
                else{
                    HashMap done = new HashMap<String,String>();
                    done.put("done","0");
                    return done;
                }
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(1000, 0, 1.0f));
        queue.add(request);
    }
}
