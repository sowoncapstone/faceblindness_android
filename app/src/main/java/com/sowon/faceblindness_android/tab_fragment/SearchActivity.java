package com.sowon.faceblindness_android.tab_fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sowon.faceblindness_android.MainActivity;
import com.sowon.faceblindness_android.R;

/* 검색
*
* */

public class SearchActivity extends Fragment {
    private String encodings;

    // 메인액티비티에서 프래그먼트로 값 수신받기
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            encodings = ((MainActivity) getActivity()).passvalue();
            Log.d("encoding", encodings);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_search, container, false);

        return view;
    }
}

