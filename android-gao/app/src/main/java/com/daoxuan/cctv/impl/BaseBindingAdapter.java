package com.daoxuan.cctv.impl;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.daoxuan.cctv.util.LogUtil;

public abstract class BaseBindingAdapter<T, D extends ViewDataBinding> extends RecyclerView.Adapter<BaseViewHolder<D>> {

    private final List<T> datas;
    private  final  int layoutId;

    protected IBaseBindingPresenter ItemPresenter;

    public BaseBindingAdapter(List<T> datas, int layoutId) {
        this.datas = datas;
        this.layoutId = layoutId;
    }

    @NonNull
    @Override
    public BaseViewHolder<D> onCreateViewHolder( ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new BaseViewHolder<>(DataBindingUtil.inflate(inflater, layoutId, parent, false));
        //doCreateViewHolder(viewHolder);BaseViewHolder<D> viewHolder =
        //return viewHolder;
    }

    //public abstract void doCreateViewHolder(BaseViewHolder<D> holder);
    public abstract void doBindViewHolder(BaseViewHolder<D> holder,T item);

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<D> holder, int position) {
        LogUtil.i("tag", "onBindViewHolder");
        doBindViewHolder(holder,datas.get(position));
        // holder.getBinding().setVariable(BR.data, mDatas.get(position));
        //holder.getBinding().setVariable(BR.itemPresenter, ItemPresenter);
        holder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return datas == null ? 0 : datas.size();
    }


    public void setItemPresenter(IBaseBindingPresenter itemPresenter) {
        ItemPresenter = itemPresenter;
    }

}