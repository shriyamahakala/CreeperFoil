package com.example.creeperfoil;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter btAdapter = null;
    BluetoothSocket btSocket = null;

    private static final int BLUETOOTH_CONNECT_CODE = 100;

    // Widgets
    public Button btnDis, btnOn, btnOff;

    Thread workerThread;
    byte[] generalBuffer;
    int generalBufferPosition;
    volatile boolean stopWorker;
    boolean on = true;

    private static final String CHANNEL_ID = "CHANNEL_ID";
    private static final String TAG = "main";

    private AtomicInteger notificationId = new AtomicInteger(0);
    int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        btnDis = (Button) findViewById(R.id.BTN_disc);
//        btnOn = (Button) findViewById(R.id.BTN_on);
//        btnOff = (Button) findViewById(R.id.BTN_off);



        createNotificationChannel();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actions);

        TextView dataTW = (TextView) findViewById(R.id.TW_data);


        btAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothDevice hc05 = btAdapter.getRemoteDevice("98:D3:41:FD:6C:5C");
        System.out.println(hc05.getName());

        btSocket = null;


        try {
            btSocket = hc05.createInsecureRfcommSocketToServiceRecord(mUUID);
            System.out.println(btSocket);
            btSocket.connect();
            Thread.sleep(3000);
            System.out.println(btSocket.isConnected());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    StringBuilder string = new StringBuilder();
                    boolean noHash = true;
                    InputStream inputStream = null;
                    try {
                        inputStream = btSocket.getInputStream();
                        inputStream.skip(inputStream.available());
                        string.append(Calendar.getInstance().getTime());
                        while (noHash) {
                            byte b = (byte) inputStream.read();

                            if (((char) b) == '#') {
                                noHash = false;
                            }
                            string.append((char) b);
                        }
                        string.append('\n');
                        System.out.print(string);
                        handler.post(new Runnable() {
                            public void run() {
                                dataTW.setText(string.toString());
                                sendNotification(notificationId.incrementAndGet());
                            }
                        });
                        string.setLength(0);
                        noHash = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

//        btnDis.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    btSocket.close();
//                    System.out.println(btSocket.isConnected());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });

//        btnOn.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                turnOn();
//            }
//        });

//        btnOff.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v)
//            {
//                turnOff();
//            }
//        });


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

//    private void toast(String s) {
//        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
//    }
//
//    public void beginListenForData() {
//        final Handler handler = new Handler(); // Interacts between this thread and UI thread
//        final byte delimiter = 35; // ASCII code for (#) end of transmission
//
//        stopWorker = false;
//        generalBufferPosition = 0;
//        generalBuffer = new byte[1024];
//        workerThread = new Thread(new Runnable() {
//            public void run() {
//                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
//                    try {
//                        int bytesAvailable = btSocket.getInputStream().available(); // Received bytes by bluetooth module
//                        if (bytesAvailable > 0) {
//                            byte[] packet = new byte[bytesAvailable];
//                            btSocket.getInputStream().read(packet);
//                            System.out.println(bytesAvailable);
//                            for (int i = 0; i < bytesAvailable; i++) {
//                                byte b = packet[i];
//                                if (b == delimiter) { // If found a # print on screen
//                                    byte[] arrivedBytes = new byte[generalBufferPosition];
//                                    System.arraycopy(generalBuffer, 0, arrivedBytes, 0, arrivedBytes.length);
//                                    final String data = new String(arrivedBytes, "US-ASCII"); // Decode from bytes to string
//                                    generalBufferPosition = 0;
//
//                                    handler.post(new Runnable() {
//                                        public void run() {
//                                            dataTW.setText(data); // Print on screen
//                                            //sendNotification(notificationId.incrementAndGet());
//                                        }
//                                    });
//                                } else { // If there is no # add bytes to buffer
//                                    generalBuffer[generalBufferPosition++] = b;
//                                }
//                            }
//                        }
//                    } catch (IOException ex) {
//                        stopWorker = true;
//                    }
//                }
//            }
//        });
//
//        workerThread.start();
//    }
//
//    private void disconnect() {
//        try {
//            System.out.println(btSocket.isConnected());
//            btSocket.close();
//            System.out.println(btSocket.isConnected());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        finish();

//    }
//
//    private void turnOff() {
//        if (btSocket != null) {
//            try {
//                btSocket.getOutputStream().write(0); // Send data to bt module: 0 (off)
//            }
//            catch (IOException e) {
//                toast("Error Sending Data");
//            }
//        }
//    }
//
//    private void turnOn() {
//        if (btSocket!=null) {
//            try {
//                btSocket.getOutputStream().write(255); // Send data to bt module: 255 (on) [max attainable value]
//            }
//            catch (IOException e) {
//                toast("Error Sending Data");
//            }
//        }
//    }

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
}


//package com.example.creeperfoil;
//
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//
//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.ListView;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import java.util.ArrayList;
//import java.util.Set;
//
//
//public class MainActivity extends AppCompatActivity {
//    // Widgets
//    Button btnPaired;
//    ListView devicelist;
//
//    private BluetoothAdapter myBluetooth = null;
//    private Set<BluetoothDevice> pairedDevices;
//    public static String EXTRA_ADDRESS = "device_address";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main_activity);
//
//        // Initialize widgets
//        btnPaired = (Button) findViewById(R.id.paired_dev_btn);
//        devicelist = (ListView) findViewById(R.id.paired_dev_listview);
//
//        // Initialize bluetooth adapter
//        myBluetooth = BluetoothAdapter.getDefaultAdapter();
//
//        if (myBluetooth == null) { // This device has not bluetooth adapter
//            Toast.makeText(getApplicationContext(), "Bluetooth Adapter Not Available", Toast.LENGTH_LONG).show();
//            finish();
//        } else if (!myBluetooth.isEnabled()) { // This device has bluetooth adapter but turned off
//            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return;
//            }
//            startActivityForResult(turnBTon, 1); // Intent to turn on bluetooth adapter
//        }
//
//        btnPaired.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                pairedDevicesList();
//            }
//        });
//
//    }
//
//    // List with paired bluetooth devices
//    private void pairedDevicesList() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        pairedDevices = myBluetooth.getBondedDevices();
//        ArrayList list = new ArrayList();
//
//        if (pairedDevices.size() > 0) // ArrayList with name and MAC address of paired devices
//            for(BluetoothDevice bt : pairedDevices)
//                list.add(bt.getName() + "\n" + bt.getAddress());
//        else
//            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
//
//
//        // Display paired devices in the listview
//        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
//        devicelist.setAdapter(adapter);
//        devicelist.setOnItemClickListener(myListClickListener);
//
//    }
//
//    // When a paired device is clicked
//    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
//        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3) {
//            // MAC address are last 17 characters of the textview clicked
//            String info = ((TextView) v).getText().toString();
//            String address = info.substring(info.length() - 17);
//
//            Intent i = new Intent(MainActivity.this, Actions.class);
//
//            // Send to next activity the MAC address of the chosen device
//            i.putExtra(EXTRA_ADDRESS, address);
//            startActivity(i);
//        }
//    };
//
//}