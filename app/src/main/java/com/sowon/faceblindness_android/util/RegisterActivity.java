package com.sowon.faceblindness_android.util;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.sowon.faceblindness_android.R;

import java.util.HashMap;
import java.util.Map;

/* 회원가입 */

public class RegisterActivity extends AppCompatActivity {
    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        final EditText et_id = (EditText) findViewById(R.id.register_id);
        final EditText et_pw = (EditText) findViewById(R.id.register_password);
        final EditText et_name = (EditText) findViewById(R.id.register_name);
        final EditText et_age = (EditText) findViewById(R.id.register_age);

        Button registerB = (Button) findViewById(R.id.register_button);
        queue = Volley.newRequestQueue(this);


        registerB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "http://" + getString(R.string.ip_address) + ":3000/register";

                final String userID = et_id.getText().toString();
                final String userPW = et_pw.getText().toString();
                //String userNmae = et_name.getText().toString();
                //int userAge = Integer.parseInt(et_age.getText().toString());

                StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.d("Response", response);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // error
                                Log.d("Error.Response", error.toString());
                            }
                        }
                ) {
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

                    @Override
                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
                        if (response.statusCode == 200) {
                            //Toast.makeText(RegisterActivity.this,"회원가입 성공!", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                        } else {
                            //Toast.makeText(RegisterActivity.this, "회원가입 실패. 다시 시도해주세요", Toast.LENGTH_LONG).show();
                        }
                        return super.parseNetworkResponse(response);
                    }
                };

                postRequest.setRetryPolicy(new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                queue.add(postRequest);
            }
        });
    }

}

