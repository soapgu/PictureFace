package com.space365.utility.adpter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;

public class ImageViewAdapters {

    @BindingAdapter(value={"http", "errorResourceId", "options"}, requireAll=false)
    public static void setImageWebSource(ImageView imageView , String httpUrl , int errorResourceId , RequestOptions options ){
        RequestBuilder<Drawable> requestBuilder = Glide.with(imageView.getContext())
                .load(httpUrl);
        if(errorResourceId > 0)
            requestBuilder = requestBuilder.error( errorResourceId );
        if( options != null )
            requestBuilder = requestBuilder.apply( options );

        requestBuilder.into(imageView);
    }

    @BindingAdapter("android:src")
    public static void setImageResource(ImageView imageView, int resource){
        imageView.setImageResource(resource);
    }

    @BindingAdapter("android:src")
    public static void setImageBitmap(ImageView imageView, Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }
}
