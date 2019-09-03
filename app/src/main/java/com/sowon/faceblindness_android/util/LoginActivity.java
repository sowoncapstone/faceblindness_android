package com.sowon.faceblindness_android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.sowon.faceblindness_android.MainActivity;
import com.sowon.faceblindness_android.R;

import java.util.HashMap;
import java.util.Map;

/* 로그인화면 */

public class LoginActivity extends AppCompatActivity {
    EditText et_id, et_pw;
    RequestQueue queue;
    SharedPreferences sharedpreferences;
    public static final String MyPREF = "MyPref";
    boolean success;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        et_id = (EditText) findViewById(R.id.login_id);
        et_pw = (EditText) findViewById(R.id.login_password);

        Button loginButton = (Button) findViewById(R.id.login_btn);

        queue = Volley.newRequestQueue(this);

        sharedpreferences = this.getSharedPreferences(MyPREF, Context.MODE_PRIVATE);

        String saved_id = sharedpreferences.getString("id", null);
        String saved_pw = sharedpreferences.getString("password", null);

        if (saved_id != null && saved_pw != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "http://" + getString(R.string.ip_address) + ":3000/login";

                final String userID = et_id.getText().toString();
                final String userPW = et_pw.getText().toString();

                StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                // response
                                Log.d("Response", response);
                                if (response.contains(userID)) {
                                    SharedPreferences.Editor editor = sharedpreferences.edit();
                                    editor.putString("id", userID);
                                    editor.putString("password", userPW);
                                    editor.commit();
                                    success = true;
                                } else {
                                    success = false;
                                }

                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // error
                                Log.d("Error.Response", error.toString());
                            }
                        }
                ){
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("id", userID);
                        params.put("pw", userPW);

                        return params;
                    }

                    @Override
                    public String getBodyContentType() {
                        return "application/x-www-form-urlencoded; charset=UTF-8";
                    }

                };

                postRequest.setRetryPolicy(new DefaultRetryPolicy(30000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


                queue.add(postRequest);


                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        // Actions to do after 10 seconds
                        if (success) {
                            Intent intent = new Intent(LoginActivity.this,
                                    MainActivity.class);
                            startActivity(intent);
                        } else {
                            AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).create(); //Read Update
                            alertDialog.setTitle("Login");
                            alertDialog.setMessage("Sorry, please retry");

                            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // here you can add functions
                                }
                            });

                            alertDialog.show();
                        }

                    }
                }, 1500);


            }
        });

    }

    public void joinButton(View view) {
        Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(registerIntent);
    }

}
