package com.example.admin.registerwxdemo.Utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import static com.example.admin.registerwxdemo.Utils.CommandUtils.execCommand;

/**
 * Created by admin on 2017/12/28.
 */

// 微信辅助类
public class WechatUtils {


    private static final String TAG = "WechatUtils";

    // 是否有微信
    public static boolean hasWechatApp(Context context) {
        PackageManager pkgManager = context.getPackageManager();
        List<PackageInfo> packageInfoList = pkgManager.getInstalledPackages(0);
        if (packageInfoList!=null){
            for (int i = 0; i < packageInfoList.size(); i++) {
                if ((packageInfoList.get(i).packageName).equals("com.tencent.mm")){

                    PackageInfo info = packageInfoList.get(i);

//                    wxVersion = info.versionName;
                    Log.i(TAG, "hasWechatApp: 已安装微信 ");
                    return true;
                }
            }
        }
        Log.i(TAG, "hasWechatApp: 微信未安装");
        return false;
    }



    //  杀死微信
    public static void killWechatByCommand(boolean isRoot) throws IOException {

//        Process exec = Runtime.getRuntime().exec("adb shell am force-stop com.tencent.mm");
//        Log.i(TAG, "killWechat: "+exec.toString());


        Runtime.getRuntime().exec("su");

        // 真机不能用adb shell的方式来执行命令。模拟器就可以
//        execCMD("am force-stop com.tencent.mm");
        // 上面这种就可以的，但是不够完善，采用这个比较完美，还有返回信息输出的。
        execCommand("am force-stop com.tencent.mm",isRoot);

    }


    /**
     * 通过 Intent的方式打开微信
     *
     * @param context 上下文
     */
    public static void openWechat(Context context) {
        Intent intent = new Intent();
        ComponentName cmp=new ComponentName("com.tencent.mm","com.tencent.mm.ui.LauncherUI");
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(cmp);
        context.startActivity(intent);
    }

    /**
     *  通过命令行的方式打开微信
     * @param isRoot 手机是否root
     */
    public static void openWechatByCommand(boolean isRoot){
        execCommand("am start -n com.tencent.mm/com.tencent.mm.ui.LauncherUI",isRoot);
    }


    /**
     *  每次都打开一个新的wechat，也就是SingleTask模式
     * @param isRoot
     */
    public static void openNewWechatByCommand(boolean isRoot){
        execCommand("am start --activity-clear-top com.tencent.mm/com.tencent.mm.ui.LauncherUI",isRoot);
    }

    /**
     * 清理微信数据
     * @param isRoot 手机是否root
     * @throws IOException
     */
    public static void clearWxData(boolean isRoot) throws IOException {
        execCommand("pm clear com.tencent.mm",isRoot);
    }

}
