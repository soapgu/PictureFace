package com.space365.utility.rx;

import android.annotation.SuppressLint;

import com.orhanobut.logger.Logger;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Predicate;

public class RxExtention {
    /**
     * 重试机制实现
     * @param errors 错误流
     * @param predicate 针对Throwable的筛选判断
     * @param maxTry 最大重试数
     * @param periodStrategy 间隔策略
     * @param timeUnit 时间单位
     * @return 返回给RetryWhen的Rx流
     */
    @SuppressLint("DefaultLocale")
    public static Flowable<?> retrySupport(Flowable<Throwable> errors, Predicate<? super Throwable> predicate , Integer maxTry , Function<Long, Long> periodStrategy , TimeUnit timeUnit ) {
        return retrySupport( errors , predicate , maxTry , periodStrategy , timeUnit , null );
    }

    /**
     * 重试机制实现
     * @param errors 错误流
     * @param predicate 针对Throwable的筛选判断
     * @param maxTry 最大重试数
     * @param periodStrategy 间隔策略
     * @param timeUnit 时间单位
     * @param reporter 错误报告
     * @return 返回给RetryWhen的Rx流
     */
    @SuppressLint("DefaultLocale")
    public static Flowable<?> retrySupport(Flowable<Throwable> errors, Predicate<? super Throwable> predicate , Integer maxTry , Function<Long, Long> periodStrategy , TimeUnit timeUnit , BiConsumer<Throwable,Long> reporter )
    {
        LongAdder errorCount = new LongAdder();
        return errors
                .doOnNext(e -> {
                    errorCount.increment();
                    long currentCount = errorCount.longValue();
                    boolean tryContinue = currentCount < maxTry && predicate.test(e) ;
                    Logger.i("No. of errors: %d , %s",  currentCount,
                            tryContinue ? String.format("please wait %d %s.", periodStrategy.apply(currentCount), timeUnit.name()) : "skip and throw");
                    if( reporter != null )
                        reporter.accept( e , currentCount );
                    if(!tryContinue)
                        throw  e;
                } )
                .flatMapSingle(e -> Single.timer( periodStrategy.apply(errorCount.longValue()), timeUnit));
    }

    /**
     * 是否运行重试
     * @param throwable 输入参数
     * @return 是否可重试
     */
    public static boolean allowRetry( Throwable throwable ){
        if( throwable instanceof retrofit2.HttpException ) {
            retrofit2.HttpException exception = (retrofit2.HttpException)throwable;
            int code = exception.code();
            Logger.w( "Http error with stateCode:%d , response:%s" , code ,  Objects.requireNonNull(exception.response()).raw().toString());
            if( code >= 400 && code < 500 )
                return false;
        }
        Logger.i( "error with exception:" + throwable.getMessage() );
        return true;
    }
}
