package com.sowon.faceblindness_android;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sowon.faceblindness_android.tab_fragment.CategoryActivity;
import com.sowon.faceblindness_android.tab_fragment.OptionActivity;
import com.sowon.faceblindness_android.tab_fragment.SearchActivity;
import com.sowon.faceblindness_android.tab_fragment.TimeActivity;
import com.sowon.faceblindness_android.tab_fragment.TimelineActivity;
import com.sowon.faceblindness_android.util.EncodedImage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import static com.sowon.faceblindness_android.util.LoginActivity.MyPREF;

/* 메인
 *
 * 기능
 * - 블루투스: 연결, 사진 수신
 * - 웹 통신: 사진 전송, 결과 수신
 * - 알림: 수신 결과 알림
 *
 * */

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_BLUETOOTH_ENABLE = 100;
    ConnectedTask mConnectedTask = null;
    static BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    static boolean isConnectionError = false;
    private String wholeImage = "";
    private Retrofit mRetrofit;
    private RetrofitAPI mRetrofitAPI;
    private SharedPreferences log_info;

    private static final String TAG = "BluetoothClient";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.login_check);

        log_info = this.getSharedPreferences(MyPREF, Context.MODE_PRIVATE);

        textView.setText("Welcome, " + log_info.getString("id", null));

        OptionActivity optionActivity = new OptionActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, optionActivity).commit();

        mConversationArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            showErrorDialog("This device is not implement Bluetooth.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        } else {
            Log.d(TAG, "Initialisation successful.");
            showPairedDevicesListDialog();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mConnectedTask != null) {
            mConnectedTask.cancel(true);
        }
    }

    public interface RetrofitAPI {
        @POST("post")
        Call<String> postComment(@Body EncodedImage encodedImage);
    }

    //프래그먼트에 값 전달하기. SearchActivity 참조.
    public String passvalue() {
        Log.d(TAG, "*********************PASS to the FRAGMENT************************");
        String sendString = wholeImage;
        wholeImage = "";
        return sendString;
    }


    //블루투스 연결
    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        private BluetoothSocket mBluetoothSocket = null;
        private BluetoothDevice mBluetoothDevice = null;

        ConnectTask(BluetoothDevice bluetoothDevice) {
            mBluetoothDevice = bluetoothDevice;
            mConnectedDeviceName = bluetoothDevice.getName();
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                Log.d(TAG, "  create socket for " + mConnectedDeviceName);

            } catch (IOException e) {
                Log.e(TAG, "socket create failed " + e.getMessage());
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mBluetoothAdapter.cancelDiscovery();
            try {
                mBluetoothSocket.connect();
            } catch (IOException e) {
                try {
                    mBluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + " socket during connection failure", e2);
                }
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean isSucess) {
            if (isSucess) {
                mConnectedTask = new ConnectedTask(mBluetoothSocket);
                mConnectedTask.execute();
            } else {
                isConnectionError = true;
                Log.d(TAG, "Unable to connect device");
                showErrorDialog("Unable to connect device");
            }
        }
    }

    //블루투스 연결 이후 사진 수신 + 전송
    private class ConnectedTask extends AsyncTask<Void, String, Boolean> {

        private InputStream mInputStream = null;
        private BluetoothSocket mBluetoothSocket = null;

        ConnectedTask(BluetoothSocket socket) {
            mBluetoothSocket = socket;
            try {
                mInputStream = mBluetoothSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "socket not created", e);
            }
            Log.d(TAG, "connected to " + mConnectedDeviceName);
        }

        /* ASYNCTASK 에서 UI 를 업데이트 할 수 있는 메소드
         * 1. onPreExecute() : doInBackground() 전에 실행됨
         * 2. onPostExecute(): doInBackground() 후에 실행됨.
         * 3. onProgressUpdate() : doInBackground() 실행 중 publishProgress()가 불리면 실행됨.
         *
         * */

        @Override
        protected Boolean doInBackground(Void... params) {
            String writeString = "";
            String beforeString = "";
            while (true) {
                if (isCancelled()) return false;
                try {
                    // 블루투스로 라즈베리파이에서 사진 수신
                    BufferedReader br = new BufferedReader(new InputStreamReader(mInputStream));
                    String bufString = br.readLine();

                    //만약 아직 아무것도 안왔다 그럼 멈춤
                    if(bufString == null){
                        Thread.sleep(2000);
                        continue;
                    }
                    writeString += bufString;

                    //만약 전에 보낸 것과 같은 사진이면 멈춤
                    if(writeString == bufString){
                        Thread.sleep(2000);
                        writeString = "";
                        continue;
                    }

                    //만약 제대로 왔다 그럼 보내
                    if (bufString.contains("ENDOFFILE")) {
                        Log.d("Sending Image", writeString);

                        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
                        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                        OkHttpClient client = new OkHttpClient.Builder()
                                .addInterceptor(interceptor)
                                .retryOnConnectionFailure(true)
                                .connectTimeout(15, TimeUnit.SECONDS)
                                .build();

                        Gson gson = new GsonBuilder()
                                .setLenient()
                                .create();

                        // 레트로핏으로 사진 서버로 전송
                        mRetrofit = new Retrofit.Builder()
                                .baseUrl("http://" + getString(R.string.ip_address) + ":3000/post")
                                .client(client)
                                .addConverterFactory(GsonConverterFactory.create(gson))
                                .build();
                        mRetrofitAPI = mRetrofit.create(RetrofitAPI.class);

                        EncodedImage ei = new EncodedImage(writeString, log_info.getString("id", null));
                        Call<String> comment = mRetrofitAPI.postComment(ei);
                        comment.enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                String result = response.body();
                                // 여기서 서버로부터 받은 데이터를 onProgressUpdate()로 넘겨줌.
                                publishProgress(result);
                            }
                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                t.printStackTrace();
                            }
                        });
                        beforeString = writeString;
                        writeString = "";
                    }
                } catch (Exception e) {
                    Log.e("ERROR2", e.getMessage());
                }
            }
        }


        @Override
        protected void onProgressUpdate(String... recvMessage) {
            //mConversationArrayAdapter.insert(mConnectedDeviceName + ": " + recvMessage[0], 0);

            final LayoutInflater inflater = MainActivity.this.getLayoutInflater();
            View mView = inflater.inflate(R.layout.dialog_unkown, null);
            final EditText input = (EditText) mView.findViewById(R.id.unknown_name);

            if (recvMessage.equals("모르는사람")) {
                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create(); //Read Update
                alertDialog.setTitle("Face Detection Result");
                alertDialog.setView(mView);
                alertDialog.setMessage("this person is unknown would you like to register?");

                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // 이름을 전달하는 새 Http 통신을 위해서 Intent 생성
                        String new_name = input.getText().toString();
                        Intent nameIntent = new Intent(MainActivity.this, RegisterName.class);
                        nameIntent.putExtra("new_name", new_name);
                        startService(nameIntent);
                    }
                });
                alertDialog.setButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                alertDialog.show();
            } else {
                // doInBackground() 에서 받은 결과값을 UI로 띄워줌.
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create(); //Read Update
                alertDialog.setTitle("Face Detection Result");
                alertDialog.setMessage("this person is" + recvMessage);

                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
                alertDialog.show();
            }

        }

        @Override
        protected void onPostExecute(Boolean isSucess) {
            super.onPostExecute(isSucess);
            if (!isSucess) {
                closeSocket();
                Log.d(TAG, "Device connection was lost");
                isConnectionError = true;
                showErrorDialog("Device connection was lost");
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);
            closeSocket();
        }

        void closeSocket() {
            try {
                mBluetoothSocket.close();
                Log.d(TAG, "close socket()");
            } catch (IOException e2) {
                Log.e(TAG, "unable to close() " +
                        " socket during connection failure", e2);
            }
        }
    }

    //얼굴 인식 결과가 모르는 사람일 경우 이름 등록
    private class RegisterName extends AsyncTask<Void, String, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {

            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
            final String userID = getIntent().getStringExtra("new_name");

            String url = "http://" + getString(R.string.ip_address) + ":3000/newname";

            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                    new com.android.volley.Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("Response", response);
                            onProgressUpdate();
                        }
                    },
                    new com.android.volley.Response.ErrorListener() {
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
                    params.put("name", userID);

                    return params;
                }
                @Override
                public String getBodyContentType() {
                    return "application/x-www-form-urlencoded; charset=UTF-8";
                }
                @Override
                protected com.android.volley.Response<String> parseNetworkResponse(NetworkResponse response) {
                    return super.parseNetworkResponse(response);
                }
            };
            postRequest.setRetryPolicy(new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(postRequest);
            return true;
        }
        @Override
        protected void onProgressUpdate(String... recvMessage) {
            // 이름이 성공적으로 등록되었다는 알림 보내기.
            Toast.makeText(getApplicationContext(),"이름이 등록되었습니다.", Toast.LENGTH_SHORT);
        }
    }


    //페어링 된 디바이스 목록 표시하는 다이얼로그
    public void showPairedDevicesListDialog() {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);
        if (pairedDevices.length == 0) {
            showQuitDialog("No devices have been paired.\n"
                    + "You must pair it with another device.");
            return;
        }
        String[] items;
        items = new String[pairedDevices.length];
        for (int i = 0; i < pairedDevices.length; i++) {
            items[i] = pairedDevices[i].getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select device");
        builder.setCancelable(false);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                ConnectTask task = new ConnectTask(pairedDevices[which]);
                task.execute();
            }
        });
        builder.create().show();
    }

    //에러 표시 다이럴로그
    public void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (isConnectionError) {
                    isConnectionError = false;
                }
            }
        });
        builder.create().show();
    }

    //종료 표시 다이얼로그
    public void showQuitDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    //블루투스 어댑터 요청 관련 onActivityResult
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BLUETOOTH_ENABLE) {
            if (resultCode == RESULT_OK) {
                showPairedDevicesListDialog();
            }
            if (resultCode == RESULT_CANCELED) {
                showQuitDialog("You need to enable bluetooth");
            }
        }
    }


    //프래그먼트 관련 onClick들
    public void onClick_option(View view) {
        OptionActivity optionActivity = new OptionActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, optionActivity).commit();
    }

    public void onClick_timeline(View view) {
        TimelineActivity timelineActivity = new TimelineActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, timelineActivity).commit();
    }

}