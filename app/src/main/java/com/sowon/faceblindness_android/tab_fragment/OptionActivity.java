package com.sowon.faceblindness_android.tab_fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.sowon.faceblindness_android.MainActivity;
import com.sowon.faceblindness_android.R;
import com.sowon.faceblindness_android.util.LoginActivity;

/* 환경설정
 *
 * 기능:
 *
 * - 로그아웃
 *
 * */

public class OptionActivity extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_option, container, false);

        final Button log_out = (Button) view.findViewById(R.id.logout_btn);
        log_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout(view);
            }
        });

        return view;
    }

    public void logout(View view) {
        SharedPreferences sharedpreferences = this.getActivity().getSharedPreferences(LoginActivity.MyPREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.clear();
        editor.commit();

        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create(); //Read Update
        alertDialog.setTitle("Logout");
        alertDialog.setMessage("You are logged out. Bye");

        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // here you can add functions
                Intent intent = new Intent(getContext(), LoginActivity.class);
                startActivity(intent);
            }
        });

        alertDialog.show();


    }

}