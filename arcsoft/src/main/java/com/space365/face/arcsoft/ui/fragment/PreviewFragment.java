package com.space365.face.arcsoft.ui.fragment;

import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.arcsoft.face.ErrorInfo;
import com.orhanobut.logger.Logger;
import com.space365.face.arcsoft.R;
import com.space365.face.arcsoft.databinding.PreviewFragmentBinding;
import com.space365.face.arcsoft.ui.model.PreviewConfig;
import com.space365.face.arcsoft.ui.viewmodel.PreviewViewModel;
import com.space365.face.arcsoft.util.ConfigUtil;
import com.space365.face.arcsoft.util.ErrorCodeUtil;
import com.space365.face.arcsoft.util.FaceRectTransformer;
import com.space365.face.arcsoft.util.ToastUtil;
import com.space365.face.arcsoft.util.camera.CameraListener;
import com.space365.face.arcsoft.util.camera.DualCameraHelper;
import com.space365.face.arcsoft.util.face.constants.LivenessType;
import com.space365.face.arcsoft.util.face.model.FacePreviewInfo;
import com.space365.face.arcsoft.widget.FaceRectView;
import com.space365.face.arcsoft.widget.RecognizeAreaView;

import java.util.List;



public class PreviewFragment extends Fragment implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "FaceFragment";

    private DualCameraHelper rgbCameraHelper;
    private DualCameraHelper irCameraHelper;
    private FaceRectTransformer rgbFaceRectTransformer;
    private FaceRectTransformer irFaceRectTransformer;

    private PreviewFragmentBinding binding;
    private PreviewViewModel previewViewModel;
    private LivenessType livenessType;
    private boolean enableLivenessDetect = false;
    RecognizeAreaView recognizeAreaView;
    TextView textViewRgb;
    TextView textViewIr;

    public static PreviewFragment newInstance() {
        return new PreviewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.preview_fragment, container, false);

        //保持亮屏
//        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            WindowManager.LayoutParams attributes = getActivity().getWindow().getAttributes();
//            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
//            getActivity().getWindow().setAttributes(attributes);
//        }
//
//        // Activity启动后就锁定为启动时的方向
//        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        initData();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModel();
        initView();
    }

    private void initData() {
        String livenessTypeStr = ConfigUtil.getLivenessDetectType(getActivity());
        if (livenessTypeStr.equals((getString(R.string.value_liveness_type_rgb)))) {
            livenessType = LivenessType.RGB;
        } else if (livenessTypeStr.equals(getString(R.string.value_liveness_type_ir))) {
            livenessType = LivenessType.IR;
        } else {
            livenessType = null;
        }
        enableLivenessDetect = !ConfigUtil.getLivenessDetectType(getActivity()).equals(getString(R.string.value_liveness_type_disable));
    }

    private void initViewModel() {
        previewViewModel = new ViewModelProvider(this).get(PreviewViewModel.class);

        previewViewModel.setLivenessType(livenessType);

        previewViewModel.getFtInitCode().observe(getViewLifecycleOwner(), ftInitCode -> {
            if (ftInitCode != ErrorInfo.MOK) {
                String error = getString(R.string.specific_engine_init_failed, "ftEngine",
                        ftInitCode, ErrorCodeUtil.arcFaceErrorCodeToFieldName(ftInitCode));
                Log.i(TAG, "initEngine: " + error);
                ToastUtil.showToast(getActivity(), error);
            }
        });
        previewViewModel.getFrInitCode().observe(getViewLifecycleOwner(), frInitCode -> {
            if (frInitCode != ErrorInfo.MOK) {
                String error = getString(R.string.specific_engine_init_failed, "frEngine",
                        frInitCode, ErrorCodeUtil.arcFaceErrorCodeToFieldName(frInitCode));
                Log.i(TAG, "initEngine: " + error);
                ToastUtil.showToast(getActivity(), error);
            }
        });
        previewViewModel.getFlInitCode().observe(getViewLifecycleOwner(), flInitCode -> {
            if (flInitCode != ErrorInfo.MOK) {
                String error = getString(R.string.specific_engine_init_failed, "flEngine",
                        flInitCode, ErrorCodeUtil.arcFaceErrorCodeToFieldName(flInitCode));
                Log.i(TAG, "initEngine: " + error);
                ToastUtil.showToast(getActivity(), error);
            }
        });
        previewViewModel.getFaceItemEventMutableLiveData().observe(getViewLifecycleOwner(), faceItemEvent -> {
            RecyclerView.Adapter adapter = binding.dualCameraRecyclerViewPerson.getAdapter();
            switch (faceItemEvent.getEventType()) {
                case REMOVED:
                    if (adapter != null) {
                        adapter.notifyItemRemoved(faceItemEvent.getIndex());
                    }
                    break;
                case INSERTED:
                    if (adapter != null) {
                        adapter.notifyItemInserted(faceItemEvent.getIndex());
                    }
                    break;
                default:
                    break;
            }
        });

        previewViewModel.getRecognizeConfiguration().observe(getViewLifecycleOwner(), recognizeConfiguration -> {
            Log.i(TAG, "initViewModel recognizeConfiguration: " + recognizeConfiguration.toString());
        });

        previewViewModel.setOnRegisterFinishedCallback((facePreviewInfo, success) -> ToastUtil.showToast(getActivity(), success ? "register success" : "register failed"));

//        previewViewModel.getRecognizeNotice().observe(getViewLifecycleOwner(), notice -> binding.setRecognizeNotice(notice));

        binding.setViewModel(previewViewModel);
    }

    private void initView() {
        if (!DualCameraHelper.hasDualCamera() || livenessType != LivenessType.IR) {
            binding.flRecognizeIr.setVisibility(View.GONE);
        }
        //在布局结束后才做初始化操作
        binding.dualCameraTexturePreviewRgb.getViewTreeObserver().addOnGlobalLayoutListener(this);
        binding.setCompareResultList(previewViewModel.getCompareResultList().getValue());
    }

    @Override
    public void onDestroy() {
        Logger.i( "----PreviewFragment onDestroy--" );
        if (irCameraHelper != null) {
            irCameraHelper.release();
            irCameraHelper = null;
        }

        if (rgbCameraHelper != null) {
            rgbCameraHelper.release();
            rgbCameraHelper = null;
        }

        previewViewModel.destroy();
        super.onDestroy();
    }

    /**
     * 调整View的宽高，使2个预览同时显示
     *
     * @param previewView        显示预览数据的view
     * @param faceRectView       画框的view
     * @param previewSize        预览大小
     * @param displayOrientation 相机旋转角度
     * @return 调整后的LayoutParams
     */
    private ViewGroup.LayoutParams adjustPreviewViewSize(View rgbPreview, View previewView, FaceRectView faceRectView, Camera.Size previewSize, int displayOrientation, float scale) {
        ViewGroup.LayoutParams layoutParams = previewView.getLayoutParams();
        int measuredWidth = previewView.getMeasuredWidth();
        int measuredHeight = previewView.getMeasuredHeight();
        float ratio = ((float) previewSize.height) / (float) previewSize.width;
        if (ratio > 1) {
            ratio = 1 / ratio;
        }
        if (displayOrientation % 180 == 0) {
            layoutParams.width = measuredWidth;
            layoutParams.height = (int) (measuredWidth * ratio);
        } else {
            layoutParams.height = measuredHeight;
            layoutParams.width = (int) (measuredHeight * ratio);
        }
        if (scale < 1f) {
            ViewGroup.LayoutParams rgbParam = rgbPreview.getLayoutParams();
            layoutParams.width = (int) (rgbParam.width * scale);
            layoutParams.height = (int) (rgbParam.height * scale);
        } else {
            layoutParams.width *= scale;
            layoutParams.height *= scale;
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        if (layoutParams.width >= metrics.widthPixels) {
            float viewRatio = layoutParams.width / ((float) metrics.widthPixels);
            layoutParams.width /= viewRatio;
            layoutParams.height /= viewRatio;
        }
        if (layoutParams.height >= metrics.heightPixels) {
            float viewRatio = layoutParams.height / ((float) metrics.heightPixels);
            layoutParams.width /= viewRatio;
            layoutParams.height /= viewRatio;
        }

        previewView.setLayoutParams(layoutParams);
        faceRectView.setLayoutParams(layoutParams);
        return layoutParams;
    }

    private void initRgbCamera() {
        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                getActivity().runOnUiThread(() -> {
                    Camera.Size previewSizeRgb = camera.getParameters().getPreviewSize();
                    ViewGroup.LayoutParams layoutParams = adjustPreviewViewSize(binding.dualCameraTexturePreviewRgb,
                            binding.dualCameraTexturePreviewRgb, binding.dualCameraFaceRectView,
                            previewSizeRgb, displayOrientation, 1.0f);
                    rgbFaceRectTransformer = new FaceRectTransformer(previewSizeRgb.width, previewSizeRgb.height,
                            layoutParams.width, layoutParams.height, displayOrientation, cameraId, isMirror,
                            ConfigUtil.isDrawRgbRectHorizontalMirror(getActivity()),
                            ConfigUtil.isDrawRgbRectVerticalMirror(getActivity()));

                    FrameLayout parentView = ((FrameLayout) binding.dualCameraTexturePreviewRgb.getParent());

                    if (textViewRgb == null) {
                        textViewRgb = new TextView(getActivity(), null);
                    } else {
                        parentView.removeView(textViewRgb);
                    }
                    textViewRgb.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    textViewRgb.setText(getString(R.string.camera_rgb_preview_size, previewSizeRgb.width, previewSizeRgb.height));
                    textViewRgb.setTextColor(Color.WHITE);
                    textViewRgb.setBackgroundColor(getResources().getColor(R.color.color_bg_notification));
                    textViewRgb.setVisibility(View.GONE);
                    parentView.addView(textViewRgb);
                    // 父View宽度和子View一致，保持居中
                    ViewGroup.LayoutParams parentLayoutParams = parentView.getLayoutParams();
                    parentLayoutParams.width = layoutParams.width;
                    parentView.setLayoutParams(parentLayoutParams);

                    // 添加recognizeAreaView，在识别区域发生变更时，更新数据给FaceHelper
                    if (ConfigUtil.isRecognizeAreaLimited(getActivity())) {
                        if (recognizeAreaView == null) {
                            recognizeAreaView = new RecognizeAreaView(getActivity());
                            recognizeAreaView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        } else {
                            parentView.removeView(recognizeAreaView);
                        }
                        recognizeAreaView.setOnRecognizeAreaChangedListener(recognizeArea -> previewViewModel.setRecognizeArea(recognizeArea));
                        parentView.addView(recognizeAreaView);
                    }

                    previewViewModel.onRgbCameraOpened(camera);
                    previewViewModel.setRgbFaceRectTransformer(rgbFaceRectTransformer);
                });
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                //Logger.i( "----onPreview data----" );
                if( !pause ) {
                    binding.dualCameraFaceRectView.clearFaceInfo();
                    List<FacePreviewInfo> facePreviewInfoList = previewViewModel.onPreviewFrame(nv21, true);
                    if (facePreviewInfoList != null && rgbFaceRectTransformer != null) {
                        drawPreviewInfo(facePreviewInfoList);
                    }
                    previewViewModel.clearLeftFace(facePreviewInfoList);
                }
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                Log.i(TAG, "onCameraConfigurationChanged:" + Thread.currentThread().getName());
                if (rgbFaceRectTransformer != null) {
                    rgbFaceRectTransformer.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        PreviewConfig previewConfig = previewViewModel.getPreviewConfig();
        rgbCameraHelper = new DualCameraHelper.Builder()
                .previewViewSize(new Point(binding.dualCameraTexturePreviewRgb.getMeasuredWidth(), binding.dualCameraTexturePreviewRgb.getMeasuredHeight()))
                .rotation(getActivity().getWindowManager().getDefaultDisplay().getRotation())
                .additionalRotation(previewConfig.getRgbAdditionalDisplayOrientation())
                .previewSize(previewViewModel.loadPreviewSize())
                .specificCameraId(previewConfig.getRgbCameraId())
                .isMirror(ConfigUtil.isDrawRgbPreviewHorizontalMirror(getActivity()))
                .previewOn(binding.dualCameraTexturePreviewRgb)
                .cameraListener(cameraListener)
                .build();
        rgbCameraHelper.init();
        rgbCameraHelper.start();
    }

    /**
     * 初始化红外相机，若活体检测类型是可见光活体检测或不启用活体，则不需要启用
     */
    private void initIrCamera() {
        if (livenessType == LivenessType.RGB || !enableLivenessDetect) {
            return;
        }
        CameraListener irCameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Camera.Size previewSizeIr = camera.getParameters().getPreviewSize();
                ViewGroup.LayoutParams layoutParams = adjustPreviewViewSize(binding.dualCameraTexturePreviewRgb,
                        binding.dualCameraTexturePreviewIr, binding.dualCameraFaceRectViewIr,
                        previewSizeIr, displayOrientation, 0.25f);

                irFaceRectTransformer = new FaceRectTransformer(previewSizeIr.width, previewSizeIr.height,
                        layoutParams.width, layoutParams.height, displayOrientation, cameraId, isMirror,
                        ConfigUtil.isDrawIrRectHorizontalMirror(getActivity()),
                        ConfigUtil.isDrawIrRectVerticalMirror(getActivity()));

                FrameLayout parentView = ((FrameLayout) binding.dualCameraTexturePreviewIr.getParent());
                if (textViewIr == null) {
                    textViewIr = new TextView(getActivity(), null);
                } else {
                    parentView.removeView(textViewIr);
                }
                textViewIr.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                textViewIr.setText(getString(R.string.camera_ir_preview_size, previewSizeIr.width, previewSizeIr.height));
                textViewIr.setTextColor(Color.WHITE);
                textViewIr.setBackgroundColor(getResources().getColor(R.color.color_bg_notification));
                parentView.addView(textViewIr);

                previewViewModel.onIrCameraOpened(camera);
                previewViewModel.setIrFaceRectTransformer(irFaceRectTransformer);
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                previewViewModel.refreshIrPreviewData(nv21);
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (irFaceRectTransformer != null) {
                    irFaceRectTransformer.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        PreviewConfig previewConfig = previewViewModel.getPreviewConfig();
        irCameraHelper = new DualCameraHelper.Builder()
                .previewViewSize(new Point(binding.dualCameraTexturePreviewIr.getMeasuredWidth(), binding.dualCameraTexturePreviewIr.getMeasuredHeight()))
                .rotation(getActivity().getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(previewConfig.getIrCameraId())
                .previewOn(binding.dualCameraTexturePreviewIr)
                .cameraListener(irCameraListener)
                .isMirror(ConfigUtil.isDrawIrPreviewHorizontalMirror(getActivity()))
                .previewSize(previewViewModel.loadPreviewSize()) //相机预览大小设置，RGB与IR需使用相同大小
                .additionalRotation(previewConfig.getIrAdditionalDisplayOrientation()) //额外旋转角度
                .build();
        irCameraHelper.init();
        try {
            irCameraHelper.start();
        } catch (RuntimeException e) {
            ToastUtil.showToast(getActivity(),e.getMessage() + getString(R.string.camera_error_notice));
        }
    }

    /**
     * 绘制RGB、IR画面的实时人脸信息
     *
     * @param facePreviewInfoList RGB画面的实时人脸信息
     */
    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        if (rgbFaceRectTransformer != null) {
            List<FaceRectView.DrawInfo> rgbDrawInfoList = previewViewModel.getDrawInfo(facePreviewInfoList, LivenessType.RGB);
            binding.dualCameraFaceRectView.drawRealtimeFaceInfo(rgbDrawInfoList);
        }
        if (irFaceRectTransformer != null) {
            List<FaceRectView.DrawInfo> irDrawInfoList = previewViewModel.getDrawInfo(facePreviewInfoList, LivenessType.IR);
            binding.dualCameraFaceRectViewIr.drawRealtimeFaceInfo(irDrawInfoList);
        }
    }

    /**
     * 在{@link PreviewFragmentBinding#dualCameraTexturePreviewRgb}第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    @Override
    public void onGlobalLayout() {
        binding.dualCameraTexturePreviewRgb.getViewTreeObserver().removeOnGlobalLayoutListener(this);

        previewViewModel.init();
        initRgbCamera();
        if (DualCameraHelper.hasDualCamera() && livenessType == LivenessType.IR) {
            initIrCamera();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeCamera();
    }

    private void resumeCamera() {
        if (rgbCameraHelper != null) {
            rgbCameraHelper.start();
        }
        if (irCameraHelper != null) {
            irCameraHelper.start();
        }
    }

    @Override
    public void onPause() {
        pauseCamera();
        super.onPause();
    }

    private void pauseCamera() {
        if (rgbCameraHelper != null) {
            rgbCameraHelper.stop();
        }
        if (irCameraHelper != null) {
            irCameraHelper.stop();
        }
    }

    private boolean pause;


    public void pauseScanFace() {
        this.pause = true;
        binding.dualCameraFaceRectView.clearFaceInfo();
        previewViewModel.clearAllFace();
    }

    public void resumeScanFace() {
        this.pause = false;
    }
}