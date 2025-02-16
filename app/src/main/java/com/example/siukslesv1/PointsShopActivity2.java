package com.example.siukslesv1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class PointsShopActivity2 extends AppCompatActivity {

    private TextView pointCountView;
    private FirebaseDatabase database;
    private DatabaseReference userRef, titleRef;
    private int pointCount;
    private ArrayList<String> titles = new ArrayList<>();
    private ArrayList<String> userTitles = new ArrayList<>();
    private DataSnapshot currentSnapshot;
    private static final String USERS = "user";
    private static final String TITLES = "titles";
    String email;
    FirebaseAuth mAuth;

    Button settingsButton;
    Button confirmButton;
    Button buyButton1;
    Button buyButton2;
    Button buyButton3;
    Button buyButton4;

    Button previousButton;

    TextView textView1;
    TextView textView2;
    TextView textView3;
    TextView textView4;

    TextView priceView1;
    TextView priceView2;
    TextView priceView3;
    TextView priceView4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points_shop2);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();
        email = mUser.getEmail();

        database = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
        userRef = database.getReference(USERS);
        titleRef = database.getReference(TITLES);

        settingsButton = findViewById(R.id.Settings);
        buyButton1 = findViewById(R.id.buyButton1);
        buyButton2 = findViewById(R.id.buyButton2);
        buyButton3 = findViewById(R.id.buyButton3);
        buyButton4 = findViewById(R.id.buyButton4);
        previousButton = findViewById(R.id.previousButton);

        textView1 = findViewById(R.id.buyView1);
        textView2 = findViewById(R.id.buyView2);
        textView3 = findViewById(R.id.buyView3);
        textView4 = findViewById(R.id.buyView4);

        priceView1 = findViewById(R.id.priceView1);
        priceView2 = findViewById(R.id.priceView2);
        priceView3 = findViewById(R.id.priceView3);
        priceView4 = findViewById(R.id.priceView4);

        priceView1.setText("100 points");
        priceView2.setText("150 points");
        priceView3.setText("200 points");
        priceView4.setText("250 points");

        pointCountView = findViewById(R.id.pointCountView);

        titleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()) {
                    titles.add(ds.getValue(String.class));
                }

                textView1.setText(titles.get(5));
                textView2.setText(titles.get(6));
                textView3.setText(titles.get(7));
                textView4.setText(titles.get(8));

//                checkAlreadyBought(textView1, buyButton1);
//                checkAlreadyBought(textView2, buyButton2);
//                checkAlreadyBought(textView3, buyButton3);
//                checkAlreadyBought(textView4, buyButton4);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for(DataSnapshot ds : snapshot.getChildren()){

                    if(ds.child("email").getValue().equals(email)){
                        pointCount = ds.child("points").getValue(int.class);
                        String pointCountText = " " + String.valueOf(pointCount);
                        pointCountView.setText(pointCountText);
                        currentSnapshot = ds;

                        userTitles.clear();

                        for (DataSnapshot dsTitle : ds.child("titles").getChildren()){
                            userTitles.add(dsTitle.getValue(String.class));
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        BottomNavigationView bottomNavigationView=findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {

            switch(item.getItemId())
            {
                case R.id.camera:
                    startActivity(new Intent(getApplicationContext(),CameraActivity.class));
                    overridePendingTransition(0,0);
                    return true;
                case R.id.home:
                    startActivity(new Intent(getApplicationContext(),MainActivity.class));
                    overridePendingTransition(0,0);
                    return true;
                case R.id.map:
                    startActivity(new Intent(getApplicationContext(),mapActivity.class));
                    overridePendingTransition(0,0);
                    return true;
            }
            return false;
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToSettings();
            }
        });

        buyButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String key = currentSnapshot.getKey();
                boolean checkSame = false;
                String currentTitle = textView1.getText().toString();

                for(int i = 0; i < userTitles.size(); i++){
                    if(currentTitle.equals(userTitles.get(i))){
                        checkSame = true;
                    }
                }

                if(pointCount >= 100 && !checkSame){
                    pointCount -= 100;
                    userTitles.add(currentTitle);
                    userRef.child(key).child("points").setValue(pointCount);
                    userRef.child(key).child("titles").setValue(userTitles);
                    Toast.makeText(PointsShopActivity2.this, "You successfully bought a title!", Toast.LENGTH_SHORT).show();
                }
                else if (pointCount < 100 && !checkSame){
                    Toast.makeText(PointsShopActivity2.this, "You don't have enough points!", Toast.LENGTH_SHORT).show();
                }
                else if(pointCount >= 100 && checkSame){
                    Toast.makeText(PointsShopActivity2.this, "You already have this title!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(PointsShopActivity2.this, "You already have this title!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buyButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String key = currentSnapshot.getKey();
                boolean checkSame = false;
                String currentTitle = textView2.getText().toString();

                for(int i = 0; i < userTitles.size(); i++){
                    if(currentTitle.equals(userTitles.get(i))){
                        checkSame = true;
                    }
                }

                if(pointCount >= 150 && !checkSame){
                    pointCount -= 150;
                    userTitles.add(currentTitle);
                    userRef.child(key).child("points").setValue(pointCount);
                    userRef.child(key).child("titles").setValue(userTitles);
                    Toast.makeText(PointsShopActivity2.this, "You successfully bought a title!", Toast.LENGTH_SHORT).show();
                }
                else if (pointCount < 150 && !checkSame){
                    Toast.makeText(PointsShopActivity2.this, "You don't have enough points!", Toast.LENGTH_SHORT).show();
                }
                else if(pointCount >= 150 && checkSame){
                    Toast.makeText(PointsShopActivity2.this, "You already have this title!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(PointsShopActivity2.this, "You already have this title!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buyButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String key = currentSnapshot.getKey();
                boolean checkSame = false;
                String currentTitle = textView3.getText().toString();

                for(int i = 0; i < userTitles.size(); i++){
                    if(currentTitle.equals(userTitles.get(i))){
                        checkSame = true;
                    }
                }

                if(pointCount >= 200 && !checkSame){
                    pointCount -= 200;
                    userTitles.add(currentTitle);
                    userRef.child(key).child("points").setValue(pointCount);
                    userRef.child(key).child("titles").setValue(userTitles);
                    Toast.makeText(PointsShopActivity2.this, "You successfully bought a title!", Toast.LENGTH_SHORT).show();
                }
                else if (pointCount < 200 && !checkSame){
                    Toast.makeText(PointsShopActivity2.this, "You don't have enough points!", Toast.LENGTH_SHORT).show();
                }
                else if(pointCount >= 200 && checkSame){
                    Toast.makeText(PointsShopActivity2.this, "You already have this title!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(PointsShopActivity2.this, "You already have this title!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buyButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String key = currentSnapshot.getKey();
                boolean checkSame = false;
                String currentTitle = textView4.getText().toString();

                for(int i = 0; i < userTitles.size(); i++){
                    if(currentTitle.equals(userTitles.get(i))){
                        checkSame = true;
                    }
                }

                if(pointCount >= 250 && !checkSame){
                    pointCount -= 250;
                    userTitles.add(currentTitle);
                    userRef.child(key).child("points").setValue(pointCount);
                    userRef.child(key).child("titles").setValue(userTitles);
                    Toast.makeText(PointsShopActivity2.this, "You successfully bought a title!", Toast.LENGTH_SHORT).show();
                }
                else if (pointCount < 250 && !checkSame){
                    Toast.makeText(PointsShopActivity2.this, "You don't have enough points!", Toast.LENGTH_SHORT).show();
                }
                else if(pointCount >= 250 && checkSame){
                    Toast.makeText(PointsShopActivity2.this, "You already have this title!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(PointsShopActivity2.this, "You already have this title!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToPrevious();
            }
        });
    }
    private void switchToSettings() {
        Intent switchActivityIntent = new Intent(this, settingsActivity.class);
        startActivity(switchActivityIntent);
    }

    private void switchToPrevious() {
        Intent switchActivityIntent = new Intent(this, PointsShopActivity.class);
        startActivity(switchActivityIntent);
    }

    private void checkAlreadyBought(TextView textView, Button button) {
        boolean bought = false;
        String currentTitle = textView.getText().toString();

        for(int i = 0; i < userTitles.size(); i++){
            if(currentTitle.equals(userTitles.get(i))){
                bought = true;
            }
        }

        if(bought == true){
            button.setEnabled(false);
        }
    }
}