package com.example.admin.registerwxdemo;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by admin on 2017/12/25.
 */

public class MyService extends AccessibilityService {

    private static final String TAG = "MyService";
    private static Context sInstance = null;
    public static Context getContext(){
        return sInstance;
    }


    public static AtomicInteger cnt = new AtomicInteger(0);
//    public static int cnt = 0;
    public static Object gs_lockObj=new Object();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        // 界面是否跳转，利用全局变量
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            Log.i(TAG, "onAccessibilityEvent: 界面变化了TYPE_WINDOW_STATE_CHANGED");
            cnt.getAndIncrement();

        }
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "onServiceConnected: 服务开启");
        // 获取到this必须在这个方法里面
        if (sInstance == null){
            sInstance = this;
        }

//        do {
//            startWechat();
//            sleepSecondRandom(20,30);
//            // 如果有返回，先按返回
//            while (hasReturn()){
//                finishAndReturn();
//                SystemClock.sleep(300);
//            }
//        }while (!startFinish());
//
//
//        if (startFinish()){
//
//            Intent i = new Intent(this,MainActivity.class);
//            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//            String start = "service_start";
//            Bundle bundle = new Bundle();
//            bundle.putString("key",start);
//            i.putExtras(bundle);
//            startActivity(i);
//        }




    }



    // 睡眠秒数。区间段
    private void sleepSecondRandom(long startSecond,long endSecond) {
        double ran = Math.random();
        long interval = endSecond - startSecond;
        long time = (long) (startSecond  * 1000 + ran * 1000 * interval );
        SystemClock.sleep(time);
    }


    AccessibilityNodeInfo returnInfo;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean hasReturn() {
        Log.i("xyz","开始查找返回键");
        int i = 0;
        do {
            // 找到左上角的返回键
            AccessibilityNodeInfo root = getRoot();
            returnInfo = findReturn(root);
            SystemClock.sleep(200);
            i++;
        }while (i<5);

        if (returnInfo == null){
            Log.i("xyz","找到的返回为null");
            return false;
        }else {
            Log.i("xyz","找到的返回不为null");
            return true;
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void finishAndReturn(){

        Log.i("xyz","开始查找返回键");
        do {
            // 找到左上角的返回键
            AccessibilityNodeInfo root = getRoot();
            returnInfo = findReturn(root);
            SystemClock.sleep(200);
        }while (returnInfo == null);


        if (returnInfo == null){
            Log.i("xyz","找到的返回为null");
        }else {
            Log.i("xyz","找到的返回不为null");
            while (returnInfo!=null && !returnInfo.isClickable()) {
                returnInfo = returnInfo.getParent();
            }
            // 点击返回
            returnInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private AccessibilityNodeInfo findReturn(AccessibilityNodeInfo root) {
        if (root == null){
            return null;
        }
        AccessibilityNodeInfo res = null;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = root.getChild(i);
            if (nodeInfo != null&&nodeInfo.getClassName().equals("android.widget.ImageView") ) {
                Log.i("fanhui","获取到ImageView");
                Log.i("fanhui","nodeInfo = "+nodeInfo);
                Rect rect = new Rect();
                nodeInfo.getBoundsInScreen(rect);
                int x = rect.centerX();
                int y = rect.centerY();
                Log.i("fanhui","x = "+ x+ " y = "+y);
                if (5 < x && x < 35 && 13 < y && y < 43) {
                    res =  nodeInfo;
                    Log.i("fanhui","找到返回键");
                    Log.i("fanhui","找到返回键的坐标 x = "+ x+ " y = "+y);
                    break; // 这里必须有这个break，表示找到返回键之后就会打破循环，将找到的值返回
                }
            }else {
                res = findReturn(nodeInfo);
                if (res != null){
                    return res;
                }
            }
        }
        return res;
    }

    private void startWechat() {
        Intent intent = new Intent();
        ComponentName cmp=new ComponentName("com.tencent.mm","com.tencent.mm.ui.LauncherUI");
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(cmp);
        startActivity(intent);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean startFinish()  {
        List<AccessibilityNodeInfo> list;
        long aa = System.currentTimeMillis();
        do {
            AccessibilityNodeInfo root = getRoot();
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 13000){
                Log.e("xyz","sss");
            }

            list = root.findAccessibilityNodeInfosByText("微信");
            SystemClock.sleep(500);
        }while (list == null || list.size() == 0);

        if (list.size() > 0){
            Log.i("xyz","微信启动完成");
            return true;
        }else {
            return false;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo getRoot() {
        AccessibilityNodeInfo root;
        do {
            root = getRootInActiveWindow();
            SystemClock.sleep(200);
        }while (root==null);
        root.refresh();
        return root;
    }

    @Override
    public void onInterrupt() {
        Log.i("xyz","服务中断");
    }
}
