package com.example.geolocationapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.regex.*;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Button startbutton;
    private Button stopbutton;
    private Button savebutton;
    private Button mapbutton;

    private TextView textView;
    private TextView textViewinfo;

    private Button showbutton;

    SqliteDBHelper dbHelper;
    SQLiteDatabase db, dbread;
    Context activitycont ;

    private ArrayList<String> timestamp;
    private ArrayList<String> latitude;
    private ArrayList<String> longitude;
    private ArrayList<String> accuracy ;

    private ArrayList<String> btDevices;
    private ArrayList<String> btTimestamp;

    int BT_REQ = 200;
    int LOC_PER = 100 ;
    BluetoothAdapter bluetoothAdapter;
    String prev_BT_name ;

    String  id="0000000000";
    int std_id_len = 10;///

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

                if ( isValidId(bluetoothDevice.getName()) ){
                    btDevices.add(bluetoothDevice.getName());
                    btTimestamp.add(Long.toString( System.currentTimeMillis()) );
                }
            }
            ////if bluetooth stops searching
            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                bluetoothAdapter.startDiscovery();
            }
        }
    };



    boolean isValidId(String id){
        boolean ret_val = false;
        if( Pattern.matches("[0-9]{"+ std_id_len + "}", id))
            ret_val = true;
         return ret_val;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get data from login call
        Intent intent = getIntent();
        id = intent.getStringExtra("UID");
        std_id_len = intent.getIntExtra("STD_ID_LEN",10);

        activitycont = this;

        startbutton = (Button) findViewById(R.id.start);
        stopbutton = (Button) findViewById(R.id.stop);
        savebutton = (Button) findViewById(R.id.save);
        mapbutton = (Button)findViewById(R.id.map);

        textView = (TextView) findViewById(R.id.text_coor);
        textViewinfo = (TextView) findViewById(R.id.info) ;

        showbutton = (Button) findViewById(R.id.show);

         dbHelper = new SqliteDBHelper(activitycont);
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

        //what intents the broadcast reciever wants to listen for
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    }

    private void enableButtons() {

            startbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fusedLocationProviderClient != null) {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        startbutton.setEnabled(!startbutton.isEnabled());
                        stopbutton.setEnabled(true);
                        registerReceiver(receiver, intentFilter);
                        bluetoothAdapter.startDiscovery();
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
                        unregisterReceiver(receiver);
                        bluetoothAdapter.cancelDiscovery();
                    }
                }
            });

            mapbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(activitycont, MapsActivity.class);
                    startActivity(intent);
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
                AlertDialog.Builder builder = new AlertDialog.Builder(activitycont);////
                LayoutInflater inflater = getLayoutInflater();

                View dialogview = inflater.inflate(R.layout.show_data,null,false);
                //to change the contents inside dialog
                TextView textdata = (TextView) dialogview.findViewById(R.id.showdatalayout);
                textdata.setText(" ");

                textdata.append("\nLOCATION TRAIL :\n");
                String[] projection = {DbSchema.COLUMN_Timestamp, DbSchema.COLUMN_Long, DbSchema.COLUMN_Lat};
                Cursor cursor = dbread.query(DbSchema.TABLE_NAME, projection, null, null, null, null, null);

                while (cursor.moveToNext()){
                    textdata.append("\n" +"time : "+ cursor.getString(cursor.getColumnIndexOrThrow(DbSchema.COLUMN_Timestamp))+
                            "\n" +"lat  : "+ cursor.getString(cursor.getColumnIndexOrThrow(DbSchema.COLUMN_Lat))+
                            "\n" +"lon  : "+ cursor.getString(cursor.getColumnIndexOrThrow(DbSchema.COLUMN_Long))+ "\n");

                }
                cursor.close();


                /////////////bt info
                textdata.append("\nBLUETOOTH DEVICES IN CONTACT :\n");
                String[] projectionbt = {DbSchema.BT_COLUMN_BtDevice, DbSchema.BT_COLUMN_TimeStamp };
                Cursor cursorbt = dbread.query(DbSchema.BT_TABLE_NAME, projectionbt, null, null, null, null, null);
                while (cursorbt.moveToNext()){
                    textdata.append("\nDevice : " + cursorbt.getString( cursorbt.getColumnIndexOrThrow( DbSchema.BT_COLUMN_BtDevice))+
                            "\nTime   : "+ cursorbt.getString( cursorbt.getColumnIndexOrThrow( DbSchema.BT_COLUMN_TimeStamp))+"\n" );
                }
                cursorbt.close();

               // builder.setView(dialogview);
                //builder.show();
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