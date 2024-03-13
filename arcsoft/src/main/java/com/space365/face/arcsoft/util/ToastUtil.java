package com.space365.face.arcsoft.util;

import android.content.Context;
import android.widget.Toast;

import com.space365.utility.Execute;

public class ToastUtil {
    public static void showToast(Context context, String message) {
        Execute.getInstance().BeginOnUIThread(() -> {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            toast.show();
        });
    }

    public static void showLongToast(Context context, String message) {
        Execute.getInstance().BeginOnUIThread(() -> {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            toast.show();
        });
    }
}
