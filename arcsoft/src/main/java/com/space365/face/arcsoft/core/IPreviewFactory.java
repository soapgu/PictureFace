package com.space365.face.arcsoft.core;

import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;

public interface IPreviewFactory {
    /**
     * 创建相关内容
     *
     * @return 相关内容Fragment
     */
    Fragment create();

    Map<String, String> getFeatureDatas();

    void registerFromFile(String dir);

    Single<Boolean> registerFromFeatureFile(String dir);

    Single<Boolean> registerFromList(List<? extends IFaceEntity> list, boolean append);

    void cancel();

    void release();
}
