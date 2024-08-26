package com.example.happyhunt.Activity;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.happyhunt.Model.Profile;
import com.example.happyhunt.Util.DBHelper;
import com.example.happyhunt.databinding.ActivityRegistrationBinding;

public class RegistrationActivity extends AppCompatActivity implements View.OnClickListener {

    ActivityRegistrationBinding registrationBinding;
    Intent intentMain;
    Intent intentLogged;
    DBHelper dbh;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbh = new DBHelper(this);

        if (isUserLogged()) {
            intentLogged = new Intent(RegistrationActivity.this, LoggedActivity.class);
            startActivity(intentLogged);
        }
        registrationBinding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        View view = registrationBinding.getRoot();
        setContentView(view);

        registrationBinding.btnBack.setOnClickListener(this);
        registrationBinding.btnSignUp.setOnClickListener(this);
    }

    private boolean isUserLogged() {
        Cursor cursor1 = dbh.readProfile();
        if (cursor1.getCount() == 0) {
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == registrationBinding.btnBack.getId()) {
            intentMain = new Intent(RegistrationActivity.this, MainActivity.class);
            startActivity(intentMain);
        } else if(v.getId() == registrationBinding.btnSignUp.getId()) {
            if(validateData()) {
                Boolean isUserRegistered = searchAccount();
                if (!isUserRegistered) {
                    registerAccount();
                    intentMain = new Intent(RegistrationActivity.this, MainActivity.class);
                    startActivity(intentMain);
                } else {
                    Toast.makeText(this, "User already registered, please login!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private boolean validateData() {
        if (registrationBinding.edtUsername.getText().toString().trim().isEmpty()) {
            registrationBinding.edtUsername.setError("Username is required");
            return false;
        }
        if (registrationBinding.edtEmail.getText().toString().trim().isEmpty()) {
            registrationBinding.edtEmail.setError("Email is required");
            return false;
        }
        if (registrationBinding.edtPassword.getText().toString().trim().isEmpty()) {
            registrationBinding.edtPassword.setError("Password is required");
            return false;
        }
        return true;
    }

    private Boolean searchAccount() {
        Boolean isRegistered = false;
        Cursor cursor1 = dbh.readProfile();
        if (cursor1.moveToFirst()) {
            do {
                @SuppressLint("Range") String userEmail = cursor1.getString(cursor1.getColumnIndex("email"));
                if (registrationBinding.edtEmail.getText().toString().trim().equals(userEmail)) {
                    isRegistered = true;
                    return isRegistered;
                }
            } while (cursor1.moveToNext());
        }
        cursor1.close();
        return isRegistered;
    }

    private void registerAccount() {
        Profile objProfile = new Profile();
        objProfile.setUsername(registrationBinding.edtUsername.getText().toString().trim());
        objProfile.setEmail(registrationBinding.edtEmail.getText().toString().trim());
        objProfile.setPassword(registrationBinding.edtPassword.getText().toString().trim());
        dbh.InsertProfileAccount(objProfile);
    }
}