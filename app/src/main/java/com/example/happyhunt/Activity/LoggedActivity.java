package com.example.happyhunt.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import com.example.happyhunt.Util.DBHelper;
import com.example.happyhunt.databinding.ActivityLoggedBinding;

public class LoggedActivity extends AppCompatActivity implements View.OnClickListener {
    
    ActivityLoggedBinding loggedBinding;
    Intent intentMain;
    Intent intentRegistration;
    DBHelper dbh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loggedBinding = ActivityLoggedBinding.inflate(getLayoutInflater());
        View view = loggedBinding.getRoot();
        setContentView(view);
        loggedBinding.btnBack.setOnClickListener(this);
        loggedBinding.btnDeleteAccount.setOnClickListener(this);
        dbh = new DBHelper(this);

        showUserData();
    }

    private void showUserData() {
        Cursor cursor1 = dbh.readProfile();
        if (cursor1.moveToFirst()) {
            @SuppressLint("Range") String userName = cursor1.getString(cursor1.getColumnIndex("username"));
            @SuppressLint("Range") String userEmail = cursor1.getString(cursor1.getColumnIndex("email"));
            String userData = "You are logged :)\n\nUsername:\n" + userName + "\n\n Email:\n" + userEmail;
            loggedBinding.edtAccountDetails.setText(userData);
        }
        cursor1.close();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == loggedBinding.btnBack.getId()) {
            intentMain = new Intent(LoggedActivity.this, MainActivity.class);
            startActivity(intentMain);
        } else if(v.getId() == loggedBinding.btnDeleteAccount.getId()) {
            Cursor cursor1 = dbh.readProfile();
            if (cursor1.moveToFirst()) {
                @SuppressLint("Range") int id = cursor1.getInt(cursor1.getColumnIndex("id"));
                int deleteResponse = dbh.deleteProfile(id);
                if (deleteResponse > 0) {
                    intentRegistration = new Intent(LoggedActivity.this, RegistrationActivity.class);
                    startActivity(intentRegistration);
                }
            }
            cursor1.close();
        }
    }
}