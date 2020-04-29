package com.example.geolocationapp;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    private Button startbutton;
    private Button stopbutton;
    private TextView textView;
    private TextView textViewinfo;
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

        textViewinfo.setText("User : 112233\n " +
                "please enable LOCATION and MOBILE DATA to use the app..." +
                "\n Ignore if already enabled");

        checkPermissions();
        startbutton.setEnabled(true);
        stopbutton.setEnabled(false);
        enableButtons();
    }

    private void enableButtons() {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 100) {
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);

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
                    textView.append("\n >> "+date+" :\n"+location.getLatitude() + "\n" + location.getLongitude());
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


}