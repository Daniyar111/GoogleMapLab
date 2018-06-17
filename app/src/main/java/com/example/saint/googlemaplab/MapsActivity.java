package com.example.saint.googlemaplab;

import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
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
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
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
    private CoordinatorLayout mCoordinatorLayout;

    private LatLng mCosmoPark;
    private LatLng mDestination;
    private ArrayList<LatLng> routeList = new ArrayList<>();
    private float mFraction;
    private Marker mCarMarker;
    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private float mHearing;
    private CameraUpdate cu;
    private boolean isMarkerRotating = false;
    private int counter = 0;
    private LatLng newLocation = null;
    private Polyline mPolyline;
    private int index, next;
    private LatLng startPosition, endPosition;
    private double lat, lng;

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
        mCoordinatorLayout = findViewById(R.id.coordinatorLayout);

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
        mCurrentLocation = new LatLng(42.83405728521415, 74.62109331508032);
        enableMyLocation();
        setMarkerOnCosmoPark();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLocation));
//        mRunnable = new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        };
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
                            for (int i = 0; i < routeList.size(); i++) {
                                Log.d("ROUTE", routeList.get(i).toString());
                            }
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
        mHandler.postDelayed(mRunnable, 3000);
    }

    private void drawRoute(ArrayList<LatLng> routeList) {
        PolylineOptions options = DirectionConverter
                .createPolyline(this, routeList, 5, Color.parseColor("#FF0000"));

        mPolyline = mMap.addPolyline(options);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(mDestination)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        mMap.addMarker(markerOptions);

        animatedCar();
    }

    private void animatedCar(){

//        ValueAnimator carAnimator = ValueAnimator.ofInt(0, 100);
//        carAnimator.setDuration(2000);
//        carAnimator.setInterpolator(new LinearInterpolator());
//        carAnimator.addUpdateListener(animation -> {
//
//            List<LatLng> latLngList = mPolyline.getPoints();
//            int percentValue = (int) carAnimator.getAnimatedValue();
////            mFraction = animation.getAnimatedFraction();
////            Log.d("FRACTION", String.valueOf(mFraction));
//            int size = latLngList.size();
//            int newPoints = (int) (size * (percentValue / 100.0f));
//            List<LatLng> p = latLngList.subList(0, newPoints);
////            mHearing = (float) SphericalUtil.computeHeading(mCosmoPark, mDestination);
////            mCarMarker.setRotation(mHearing);
//
//        });
//        carAnimator.start();
//

        final Handler handler = new Handler();
        index = -1;
        next = 1;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (index < mPolyline.getPoints().size() - 1) {
                    index++;
                    next = index + 1;
                }

                if (index < mPolyline.getPoints().size() - 1) {
                    startPosition = routeList.get(index);
                    endPosition = routeList.get(next);
                }

                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
                valueAnimator.setDuration(3000);
                valueAnimator.setInterpolator(new LinearInterpolator());
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        mFraction = valueAnimator.getAnimatedFraction();
                        lng = mFraction * endPosition.longitude + (1 - mFraction) * startPosition.longitude;
                        lat = mFraction * endPosition.latitude + (1 - mFraction) * startPosition.latitude;

                        LatLng newPos = new LatLng(lat, lng);
                        mCarMarker.setPosition(newPos);
                        mCarMarker.setAnchor(0.5f, 0.5f);
                        mCarMarker.setRotation(bearingBetweenLocations(startPosition, newPos));
                        mMap.moveCamera(CameraUpdateFactory
                                .newCameraPosition
                                        (new CameraPosition.Builder()
                                                .target(newPos)
                                                .zoom(15.5f)
                                                .build()));
                    }
                });
                valueAnimator.start();
                handler.postDelayed(this, 3000);
            }
        });
    }

    private float bearingBetweenLocations(LatLng latLng1, LatLng latLng2) {

        double PI = 3.14159;
        double lat1 = latLng1.latitude * PI / 180;
        double long1 = latLng1.longitude * PI / 180;
        double lat2 = latLng2.latitude * PI / 180;
        double long2 = latLng2.longitude * PI / 180;

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;

        return (float) brng;
    }

    private void rotateMarker(final Marker marker, final float toRotation) {
        if(!isMarkerRotating) {
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final float startRotation = marker.getRotation();
            final long duration = 1000;

            final Interpolator interpolator = new LinearInterpolator();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    isMarkerRotating = true;

                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);

                    float rot = t * toRotation + (1 - t) * startRotation;

                    marker.setRotation(-rot > 180 ? rot / 2 : rot);
                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    } else {
                        isMarkerRotating = false;
                    }
                }
            });
        }
    }

    private void animateCar() {
        final Handler handler = new Handler();

        final long startTime = SystemClock.uptimeMillis();
        final long duration = 3000; // ms

        Projection proj = mMap.getProjection();
        final LatLng markerLatLng = routeList.get(counter);
        counter++;
        Point startPoint = proj.toScreenLocation(markerLatLng);
        startPoint.offset(0, -10);
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);

        final Interpolator interpolator = new BounceInterpolator();

        Log.d("CARDANI", markerLatLng.toString());
        Log.d("CARDANI", startPoint.toString());
        Log.d("CARDANI", startLatLng.toString());
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - startTime;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * markerLatLng.longitude + (1 - t) * startLatLng.longitude;
                double lat = t * markerLatLng.latitude + (1 - t) * startLatLng.latitude;
                mCarMarker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later (60fps)
                    handler.postDelayed(this, 16);
                }
            }
        });
    }
}
