package com.space365.utility.ui;

import android.app.Activity;
import android.app.Dialog;
import android.view.WindowManager;

/**
 * 模态框相关方法
 */
public abstract class DialogFns {

    /**
     * 沉浸式显示模态框
     * @param dialog 模态框
     * @param context 当前调用端Activity
     */
    public static void showImmersive(Dialog dialog, Activity context){
        //Here's the magic..
        //Set the dialog to not focusable (makes navigation ignore us adding the window)
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        //Set the dialog to immersive
        dialog.getWindow().getDecorView().setSystemUiVisibility(
                context.getWindow().getDecorView().getSystemUiVisibility());

        //Show the dialog! (Hopefully no soft navigation...)
        dialog.show();

        //Clear the not focusable flag from the window
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
}
