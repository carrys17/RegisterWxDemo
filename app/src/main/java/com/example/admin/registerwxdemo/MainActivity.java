package com.example.admin.registerwxdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.admin.registerwxdemo.CommandUtils.execCommand;
import static com.example.admin.registerwxdemo.WechatServerHelper.getInstance;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";
    EditText mPassword, mChn_id; // 用户填入的password，chn_id
    TextView mHintView;          // 界面提示信息
    Button mOk;                   // ok按钮

    WechatServerHelper helper;

    public static final String WECHAT_PATH = "/data/data/com.tencent.mm/";

    public static final String WECHAT_USERNAME_PATH = "/data/data/com.tencent.mm/shared_prefs/com.tencent.mm_preferences.xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPassword = findViewById(R.id.password);
        mChn_id = findViewById(R.id.chn_id);
        mHintView = findViewById(R.id.hint);
        mOk = findViewById(R.id.ok);

        helper = getInstance();


        mOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 开启任务
                            doTask();
                        } catch (Exception e) {
                            e.printStackTrace();
                            String error = e.getMessage();// 将错误信息显示在主界面
//                                mHintView.setText("" + error);
                        }
                    }
                }).start();


            }
        });

    }

    //  （1）停止微信app
    //	（2）连接内网wifi，将原来为4G的网络变为连接wifi
    //	（3）将数据上传到接口 http://192.168.0.213:8000/wechat/userInfoForOld （上传失败话，重试3次，还是失败的话 提示，并停止）
    //	（4）清理微信数据
    //	（5）断开wifi，开启飞行模式
    //	（6）关闭飞行模式，连回4G
    //   (7) 随机应用变量，保存
    //	（8）显示完成
    private void doTask() throws MyRuntimeException, IOException, DocumentException {

        final String password = mPassword.getText().toString().trim();
        final String chn_id = mChn_id.getText().toString().trim();

        if (password.equals("") || password == null || chn_id.equals("") || chn_id == null) {
            throw new MyRuntimeException("password和chn_id不能为空");
        }


        // 1、停止微信app
        if (hasWechatApp()) {
            killWechat();
        } else {
            throw new MyRuntimeException("未安装微信，请先安装微信再重试");
        }

        // 2、连接内网wifi，将原来为4G的网络变为连接wifi
        toggleWiFi(this, true);

        // 3、上传数据
        uploadToService(password, chn_id);





    }

    private void uploadToService(String password, String chn_id) throws IOException, DocumentException, MyRuntimeException {

        String username = getLoginUsernameFromWxXml();
        Log.i(TAG, "uploadToService: username = " + username);

        // 登陆机器， 由手机名称 + 手机真实imei组成
        String loginMachine = getLoginMachine();

        // 读取应用变量信息
        String appenvInfo = getAppenvInfo();


//        helper.userInfoForOld(-1,username,password,loginMachine,0,appenvInfo,);
    }

    private String getAppenvInfo() throws IOException {
        /*从应用变量的XPOSED.xml文件中读取应用变量信息*/
        File xposedXml = new File("/data/data/com.sollyu.android.appenv.dev/shared_prefs/XPOSED.xml");
        String appenvInfo = FileUtils.readFileToString(xposedXml, "utf8");
        Log.i(TAG, "getAppenvInfo: "+appenvInfo);
        return appenvInfo;
    }

    private String getLoginMachine() throws MyRuntimeException {

        // 手机名称
        String phoneName = android.os.Build.MODEL;
        String imei = getIMEI();
        String res = phoneName +"_"+ imei;

        Log.i(TAG, "getLoginMachine: "+res);

        return res;
    }

    // 手机真实IMEI
    private String getIMEI() throws MyRuntimeException {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            throw new MyRuntimeException("Manifest.permission.READ_PHONE_STATE 未被允许授权");
        }
        String IMEI = tm.getDeviceId();
        return IMEI;
    }

    private String getLoginUsernameFromWxXml() throws FileNotFoundException, DocumentException {

        execCommand("chmod -R 777 " + WECHAT_PATH, true);

        File file = new File(WECHAT_USERNAME_PATH);
        String username = "";

        FileInputStream fis = new FileInputStream(file);
        // 利用dom4j里面的类
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(fis);
        Element root = document.getRootElement();
        List<Element> list = root.elements();

        for (Element element : list){
//            Log.i(TAG, "getLoginUsernameFromWxXml: "+element.attributeValue("name"));
            if ("login_user_name".equals(element.attributeValue("name"))){
//                username = element.attributeValue("value");
                // 现在是String的类型 <string name="login_user_name">xxxx</string>, 没有value字符的，所以不能用上面的attributeValue("value");
                //  <int name="_auth_uin" value="-1399579528" />
                username = element.getStringValue();
                Log.i(TAG,"currentUin = "+username);
                return username;
            }
        }

        return username;
    }


    // 控制wifi的开关
    private void toggleWiFi(Context context,boolean b) {
        WifiManager manager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        manager.setWifiEnabled(b);
    }


    //  杀死微信
    private void killWechat() throws IOException {

//        Process exec = Runtime.getRuntime().exec("adb shell am force-stop com.tencent.mm");
//        Log.i(TAG, "killWechat: "+exec.toString());



        // 真机不能用adb shell的方式来执行命令。模拟器就可以
//        execCMD("am force-stop com.tencent.mm");
        // 上面这种就可以的，但是不够完善，采用这个比较完美，还有返回信息输出的。
        execCommand("am force-stop com.tencent.mm",true);




    }

//    private void execCMD(String paramString) {
//        try {
//            Process process = Runtime.getRuntime().exec("su");
//            Object object = process.getOutputStream();
//            DataOutputStream dos = new DataOutputStream((OutputStream) object);
//            String s = String.valueOf(paramString);
//            object = s +"\n";
//            dos.writeBytes((String) object);
//            dos.flush();
//            dos.writeBytes("exit\n");
//            dos.flush();
//            process.waitFor();
//            object = process.exitValue();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    // 是否有微信
    private boolean hasWechatApp() {
        PackageManager pkgManager = getPackageManager();
        List<PackageInfo> packageInfoList = pkgManager.getInstalledPackages(0);
        if (packageInfoList!=null){
            for (int i = 0; i < packageInfoList.size(); i++) {
                if ((packageInfoList.get(i).packageName).equals("com.tencent.mm")){
                    Log.i(TAG, "hasWechatApp: 已安装微信");
                    return true;
                }
            }
        }
        Log.i(TAG, "hasWechatApp: 微信未安装");
        return false;
    }

}
