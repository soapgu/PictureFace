package com.space365.utility.adpter;

import android.view.View;
import android.widget.TextView;


import androidx.databinding.BindingAdapter;

public class ShareBindingAdapters {
    @BindingAdapter("onLongClick")
    public static void setOnLongClickListener(View view, View.OnLongClickListener listener) {
        view.setOnLongClickListener(listener);
    }

    @BindingAdapter("onClick")
    public static void setOnClickListener(View view, View.OnClickListener listener) {
        view.setOnClickListener(listener);
    }

    @BindingAdapter("textStyle")
    public static void setTextStyle(TextView textView, int style){
        textView.setTypeface(null,style);
    }
}
