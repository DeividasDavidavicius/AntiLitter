package com.example.siukslesv1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ProfileEditActivity extends AppCompatActivity {

    private TextInputEditText editTextUsername;
    private Spinner spinner;
    private FirebaseDatabase database;
    private DatabaseReference userRef;
    private static final String USERS = "user";
    private ArrayList<String> titles = new ArrayList<>();
    private DataSnapshot currentSnapshot;
    String email;
    FirebaseAuth mAuth;

    Button settingsButton;
    Button confirmButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();
        email = mUser.getEmail();

        database = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
        userRef = database.getReference(USERS);

        editTextUsername = findViewById(R.id.username);
        spinner = findViewById(R.id.titleSpinner);

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()) {

                    if (ds.child("email").getValue().equals(email)) {
                        String usernameTemp = ds.child("username").getValue(String.class);
                        editTextUsername.setText(usernameTemp);
                        currentSnapshot = ds;

                        for (DataSnapshot dsTitle : ds.child("titles").getChildren()){
                            titles.add(dsTitle.getValue(String.class));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        titles.clear();
        titles.add("Choose title");

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, titles);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        // Initialize and assign variable
        BottomNavigationView bottomNavigationView=findViewById(R.id.bottom_navigation);

        // Perform item selected listener
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

        settingsButton = findViewById(R.id.Settings);
        confirmButton = findViewById(R.id.ConfirmChangesButton);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToSettings();
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String key = currentSnapshot.getKey();

                boolean checkUpdateUsername = false;
                boolean checkUpdateTitle = false;


                String updatedTitle = spinner.getSelectedItem().toString();
                String updatedUsername = editTextUsername.getText().toString();

                if(!updatedTitle.equals("Choose title")){
                    userRef.child(key).child("title").setValue(updatedTitle);
                    checkUpdateTitle = true;
                }
                else{
                    Toast.makeText(ProfileEditActivity.this, "You have to choose a title!", Toast.LENGTH_SHORT).show();
                }

                if(!updatedUsername.equals("")){
                    userRef.child(key).child("username").setValue(updatedUsername);
                    checkUpdateUsername = true;
                }
                else{
                    Toast.makeText(ProfileEditActivity.this, "Username can't be empty!", Toast.LENGTH_SHORT).show();
                }

                if(checkUpdateTitle && checkUpdateUsername){
                    switchToProfile();
                }
            }
        });
    }
    private void switchToSettings() {
        Intent switchActivityIntent = new Intent(this, settingsActivity.class);
        startActivity(switchActivityIntent);
    }

    private void switchToProfile() {
        Intent switchActivityIntent = new Intent(this, profileActivity.class);
        startActivity(switchActivityIntent);
    }
}