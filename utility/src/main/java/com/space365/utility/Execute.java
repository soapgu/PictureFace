package com.space365.utility;

import android.os.Handler;
import android.os.Looper;

import androidx.core.os.HandlerCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Execute {
    private static volatile Execute instance;
    private final Handler mainThreadHandler;
    private final ExecutorService executor;

    private Execute()
    {
        this.mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static Execute getInstance()
    {
        if( instance == null ) {
            synchronized ( Execute.class ) {
                if( instance == null ){
                    instance = new Execute();
                }
            }
        }
        return instance;
    }

    /**
     * Post Delegate to UI Thread
     * @param r 相关代理
     */
    public void BeginOnUIThread(Runnable r)
    {
        this.mainThreadHandler.post(r);
    }

    /**
     * Excute on sub thread
     * @param r 相关代理
     */
    public void BeginOnSubThread( Runnable r ){
        this.executor.execute( r );
    }
}
