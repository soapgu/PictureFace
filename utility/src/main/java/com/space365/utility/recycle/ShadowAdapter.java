package com.space365.utility.recycle;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableList;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.List;

public class ShadowAdapter<T> extends RecyclerView.Adapter<ShadowAdapter.ViewHolder> {
    private List<T> items;
    private final ItemTemplate itemTemplate;
    private int leftOffset;
    private int topOffset;
    private RecyclerView recyclerView;
    private WeakReferenceOnListChangedCallback<T> callback;

    public ShadowAdapter(ItemTemplate itemTemplate) {
        this.itemTemplate = itemTemplate;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<T> items) {
        if (this.items == items) {
            return;
        }
        // If a recyclerview is listening, set up listeners. Otherwise wait until one is attached.
        // No need to make a sound if nobody is listening right?
        if (recyclerView != null) {
            if (this.items instanceof ObservableList) {
                ((ObservableList<T>) this.items).removeOnListChangedCallback(callback);
                callback = null;
            }
            if (items instanceof ObservableList) {
                callback = new WeakReferenceOnListChangedCallback<>(this);
                ((ObservableList<T>) items).addOnListChangedCallback(callback);
            }
        }
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (this.recyclerView == null && items instanceof ObservableList) {
            callback = new WeakReferenceOnListChangedCallback<>(this);
            ((ObservableList<T>) items).addOnListChangedCallback(callback);
        }
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (this.recyclerView != null && items instanceof ObservableList) {
            ((ObservableList<T>) items).removeOnListChangedCallback(callback);
            callback = null;
        }
        this.recyclerView = null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view, which defines the UI of the list item
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, itemTemplate.getTemplateId(), parent, false);
        return new ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViewDataBinding binding = DataBindingUtil.getBinding(holder.itemView);
        assert binding != null;
        binding.setVariable(this.itemTemplate.getVariableId(), this.items.get(position));
        if (this.itemTemplate.getExtraVariable() != null) {
            this.itemTemplate.getExtraVariable().forEach(t -> binding.setVariable(t.first, t.second));
        }
        if ((leftOffset != 0 || topOffset != 0) && position != 0) {
            setMargins(holder.itemView, leftOffset, topOffset, 0, 0);
        }
        binding.executePendingBindings();
    }

    public void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }

    public void setLeftOffset(int leftOffset) {
        this.leftOffset = leftOffset;
    }

    public void setTopOffset(int topOffset) {
        this.topOffset = topOffset;
    }

    @Override
    public int getItemCount() {
        return items != null ? this.items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private static class WeakReferenceOnListChangedCallback<T> extends ObservableList.OnListChangedCallback<ObservableList<T>> {
        final WeakReference<ShadowAdapter<T>> adapterRef;

        WeakReferenceOnListChangedCallback(ShadowAdapter<T> adapter) {
            this.adapterRef = new WeakReference<>(adapter);
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onChanged(ObservableList sender) {
            ShadowAdapter<T> adapter = adapterRef.get();
            if (adapter == null) {
                return;
            }
            //Utils.ensureChangeOnMainThread();
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(ObservableList sender, final int positionStart, final int itemCount) {
            ShadowAdapter<T> adapter = adapterRef.get();
            if (adapter == null) {
                return;
            }
            //Utils.ensureChangeOnMainThread();
            adapter.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(ObservableList sender, final int positionStart, final int itemCount) {
            ShadowAdapter<T> adapter = adapterRef.get();
            if (adapter == null) {
                return;
            }
            //Utils.ensureChangeOnMainThread();
            adapter.notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(ObservableList sender, final int fromPosition, final int toPosition, final int itemCount) {
            ShadowAdapter<T> adapter = adapterRef.get();
            if (adapter == null) {
                return;
            }
            //Utils.ensureChangeOnMainThread();
            for (int i = 0; i < itemCount; i++) {
                adapter.notifyItemMoved(fromPosition + i, toPosition + i);
            }
        }

        @Override
        public void onItemRangeRemoved(ObservableList sender, final int positionStart, final int itemCount) {
            ShadowAdapter<T> adapter = adapterRef.get();
            if (adapter == null) {
                return;
            }
            //Utils.ensureChangeOnMainThread();
            adapter.notifyItemRangeRemoved(positionStart, itemCount);
        }
    }
}


