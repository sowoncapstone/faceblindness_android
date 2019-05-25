package com.sowon.faceblindness_android;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_BLUETOOTH_ENABLE = 100;
    ConnectedTask mConnectedTask = null;
    static BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    static boolean isConnectionError = false;
    private static final String TAG = "BluetoothClient";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OptionActivity checklistActivity = new OptionActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, checklistActivity).commit();

        mConversationArrayAdapter = new ArrayAdapter<>( this, android.R.layout.simple_list_item_1 );

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            showErrorDialog("This device is not implement Bluetooth.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        }else {
            Log.d(TAG, "Initialisation successful.");
            showPairedDevicesListDialog();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( mConnectedTask != null ) {
            mConnectedTask.cancel(true);
        }
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
                Log.d( TAG, "create socket for "+mConnectedDeviceName);

            }catch (IOException e) {
                Log.e( TAG, "socket create failed " + e.getMessage());
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
            if ( isSucess ) {
                mConnectedTask = new ConnectedTask(mBluetoothSocket);
                mConnectedTask.execute();
            }else{
                isConnectionError = true;
                Log.d( TAG,  "Unable to connect device");
                showErrorDialog("Unable to connect device");
            }
        }
    }


    //연결된 이후 메세지 받기
    private class ConnectedTask extends AsyncTask<Void, String, Boolean> {

        private InputStream mInputStream = null;
        private BluetoothSocket mBluetoothSocket = null;

        ConnectedTask(BluetoothSocket socket){
            mBluetoothSocket = socket;
            try {
                mInputStream = mBluetoothSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "socket not created", e );
            }
            Log.d( TAG, "connected to "+mConnectedDeviceName);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            byte [] readBuffer = new byte[1024];
            int readBufferPosition = 0;
            while (true) {
                if ( isCancelled() ) return false;
                try {
                    int bytesAvailable = mInputStream.available();
                    if(bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mInputStream.read(packetBytes);
                        for(int i=0;i<bytesAvailable;i++) {
                            byte b = packetBytes[i];
                            if(b == '\t' && packetBytes[i+1] == '\n'){
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                        encodedBytes.length);

                                String recvMessage = new String(encodedBytes, "UTF-8");
                                readBufferPosition = 0;
                                publishProgress(recvMessage);
                            }
                            else{
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                }catch (IOException e) {
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
            if ( !isSucess ) {
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

        void closeSocket(){
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
    public void showPairedDevicesListDialog(){
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);
        if ( pairedDevices.length == 0 ){
            showQuitDialog( "No devices have been paired.\n"
                    +"You must pair it with another device.");
            return;
        }
        String[] items;
        items = new String[pairedDevices.length];
        for (int i=0;i<pairedDevices.length;i++) {
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
    public void showErrorDialog(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if ( isConnectionError  ) {
                    isConnectionError = false;
                }
            }
        });
        builder.create().show();
    }


    //종료 표시 다이얼로그
    public void showQuitDialog(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
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
        if(requestCode == REQUEST_BLUETOOTH_ENABLE){
            if (resultCode == RESULT_OK){
                showPairedDevicesListDialog();
            }
            if(resultCode == RESULT_CANCELED){
                showQuitDialog( "You need to enable bluetooth");
            }
        }
    }


    //프래그먼트 관련 onClick들
    public void onClick_option(View view){
        OptionActivity optionActivity = new OptionActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, optionActivity).commit();
    }
    public void onClick_category(View view){
        CategoryActivity categoryActivity = new CategoryActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, categoryActivity).commit();
    }
    public void onClick_time(View view){
        TimeActivity timeActivity = new TimeActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, timeActivity).commit();
    }
    public void onClick_timeline(View view){
        TimelineActivity timelineActivity = new TimelineActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, timelineActivity).commit();
    }
    public void onClick_search(View view){
        SearchActivity searchActivity = new SearchActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, searchActivity).commit();
    }

}