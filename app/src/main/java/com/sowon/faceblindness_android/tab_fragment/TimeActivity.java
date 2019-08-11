package com.sowon.faceblindness_android.tab_fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sowon.faceblindness_android.R;

/* 시간별 */

public class TimeActivity extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        final View view = inflater.inflate(R.layout.activity_time, container, false);
        return view;
    }

}