package com.example.admin.registerwxdemo.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.content.Context.*;

/**
 * Created by admin on 2017/12/29.
 */

// 网络相关操作助手
public class NetworkUtils {

//    <!--网络相关-->
//    <uses-permission android:name="android.permission.INTERNET"/>
//    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
//    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
//    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE"/>
//    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
    private static final String TAG = "NetworkUtils";


    // 判断当前是否有网络
    public static boolean isNetWorkConnected(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo!=null){
            return networkInfo.isAvailable();
        }else {
            return false;
        }
    }


    // 判断Mobile网络是否可用
    public static boolean isMobileConnected(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (networkInfo!=null){
            return networkInfo.isAvailable();
        }else {
            return false;
        }
    }

    // 控制Mobile网络的开关(通过反射setMobileDataEnabled方法来实现)
    public static void toggleMobile(Context context,boolean isOpen) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        Class ownerClass = manager.getClass();
        Class[] argsClass = new Class[1];
        argsClass[0] = boolean.class;
        Method method = ownerClass.getMethod("setMobileDataEnabled",argsClass);
        method.invoke(manager,isOpen);
    }

    // 控制wifi的开关
    public static void toggleWiFi(Context context, boolean isOpen) {
        WifiManager manager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        manager.setWifiEnabled(isOpen);
    }

    // 判断wifi是否开启
    public static boolean isWiFiConnected(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null && wm.isWifiEnabled()) {
            Log.i(TAG, "Wifi网络已经开启");
            return true;
        }
        Log.i(TAG, "Wifi网络还未开启");
        return false;
    }


    /**
     *  在规定的时间内等待WiFi关闭
     * @param context
     * @param overTime 超时时间
     */
    private void waitForCloseWifi(Context context,long overTime)  {
        long before = System.currentTimeMillis();
        do {
            long after = System.currentTimeMillis();
            if (after - before >= overTime){
                Log.e(TAG, "waitForCloseWifi: 等待WiFi关闭超时");
                break;
            }
            SystemClock.sleep(500);
        }while (isWiFiConnected(context));
    }


    /**
     *  在规定的时间内等待WiFi开启
     * @param context
     * @param overTime 超时时间
     */
    private void waitForOpenWifi(Context context,long overTime)  {
        long before = System.currentTimeMillis();
        do {
            long after = System.currentTimeMillis();
            if (after - before >= overTime){
                Log.e(TAG, "waitForOpenWifi: 等待WiFi开启超时");
                break;
            }
            SystemClock.sleep(500);
        }while (!isWiFiConnected(context));
    }


    /**
     * 判断是否开启飞行模式
     * @param context
     * @return
     */
    public static boolean isAirModeOn(Context context){
        return (Settings.System.getInt(context.getContentResolver(),Settings.System.AIRPLANE_MODE_ON,0) == 1 ?true : false);
    }


    /**
     * 在规定的时间内等待飞行模式的开启
     * @param context
     * @param overTime 超时时间
     */
    private void waitForOpenAirMode(Context context,long overTime)  {
        long before = System.currentTimeMillis();
        do {
            long after = System.currentTimeMillis();
            if (after - before >= overTime){
                Log.e(TAG, "waitForOpenAirMode: 等待飞行模式开启超时" );
                break;
            }
            SystemClock.sleep(500);
        }while (!isAirModeOn(context));
    }

}
