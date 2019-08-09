package com.example.googlewebrtc.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.googlewebrtc.bean.base.UserModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by louisgeek on 2019/8/8.
 */
public class MyBaseAdapter extends BaseAdapter {
    private List<UserModel> mUserModelList = new ArrayList<>();

    public void refreshDataList(List<UserModel> userModelList) {
        mUserModelList.clear();
        mUserModelList.addAll(userModelList);
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mUserModelList.size();
    }

    @Override
    public Object getItem(int i) {
        return mUserModelList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = LayoutInflater.from(viewGroup.getContext()).inflate(android.R.layout.simple_list_item_2, null, false);
            view.setTag(viewHolder);
            //
            viewHolder.mTextView1 = view.findViewById(android.R.id.text1);
            viewHolder.mTextView2 = view.findViewById(android.R.id.text2);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.mTextView1.setText(mUserModelList.get(i).socketId);
        viewHolder.mTextView2.setText(mUserModelList.get(i).userName);
        //
        return view;
    }

    public static class ViewHolder {
        public TextView mTextView1;
        public TextView mTextView2;
    }
}
