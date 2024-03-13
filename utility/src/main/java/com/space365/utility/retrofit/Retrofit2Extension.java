package com.space365.utility.retrofit;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.Objects;

import retrofit2.Response;

public abstract class Retrofit2Extension {
    public static String parseErrorFromThrowable( Throwable throwable ){
        String retValue = "";
        if( throwable instanceof retrofit2.HttpException ) {
            retrofit2.HttpException exception = (retrofit2.HttpException) throwable;
            try {
                Logger.w( "Http error with stateCode:%d , response:%s" , exception.code() ,  Objects.requireNonNull(exception.response()).raw().toString());
                if( exception.code() == 404 ){
                    retValue = "404 stateCode";
                }
                else {
                    Response<?> response = exception.response();
                    if (response != null) {
                        assert response.errorBody() != null;
                        retValue = response.errorBody().string();
                    }
                }
            } catch (IOException e) {
                Logger.e( "parseErrorFromThrowable error",e );
            }
        }
        return retValue;
    }
}
