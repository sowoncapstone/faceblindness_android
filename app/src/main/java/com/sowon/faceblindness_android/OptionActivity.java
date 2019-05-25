package com.sowon.faceblindness_android;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.nio.ByteBuffer;

public class OptionActivity extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        final View view = inflater.inflate(R.layout.activity_option, container, false);

        Bitmap bitmap  = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.bitmap);
        final int lnth = bitmap.getByteCount();
        ByteBuffer dst= ByteBuffer.allocate(lnth);
        bitmap.copyPixelsToBuffer(dst);
        byte[] barray = dst.array();

        String buffer = barray.toString();
        Log.d("String", buffer);

        return view;
    }

}