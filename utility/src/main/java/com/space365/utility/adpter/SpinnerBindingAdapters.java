package com.space365.utility.adpter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;

import com.space365.utility.listener.OnMyItemSelectedListener;

import java.util.List;
import java.util.Optional;

public class SpinnerBindingAdapters {
    @SuppressLint("ResourceType")
    @BindingAdapter(value = {"itemsSource","onItemSelected","selectedIndex","resource","tvResource","dropdownResource","iconResource","icon"},requireAll = false)
    public static <T> void setSpinnerItems(Spinner spinner , List<T> itemsSource, final OnMyItemSelectedListener listener , int index, @LayoutRes int resource, @IdRes int tvResource, @LayoutRes int dropdownResource , @IdRes int iconResource, @DrawableRes int icon){
        SimpleArrayAdapter<T> adapter;
        @SuppressWarnings("unchecked")
        SimpleArrayAdapter<T> oldAdapter = (SimpleArrayAdapter<T>)spinner.getAdapter();
        boolean createNew = oldAdapter != null && oldAdapter.list == itemsSource;
        if( createNew ){
            adapter = oldAdapter;
            adapter.notifyDataSetChanged();
        }
        else {
            int viewResource = resource > 0 ? resource : android.R.layout.simple_spinner_item;
            adapter = new SimpleArrayAdapter<>(spinner.getContext(), viewResource, tvResource, iconResource , icon , itemsSource);
            adapter.setDropDownViewResource( dropdownResource > 0 ? dropdownResource : android.R.layout.simple_spinner_dropdown_item);
            if( listener != null ) {
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                    private Optional<Integer> lastPosition = Optional.empty();

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        //String itemValue = (String) parent.getItemAtPosition(position);
                        if (lastPosition.isPresent() && lastPosition.get() == position) {
                            return;
                        }
                        lastPosition = Optional.of(position);
                        adapter.setSelectedPosition( position );
                        listener.onItemSelected(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }
        }
        spinner.setAdapter(adapter);

        if( spinner.getSelectedItemPosition() != index ) {
            spinner.setSelection(index);
        }

    }

    public static class SimpleArrayAdapter<T> extends ArrayAdapter<T>{
        private final List<T> list;
        private final int tvResource;
        private int selectedPosition;
        private final int iconResource;
        private final int icon;

        public SimpleArrayAdapter(Context context, @LayoutRes int resource,@IdRes int tvResource,@IdRes int iconResource,@DrawableRes int icon,
                                  List<T> objects){
            super(context,resource, tvResource ,objects);
            this.list = objects;
            this.tvResource = tvResource;
            this.iconResource = iconResource;
            this.icon = icon;
        }

        public void setSelectedPosition(int selectedPosition) {
            this.selectedPosition = selectedPosition;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            if( tvResource > 0 ) {
                TextView textView = view.findViewById(tvResource);
                textView.setTextColor( selectedPosition == position ? 0xFF007AFF : Color.BLACK );
            }
            return view;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if( iconResource > 0 && icon > 0 ){
                ImageView imageView = view.findViewById(iconResource);
                imageView.setImageResource(icon);
            }
            return view;
        }
    }
}
