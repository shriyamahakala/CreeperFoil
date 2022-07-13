package com.example.creeperfoil;


import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleBiFunction;

public class Actions extends AppCompatActivity {
    // Widgets
    public Button btnDis, btnOn, btnOff;
    public TextView dataTW;
    public String address = null;

    // Bluetooth
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBTConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    Thread workerThread;
    byte[] generalBuffer;
    int generalBufferPosition;
    volatile boolean stopWorker;

    private static final String CHANNEL_ID = "CHANNEL_ID";
    private static final String TAG = "main";

    private AtomicInteger notificationId = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent newInt = getIntent();
        //address = newInt.getStringExtra(MainActivity.EXTRA_ADDRESS); // MAC address of the chosen device

        setContentView(R.layout.activity_actions);

        // Initialize widgets
        btnDis = (Button) findViewById(R.id.BTN_disc);
//        btnOn = (Button) findViewById(R.id.BTN_on);
//        btnOff = (Button) findViewById(R.id.BTN_off);
        dataTW = (TextView) findViewById(R.id.TW_data);
        createNotificationChannel();

        new ConnectBT().execute(); // Connection class

        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        btnOn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                turnOn();
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                turnOff();
            }
        });


    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }



    private void turnOff() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(0); // Send data to bt module: 0 (off)
            }
            catch (IOException e) {
                toast("Error Sending Data");
            }
        }
    }

    private void turnOn() {
        if (btSocket!=null) {
            try {
                btSocket.getOutputStream().write(255); // Send data to bt module: 255 (on) [max attainable value]
            }
            catch (IOException e) {
                toast("Error Sending Data");
            }
        }
    }

    // If go back disconnect
    @Override
    public void onBackPressed() {
        disconnect();
        finish();
        return;
    }

    // Disconnection
    private void disconnect() {
        if (btSocket != null) { // If bluetooth socket is taken then disconnect
            try {
                btSocket.close(); // Close bluetooth connection
            } catch (IOException e) {
                toast("Error Closing Socket");
            }
        }
        finish();
    }

    private void toast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void beginListenForData() {
        final Handler handler = new Handler(); // Interacts between this thread and UI thread
        final byte delimiter = 35; // ASCII code for (#) end of transmission

        stopWorker = false;
        generalBufferPosition = 0;
        generalBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {

                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = btSocket.getInputStream().available(); // Received bytes by bluetooth module

                        if (bytesAvailable > 0) {
                            byte[] packet = new byte[bytesAvailable];
                            btSocket.getInputStream().read(packet);

                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packet[i];
                                if (b == delimiter) { // If found a # print on screen
                                    byte[] arrivedBytes = new byte[generalBufferPosition];
                                    System.arraycopy(generalBuffer, 0, arrivedBytes, 0, arrivedBytes.length);
                                    final String data = new String(arrivedBytes, "US-ASCII"); // Decode from bytes to string
                                    generalBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            dataTW.setText(data + " °C"); // Print on screen
                                            sendNotification(notificationId.incrementAndGet());
                                        }
                                    });
                                } else { // If there is no # add bytes to buffer
                                    generalBuffer[generalBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendNotification(int notificationId) {

        // Create an explicit intent for an Activity in your app
        /* Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0); */

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.creeper)
                .setContentTitle("Creeper Alert")
                .setContentText("A creeper has been detected around your cup!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId, builder.build());
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> { // UI thread

        private boolean connectionSuccess = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(Actions.this, "Connecting...", "Please wait!"); // Connection loading dialog
        }

        @Override
        protected Void doInBackground(Void... devices) { // Connect with bluetooth socket

            try {
                if (btSocket == null || !isBTConnected) { // If socket is not taken or device not connected
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice device = myBluetooth.getRemoteDevice(address); // Connect to the chosen MAC address
                    if (ActivityCompat.checkSelfPermission(Actions.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
//                        return;
                    }
                    btSocket = device.createInsecureRfcommSocketToServiceRecord(myUUID); // This connection is not secure (mitm attacks)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery(); // Discovery process is heavy
                    btSocket.connect();
                }
            }
            catch (IOException e) {
                connectionSuccess = false;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) { // After doInBackground
            super.onPostExecute(result);

            if (!connectionSuccess) {
                toast("Connection Failed. Try again.");
                finish();
            }
            else {
                toast("Connected.");
                beginListenForData();
                isBTConnected = true;
            }
            progress.dismiss();
        }
    }
}
