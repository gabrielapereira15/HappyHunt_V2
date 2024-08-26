package com.example.happyhunt.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.happyhunt.Util.DBHelper;
import com.example.happyhunt.Model.Favorite;
import com.example.happyhunt.Adapter.FavoriteAdapter;
import com.example.happyhunt.R;
import com.example.happyhunt.databinding.ActivityFavoriteBinding;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity {
   ActivityFavoriteBinding favoriteBinding;
    FavoriteAdapter MyAdapter;
    List<Object> dataList = new ArrayList<>();
    DBHelper dbh;
    Intent intentMap;
    Intent intentMain;
    Intent intentAccount;
    Intent intentAbout;
    ActionBarDrawerToggle mToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        favoriteBinding = ActivityFavoriteBinding.inflate(getLayoutInflater());
        View view = favoriteBinding.getRoot();
        setContentView(view);
        dbh = new DBHelper(this);
        init();
    }

    private void init() {
        mToggle = new ActionBarDrawerToggle(this, favoriteBinding.drawerLayout, favoriteBinding.materialToolbar, R.string.nav_open, R.string.nav_close);
        favoriteBinding.drawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();
        setSupportActionBar(favoriteBinding.materialToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SetNavigationDrawer();
        SetBottomNavigation();

        if (isUserLogged()) {
            Cursor cursor1 = dbh.readFavorites();
            if (cursor1 == null || cursor1.getCount() == 0) {
                Toast.makeText(this, "No favorite records found", Toast.LENGTH_LONG).show();
            } else {
                cursor1.moveToFirst();
                do {
                    Favorite favObj = new Favorite();
                    favObj.setId(cursor1.getInt(0));
                    favObj.setPlaceName(cursor1.getString(1));
                    favObj.setPlaceAddress(cursor1.getString(2));
                    favObj.setType(cursor1.getString(3));
                    dataList.add(favObj);
                } while (cursor1.moveToNext());
                cursor1.close();
                dbh.close();
                bindAdapter();
            }
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("No favorite records, please register an account.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            intentMain = new Intent(FavoriteActivity.this, MainActivity.class);
                            startActivity(intentMain);
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private boolean isUserLogged() {
        Cursor cursor1 = dbh.readProfile();
        if (cursor1.getCount() == 0) {
            return false;
        }
        return true;
    }

    private void SetNavigationDrawer() {
        favoriteBinding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId()==R.id.nav_account_menu) {
                    intentAccount = new Intent(FavoriteActivity.this, RegistrationActivity.class);
                    startActivity(intentAccount);
                } else if(item.getItemId()==R.id.nav_about_menu) {
                    intentAbout = new Intent(FavoriteActivity.this, AboutActivity.class);
                    startActivity(intentAbout);
                }
                return false;
            }
        });
    }

    private void SetBottomNavigation() {
        favoriteBinding.bottomNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_bottom_map) {
                    intentMap = new Intent(FavoriteActivity.this, MapActivity.class);
                    startActivity(intentMap);
                } else if (item.getItemId() == R.id.nav_bottom_list) {
                    intentMain = new Intent(FavoriteActivity.this, MainActivity.class);
                    startActivity(intentMain);
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

    private void bindAdapter() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FavoriteActivity.this);
        favoriteBinding.recyclerView.setLayoutManager(layoutManager);
        MyAdapter = new FavoriteAdapter(dataList, FavoriteActivity.this);
        favoriteBinding.recyclerView.setAdapter(MyAdapter);
        MyAdapter.notifyDataSetChanged();
    }
}