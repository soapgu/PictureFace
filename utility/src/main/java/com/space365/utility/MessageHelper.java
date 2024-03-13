package com.space365.utility;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MessageHelper {
    /*
    public static void ShowToast(Context context, String message) {
        Execute.getInstance().BeginOnUIThread(() -> {
            Toast toast = new Toast(context);
            View view = View.inflate(context, R.layout.toast_view, null);
            TextView text = view.findViewById(R.id.text);
            text.setText(message);
            toast.setView(view);
            toast.setGravity(Gravity.CENTER, 0, 300);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
        });
    }
    */
    public static void ShowToast(Context context, String message) {
        Execute.getInstance().BeginOnUIThread(() -> {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            toast.show();
        });
    }
}