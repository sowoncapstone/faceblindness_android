package com.sowon.faceblindness_android;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_BLUETOOTH_ENABLE = 100;
    ConnectedTask mConnectedTask = null;
    static BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    static boolean isConnectionError = false;
    private String wholeImage = "";
    private JSONObject jsonObject;
    private String encodings;


    private static final String TAG = "BluetoothClient";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mConnectedTask != null) {
            mConnectedTask.cancel(true);
        }
    }

    //프래그먼트에 전달하는 인터페이스를 위한 메소드
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
                Log.d(TAG, "create socket for " + mConnectedDeviceName);

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


    //연결된 이후 메세지 받기
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
            // 이미지 한 개 담을 스트링

            byte[] readBuffer = new byte[1024];
            int readBufferPosition = 0;
            while (true) {
                if (isCancelled()) return false;
                try {
                    int bytesAvailable = mInputStream.available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mInputStream.read(packetBytes);
                        Log.d(TAG, "*********************rcv success************************");
                        for (int i = 0; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            if (b == '\t' && packetBytes[i + 1] == '\n') {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                        encodedBytes.length);
                                String recvMessage = new String(encodedBytes, "UTF-8");
                                // 파일의 끝을 의미하는 바이트 찾을 때까지
                                // 이미지 계속 추가.
                                wholeImage += recvMessage;

                                if (recvMessage.contains("ENDOFFILE")) {
                                    Log.d(TAG, "*********************END OF FILE FOUND************************");
                                    Log.d(TAG, wholeImage);

                                    try {
                                        Log.d("json", "*********************Action Start************************");

                                        //JSONObject를 만들고 key value 형식으로 값을 저장해준다.
                                        jsonObject = new JSONObject();


                                        jsonObject.accumulate("user_id", "soyoung");
                                        //jsonObject.accumulate("name", "why not working?");
                                        //Log.d("json", encodings);

                                        jsonObject.accumulate("name", wholeImage);

                                        Log.d("json", "*********************JSON FILE Created************************");


                                        HttpURLConnection con = null;
                                        BufferedReader reader = null;

                                        try {
                                            URL url = new URL("http://3.16.82.152:3000/post");
                                            //URL url = new URL(urls[0]);
                                            //연결을 함
                                            con = (HttpURLConnection) url.openConnection();

                                            con.setRequestMethod("POST");//POST방식으로 보냄
                                            con.setRequestProperty("Cache-Control", "no-cache");//캐시 설정
                                            con.setRequestProperty("Content-Type", "application/json");//application JSON 형식으로 전송
                                            con.setRequestProperty("Accept", "text/html");//서버에 response 데이터를 html로 받음
                                            con.setDoOutput(true);//Outstream으로 post 데이터를 넘겨주겠다는 의미
                                            con.setDoInput(true);//Inputstream으로 서버로부터 응답을 받겠다는 의미
                                            con.connect();
                                            Log.d("URL CONNECTION", "*********************success************************");
                                            //서버로 보내기위해서 스트림 만듬
                                            OutputStream outStream = con.getOutputStream();
                                            //버퍼를 생성하고 넣음
                                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream));
                                            writer.write(jsonObject.toString());
                                            writer.flush();
                                            writer.close();//버퍼를 받아줌
                                            Log.d("URL CONNECTION", "*********************SENT************************");

                                            wholeImage = "";
                                            //서버로 부터 데이터를 받음
                                            InputStream stream = con.getInputStream();

                                            reader = new BufferedReader(new InputStreamReader(stream));

                                            StringBuffer buffer = new StringBuffer();

                                            String line = "";
                                            while ((line = reader.readLine()) != null) {
                                                buffer.append(line);
                                            }

                                        } catch (MalformedURLException e) {
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } finally {
                                            if (con != null) {
                                                con.disconnect();
                                            }
                                            try {
                                                if (reader != null) {
                                                    reader.close();//버퍼를 닫아줌
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }


                                readBufferPosition = 0;
                                publishProgress(recvMessage);
                            } else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }


                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    return false;
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

    //서버 접속하는 네트워크 커넥션 만들기
    public class NetworkTask extends AsyncTask<Void, Void, String> {

        private String url;
        private ContentValues values;

        public NetworkTask(String url, ContentValues values) {

            this.url = url;
            this.values = values;
        }

        @Override
        protected String doInBackground(Void... params) {

            String result; // 요청 결과를 저장할 변수.
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, values); // 해당 URL로 부터 결과물을 얻어온다.

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            //doInBackground()로 부터 리턴된 값이 onPostExecute()의 매개변수로 넘어오므로 s를 출력한다.
            //결과값 표시
            //tv_outPut.setText(s);
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