package com.example.happyhunt.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.happyhunt.databinding.ActivityAboutBinding;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {
    
    ActivityAboutBinding aboutBinding;
    Intent intentMain;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aboutBinding = ActivityAboutBinding.inflate(getLayoutInflater());
        View view = aboutBinding.getRoot();
        setContentView(view);

        aboutBinding.btnBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == aboutBinding.btnBack.getId()) {
            intentMain = new Intent(AboutActivity.this, MainActivity.class);
            startActivity(intentMain);
        }
    }
}