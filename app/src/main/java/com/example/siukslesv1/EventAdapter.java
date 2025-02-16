package com.example.siukslesv1;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Handler;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.MyViewHolder>{
    Context mContext;
    List<Event> mData;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference userReference;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private static final double EARTH_RADIUS = 6371;
    String message;
    double latitude;
    double longitude;

    private android.os.Handler handlerr = new android.os.Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            firebaseDatabase = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
            DatabaseReference eventRef = firebaseDatabase.getReference().child("events");
            eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot eventSnapShot : dataSnapshot.getChildren()) {
                        String eventId = eventSnapShot.getKey();
                        EventAdapter.this.deleteEventIfExpired(eventId); ///////////// TEMPORARY
                    }
                }

                @Override
                public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

                }
            });
            handlerr.postDelayed(runnable, 10000000);
        }
    };
    private void startRepeatingTask() {
        runnable.run();
    }
    private void stopRepeatingTask() {
        handlerr.removeCallbacks(runnable);
    }


    public EventAdapter (Context mContext, List<Event> mData)
    {
        this.mContext = mContext;
        this.mData = mData;
    }

    public MyViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType){
        View row = LayoutInflater.from(mContext).inflate(R.layout.row_event_item,parent,false);

        return new MyViewHolder(row);
    }

    @Override
    public void onBindViewHolder (@NonNull MyViewHolder holder, int position){
        // Set a click listener on the ConstraintLayout to open the EventDetailActivity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create an Intent to launch the EventDetailActivity
                Intent intent = new Intent(mContext, EventDetailsActivity.class);

                // Add the event data to the Intent
                intent.putExtra("event_title", mData.get(position).getName());
                intent.putExtra("event_image", mData.get(position).getUri());
                intent.putExtra("event_location", mData.get(position).getLocation());
                intent.putExtra("event_start", mData.get(position).getStart());
                intent.putExtra("event_end", mData.get(position).getEnd());
                intent.putExtra("event_id", mData.get(position).getEventid());

                // Launch the EventDetailActivity
                mContext.startActivity(intent);
            }
        });

        holder.tvTitle.setText(mData.get(position).getName());
        Glide.with(mContext).load(mData.get(position).getUri()).into(holder.imgEvent);
        int clickedPosition = -1;
    }

    @Override
    public int getItemCount(){
        return mData.size();
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ImageView imgEvent;
        public MyViewHolder(View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.row_event_title);
            imgEvent = itemView.findViewById(R.id.row_event_image);
            Button btnJoin = itemView.findViewById(R.id.Join);
            btnJoin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    joinEvent(getAdapterPosition());
                }
            });
        }

        private String getCurrentUserId() {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                return user.getUid();
            } else {
                return "";
            }
        }
    }

    private void deleteEventIfExpired(String eventId) {
        firebaseDatabase = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
        DatabaseReference eventRef = firebaseDatabase.getReference().child("events").child(eventId);
        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Event event = dataSnapshot.getValue(Event.class);
                if (event != null) {
                    Calendar calendar = Calendar.getInstance();
                    long currentTimeMillis = calendar.getTimeInMillis();
                    long eventTimeMillis = event.getTime();
                    if (currentTimeMillis - eventTimeMillis < 604800000) {
                        eventRef.removeValue(); // Delete the event from Firebase
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void joinEvent(int position) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // User is not logged in, show a message and return
            Toast.makeText(mContext, "Please log in to join the event", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = mData.get(position);
        String eventId = event.getEventid();
        firebaseDatabase = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
        // Add the current user to the event's participants
        DatabaseReference eventRef = firebaseDatabase.getReference().child("events");
        Query query = eventRef.orderByChild("eventid").equalTo(eventId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String eventID = dataSnapshot.getChildren().iterator().next().getKey();
                DatabaseReference RefRef = firebaseDatabase.getReference().child("events").child(eventID).child("participants");
                RefRef.runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                        Event event = mData.get(position);
                        if (event == null) {
                            return Transaction.success(mutableData);
                        }

                        // Check if the user is already a participant
                        List<String> participants = event.getParticipants();
                        if (participants != null && participants.contains(currentUser.getUid())) {
                            message = "You have already joined the event";
                            return Transaction.success(mutableData);
                        }

                        long currentTimeMillis = Calendar.getInstance().getTimeInMillis();
                        long start = event.getStart();
                        long end = event.getEnd();
                        String[] arrayOfStr = event.getLocation().split(" ", 5);
                        for (int i = 0; i < arrayOfStr.length; i++)
                        {
                            arrayOfStr[i] = arrayOfStr[i].replace(',', '.');
                        }

                        locationManager = (LocationManager) mContext.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation != null) {
                            latitude = lastKnownLocation.getLatitude();
                            longitude = lastKnownLocation.getLongitude();

                            // Do something with the latitude and longitude
                        }

                        double lat = Double.parseDouble(arrayOfStr[0]);
                        double lon = Double.parseDouble(arrayOfStr[1]);
                        double distance = calculateDistance(lat, lon, latitude, longitude) * 1000;
                        if(distance > 150) {
                            message = "You are not close enough to the event";
                            return Transaction.success(mutableData);
                        }

                        if(currentTimeMillis > end || currentTimeMillis < start) {
                            message = "Event hasn't started yet";
                            return Transaction.success(mutableData);
                        }

                        // Add the user to the participants list
                        if (participants == null) {
                            participants = new ArrayList<>();
                        }
                        userReference = firebaseDatabase.getReference("user");
                        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@android.support.annotation.NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                                    String userId = postSnapShot.getKey();
                                    DatabaseReference userRef = firebaseDatabase.getReference().child("user").child(userId);
                                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@android.support.annotation.NonNull DataSnapshot dataSnapshot) {
                                            User user = dataSnapshot.getValue(User.class);
                                            if(user.getEmail().equals(currentUser.getEmail()))
                                            {
                                                userRef.child("points").setValue(user.getPoints() + 5);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@android.support.annotation.NonNull DatabaseError databaseError) {
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@android.support.annotation.NonNull DatabaseError databaseError) {
                            }
                        });

                        message = "You have joined the event";
                        participants.add(getCurrentUserId());
                        event.setParticipants(participants);
                        mutableData.setValue(event.getParticipants());


                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                        if (databaseError == null) {
                            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, "Failed to join the event: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }


            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

            }
        });
    }
    private String getCurrentUserId() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        } else {
            return "";
        }
    }
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Calculate the differences between the coordinates
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // Apply the Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;

        return distance;
    }
}

