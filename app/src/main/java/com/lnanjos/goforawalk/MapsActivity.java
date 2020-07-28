package com.lnanjos.goforawalk;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.lnanjos.models.NearbyPlaces;
import com.lnanjos.models.Result;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private PlacesServices placesServices;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private boolean mLocationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int DEFAULT_ZOOM = 15;
    private Location mLastKnownLocation;
    private static final String TAG = MapsActivity.class.getSimpleName();

    private ImageButton buttonRandom;
    private ImageButton buttonCategories;
    private ImageButton buttonClose;
    private TableLayout chooseWalkLayout;
    private ConstraintLayout distanceLayout;
    private LinearLayout categoriesLayout;
    private SeekBar seekBarDistance;
    private ProgressBar progressBarDistance;
    private ProgressBar progressBarDistanceIndeterminate;
    private TextView textViewDistance;
    private Button buttonStart;
    private Button buttonNext;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        buttonRandom = findViewById(R.id.button_random);
        chooseWalkLayout = findViewById(R.id.choose_walk_layout);
        distanceLayout = findViewById(R.id.distance_layout);
        seekBarDistance = findViewById(R.id.seekBar_distance);
        progressBarDistance = findViewById(R.id.progressBar_distance);
        progressBarDistanceIndeterminate = findViewById(R.id.progressBar_distance_indeterminate);
        textViewDistance = findViewById(R.id.textView_distance);
        buttonClose = findViewById(R.id.button_close);
        buttonStart = findViewById(R.id.button_start);
        buttonNext = findViewById(R.id.button_next);
        buttonCategories = findViewById(R.id.button_categories);
        categoriesLayout = findViewById(R.id.categories_layout);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        buttonRandom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseWalkLayout.setVisibility(View.GONE);
                distanceLayout.setVisibility(View.VISIBLE);
            }
        });

        buttonCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseWalkLayout.setVisibility(View.GONE);
                categoriesLayout.setVisibility(View.VISIBLE);
            }
        });

        buttonClose.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    chooseWalkLayout.setVisibility(View.VISIBLE);
                    distanceLayout.setVisibility(View.GONE);
                    categoriesLayout.setVisibility(View.GONE);
                    progressBarDistance.setVisibility(View.VISIBLE);
                    progressBarDistanceIndeterminate.setVisibility(View.INVISIBLE);
                    seekBarDistance.setAlpha(1f);
                    seekBarDistance.setEnabled(true);
                }
                return false;
            }
        });

        seekBarDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressBarDistance.setProgress(progress);
                textViewDistance.setText(progress + getString(R.string.km));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastKnownLocation != null) {
                    seekBarDistance.setAlpha(0.5f);
                    seekBarDistance.setEnabled(false);
                    progressBarDistance.setVisibility(View.INVISIBLE);
                    progressBarDistanceIndeterminate.setVisibility(View.VISIBLE);
                    searchPlaces(mLastKnownLocation, seekBarDistance.getProgress());
                } else {
                    getDeviceLocation();
                }
            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoriesLayout.setVisibility(View.GONE);
                distanceLayout.setVisibility(View.VISIBLE);
            }
        });

    }

    private void searchPlaces(Location location, int distance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        placesServices = retrofit.create(PlacesServices.class);

        if (distance == 0) {
            distance = 1;
        }

        Call<NearbyPlaces> listCall = placesServices.listRandomPlaces(location.getLatitude() + "," + location.getLongitude(),
                distance * 1000,
                getString(R.string.google_maps_key));

        listCall.enqueue(new Callback<NearbyPlaces>() {
            @Override
            public void onResponse(@NonNull Call<NearbyPlaces> call, @NonNull Response<NearbyPlaces> response) {
                if (response.isSuccessful() && response.body() != null) {

                    Result result = drawPlace(response.body().getResults());
                    assert result != null;
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(result.getGeometry().getLocation().getLat(), result.getGeometry().getLocation().getLng()))
                            .zoom(DEFAULT_ZOOM)
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 3000, null);
                    mMap.addMarker(new MarkerOptions()
                            .title(result.getName())
                            .position(new LatLng(result.getGeometry().getLocation().getLat(), result.getGeometry().getLocation().getLng()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                } else {
                    Log.i("Falhou", response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<NearbyPlaces> call, @NonNull Throwable t) {
                t.printStackTrace();
                Log.i("FALHOU", "FALHOU");
            }
        });
    }

    private boolean verifyTypePolitical(List<String> types) {
        if (!types.isEmpty()) {
            for (String type : types) {
                if (type.equalsIgnoreCase("political")) {
                    return true;
                }
            }
        }
        return false;
    }

    private Result drawPlace(List<Result> results) {

        if (!results.isEmpty()) {
            int drawNumber = ThreadLocalRandom.current().nextInt(results.size());
            if (!verifyTypePolitical(results.get(drawNumber).getTypes())) {
                return results.get(drawNumber);
            } else {
                results.remove(drawNumber);
                return drawPlace(results);
            }
        }
        return null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        try {
            //delay to wait GPS turn on
            Thread.sleep(3000);
            // Get the current location of the device and set the position of the map.
            getDeviceLocation();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", "" + e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Founded location");
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {
                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                        .target(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()))
                                        .zoom(DEFAULT_ZOOM)
                                        .build();
                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 3000, null);
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            getDeviceLocation();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", "" + e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}