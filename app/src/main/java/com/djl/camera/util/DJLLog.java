package com.djl.camera.util;

import android.util.Log;

/**
 * Created by DJl on 2016/8/19.
 * email:1554068430@qq.com
 */

public class DJLLog {

    private static final String TAG = "DJL_LOG";

    public static void i(Object o) {
        String message = toMessage(o);
        Log.i(TAG, message);
    }

    public static void e(Object o) {
        String message = toMessage(o);
        Log.e(TAG, message);
    }

    public static void d(Object o) {
        String message = toMessage(o);
        Log.d(TAG, message);
    }

    public static void w(Object o) {
        String message = toMessage(o);
        Log.w(TAG, message);
    }

    private static String toMessage(Object o) {
        return o == null ? "null to log" : o.toString();
    }
}
