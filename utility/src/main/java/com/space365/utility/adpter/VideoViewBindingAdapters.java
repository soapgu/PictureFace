package com.space365.utility.adpter;

import android.widget.VideoView;

import androidx.databinding.BindingAdapter;

/*
@BindingMethods({
        @BindingMethod(type = VideoView.class, attribute = "android:onPrepared", method = "setOnPreparedListener")
})
 */
public class VideoViewBindingAdapters {
    @BindingAdapter("android:path")
    public static void setVideoViewPath(VideoView videoView , String path ){
        videoView.setVideoPath(path);
        videoView.setOnPreparedListener(mediaPlayer -> mediaPlayer.setLooping(true));
        videoView.start();
    }
}
