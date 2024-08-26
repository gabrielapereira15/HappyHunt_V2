package com.example.happyhunt.Util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.happyhunt.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;

public class LocationHelper {

    private Context mContext;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationListener mListener;
    private static final String TAG = "LocationHelper";
    private final PlacesClient placesClient;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    public interface PlacesCallback {
        void onPlacesReceived(List<Place> places);
    }

    public interface LocationListener {
        void onLocationReceived(Location location);
    }

    public LocationHelper(Context context) {
        this.mContext = context;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        createLocationCallback();

        // Initialize the Places API
        Places.initialize(context, context.getString(R.string.API_KEY));
        placesClient = Places.createClient(context);
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (mListener != null) {
                        mListener.onLocationReceived(location);
                    }
                }
            }
        };
    }

    public void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                PERMISSION_REQUEST_CODE);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mListener != null) {
                    requestLocationUpdates(mListener);
                }
            } else {
                Toast.makeText(mContext, "Location permission is not allowed! This app needs to access your location!", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void requestLocationUpdates(final LocationListener listener) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions((Activity) mContext);
        }
        mListener = listener;
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);

        mFusedLocationClient.requestLocationUpdates(locationRequest,
                mLocationCallback,
                Looper.getMainLooper());
    }

    public void removeLocationUpdates() {
        if (mFusedLocationClient != null && mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    public void getNearbyPlaces(double latitude, double longitude, int radius, PlacesCallback callback) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions((Activity) mContext);
        }

        // Define the fields to specify which types of place data to return
        List<Place.Field> placeFields = new ArrayList<>();
        placeFields.add(Place.Field.NAME);
        placeFields.add(Place.Field.LAT_LNG);
        placeFields.add(Place.Field.ADDRESS);
        placeFields.add(Place.Field.RATING);
        placeFields.add(Place.Field.TYPES);

        // Create a FindCurrentPlaceRequest
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);

        Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
        placeResponse.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
            @Override
            public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                if (task.isSuccessful()) {
                    FindCurrentPlaceResponse response = task.getResult();
                    if (response != null) {
                        List<Place> places = new ArrayList<>();
                        for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                            Place place = placeLikelihood.getPlace();
                            if (place != null) {
                                places.add(place);
                            }
                        }
                        callback.onPlacesReceived(places);
                    } else {
                        Log.e(TAG, "No place data found");
                    }
                } else {
                    Log.e(TAG, "Failed to get place data: " + task.getException().getMessage());
                }
            }
        });
    }
}
