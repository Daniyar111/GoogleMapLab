package com.example.saint.googlemaplab;

import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.example.saint.googlemaplab.utils.AppConstants;
import com.example.saint.googlemaplab.utils.PermissionUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng mCurrentLocation;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private EditText mEtTarget;
    private Button mBtnGo;
    private Button mBtnStop;

    private LatLng mCosmoPark;
    private LatLng mDestination;
    private ArrayList<LatLng> routeList = new ArrayList<>();
    private float mFraction;
    private Marker mCarMarker;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mEtTarget = findViewById(R.id.et_target);
        mBtnGo = findViewById(R.id.btn_go);
        mBtnStop = findViewById(R.id.btn_stop);

        mBtnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(mEtTarget.getText().toString())) {
                    getLatLngByAddress(mEtTarget.getText().toString());
                    getRouteList();
                }
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        mCurrentLocation = new LatLng(-34, 151);
        enableMyLocation();
        setMarkerOnCosmoPark();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLocation));
    }

    private void enableMyLocation() {
        if (PermissionUtils.checkLocationPermission(this))
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.setMaxZoomPreference(18);
            }
    }

    private void getMyCurrentLocation() {
        if(PermissionUtils.checkLocationPermission(this)) {
            Task<Location> locationTask = mFusedLocationProviderClient.getLastLocation();

            locationTask.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if(task.getResult() != null) {
                        mCurrentLocation = new LatLng(task.getResult().getLatitude(),
                                task.getResult().getLongitude());
                    } else {
                        Toast.makeText(MapsActivity.this, "No location detected!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == AppConstants.REQUEST_CODE_LOCATION_PERMISSION) {
            for(int result : grantResults) {
                if(result == PackageManager.PERMISSION_GRANTED) {
                    getMyCurrentLocation();
                }
            }
        }
    }

    private void setMarkerOnCosmoPark() {
        mCosmoPark = new LatLng(42.83405728521415, 74.62109331508032);
        MarkerOptions options = new MarkerOptions()
                .position(mCosmoPark)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car_icon))
                .title("CosmoPark");
        mCarMarker = mMap.addMarker(options);
    }

    private void getLatLngByAddress(String address) {
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses = new ArrayList<>();
        try {
            addresses = geocoder.getFromLocationName(address, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(addresses.size() > 0) {
            double latitude= addresses.get(0).getLatitude();
            double longitude= addresses.get(0).getLongitude();
            mDestination = new LatLng(latitude, longitude);
            Log.d("MY_LOG", String.format("lat = %f, lon = %f", latitude, longitude));
        }
    }

    private void getRouteList() {
        GoogleDirection
                .withServerKey(getString(R.string.google_maps_key))
                .from(mCosmoPark)
                .to(mDestination)
                .transitMode(TransportMode.DRIVING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if (direction.isOK()) {
                            routeList = direction
                                    .getRouteList()
                                    .get(0)
                                    .getLegList()
                                    .get(0)
                                    .getDirectionPoint();
                            drawRoute(routeList);
                        } else {
                            /* если на разных материках */
                            Toast.makeText(MapsActivity.this, "Error in direction!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        Toast.makeText(MapsActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void drawRoute(ArrayList<LatLng> routeList) {
        PolylineOptions options = DirectionConverter
                .createPolyline(this, routeList, 5, Color.parseColor("#FF0000"));

        mMap.addPolyline(options);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(mDestination)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        mMap.addMarker(markerOptions);
    }
}
