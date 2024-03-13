package com.space365.utility.mvvm;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.util.Iterator;



/**
 * 使用MVVM架构的Fragment抽象类
 * @param <VM> VieModel类型
 */
public abstract class MVVMFragment<VM extends ViewModel> extends Fragment {


    private final Class<VM> classOfVM;
    @LayoutRes
    private final int layoutId;
    private final int variableId;
    protected VM viewModel;

     /**
     * 构造函数
     * @param classOfVM ViewModel的Class
     * @param layoutId 对应layout的id
     * @param variableId layout的变量值 Sample:BR.dataContext
     */
    public MVVMFragment(Class<VM> classOfVM , @LayoutRes int layoutId , int variableId){
        this.classOfVM = classOfVM;
        this.layoutId = layoutId;
        this.variableId = variableId;
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

    /**
     * 获取额外的变量数据源
     * @return 枚举键值对
     */
    protected Iterable<Pair<Integer,Object>> getExtraVariableSource(){
        return () -> new Iterator<Pair<Integer, Object>>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Pair<Integer, Object> next() {
                return null;
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.viewModel =  new ViewModelProvider(this.provideViewModelStoreOwner()).get(classOfVM);
        ViewDataBinding binding = DataBindingUtil.inflate( inflater , layoutId , container ,false );
        binding.setVariable( this.variableId , this.viewModel );
        binding.setLifecycleOwner(this.getViewLifecycleOwner());
        for( Pair<Integer, Object> each : getExtraVariableSource() ){
            binding.setVariable( each.first , each.second );
        }
        return binding.getRoot();
    }
}
