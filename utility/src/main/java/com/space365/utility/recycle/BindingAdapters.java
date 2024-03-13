package com.space365.utility.recycle;

import android.util.Pair;

import androidx.annotation.LayoutRes;
import androidx.databinding.BindingAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.List;

public class BindingAdapters {
    @BindingAdapter(value = {"itemsSource", "itemTemplate", "variableId", "extraVariable", "leftOffset", "topOffset"}, requireAll = false)
    public static <T> void setItems(RecyclerView recyclerView, List<T> itemsSource, @LayoutRes int itemTemplate, int variableId, List<Pair<Integer, Object>> extraVariable, int leftOffset, int topOffset) {
        ShadowAdapter<T> adapter;

        @SuppressWarnings("unchecked")
        ShadowAdapter<T> oldAdapter = (ShadowAdapter<T>) recyclerView.getAdapter();
        if (oldAdapter == null) {
            ItemTemplate template = ItemTemplate.of(itemTemplate, variableId);
            if (extraVariable != null && !extraVariable.isEmpty()) {
                template.setExtraVariable(extraVariable);
            }
            adapter = new ShadowAdapter<>(template);
            adapter.setLeftOffset(leftOffset);
            adapter.setTopOffset(topOffset);
        } else {
            adapter = oldAdapter;
        }
        adapter.setItems(itemsSource);
        if (oldAdapter != adapter) {
            recyclerView.setAdapter(adapter);
        }
    }

    @BindingAdapter(value = {"itemsSource", "itemTemplate", "variableId", "extraVariable", "indicator"}, requireAll = false)
    public static <T> void setPager2Items(ViewPager2 viewPager2, List<T> itemsSource, @LayoutRes int itemTemplate, int variableId, List<Pair<Integer, Object>> extraVariable, DotsIndicator indicator) {
        ShadowAdapter<T> adapter;

        @SuppressWarnings("unchecked")
        ShadowAdapter<T> oldAdapter = (ShadowAdapter<T>) viewPager2.getAdapter();
        if (oldAdapter == null) {
            ItemTemplate template = ItemTemplate.of(itemTemplate, variableId);
            if (extraVariable != null && !extraVariable.isEmpty()) {
                template.setExtraVariable(extraVariable);
            }
            adapter = new ShadowAdapter<>(template);
        } else {
            adapter = oldAdapter;
        }
        adapter.setItems(itemsSource);
        if (oldAdapter != adapter) {
            viewPager2.setAdapter(adapter);
        }

        if (indicator != null) {
            indicator.setViewPager2(viewPager2);
        }

    }
}