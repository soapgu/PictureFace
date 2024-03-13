package com.space365.utility.os;

import android.os.Build;

public abstract class OsFns {

    /***
     * 当前运行环境是否为模拟器
     * @return is Emulator
     */
    public static boolean isEmulator(){
        return Build.MODEL.contains("Android SDK built for x86") || Build.MODEL.contains("sdk_gphone_arm64");
    }
}
