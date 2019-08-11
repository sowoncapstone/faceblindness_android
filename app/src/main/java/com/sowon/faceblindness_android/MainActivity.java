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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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

        OptionActivity checklistActivity = new OptionActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, checklistActivity).commit();

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

        TextView welcomeMessage = (TextView) findViewById(R.id.login_check);
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

        @Override
        protected Boolean doInBackground(Void... params) {
            String writeString = "";
            while (true) {
                if (isCancelled()) return false;
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(mInputStream));
                    String bufString = br.readLine();
                    writeString += bufString;

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

                        mRetrofit = new Retrofit.Builder()
                                .baseUrl("http://" + getString(R.string.ip_address) + ":3000")
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
                                Log.d(TAG, result);
                            }

                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                t.printStackTrace();
                            }
                        });

                        writeString = "";
                    }

                } catch (Exception e) {
                    Log.e("ERROR2", e.getMessage());
                }
            }
        }


        @Override
        protected void onProgressUpdate(String... recvMessage) {
            mConversationArrayAdapter.insert(mConnectedDeviceName + ": " + recvMessage[0], 0);
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

    public void onClick_category(View view) {
        CategoryActivity categoryActivity = new CategoryActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, categoryActivity).commit();
    }

    public void onClick_time(View view) {
        TimeActivity timeActivity = new TimeActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, timeActivity).commit();
    }

    public void onClick_timeline(View view) {
        TimelineActivity timelineActivity = new TimelineActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, timelineActivity).commit();
    }

    public void onClick_search(View view) {
        SearchActivity searchActivity = new SearchActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, searchActivity).commit();
    }

}