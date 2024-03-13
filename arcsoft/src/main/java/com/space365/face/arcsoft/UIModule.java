package com.space365.face.arcsoft;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Base64;

import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;
import com.space365.face.arcsoft.core.IFaceEntity;
import com.space365.face.arcsoft.core.IPreviewFactory;
import com.space365.face.arcsoft.facedb.FaceDatabase;
import com.space365.face.arcsoft.facedb.entity.FaceEntity;
import com.space365.face.arcsoft.faceserver.FaceServer;
import com.space365.face.arcsoft.ui.callback.BatchRegisterCallback;
import com.space365.face.arcsoft.ui.fragment.PreviewFragment;
import com.space365.face.arcsoft.util.FileUtil;
import com.space365.face.arcsoft.util.ToastUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class UIModule {

    public IPreviewFactory previewFactory(Context context) {
        return new IPreviewFactory() {
            private Disposable disposable;

            private static final String SUFFIX_JPEG = ".jpeg";
            private static final String SUFFIX_JPG = ".jpg";
            private static final String SUFFIX_PNG = ".png";
            private static final String SUFFIX_BMP = ".bmp";

            @Override
            public Fragment create() {
                return PreviewFragment.newInstance();
            }

            @Override
            public Map<String, String> getFeatureDatas() {
                Map<String, String> features = new HashMap<>();
                FaceDatabase.getInstance(context).faceDao().getAllFaces().forEach(each ->
                        features.put(each.getUserName(), Base64.encodeToString(each.getFeatureData(), Base64.NO_WRAP)));
                return features;
            }

            @Override
            public void registerFromFile(String dir) {
                registerFromFiles(dir);
            }

            @Override
            public Single<Boolean> registerFromFeatureFile(String dir) {
                return FaceServer.getInstance().registerFromFeature(dir);
            }

            @Override
            public Single<Boolean> registerFromList(List<? extends IFaceEntity> list, boolean append) {
                return FaceServer.getInstance().registerFromList(list,append);
            }

            @Override
            public void cancel() {
                stopRegisterIfDoing();
            }

            @Override
            public void release() {
                FaceServer.getInstance().release();
            }

            /**
             * 从文件夹注册
             */
            private void registerFromFiles(String dir) {
                registerFromFile(context, new File(dir), new BatchRegisterCallback() {

                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onProcess(int current, int failed, int total) {
                        ToastUtil.showLongToast(context, String.format("正在导入特征库。当前第%d个，共%d个，其中失败%d个。", current, total, failed));
                    }

                    @Override
                    public void onFinish(int current, int failed, int total, String errMsg) {
                        if (errMsg != null) {
                            ToastUtil.showLongToast(context, errMsg);
                        } else {
                            ToastUtil.showToast(context, "导入完成");
                        }
                    }
                });
            }

            @SuppressLint("CheckResult")
            private void registerFromFile(Context context, File dir, BatchRegisterCallback callback) {
                if (!dir.exists()) {
                    callback.onFinish(0, 0, 0, context.getString(R.string.please_put_photos, dir.getAbsolutePath()));
                    return;
                }
                File[] files = dir.listFiles((dir1, name) -> {
                    String nameLowerCase = name.toLowerCase();
                    return nameLowerCase.endsWith(SUFFIX_JPG)
                            || nameLowerCase.endsWith(SUFFIX_JPEG)
                            || nameLowerCase.endsWith(SUFFIX_PNG)
                            || nameLowerCase.endsWith(SUFFIX_BMP);
                });

                if (files == null || files.length == 0) {
                    callback.onFinish(0, 0, 0, context.getString(R.string.please_put_photos, dir.getAbsolutePath()));
                    return;
                }

                int total = files.length;
                final int[] failed = {0};
                final int[] success = {0};
                Single.fromSupplier(() -> FaceServer.getInstance().clearAllFaces())
                        .flatMapObservable(i -> Observable.fromArray(files))
                        .flatMap((Function<File, ObservableSource<Boolean>>) file -> {
                            byte[] bytes = FileUtil.fileToData(file);
                            String name = file.getName();
                            Logger.i("begin registerJpeg file: %s", name);
                            int suffixIndex = name.indexOf(".");
                            if (suffixIndex > 0) {
                                name = name.substring(0, suffixIndex);
                            }
                            FaceEntity faceEntity = FaceServer.getInstance().registerJpeg(context, bytes, name);
                            success[0]++;
                            if (faceEntity == null) {
                                failed[0]++;
                            }
                            return observer -> observer.onNext(faceEntity == null);
                        })
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.io())
                        .subscribe(new Observer<Boolean>() {
                            @Override
                            public void onSubscribe( Disposable d) {
                                disposable = d;
                            }

                            @Override
                            public void onNext( Boolean res) {
                                int succeedSize = success[0];
                                int failedSize = failed[0];
                                if (total == succeedSize + failedSize) {
                                    callback.onFinish(success[0], failed[0], total, null);
                                } else {
                                    callback.onProcess(success[0], failed[0], total);
                                }
                            }

                            @Override
                            public void onError( Throwable e) {
                                Logger.e(e, "error on registerJpeg");
                                callback.onFinish(success[0], failed[0], total, e.getMessage());
                                disposable.dispose();
                            }

                            @Override
                            public void onComplete() {
                                FaceServer.getInstance().initFaceList(context, null);
                                Logger.i("complete registerJpeg>>>>>>>");
                            }
                        });
            }

            /**
             * 停止注册
             *
             * @return 是否停止成功
             */
            private boolean stopRegisterIfDoing() {
                if (disposable != null && !disposable.isDisposed()) {
                    disposable.dispose();
                    disposable = null;
                    return true;
                }
                return false;
            }
        };
    }
}
