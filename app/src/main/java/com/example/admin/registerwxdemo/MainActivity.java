package com.example.admin.registerwxdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.provider.Settings;
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
import org.apache.commons.lang3.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.internal.Util;

import static com.example.admin.registerwxdemo.CommandUtils.execCommand;
import static com.example.admin.registerwxdemo.WechatServerHelper.getInstance;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";
    EditText mPassword, mChn_id; // 用户填入的password，chn_id
    TextView mHintView;          // 界面提示信息
    Button mOk;                   // ok按钮

    WechatServerHelper helper;   // 上传接口的对象

    private static final String WECHAT_PATH = "/data/data/com.tencent.mm/";  // 微信路径

    // 微信com.tencent.mm_preferences的路径。为了拿login_user_name
    private static final String WECHAT_USERNAME_PATH = "/data/data/com.tencent.mm/shared_prefs/com.tencent.mm_preferences.xml";

    private static final String YYBL = "/data";

    String wxVersion; // 微信版本号

    private static final String OPENFLY = "settings put global airplane_mode_on 1;am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true";
    private static final String CLOSEFLY = "settings put global airplane_mode_on 0;am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPassword = findViewById(R.id.password);
        mChn_id = findViewById(R.id.chn_id);
        mHintView = findViewById(R.id.hint);
        mOk = findViewById(R.id.ok);

        helper = getInstance();


        loadApps();

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


    // 获取手机中所有的应用及其包名
    private void loadApps(){
        List<ResolveInfo> apps = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN,null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        apps = getPackageManager().queryIntentActivities(intent,0);
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo info = apps.get(i);
            String packageName = info.activityInfo.packageName;
            CharSequence cls = info.activityInfo.name;
            CharSequence name = info.activityInfo.loadLabel(getPackageManager());
            Log.i(TAG, "loadApps: " +name+"----"+packageName+"----"+cls);
        }

    }



    //  （1）停止微信app
    //	（2）连接内网wifi，将原来为4G的网络变为连接wifi
    //	（3）将数据上传到接口 http://192.168.0.213:8000/wechat/userInfoForOld （上传失败话，重试3次，还是失败的话 提示，并停止）
    //	（4）清理微信数据
    //	（5）断开wifi，开启飞行模式
    //	（6）关闭飞行模式，连回4G
    //   (7) 随机应用变量，保存
    //	（8）显示完成
    private void doTask() throws MyRuntimeException, IOException, DocumentException, PackageManager.NameNotFoundException, JSONException {

        final String password = mPassword.getText().toString().trim();
        final String chn_id = mChn_id.getText().toString().trim();
        int id = Integer.parseInt(chn_id);


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




//        String android  = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
//        Log.i(TAG, "doTask: android -----"+android);


        // 3、等待wifi开启成功后上传数据
        waitForOpenWifi(100000);
        if (isWiFiConnected()){
            Log.i(TAG, "doTask: 上传数据");
            uploadToServiceThreeTime(password,id);
        }


        // 4、清理微信数据
        clearWxData();

        // 5、断开wifi
        toggleWiFi(this,false);
        waitForCloseWifi(100000);
        if (!isWiFiConnected() && !isAirModeOn(this)){
            // 开启飞行模式
            execCommand(OPENFLY,true);
        }


        // 关闭飞行模式
        waitForOpenAirMode(100000);
        if (isAirModeOn(this)){
            execCommand(CLOSEFLY,true);
        }








    }

    private void waitForOpenAirMode(long overTime) throws MyRuntimeException {
        long before = System.currentTimeMillis();
        do {
            long after = System.currentTimeMillis();
            if (after - before >= overTime){
                throw new MyRuntimeException("等待开启飞行模式失败");
            }
            SystemClock.sleep(500);
        }while (!isAirModeOn(this));
    }


    private boolean isAirModeOn(Context context){
        return (Settings.System.getInt(context.getContentResolver(),Settings.System.AIRPLANE_MODE_ON,0) == 1 ?true : false);
    }



    private void uploadToServiceThreeTime(String password,int id) throws JSONException, PackageManager.NameNotFoundException, MyRuntimeException, DocumentException, IOException {
        JSONObject res = null;
        int i = 0;
        do {

            if (i>=3){
                throw new MyRuntimeException("重复上传数据3次失败");
            }
            res = uploadToService(password, id);
            SystemClock.sleep(500);
            i++;
        }while (!(res!=null && (res.getInt("result") ==1)));

    }

    private void waitForCloseWifi(long overTime) throws MyRuntimeException {
        long before = System.currentTimeMillis();
        do {
            long after = System.currentTimeMillis();
            if (after - before >= overTime){
                throw new MyRuntimeException("等待wifi开启超时");
            }
            SystemClock.sleep(500);
        }while (isWiFiConnected());
    }


    private void waitForOpenWifi(long overTime) throws MyRuntimeException {
        long before = System.currentTimeMillis();
        do {
            long after = System.currentTimeMillis();
            if (after - before >= overTime){
                throw new MyRuntimeException("等待wifi开启超时");
            }
            SystemClock.sleep(500);
        }while (!isWiFiConnected());
    }

    private boolean isWiFiConnected() {
        WifiManager wm = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null && wm.isWifiEnabled()) {
            Log.i(TAG, "Wifi网络已经开启");
            return true;
        }
        Log.i(TAG, "Wifi网络还未开启");
        return false;

    }

    // 清理微信数据
    private void clearWxData() throws IOException {
        execCommand("pm clear com.tencent.mm",true);
    }


    private JSONObject uploadToService(String password, int chn_id) throws IOException, DocumentException, MyRuntimeException, PackageManager.NameNotFoundException, JSONException {

        String username = getInfoFromXML(WECHAT_PATH,WECHAT_USERNAME_PATH,true,"login_user_name",true);
        Log.i(TAG, "uploadToService: username = " + username);

        // 登陆机器， 由手机名称 + "_"+ 手机真实imei组成
        String loginMachine = getLoginMachine();

        // 读取应用变量信息
        String appenvInfo = getAppenvInfo();

        // 当前软件版本
        String softVersion = null;
        PackageManager manager = this.getPackageManager();
        PackageInfo info = manager.getPackageInfo(this.getPackageName(),0);
        softVersion = info.versionName;
        Log.i(TAG, "uploadToService: softVersion: "+softVersion);



        // 62数据
        JSONObject wx62data = getWx62Data(appenvInfo);





        JSONObject res = helper.userInfoForOld(-1,username,password,loginMachine,0,appenvInfo, chn_id,0,softVersion,wxVersion,null,wx62data);

        return res;

    }


    // 获取62Json数据
    private JSONObject getWx62Data(String appenvInfo) throws IOException, JSONException {
        /*从微信文件CompatibleInfo.cfg中读取微信的62数据*/
        String compatibleInfoBase64 = getCompatibleInfoBase64();
        // 拼接成JSONObject数据
        JSONObject wx62data = createWx62dataJSON(appenvInfo,compatibleInfoBase64);
        return wx62data;
    }

    //  将应用变量信息跟加密后的62数据拼接成62Json数据
    private JSONObject createWx62dataJSON(String appenvInfo,String compatibleInfoBase64) throws JSONException {
        JSONObject wx62data = null;
        String unescapeInfo = StringEscapeUtils.unescapeXml(appenvInfo);
        String appenvJSONString = Utils.reg(unescapeInfo, "\\{.*\\}");
        if(Utils.isJSONString(appenvJSONString) == true) {
            JSONObject appenvJSON = new JSONObject(appenvJSONString);
            wx62data = new JSONObject();
            wx62data.put("buildManufacturer", appenvJSON.getString("buildManufacturer"));
            wx62data.put("buildModel", appenvJSON.getString("buildModel"));
            wx62data.put("buildSerial", appenvJSON.getString("buildSerial"));
            wx62data.put("phonenum", appenvJSON.getString("telephonyGetLine1Number"));
            wx62data.put("imei", appenvJSON.getString("telephonyGetDeviceId"));
            wx62data.put("SimSerialNumber", appenvJSON.getString("telephonyGetSimSerialNumber"));
            wx62data.put("androidID", appenvJSON.getString("settingsSecureAndroidId"));
            wx62data.put("CompatibleInfoBase64", compatibleInfoBase64);
        }

        Log.i(TAG, "createWx62dataJSON: wx62data ---"+wx62data.toString());
        return wx62data;

    }


    // 从微信文件CompatibleInfo.cfg中读取微信的62数据
    private String getCompatibleInfoBase64() throws IOException {
        String compatibleInfo = null;
        execCommand("chmod 777 /data/data/com.tencent.mm/MicroMsg",true); //执行chmod命令，确保能读文件
        execCommand("chmod 777 /data/data/com.tencent.mm/MicroMsg/CompatibleInfo.cfg",true); //执行chmod命令，确保能读文件
        compatibleInfo = encodeFromFile("/data/data/com.tencent.mm/MicroMsg/CompatibleInfo.cfg");
        return compatibleInfo;

    }

    // Base64加密62数据
    private String encodeFromFile(String path) throws IOException {
        String encodedString = null;
        File file = new File(path);
        if(file.exists() == true) {
            FileInputStream inputFile = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            inputFile.read(buffer);
            inputFile.close();
            encodedString = android.util.Base64.encodeToString(buffer, android.util.Base64.DEFAULT);
            Log.e(TAG, "encodeFromFile: Base64---->"+ encodedString);

        }
        return encodedString;
    }

    private String getAppenvInfo()  {
        /*从应用变量的XPOSED.xml文件中读取应用变量信息*/
        execCommand("chmod -R 777 /data/data/com.sollyu.android.appenv.dev/",true);
        File xposedXml = new File("/data/data/com.sollyu.android.appenv.dev/shared_prefs/XPOSED.xml");
        String appenvInfo = null;
        try {
            appenvInfo = FileUtils.readFileToString(xposedXml, "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

//    private String getLoginUsernameFromWxXml() throws FileNotFoundException, DocumentException {
//
//        execCommand("chmod -R 777 " + WECHAT_PATH, true);
//
//        File file = new File(WECHAT_USERNAME_PATH);
//        String username = "";
//
//        FileInputStream fis = new FileInputStream(file);
//        // 利用dom4j里面的类
//        SAXReader saxReader = new SAXReader();
//        Document document = saxReader.read(fis);
//        Element root = document.getRootElement();
//        List<Element> list = root.elements();
//
//        for (Element element : list){
////            Log.i(TAG, "getLoginUsernameFromWxXml: "+element.attributeValue("name"));
//            if ("login_user_name".equals(element.attributeValue("name"))){
////                username = element.attributeValue("value");
//                // 现在是String的类型 <string name="login_user_name">xxxx</string>, 没有value字符的，所以不能用上面的attributeValue("value");
//                //  <int name="_auth_uin" value="-1399579528" />
//                username = element.getStringValue();
//                Log.i(TAG,"currentUin = "+username);
//                return username;
//            }
//        }
//
//        return username;
//    }

    /**
     *  从xml文件中获取信息
     * @param fatherPath     文件父目录
     * @param path              文件目录
     * @param isStringType    获取的信息是否是String的类型<string name="login_user_name">xxxx</string>。
     *                      还是说是<int name="_auth_uin" value="-1399579528" /> 这种类型
     * @param nodeName        要获取的节点名称
     * @param isRoot          手机是否root
     * @return                 节点信息
     */
    private String getInfoFromXML(String fatherPath,String path,boolean isStringType,String nodeName,boolean isRoot) throws FileNotFoundException, DocumentException {

        execCommand("chmod -R 777 " + fatherPath, isRoot);

        File file = new File(path);
        String res = "";

        FileInputStream fis = new FileInputStream(file);
        // 利用dom4j里面的类
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(fis);
        Element root = document.getRootElement();
        List<Element> list = root.elements();

        for (Element element : list){
            if (nodeName.equals(element.attributeValue("name"))){

                if (isStringType){
                    res = element.getStringValue();
                }else {
                    res = element.attributeValue("value");
                }
                Log.i(TAG,"getInfoFromXML : res = "+res);
                return res;
            }
        }

        return res;


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


        Runtime.getRuntime().exec("su");

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

                    PackageInfo info = packageInfoList.get(i);
                    wxVersion = info.versionName;
                    Log.i(TAG, "hasWechatApp: 已安装微信 wxVersion: "+wxVersion);
                    return true;
                }
            }
        }
        Log.i(TAG, "hasWechatApp: 微信未安装");
        return false;
    }

}
