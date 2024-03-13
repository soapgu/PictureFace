package com.space365.utility.adpter;

import android.annotation.SuppressLint;
import android.webkit.WebView;

import androidx.databinding.BindingAdapter;

public class WebViewBindingAdapter {
    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @BindingAdapter(value={"android:url","jsObject","jsName"},requireAll = false)
    public static void setWebViewUrl(WebView webView , String url, Object jsObject , String jsName ){
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl( url );
        if( jsObject != null && jsName != null )
        webView.addJavascriptInterface( jsObject , jsName );
    }
}
