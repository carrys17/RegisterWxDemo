package com.example.admin.registerwxdemo;

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by admin on 2017/12/22.
 */

public class Utils {
    /**
     * 从源字符串src中获取符合reg规则的子字符串，并且返回，如果src字符串中有多个子字符串符合reg规则，则返回第一个符合的子字符串
     *
     * @param src
     *            源字符串
     * @param reg
     *            正则表达式
     * @return
     */
    public static String reg(String src, String reg) {
        Pattern p = Pattern.compile(reg);// 在这里，编译成一个正则
        Matcher m = p.matcher(src);// 获得匹配
        String res = "";
        while (m.find()) { // 注意这里，是while不是if
            res = m.group();
            break;
        }
        return res;
    }


    /**
     * 判断data字符串是不是JSON格式
     *
     * @param data
     * @return
     */
    public static boolean isJSONString(String data) {
        try {
            new JSONObject(data);
            return true;
        } catch (Exception e) {
            // TODO: handle exception
        }
        return false;
    }
}
