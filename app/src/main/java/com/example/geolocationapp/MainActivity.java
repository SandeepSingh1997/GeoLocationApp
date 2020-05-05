package com.example.geolocationapp;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;

import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

public class MainActivity extends AppCompatActivity {

    private Button startbutton;
    private Button stopbutton;
    private TextView textView;
    private TextView textViewinfo;
    private TextView textViewbt;
    int BT_REQ = 200;
    int LOC_PER = 100 ;

    BluetoothAdapter bluetoothAdapter;
    private String  id ="7042516805";

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    java.util.Date date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startbutton = (Button) findViewById(R.id.start);
        stopbutton = (Button) findViewById(R.id.stop);
        textView = (TextView) findViewById(R.id.text_coor);
        textViewinfo = (TextView) findViewById(R.id.info) ;
        textViewbt = (TextView) findViewById(R.id.text_bt);

        textViewinfo.setText("User : " + id +
                "\nPlease enable LOCATION and MOBILE DATA to use the app..." +
                "\n Ignore if already enabled");

        checkPermissions();
        startbutton.setEnabled(true);
        stopbutton.setEnabled(false);
        enableButtons();
    }

    private void enableButtons() {
        // enable bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //setBluetooth name
        bluetoothAdapter.setName(id);

        //checking if enabled
        if(!bluetoothAdapter.isEnabled()){
        Intent btintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(btintent, BT_REQ);}

        //make your device discoverable
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,100);
        startActivity(intent);

        //register broadcast reciver for discovered devices
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, intentFilter);

        // start searching
        bluetoothAdapter.startDiscovery();

            startbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fusedLocationProviderClient != null) {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        startbutton.setEnabled(!startbutton.isEnabled());
                        stopbutton.setEnabled(true);
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
                    }
                }
            });
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    textViewbt.append("\n " + bluetoothDevice.getName() + "\n ");
                }
            }catch (NullPointerException e){}
        }
    } ;

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
                    date = new java.util.Date(location.getTime());
                    textView.append("\nTime : >> "+date+" \nLat  :"+location.getLatitude() + "\nLong :" + location.getLongitude());
                }
            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.cancelDiscovery();
        unregisterReceiver(receiver);
    }
}