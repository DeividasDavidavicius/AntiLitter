package com.example.siukslesv1;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveCanceledListener;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveListener;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class mapActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        OnCameraMoveStartedListener,
        OnCameraMoveCanceledListener,
        OnCameraIdleListener,
        OnCameraMoveListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener {

    Button settingsButton;
    Button profileButton;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    DatabaseReference databaseEventReference;
    private TextView distanceDisplay;
    private GoogleMap map;
    public static final int LOCATION_REQUEST_CODE = 44;
    List<Post> postList = new ArrayList<>();
    List<Event> eventList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
        mapFragment.getMapAsync(this);

        // Initialize and assign variable
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        distanceDisplay = findViewById(R.id.distance);
        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.map);

        // Perform item selected listener
        bottomNavigationView.setOnItemSelectedListener(item -> {

            switch (item.getItemId()) {
                case R.id.camera:
                    startActivity(new Intent(getApplicationContext(), CameraActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.map:
                    return true;
                case R.id.home:
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
            }
            return false;
        });
        settingsButton = findViewById(R.id.Settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToSettings();
            }
        });
        profileButton = findViewById(R.id.Profile);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToProfile();
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        map.setOnCameraIdleListener(this);
        map.setOnCameraMoveStartedListener(this);
        map.setOnCameraMoveListener(this);
        map.setOnCameraMoveCanceledListener(this);

        getCurrentLocation();

        LatLng southWest = new LatLng(53.8965, 20.653);
        LatLng northEast = new LatLng(56.4464, 26.835);

        LatLngBounds bounds = new LatLngBounds(southWest, northEast);

        map.setLatLngBoundsForCameraTarget(bounds);
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
        map.animateCamera(CameraUpdateFactory.zoomTo(7));


        firebaseDatabase = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
        databaseReference = firebaseDatabase.getReference("posts");
        databaseEventReference = firebaseDatabase.getReference("events");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot markerSnap : snapshot.getChildren()){
                    Post post = markerSnap.getValue(Post.class);
                    postList.add(post);
                    assert post != null;
                    String[] arrayOfStr = post.getLocation().split(" ", 5);
                    for (int i = 0; i < arrayOfStr.length; i++)
                    {
                        arrayOfStr[i] = arrayOfStr[i].replace(',', '.');
                    }
                    double lat = Double.parseDouble(arrayOfStr[0]);
                    double lon = Double.parseDouble(arrayOfStr[1]);
                    LatLng position = new LatLng(lat, lon);
                    map.addMarker(new MarkerOptions()
                            .position(position)
                            .icon(BitmapDescriptorFactory.fromBitmap(resizeBitmap("trash", 112, 116)))
                            .title(post.getName())
                            .snippet("Click me to see more info"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        databaseEventReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot markerEvent : snapshot.getChildren()) {
                    Event event = markerEvent.getValue(Event.class);
                    eventList.add(event);
                    assert event != null;
                    String[] arrayOfStr = event.getLocation().split(" ", 5);
                    for (int i = 0; i < arrayOfStr.length; i++)
                    {
                        arrayOfStr[i] = arrayOfStr[i].replace(',', '.');
                    }
                    double lat = Double.parseDouble(arrayOfStr[0]);
                    double lon = Double.parseDouble(arrayOfStr[1]);
                    LatLng position = new LatLng(lat, lon);
                    map.addMarker(new MarkerOptions()
                            .position(position)
                            .icon(BitmapDescriptorFactory.fromBitmap(resizeBitmap("event", 156, 156)))
                            .title(event.getName())
                            .snippet("Event. Click for more info")
                    );
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnInfoWindowClickListener(this);
    }

    @Override
    public void onCameraMoveStarted(int i) {

    }

    @Override
    public void onCameraIdle() {

    }

    @Override
    public void onCameraMoveCanceled() {

    }

    @Override
    public void onCameraMove() {

    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {

        if (checkPermissions()) {

            if (isLocationEnabled()) {
                map.setMyLocationEnabled(true);
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {
        // Create an Intent to launch the PostDetailActivity
        Intent intentPost = new Intent(this, PostDetailsActivity.class);
        Intent intentEvent = new Intent(this, EventDetailsActivity.class);
        // Add the post data to the Intent
        boolean post = false;
        boolean event = false;
        for (int i = 0; i < postList.size(); i++)
        {
            if (Objects.equals(marker.getTitle(), postList.get(i).getName())) {
                intentPost.putExtra("post_title", postList.get(i).getName());
                intentPost.putExtra("post_votes", String.valueOf(postList.get(i).getVoteCount()));
                intentPost.putExtra("post_image", postList.get(i).getUri());
                intentPost.putExtra("post_location", postList.get(i).getLocation());
                post = true;
            }
        }
        for (int i = 0; i < eventList.size(); i++)
        {
            if (Objects.equals(marker.getTitle(), eventList.get(i).getName())) {
                intentEvent.putExtra("event_title", eventList.get(i).getName());
                intentEvent.putExtra("event_image", eventList.get(i).getUri());
                intentEvent.putExtra("event_location", eventList.get(i).getLocation());
                intentEvent.putExtra("event_start", eventList.get(i).getStart());
                intentEvent.putExtra("event_end", eventList.get(i).getEnd());
                intentEvent.putExtra("event_id", eventList.get(i).getEventid());
                event = true;
            }
        }
        // Launch the PostDetailActivity
        if (post)
        {
            startActivity(intentPost);
        }
        else if (event)
        {
            startActivity(intentEvent);
        }
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f));
        if (checkPermissions())
        {
            if (isLocationEnabled())
            {
                Location markerLoc = new Location("Marker");
                markerLoc.setLatitude(marker.getPosition().latitude);
                markerLoc.setLongitude(marker.getPosition().longitude);
                double distance = map.getMyLocation().distanceTo(markerLoc);
                distance = distance / 1000;
                String distanceString = String.format("%.2f",distance);
                distanceDisplay.setText("Distance: " + distanceString + "km");
            }
        }
        return false;
    }
    public Bitmap resizeBitmap(String drawableName,int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(drawableName, "drawable", getPackageName()));
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false);
    }

}