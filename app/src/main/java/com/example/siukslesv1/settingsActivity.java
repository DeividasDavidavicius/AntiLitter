package com.example.siukslesv1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.sax.Element;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class settingsActivity extends AppCompatActivity {

    private ImageButton backButton;
    private ImageButton contactUsButton;
    private ImageButton aboutUsButton;
    private androidx.appcompat.widget.AppCompatButton logoutButton;
    private androidx.appcompat.widget.SwitchCompat themeBtn;

    FirebaseAuth auth;
    Button buttonLogout;
    FirebaseUser user;
    Button profileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        themeBtn = (androidx.appcompat.widget.SwitchCompat) findViewById(R.id.switchBtn);

        //Element appSettingPrefs: SharedPreferences = getSharedPreferences("AppSettingsPrefs", 0)
        backButton = (ImageButton) findViewById(R.id.homePage1);
        aboutUsButton = (ImageButton) findViewById(R.id.aboutUs);
        logoutButton = (androidx.appcompat.widget.AppCompatButton) findViewById(R.id.logout);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMainActivity();
            }
        });
        aboutUsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAboutUsActivity();
            }
        });
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout(view);
            }
        });

//        int mode = AppCompatDelegate.getDefaultNightMode();
//        if(mode == 2){
//            themeBtn.toggle();
//        }
        themeBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                else{
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });
    }
    public void openMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    public void openAboutUsActivity(){
        Intent intent = new Intent(this, aboutUsActivity.class);
        startActivity(intent);
    }
    public void logout(View view)
    {
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getApplicationContext(), Login.class);
        startActivity(intent);
        finish();
    }
}