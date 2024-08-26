package com.example.happyhunt.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.happyhunt.Util.LocationHelper;
import com.example.happyhunt.R;
import com.example.happyhunt.databinding.ActivityMapBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LocationHelper.LocationListener, View.OnClickListener {
    private GoogleMap myMap;
    ActivityMapBinding mapBinding;
    ActionBarDrawerToggle mToggle;
    Intent intentMain;
    Intent intentAbout;
    Intent intentAccount;
    Intent intentFavorite;
    private LocationHelper locationHelper;
    private static final int DEFAULT_RADIUS_METERS = 5000;
    private Location currentLocation;
    private ArrayList<String> placeTypes = new ArrayList<>();
    boolean isPlaceAround;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapBinding = ActivityMapBinding.inflate(getLayoutInflater());
        View view = mapBinding.getRoot();
        setContentView(view);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);

        locationHelper = new LocationHelper(this);

        init();
    }

    private void performSearch() {
        if (currentLocation != null) {
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
            performNearbySearch(currentLocation);
        }
    }

    private void init() {
        mToggle = new ActionBarDrawerToggle(this, mapBinding.drawerLayout, mapBinding.materialToolbar, R.string.nav_open, R.string.nav_close);
        mapBinding.drawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();

        // Listeners
        mapBinding.searchHeader.foodTypeButton.setOnClickListener(this);
        mapBinding.searchHeader.parkTypeButton.setOnClickListener(this);
        mapBinding.searchHeader.amusementTypeButton.setOnClickListener(this);
        mapBinding.searchHeader.searchButton.setOnClickListener(this);

        setSupportActionBar(mapBinding.materialToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SetNavigationDrawer();
        SetBottomNavigation();
    }

    private void SetNavigationDrawer() {
        mapBinding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId()==R.id.nav_account_menu) {
                    intentAccount = new Intent(MapActivity.this, RegistrationActivity.class);
                    startActivity(intentAccount);
                } else if(item.getItemId()==R.id.nav_about_menu) {
                    intentAbout = new Intent(MapActivity.this, AboutActivity.class);
                    startActivity(intentAbout);
                } else if(item.getItemId()==R.id.nav_favorite_menu) {
                    intentFavorite = new Intent(MapActivity.this, FavoriteActivity.class);
                    startActivity(intentFavorite);
                }
                return false;
            }
        });
    }

    private void SetBottomNavigation() {
        mapBinding.bottomNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId()==R.id.nav_bottom_list) {
                    intentMain = new Intent(MapActivity.this, MainActivity.class);
                    startActivity(intentMain);
                }
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(mToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        locationHelper.requestLocationUpdates(this);
    }

    @Override
    public void onLocationReceived(Location location) {
        if (location != null) {
            currentLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions options = new MarkerOptions().position(latLng).title("Current Location");
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            myMap.addMarker(options);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationHelper.removeLocationUpdates();
    }

    private void performNearbySearch(Location location) {
        locationHelper.getNearbyPlaces(location.getLatitude(), location.getLongitude(), DEFAULT_RADIUS_METERS, new LocationHelper.PlacesCallback() {
            @Override
            public void onPlacesReceived(List<Place> places) {
                // Check if the places returned from google api is null or empty
                if (places != null && !places.isEmpty()) {
                    // Clear previous markers
                    myMap.clear();
                    // Iterate between each place returned from google api
                    for (Place place : places) {
                        // Check if the customer place type filter is empty
                        if (placeTypes.isEmpty()) {
                            // Plot the retrieved places on the map
                            LatLng placeLatLng = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                            myMap.addMarker(new MarkerOptions().position(placeLatLng).title(place.getName()));
                            isPlaceAround = true;
                        } else {
                            // If the customer place type filter is not empty check the google place types
                            for (String apiPlaceType : Objects.requireNonNull(place.getPlaceTypes())) {
                                boolean result = placeTypes.contains(apiPlaceType);
                                if (result) {
                                    // Plot the retrieved places on the map
                                    LatLng placeLatLng = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                                    myMap.addMarker(new MarkerOptions().position(placeLatLng).title(place.getName()));
                                    isPlaceAround = true;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Oh! No Place founded around you :/", Toast.LENGTH_LONG).show();
                }
                if(!isPlaceAround) {
                    Toast.makeText(getApplicationContext(), "Oh! No Place founded around you :/", Toast.LENGTH_LONG).show();
                }
                isPlaceAround = false;
            }
        });
    }

    private void populateTypes (String type, ImageButton button) {
        if (placeTypes.contains(type)) {
            placeTypes.remove(type);
            button.setBackgroundColor(Color.LTGRAY);
        } else {
            button.setBackgroundColor(Color.DKGRAY);
            placeTypes.add(type);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == mapBinding.searchHeader.searchButton.getId()) {
            performSearch();
        } else if (v.getId() == mapBinding.searchHeader.foodTypeButton.getId()) {
            populateTypes("food", mapBinding.searchHeader.foodTypeButton);
        } else if (v.getId() == mapBinding.searchHeader.parkTypeButton.getId()) {
            populateTypes("park", mapBinding.searchHeader.parkTypeButton);
        } else if (v.getId() == mapBinding.searchHeader.amusementTypeButton.getId()) {
            populateTypes("amusement_park", mapBinding.searchHeader.amusementTypeButton);
        }
    }
}