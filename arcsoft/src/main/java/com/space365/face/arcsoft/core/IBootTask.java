package com.space365.face.arcsoft.core;

import io.reactivex.rxjava3.core.Single;

public interface IBootTask {
    Single<Boolean> run();
}
