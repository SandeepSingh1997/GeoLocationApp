package com.example.geolocationapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.nearby.messages.Distance;

import java.text.DateFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Button startbutton;
    private Button stopbutton;
    private Button savebutton;
    private TextView textView;
    private TextView textViewinfo;
    private TextView textViewdata;
    private Button showbutton;

    SqliteDBHelper dbHelper;
    SQLiteDatabase db, dbread;

    private ArrayList<String> timestamp;
    private ArrayList<String> latitude;
    private ArrayList<String> longitude;
    private ArrayList<String> accuracy ;

    private ArrayList<String> btDevices;
    private ArrayList<String> btTimestamp;

    int BT_REQ = 200;
    int LOC_PER = 100 ;

    BluetoothAdapter bluetoothAdapter;
    private String  id ="1234567";
    String prev_BT_name ;

    IntentFilter intentFilter;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    //broadcast reciever for bluetooth
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                textView.append("\nBT device : "+bluetoothDevice.getName()+"\n");
                btDevices.add(bluetoothDevice.getName());
                btTimestamp.add(Long.toString( System.currentTimeMillis()) );
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startbutton = (Button) findViewById(R.id.start);
        stopbutton = (Button) findViewById(R.id.stop);

        savebutton = (Button) findViewById(R.id.save);
        textView = (TextView) findViewById(R.id.text_coor);
        textViewinfo = (TextView) findViewById(R.id.info) ;
        textViewdata = (TextView) findViewById(R.id.text_bt);
        showbutton = (Button) findViewById(R.id.show);

         dbHelper = new SqliteDBHelper(getBaseContext());
         db = dbHelper.getWritableDatabase();
         dbread = dbHelper.getReadableDatabase();

         timestamp = new ArrayList<>();
         latitude = new ArrayList<>();
         longitude = new ArrayList<>();
         accuracy = new ArrayList<>();
         btDevices = new ArrayList<>();
         btTimestamp = new ArrayList<>();

        textViewinfo.setText("User : " + id +
                "\nEnable LOCATION and MOBILE DATA to use the app...Ignore if already enabled");
        checkPermissions();
        startbutton.setEnabled(true);
        stopbutton.setEnabled(false);
        enableBluetooth();
        enableButtons();
    }

    private void enableBluetooth(){
        // enable bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //setBluetooth name
        prev_BT_name = bluetoothAdapter.getName();
        bluetoothAdapter.setName(id);

        //checking if enabled
        if(!bluetoothAdapter.isEnabled()){
            Intent btintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btintent, BT_REQ);
        }

        //make your device discoverable
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,600);
        startActivity(intent);

        //register broadcast reciver for discovered devices
        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    }

    private void enableButtons() {

            startbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (fusedLocationProviderClient != null) {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        startbutton.setEnabled(!startbutton.isEnabled());
                        stopbutton.setEnabled(true);
                        bluetoothAdapter.startDiscovery();
                        registerReceiver(receiver, intentFilter);
                    }
                }
            });

            stopbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fusedLocationProviderClient != null) {
                       fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                        stopbutton.setEnabled(!stopbutton.isEnabled());
                        startbutton.setEnabled(true);
                        bluetoothAdapter.cancelDiscovery();
                        unregisterReceiver(receiver);
                    }
                }
            });

            savebutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!timestamp.isEmpty()){

                            for (int j = 0; j < timestamp.size(); j++) {

                                ContentValues values = new ContentValues();
                                values.put(DbSchema.COLUMN_Timestamp, timestamp.get(j));
                                values.put(DbSchema.COLUMN_Long, longitude.get(j));
                                values.put(DbSchema.COLUMN_Lat, latitude.get(j));
                                values.put(DbSchema.COLUMN_Acc, accuracy.get(j));

                                db.insert(DbSchema.TABLE_NAME, null, values);
                            }
                        //remove data from arraylist
                        timestamp.clear();
                        longitude.clear();
                        latitude.clear();
                        accuracy.clear();
                    }

                    if(!btDevices.isEmpty()){
                        for(int i=0; i< btDevices.size(); i++){
                            ContentValues values = new ContentValues();
                            values.put(DbSchema.BT_COLUMN_BtDevice, btDevices.get(i));
                            values.put(DbSchema.BT_COLUMN_TimeStamp, btTimestamp.get(i));
                            db.insert(DbSchema.BT_TABLE_NAME,null, values);
                        }
                        btDevices.clear();
                        btTimestamp.clear();
                    }
                }
            });

            showbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textViewdata.setText(" ");

                    textViewdata.append("\n LOCATION TRAIL :");
                    String[] projection = {DbSchema.COLUMN_Timestamp, DbSchema.COLUMN_Long, DbSchema.COLUMN_Lat};
                    Cursor cursor = dbread.query(DbSchema.TABLE_NAME, projection, null, null, null, null, null);

                    while (cursor.moveToNext()){
                        textViewdata.append("\n" +"time : "+ cursor.getString(cursor.getColumnIndexOrThrow(DbSchema.COLUMN_Timestamp))+
                                        "\n" +"lat : "+ cursor.getString(cursor.getColumnIndexOrThrow(DbSchema.COLUMN_Lat))+
                                "\n" +"lon : "+ cursor.getString(cursor.getColumnIndexOrThrow(DbSchema.COLUMN_Long))+ "\n");
                    }
                    cursor.close();

        /////////////bt info
                    textViewdata.append("\n BLUETOOTH INFO :");
                    String[] projectionbt = {DbSchema.BT_COLUMN_BtDevice, DbSchema.BT_COLUMN_TimeStamp };
                    Cursor cursorbt = dbread.query(DbSchema.BT_TABLE_NAME, projectionbt, null, null, null, null, null);
                    while (cursorbt.moveToNext()){
                        textViewdata.append("\nDevice :" + cursorbt.getString( cursorbt.getColumnIndexOrThrow( DbSchema.BT_COLUMN_BtDevice))+
                                "\nTime :"+ cursorbt.getString( cursorbt.getColumnIndexOrThrow( DbSchema.BT_COLUMN_TimeStamp))+"\n" );
                    }
                    cursorbt.close();
                }
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOC_PER) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                buildLocationRequest();
                buildLocationCallback();
            } else {
                // Permission was denied or request was cancelled
                checkPermissions();
            }
        }
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOC_PER);

        } else {
            //if permissions already granted
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            buildLocationRequest();
            buildLocationCallback();
        }
    }

    private void buildLocationCallback() {

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {

                        String TimeStamp = Long.toString(location.getTime());
                        String Longitude =Double.toString(location.getLongitude());
                        String Latitude = Double.toString(location.getLatitude());
                        String Accuracy = Double.toString(location.getAccuracy());

                        timestamp.add(TimeStamp);
                        longitude.add(Longitude);
                        latitude.add(Latitude);
                        accuracy.add(Accuracy);

                        textView.append("\nTime : " + TimeStamp + " ms \nLat  :" + Latitude +
                                "\nLong :" + Longitude + "\n Accuracy :" + Accuracy+"\n" );
                }
            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(6000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        dbHelper.close();
        db.close();
        dbread.close();

        bluetoothAdapter.setName(prev_BT_name);
        bluetoothAdapter.disable();
        unregisterReceiver(receiver);
    }
}