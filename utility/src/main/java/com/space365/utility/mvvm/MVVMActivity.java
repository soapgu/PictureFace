package com.space365.utility.mvvm;

import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

public abstract class MVVMActivity<VM extends ViewModel> extends AppCompatActivity {

    private final Class<VM> classOfVM;
    @LayoutRes
    private final int layoutId;
    private final int variableId;
    protected VM viewModel;

    public MVVMActivity(Class<VM> classOfVM , @LayoutRes int layoutId , int variableId){
        this.classOfVM = classOfVM;
        this.layoutId = layoutId;
        this.variableId = variableId;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.viewModel =  new ViewModelProvider(this.provideViewModelStoreOwner()).get(classOfVM);
        ViewDataBinding binding = DataBindingUtil.setContentView( this, layoutId  );
        binding.setVariable( this.variableId , this.viewModel );
        binding.setLifecycleOwner(this);
    }

    /**
     * 提供ViewModelStoreOwner给ViewModelProvider
     * extend class call override
     * @return ViewModelStoreOwner
     */
    @NonNull
    protected ViewModelStoreOwner provideViewModelStoreOwner(){
        return this;
    }
}
