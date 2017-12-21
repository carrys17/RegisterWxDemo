package com.example.admin.registerwxdemo;

/**
 * Created by admin on 2017/12/21.
 */

public class MyRuntimeException extends Exception {

    private static final long serialVersionUID = 1L;


    public MyRuntimeException(String errorMsg){
        super(errorMsg);
    }
}
