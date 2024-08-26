package com.example.happyhunt.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.happyhunt.Adapter.ListAdapter;
import com.example.happyhunt.R;
import com.example.happyhunt.Util.LocationHelper;
import com.example.happyhunt.databinding.ActivityMainBinding;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ActivityMainBinding mainBinding;
    ActionBarDrawerToggle mToggle;
    Intent intentMap;
    Intent intentAccount;
    Intent intentAbout;
    Intent intentFavorite;
    private LocationHelper locationHelper;
    private Location currentLocation;
    private static final int DEFAULT_RADIUS_METERS = 5000;
    private ArrayList<String> placeTypes = new ArrayList<>();
    boolean isPlaceAround;
    ListAdapter MyAdapter;
    List<Place> dataList = new ArrayList<Place>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = mainBinding.getRoot();
        setContentView(view);

        // Initialize LocationHelper
        locationHelper = new LocationHelper(this);

        // Request device locatization updates
        locationHelper.requestLocationUpdates(new LocationHelper.LocationListener() {
            @Override
            public void onLocationReceived(Location location) {
                currentLocation = location;
                performNearbySearch(currentLocation);
            }
        });

        init();
    }

    private void init() {
        mToggle = new ActionBarDrawerToggle(this, mainBinding.drawerLayout, mainBinding.materialToolbar, R.string.nav_open, R.string.nav_close);
        mainBinding.drawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();

        // Listeners
        mainBinding.searchHeader.foodTypeButton.setOnClickListener(this);
        mainBinding.searchHeader.parkTypeButton.setOnClickListener(this);
        mainBinding.searchHeader.amusementTypeButton.setOnClickListener(this);
        mainBinding.searchHeader.searchButton.setOnClickListener(this);

        setSupportActionBar(mainBinding.materialToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SetNavigationDrawer();
        SetBottomNavigation();
    }

    private void SetNavigationDrawer() {
        mainBinding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId()==R.id.nav_account_menu) {
                    intentAccount = new Intent(MainActivity.this, RegistrationActivity.class);
                    startActivity(intentAccount);
                } else if(item.getItemId()==R.id.nav_about_menu) {
                    intentAbout = new Intent(MainActivity.this, AboutActivity.class);
                    startActivity(intentAbout);
                } else if(item.getItemId()==R.id.nav_favorite_menu) {
                    intentFavorite = new Intent(MainActivity.this, FavoriteActivity.class);
                    startActivity(intentFavorite);
                }
                return false;
            }
        });
    }

    private void SetBottomNavigation() {
        mainBinding.bottomNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_bottom_map) {
                    intentMap = new Intent(MainActivity.this, MapActivity.class);
                    startActivity(intentMap);
                }
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        if(v.getId() == mainBinding.searchHeader.searchButton.getId()) {
            performNearbySearch(currentLocation);
        } else if (v.getId() == mainBinding.searchHeader.foodTypeButton.getId()) {
            populateTypes("food", mainBinding.searchHeader.foodTypeButton);
        } else if (v.getId() == mainBinding.searchHeader.parkTypeButton.getId()) {
            populateTypes("park", mainBinding.searchHeader.parkTypeButton);
        } else if (v.getId() == mainBinding.searchHeader.amusementTypeButton.getId()) {
            populateTypes("amusement_park", mainBinding.searchHeader.amusementTypeButton);
        }
    }

    private void bindAdapter() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        mainBinding.recyclerView.setLayoutManager(layoutManager);
        MyAdapter = new ListAdapter(dataList, MainActivity.this);
        mainBinding.recyclerView.setAdapter(MyAdapter);
        MyAdapter.notifyDataSetChanged();
    }

    private void performNearbySearch(Location location) {
        locationHelper.getNearbyPlaces(location.getLatitude(), location.getLongitude(), DEFAULT_RADIUS_METERS, new LocationHelper.PlacesCallback() {
            @Override
            public void onPlacesReceived(List<Place> places) {
                dataList = new ArrayList<>();
                // Check if the places returned from google api is null or empty
                if (places != null && !places.isEmpty()) {
                    if (placeTypes.isEmpty()) {
                        isPlaceAround = true;
                        dataList = places;
                    } else {
                        // Iterate between each place returned from google api
                        for (Place place : places) {
                            // Check if the customer place type filter is empty
                            // If the customer place type filter is not empty check the google place types
                            for (String apiPlaceType : Objects.requireNonNull(place.getPlaceTypes())) {
                                boolean result = placeTypes.contains(apiPlaceType);
                                if (result) {
                                    dataList.add(place);
                                    isPlaceAround = true;
                                    break;
                                }
                            }
                        }
                    }
                    if(!isPlaceAround) {
                        Toast.makeText(getApplicationContext(), "Oh! No Place founded around you :/", Toast.LENGTH_LONG).show();
                    }
                    bindAdapter();
                    isPlaceAround = false;
                } else {
                    Toast.makeText(getApplicationContext(), "Oh! No Place founded around you :/", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
