package com.space365.face.arcsoft.ui.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.LivenessParam;
import com.arcsoft.face.MaskInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.orhanobut.logger.Logger;
import com.space365.face.arcsoft.R;
import com.space365.face.arcsoft.facedb.entity.FaceEntity;
import com.space365.face.arcsoft.faceserver.FaceServer;
import com.space365.face.arcsoft.ui.callback.BatchRegisterCallback;
import com.space365.face.arcsoft.ui.callback.OnRegisterFinishedCallback;
import com.space365.face.arcsoft.ui.model.CompareResult;
import com.space365.face.arcsoft.ui.model.PreviewConfig;
import com.space365.face.arcsoft.util.ConfigUtil;
import com.space365.face.arcsoft.util.FaceRectTransformer;
import com.space365.face.arcsoft.util.FileUtil;
import com.space365.face.arcsoft.util.ToastUtil;
import com.space365.face.arcsoft.util.face.FaceHelper;
import com.space365.face.arcsoft.util.face.RecognizeCallback;
import com.space365.face.arcsoft.util.face.constants.LivenessType;
import com.space365.face.arcsoft.util.face.constants.RecognizeColor;
import com.space365.face.arcsoft.util.face.constants.RequestFeatureStatus;
import com.space365.face.arcsoft.util.face.model.FacePreviewInfo;
import com.space365.face.arcsoft.util.face.model.RecognizeConfiguration;
import com.space365.face.arcsoft.widget.FaceRectView;
import com.space365.utility.mvvm.ObservableViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PreviewViewModel extends ObservableViewModel implements RecognizeCallback {
    private static final String TAG = "PreviewViewModel";

    private static final String SUFFIX_JPEG = ".jpeg";
    private static final String SUFFIX_JPG = ".jpg";
    private static final String SUFFIX_PNG = ".png";
    private static final String SUFFIX_BMP = ".bmp";
    private static final String SUFFIX_TXT = ".txt";
    private static final int PAGE_SIZE = 20;

    private Disposable disposable;

    /**
     * 人脸识别过程中数据的更新类型
     */
    public enum EventType {
        /**
         * 人脸插入
         */
        INSERTED,
        /**
         * 人脸移除
         */
        REMOVED
    }

    public static class FaceItemEvent {
        private int index;
        private EventType eventType;

        public FaceItemEvent(int index, EventType eventType) {
            this.index = index;
            this.eventType = eventType;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public EventType getEventType() {
            return eventType;
        }

        public void setEventType(EventType eventType) {
            this.eventType = eventType;
        }
    }

    private OnRegisterFinishedCallback onRegisterFinishedCallback;
    /**
     * 注册人脸状态码，准备注册
     */
    public static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    public static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    public static final int REGISTER_STATUS_DONE = 2;
    /**
     * 人脸识别的状态，预设值为：已结束
     */
    private Integer registerStatus = REGISTER_STATUS_DONE;
    private static final int MAX_DETECT_NUM = 10;
    /**
     * 相机预览的分辨率
     */
    private Camera.Size previewSize;
    /**
     * 用于头像RecyclerView显示的信息
     */
    private MutableLiveData<List<CompareResult>> compareResultList;
    private MutableLiveData<FaceItemEvent> faceItemEventMutableLiveData = new MutableLiveData<>();
    // 各个引擎初始化的错误码
    private MutableLiveData<Integer> ftInitCode = new MutableLiveData<>();
    private MutableLiveData<Integer> frInitCode = new MutableLiveData<>();
    private MutableLiveData<Integer> flInitCode = new MutableLiveData<>();

    /**
     * 人脸操作辅助类，推帧即可，内部会进行特征提取、识别
     */
    private FaceHelper faceHelper;
    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪及图像质量检测
     */
    private FaceEngine ftEngine;
    /**
     * 用于特征提取的引擎
     */
    private FaceEngine frEngine;
    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private FaceEngine flEngine;
    private PreviewConfig previewConfig;

    private MutableLiveData<RecognizeConfiguration> recognizeConfiguration = new MutableLiveData<>();
    private MutableLiveData<String> recognizeNotice = new MutableLiveData<>();

    /**
     * 检测ir活体前，是否需要更新faceData
     */
    private boolean needUpdateFaceData;
    /**
     * 当前活体检测的检测类型
     */
    private LivenessType livenessType;
    /**
     * IR活体数据
     */
    private byte[] irNV21 = null;

    public void refreshIrPreviewData(byte[] irPreviewData) {
        irNV21 = irPreviewData;
    }

    public PreviewViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * 设置当前活体检测的检测类型
     *
     * @param livenessType 活体检测的检测类型
     */
    public void setLivenessType(LivenessType livenessType) {
        this.livenessType = livenessType;
    }

    public void setRgbFaceRectTransformer(FaceRectTransformer rgbFaceRectTransformer) {
        faceHelper.setRgbFaceRectTransformer(rgbFaceRectTransformer);
    }

    public void setIrFaceRectTransformer(FaceRectTransformer irFaceRectTransformer) {
        faceHelper.setIrFaceRectTransformer(irFaceRectTransformer);
    }

    /**
     * 注册实时NV21数据
     *
     * @param nv21            实时相机预览的NV21数据
     * @param facePreviewInfo 人脸信息
     */
    private void registerFace(final byte[] nv21, FacePreviewInfo facePreviewInfo) {
        registerStatus = REGISTER_STATUS_PROCESSING;
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            boolean success = FaceServer.getInstance().registerNv21(this.getApplication(), nv21.clone(), previewSize.width,
                    previewSize.height, facePreviewInfo, "registered_" + faceHelper.getTrackedFaceCount());
            emitter.onNext(success);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean success) {
                        if (onRegisterFinishedCallback != null) {
                            onRegisterFinishedCallback.onRegisterFinished(facePreviewInfo, success);
                        }
                        registerStatus = REGISTER_STATUS_DONE;
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (onRegisterFinishedCallback != null) {
                            onRegisterFinishedCallback.onRegisterFinished(facePreviewInfo, false);
                        }
                        registerStatus = REGISTER_STATUS_DONE;
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public MutableLiveData<List<CompareResult>> getCompareResultList() {
        if (compareResultList == null) {
            compareResultList = new MutableLiveData<>();
            compareResultList.setValue(new ArrayList<>());
        }
        return compareResultList;
    }

    /**
     * 初始化引擎
     */
    public void init() {
        Context context = this.getApplication().getApplicationContext();

        boolean switchCamera = ConfigUtil.isSwitchCamera(context);
        previewConfig = new PreviewConfig(
                switchCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK,
                switchCamera ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT,
                Integer.parseInt(ConfigUtil.getRgbCameraAdditionalRotation(context)),
                Integer.parseInt(ConfigUtil.getIrCameraAdditionalRotation(context))
        );

        // 填入在设置界面设置好的配置信息
        boolean enableLiveness = !ConfigUtil.getLivenessDetectType(context).equals(context.getString(R.string.value_liveness_type_disable));
        boolean enableFaceQualityDetect = ConfigUtil.isEnableImageQualityDetect(context);
        boolean enableFaceMoveLimit = ConfigUtil.isEnableFaceMoveLimit(context);
        boolean enableFaceSizeLimit = ConfigUtil.isEnableFaceSizeLimit(context);
        RecognizeConfiguration configuration = new RecognizeConfiguration.Builder()
                .enableFaceMoveLimit(enableFaceMoveLimit)
                .enableFaceSizeLimit(enableFaceSizeLimit)
                .faceSizeLimit(ConfigUtil.getFaceSizeLimit(context))
                .faceMoveLimit(ConfigUtil.getFaceMoveLimit(context))
                .enableLiveness(enableLiveness)
                .enableImageQuality(enableFaceQualityDetect)
                .maxDetectFaces(ConfigUtil.getRecognizeMaxDetectFaceNum(context))
                .keepMaxFace(ConfigUtil.isKeepMaxFace(context))
                .similarThreshold(ConfigUtil.getRecognizeThreshold(context))
                .imageQualityNoMaskRecognizeThreshold(ConfigUtil.getImageQualityNoMaskRecognizeThreshold(context))
                .imageQualityMaskRecognizeThreshold(ConfigUtil.getImageQualityMaskRecognizeThreshold(context))
                .livenessParam(new LivenessParam(ConfigUtil.getRgbLivenessThreshold(context), ConfigUtil.getIrLivenessThreshold(context)))
                .build();
        int dualCameraHorizontalOffset = ConfigUtil.getDualCameraHorizontalOffset(context);
        int dualCameraVerticalOffset = ConfigUtil.getDualCameraVerticalOffset(context);
        if (dualCameraHorizontalOffset != 0 || dualCameraVerticalOffset != 0) {
            needUpdateFaceData = true;
        }

        ftEngine = new FaceEngine();
        int ftEngineMask = FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_MASK_DETECT;
        ftInitCode.postValue(ftEngine.init(context, DetectMode.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(context),
                ConfigUtil.getRecognizeMaxDetectFaceNum(context), ftEngineMask));

        frEngine = new FaceEngine();
        int frEngineMask = FaceEngine.ASF_FACE_RECOGNITION;
        if (enableFaceQualityDetect) {
            frEngineMask |= FaceEngine.ASF_IMAGEQUALITY;
        }
        frInitCode.postValue(frEngine.init(context, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                10, frEngineMask));

        //启用活体检测时，才初始化活体引擎
        if (enableLiveness) {
            flEngine = new FaceEngine();
            int flEngineMask = (livenessType == LivenessType.RGB ? FaceEngine.ASF_LIVENESS : (FaceEngine.ASF_IR_LIVENESS | FaceEngine.ASF_FACE_DETECT));
            if (needUpdateFaceData) {
                flEngineMask |= FaceEngine.ASF_UPDATE_FACEDATA;
            }
            flInitCode.postValue(flEngine.init(context, DetectMode.ASF_DETECT_MODE_IMAGE,
                    DetectFaceOrientPriority.ASF_OP_ALL_OUT, 10, flEngineMask));
            LivenessParam livenessParam = new LivenessParam(ConfigUtil.getRgbLivenessThreshold(context), ConfigUtil.getIrLivenessThreshold(context));
            flEngine.setLivenessParam(livenessParam);
        }
        recognizeConfiguration.setValue(configuration);
    }

    /**
     * 停止注册
     *
     * @return 是否停止成功
     */
    public boolean stopRegisterIfDoing() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
            return true;
        }
        return false;
    }

    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private void unInit() {
        if (ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
        if (frEngine != null) {
            synchronized (frEngine) {
                int frUnInitCode = frEngine.unInit();
                Log.i(TAG, "unInitEngine: " + frUnInitCode);
            }
        }
        if (flEngine != null) {
            synchronized (flEngine) {
                int flUnInitCode = flEngine.unInit();
                Log.i(TAG, "unInitEngine: " + flUnInitCode);
            }
        }
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    public void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        List<CompareResult> compareResults = compareResultList.getValue();
        if (compareResults != null) {
            for (int i = compareResults.size() - 1; i >= 0; i--) {
                boolean contains = false;
                for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                    if (facePreviewInfo.getTrackId() == compareResults.get(i).getTrackId()) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    compareResults.remove(i);
                    getFaceItemEventMutableLiveData().postValue(new FaceItemEvent(i, EventType.REMOVED));
                }
            }
        }
    }

    /**
     * 释放操作
     */
    public void destroy() {
        Logger.i("----destroy PreviewViewModel----");
        unInit();
        stopRegisterIfDoing();

        if (faceHelper != null) {
            ConfigUtil.setTrackedFaceCount(this.getApplication().getApplicationContext(), faceHelper.getTrackedFaceCount());
            faceHelper.release();
            faceHelper = null;
        }
//        FaceServer.getInstance().release();
    }

    /**
     * 当相机打开时由activity调用，进行一些初始化操作
     *
     * @param camera 相机实例
     */
    public void onRgbCameraOpened(Camera camera) {
        Camera.Size lastPreviewSize = previewSize;
        previewSize = camera.getParameters().getPreviewSize();
        // 切换相机的时候可能会导致预览尺寸发生变化
        initFaceHelper(lastPreviewSize);
    }

    /**
     * 当相机打开时由activity调用，进行一些初始化操作
     *
     * @param camera 相机实例
     */
    public void onIrCameraOpened(Camera camera) {
        Camera.Size lastPreviewSize = previewSize;
        previewSize = camera.getParameters().getPreviewSize();
        // 切换相机的时候可能会导致预览尺寸发生变化
        initFaceHelper(lastPreviewSize);
    }

    private void initFaceHelper(Camera.Size lastPreviewSize) {
        if (faceHelper == null ||
                lastPreviewSize == null ||
                lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
            Integer trackedFaceCount = null;
            // 记录切换时的人脸序号
            if (faceHelper != null) {
                trackedFaceCount = faceHelper.getTrackedFaceCount();
                faceHelper.release();
            }
            Context context = this.getApplication().getApplicationContext();
            int horizontalOffset = ConfigUtil.getDualCameraHorizontalOffset(context);
            int verticalOffset = ConfigUtil.getDualCameraVerticalOffset(context);
            int maxDetectFaceNum = ConfigUtil.getRecognizeMaxDetectFaceNum(context);
            faceHelper = new FaceHelper.Builder()
                    .ftEngine(ftEngine)
                    .frEngine(frEngine)
                    .flEngine(flEngine)
                    .needUpdateFaceData(needUpdateFaceData)
                    .frQueueSize(maxDetectFaceNum)
                    .flQueueSize(maxDetectFaceNum)
                    .previewSize(previewSize)
                    .recognizeCallback(this)
                    .recognizeConfiguration(recognizeConfiguration.getValue())
                    .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(context) : trackedFaceCount)
                    .dualCameraFaceInfoTransformer(faceInfo -> {
                        FaceInfo irFaceInfo = new FaceInfo(faceInfo);
                        irFaceInfo.getRect().offset(horizontalOffset, verticalOffset);
                        return irFaceInfo;
                    })
                    .build();
        }
    }

    @Override
    public void onRecognized(CompareResult compareResult, Integer liveness, boolean similarPass) {
        if (similarPass) {
            boolean isAdded = false;
            List<CompareResult> compareResults = compareResultList.getValue();
            if (compareResults.size() > 0) {
                for (CompareResult compareResult1 : compareResults) {
                    if (compareResult1.getTrackId() == compareResult.getTrackId()) {
                        isAdded = true;
                        break;
                    }
                }
            }
            if (!isAdded) {
                //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                if (compareResults.size() >= MAX_DETECT_NUM) {
                    compareResults.remove(0);
                    getFaceItemEventMutableLiveData().postValue(new FaceItemEvent(0, EventType.REMOVED));
                }
                compareResults.add(compareResult);
                FaceEntity faceEntity = compareResult.getFaceEntity();
                sendFaceBroadcast(faceEntity.getUserName(),faceEntity.getUserId());

                getFaceItemEventMutableLiveData().postValue(new FaceItemEvent(compareResults.size() - 1, EventType.INSERTED));
            }
        }
    }

    @Override
    public void onNoticeChanged(String notice) {
        if (recognizeNotice != null) {
            recognizeNotice.postValue(notice);
        }
    }


    /**
     * 设置实时注册的结果回调
     *
     * @param onRegisterFinishedCallback 实时注册的结果回调
     */
    public void setOnRegisterFinishedCallback(OnRegisterFinishedCallback
                                                      onRegisterFinishedCallback) {
        this.onRegisterFinishedCallback = onRegisterFinishedCallback;
    }

    public MutableLiveData<Integer> getFtInitCode() {
        return ftInitCode;
    }

    public MutableLiveData<Integer> getFrInitCode() {
        return frInitCode;
    }

    public MutableLiveData<Integer> getFlInitCode() {
        return flInitCode;
    }

    public MutableLiveData<String> getRecognizeNotice() {
        return recognizeNotice;
    }

    public MutableLiveData<FaceItemEvent> getFaceItemEventMutableLiveData() {
        return faceItemEventMutableLiveData;
    }

    /**
     * 准备注册，将注册的状态值修改为待注册
     */
    public void prepareRegister() {
        if (registerStatus == REGISTER_STATUS_DONE) {
            registerStatus = REGISTER_STATUS_READY;
        }
    }

    /**
     * 根据预览信息生成绘制信息
     *
     * @param facePreviewInfoList 预览信息
     * @return 绘制信息
     */
    public List<FaceRectView.DrawInfo> getDrawInfo
    (List<FacePreviewInfo> facePreviewInfoList, LivenessType livenessType) {
        List<FaceRectView.DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            int trackId = facePreviewInfoList.get(i).getTrackId();
            String name = faceHelper.getName(trackId);
            Integer liveness = faceHelper.getLiveness(trackId);
            Integer recognizeStatus = faceHelper.getRecognizeStatus(trackId);

            // 根据识别结果和活体结果设置颜色
            int color = RecognizeColor.COLOR_UNKNOWN;
            if (recognizeStatus != null) {
                if (recognizeStatus == RequestFeatureStatus.FAILED) {
                    color = RecognizeColor.COLOR_FAILED;
                }
                if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
                    color = RecognizeColor.COLOR_SUCCESS;
                }
            }
            if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
                color = RecognizeColor.COLOR_FAILED;
            }

            drawInfoList.add(new FaceRectView.DrawInfo(
                    livenessType == LivenessType.RGB ?
                            facePreviewInfoList.get(i).getRgbTransformedRect() :
                            facePreviewInfoList.get(i).getIrTransformedRect(),
                    GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, liveness == null ? LivenessInfo.UNKNOWN : liveness, color,
                    name == null ? "" : name));
        }
        return drawInfoList;
    }


    /**
     * 传入可见光相机预览数据
     *
     * @param nv21        可见光相机预览数据
     * @param doRecognize 是否进行识别
     * @return 当前帧的检测结果信息
     */
    public List<FacePreviewInfo> onPreviewFrame(byte[] nv21, boolean doRecognize) {
        if (faceHelper != null) {
            if (livenessType == LivenessType.IR && irNV21 == null) {
                return null;
            }
            List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21, irNV21, doRecognize);
            if (registerStatus == REGISTER_STATUS_READY && !facePreviewInfoList.isEmpty()) {
                FacePreviewInfo facePreviewInfo = facePreviewInfoList.get(0);
                if (facePreviewInfo.getMask() != MaskInfo.WORN) {
                    registerFace(nv21, facePreviewInfoList.get(0));
                } else {
                    Toast.makeText(this.getApplication(), "注册照要求不戴口罩", Toast.LENGTH_SHORT).show();
                    registerStatus = REGISTER_STATUS_DONE;
                }
            }
            return facePreviewInfoList;
        }
        return null;
    }

    /**
     * 设置可识别区域（相对于View）
     *
     * @param recognizeArea 可识别区域
     */
    public void setRecognizeArea(Rect recognizeArea) {
        if (faceHelper != null) {
            faceHelper.setRecognizeArea(recognizeArea);
        }
    }

    public MutableLiveData<RecognizeConfiguration> getRecognizeConfiguration() {
        return recognizeConfiguration;
    }

    public PreviewConfig getPreviewConfig() {
        return previewConfig;
    }

    public Point loadPreviewSize() {
        String[] size = ConfigUtil.getPreviewSize(this.getApplication()).split("x");
        return new Point(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
    }

    /**
     * 将准备注册的状态置为待注册
     */
    public void register() {
        prepareRegister();
    }

    /**
     * 从文件夹注册
     */
    public void registerFromFiles() {
        Context context = this.getApplication().getApplicationContext();
        registerFromFile(context, new File(context.getString(R.string.register_faces_dir)), new BatchRegisterCallback() {

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

    public void registerFromFeatures() {
        Context context = this.getApplication().getApplicationContext();
        registerFromFeature(context, new File(context.getString(R.string.register_features_dir)), new BatchRegisterCallback() {

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
                    FaceEntity faceEntity = null;
                    faceEntity = FaceServer.getInstance().registerJpeg(context, bytes, name);
                    success[0]++;
                    if (faceEntity == null) {
                        failed[0]++;
                    }
                    FaceEntity finalFaceEntity = faceEntity;
                    return observer -> observer.onNext(finalFaceEntity == null);
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(Boolean res) {
                        int succeedSize = success[0];
                        int failedSize = failed[0];
                        if (total == succeedSize + failedSize) {
                            callback.onFinish(success[0], failed[0], total, null);
                        } else {
                            callback.onProcess(success[0], failed[0], total);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Logger.e(e, "error on registerJpeg");
                        callback.onFinish(success[0], failed[0], total, e.getMessage());
                        disposable.dispose();
                    }

                    @Override
                    public void onComplete() {
                        Logger.i("complete registerJpeg>>>>>>>");
                    }
                });
    }

    @SuppressLint("CheckResult")
    private void registerFromFeature(Context context, File dir, BatchRegisterCallback callback) {
        if (!dir.exists()) {
            callback.onFinish(0, 0, 0, context.getString(R.string.please_put_features, dir.getAbsolutePath()));
            return;
        }
        File[] files = dir.listFiles((dir1, name) -> {
            String nameLowerCase = name.toLowerCase();
            return nameLowerCase.endsWith(SUFFIX_TXT);
        });

        if (files == null || files.length == 0) {
            callback.onFinish(0, 0, 0, context.getString(R.string.please_put_features, dir.getAbsolutePath()));
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
                    int suffixIndex = name.indexOf(".");
                    if (suffixIndex > 0) {
                        name = name.substring(0, suffixIndex);
                    }
                    String imgPath = file.getPath().substring(0, file.getPath().lastIndexOf(".")) + ".jpg";
                    File imgFile = new File(imgPath);
                    if (!imgFile.exists()) {
                        imgPath = file.getPath().substring(0, file.getPath().lastIndexOf(".")) + ".png";
                    }
                    Boolean result = FaceServer.getInstance().registerFeature(context, bytes, imgPath, name);
                    success[0]++;
                    if (!result) {
                        failed[0]++;
                    }
                    return observer -> observer.onNext(result);
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(Boolean res) {
                        int succeedSize = success[0];
                        int failedSize = failed[0];
                        if (total == succeedSize + failedSize) {
                            callback.onFinish(success[0], failed[0], total, null);
                        } else {
                            callback.onProcess(success[0], failed[0], total);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        callback.onFinish(success[0], failed[0], total, e.getMessage());
                        disposable.dispose();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void sendFaceBroadcast(String name,String userId) {
        Intent intent = new Intent();
        intent.setAction("com.space365.intent.broadcast.face");
        intent.putExtra("name", name);
        intent.putExtra("uid",userId);
        Logger.i("Sent Face Broadcasts");
        this.getApplication().getApplicationContext().sendBroadcast(intent);
    }

    public void clearAllFace(){
        //清理列表人脸UI列表
        this.clearLeftFace( new ArrayList<>() );
        //清理已识别人脸内置字典列表
        this.faceHelper.clearAllFace();
    }
}