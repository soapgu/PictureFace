package com.space365.face.arcsoft;

import android.content.Context;
import android.os.Environment;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.orhanobut.logger.Logger;
import com.space365.face.arcsoft.core.IBootTask;
import com.space365.face.arcsoft.faceserver.FaceServer;
import com.space365.utility.os.OsFns;

import java.io.File;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FaceTask implements IBootTask {
    private final Context context;
    private boolean complete;

    public FaceTask(Context context) {
        this.context = context;
    }

    /**
     * 是否已经完成初始化
     * @return
     */
    public boolean isComplete(){
        return complete;
    }

    /**
     * 实现IBootTask执行启动函数
     *
     * @return Single bool 是否成功
     */
    @Override
    public Single<Boolean> run() {
        return Single.<Boolean>create( emitter -> {
            if( !OsFns.isEmulator()) {
                String authFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + context.getString(R.string.active_file_name);
                if (!isActivated(context)) {
                    Logger.w("face is not Activated,activeOffline now");
                    activeOffline(context, authFilePath);
                }
                FaceServer.getInstance().init(context,size ->{
                    Logger.i("first init complete,count:%d",size);
                    complete = true;
                    emitter.onSuccess(true);
                }, ()-> emitter.onSuccess(false) );
            } else {
                emitter.onSuccess(true);
            }
        });
    }

    private boolean isActivated(Context context) {
        return FaceEngine.getActiveFileInfo(context, new ActiveFileInfo()) == ErrorInfo.MOK;
    }

    public void activeOffline(Context context, String path) {
        File file = new File(path);
        if( file.exists() ){
            FaceEngine.activeOffline(context, path);
        } else {
            Logger.e("active path: %s is not exists!",path);
        }

    }
}
