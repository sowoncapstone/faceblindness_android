package com.sowon.faceblindness_android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    EditText et_id, et_pw;
    String sId, sPw;
    RequestQueue queue;
    SharedPreferences sharedpreferences;
    public static final String MyPREF = "MyPref" ;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        et_id = (EditText) findViewById(R.id.login_id);
        et_pw = (EditText) findViewById(R.id.login_password);

        Button loginButton = (Button) findViewById(R.id.login_btn);

        sharedpreferences = getSharedPreferences(MyPREF, Context.MODE_PRIVATE);

        queue = Volley.newRequestQueue(this);

        sharedpreferences = this.getSharedPreferences(MyPREF, Context.MODE_PRIVATE);

        String saved_id= sharedpreferences.getString("id", null);
        String saved_pw= sharedpreferences.getString("password", null);

        if(saved_id != null && saved_pw !=null){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }

        final Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                // This is where you do your work in the UI thread.
                // Your worker tells you in the message what to do.
            }
        };

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "http://13.59.47.23:3000/login";

                final String userID = et_id.getText().toString();
                final String userPW = et_pw.getText().toString();




                StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                // response
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
                        //updateView(response.statusCode);
                        final String responseStr = response.toString();
                        if (response.statusCode == 200) {
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString("id", userID);
                            editor.putString("password", userPW);
                            editor.commit();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                        } else {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                            builder.setMessage("로그인 실패!")
                                                    .setNegativeButton("확인", null)
                                                    .create()
                                                    .show();
                                        }
                                    });
                                }
                            }).start();

                        }
                        return super.parseNetworkResponse(response);
                    }
                };


                postRequest.setRetryPolicy(new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


                queue.add(postRequest);


            }
        });


    }

    public void joinButton(View view) {
        Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(registerIntent);
    }


    public void updateView(int statusCode) {
        if (statusCode == 200) {

            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setMessage("로그인 성공!")
                    .setPositiveButton("확인", null)
                    .create()
                    .show();

        } else {

        }
    }

//    public void loginButton(View view) {
//        try {
//            sId = et_id.getText().toString();
//            sPw = et_pw.getText().toString();
//            if(sId.equals("admin") && sPw.equals("0000")){
//                Intent intent=new Intent(this, MainActivity.class);
//                startActivity(intent);
//            }
//            else if (sId != null && sPw != null) {
//                LoginDB loginDB = new LoginDB();
//                loginDB.execute();
//            }
//        } catch (NullPointerException e){
//            e.printStackTrace();
//        }
//
//    }

    public class LoginRequest extends StringRequest {
        final static private String URL = "http://13.59.109.120:3000/login";
        private Map<String, String> params;

        public LoginRequest(String userID, String userPW, Response.Listener<String> listener) {
            super(Method.POST, URL, listener, null);

            params = new HashMap<>();

            params.put("id", userID);
            params.put("password", userPW);
        }

        @Override
        public Map<String, String> getParams() {
            return params;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json; charset=utf-8");
            return headers;
        }
    }


    public class LoginDB extends AsyncTask<Void, Integer, Void> {
        String data = "";

        @Override
        protected Void doInBackground(Void... voids) {
            String param = "u_id=" + sId + "&u_pw=" + sPw + "";
            Log.e("POST", param);
            try {
                /* 서버연결 */
                URL url = new URL(
                        "http://13.59.109.120:3000/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.connect();

                /* 안드로이드 -> 서버 전달 */
                OutputStream outs = conn.getOutputStream();
                outs.write(param.getBytes("UTF-8"));
                outs.flush();
                outs.close();

                /* 서버 -> 안드로이드 전달 */
                InputStream is = null;
                BufferedReader in = null;

                is = conn.getInputStream();
                in = new BufferedReader(new InputStreamReader(is), 8 * 1024);
                String line = null;
                StringBuffer buff = new StringBuffer();
                while ((line = in.readLine()) != null) {
                    buff.append(line + "\n");
                }
                data = buff.toString().trim();

                /* 서버에서 응답 */
                Log.e("RECV DATA", data);

                if (data.equals("0")) {
                    Log.e("RESULT", "성공적으로 처리되었습니다!");
                } else {
                    Log.e("RESULT", "에러 발생! ERRCODE = " + data);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(LoginActivity.this);

            if (data.equals("1")) {
                Log.e("RESULT", "성공적으로 처리되었습니다!");
                alertBuilder
                        .setTitle("알림")
                        .setMessage("성공적으로 등록되었습니다!")
                        .setCancelable(true)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //로그인 드디어 성공해서 메인엑티비티 넘겨줌
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                AlertDialog dialog = alertBuilder.create();
                dialog.show();
            } else if (data.equals("0")) {
                Log.e("RESULT", "비밀번호가 일치하지 않습니다.");
                alertBuilder
                        .setTitle("알림")
                        .setMessage("비밀번호가 일치하지 않습니다.")
                        .setCancelable(true)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //finish();
                            }
                        });
                AlertDialog dialog = alertBuilder.create();
                dialog.show();
            } else {
                Log.e("RESULT", "에러 발생! ERRCODE = " + data);
                alertBuilder
                        .setTitle("알림")
                        .setMessage("등록중 에러가 발생했습니다! errcode : " + data)
                        .setCancelable(true)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //finish();
                            }
                        });
                AlertDialog dialog = alertBuilder.create();
                dialog.show();
            }
        }
    }
}
