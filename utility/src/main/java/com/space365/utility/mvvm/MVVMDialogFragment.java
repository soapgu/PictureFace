package com.space365.utility.mvvm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

public abstract class MVVMDialogFragment<VM extends ViewModel> extends DialogFragment {
    private final Class<VM> classOfVM;
    @LayoutRes
    private final int layoutId;
    private final int variableId;
    protected VM viewModel;

    /**
     * 构造函数
     *
     * @param classOfVM  ViewModel的Class
     * @param layoutId   对应layout的id
     * @param variableId layout的变量值 Sample:BR.dataContext
     */
    public MVVMDialogFragment(Class<VM> classOfVM, @LayoutRes int layoutId, int variableId) {
        this.classOfVM = classOfVM;
        this.layoutId = layoutId;
        this.variableId = variableId;
    }

    /**
     * 提供ViewModelStoreOwner给ViewModelProvider
     * extend class call override
     *
     * @return ViewModelStoreOwner
     */
    @NonNull
    protected ViewModelStoreOwner provideViewModelStoreOwner() {
        return this;
    }

    /**
     * 设置是否沉浸模式，默认是
     * 派生类可以改写此方法
     *
     * @return 是否全屏
     */
    protected boolean showImmersive() {
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.viewModel = new ViewModelProvider(this.provideViewModelStoreOwner()).get(classOfVM);
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, container, false);
        binding.setVariable(this.variableId, this.viewModel);
        showFun();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (showImmersive()) {
            showFun();
        }
    }

    public void showFun() {
        requireDialog().getWindow().getDecorView().setSystemUiVisibility(requireActivity().getWindow().getDecorView().getSystemUiVisibility());
        requireDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        requireDialog().setOnShowListener(dialog -> {
            //Clear the not focusable flag from the window
            requireDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        });
    }
}
