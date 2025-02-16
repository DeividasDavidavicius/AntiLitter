package com.example.siukslesv1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.siukslesv1.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    FirebaseAuth auth;
    Button settingsButton;
    Button votedPostsButton;
    Button profileButton;
    FirebaseUser user;

    PostAdapter postAdapter;

    RecyclerView PostRecyclerView;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    DatabaseReference timerReference;
    DatabaseReference eventReference;
    DatabaseReference userReference;
    List<Post> postList;
    TextView timer;
    long votingTime;
    private String email;
    private Event event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
        databaseReference = firebaseDatabase.getReference("posts");
        eventReference = firebaseDatabase.getReference("events");
        userReference = firebaseDatabase.getReference("user");

        if(user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_main);

        PostRecyclerView = findViewById(R.id.postRV);

        TimeZone tz = TimeZone.getTimeZone("Europe/Vilnius");
        Calendar cal = Calendar.getInstance(tz);
        cal.set(2023, 3, 30, 12, 0, 0);
        long voteTime = cal.getTimeInMillis();

        Calendar calendar = Calendar.getInstance(tz);
        long currentTime = calendar.getTimeInMillis();


        while(currentTime > voteTime)
        {
            voteTime += 604800000;
        }

        timer = findViewById(R.id.timer);
        long timerDuration = voteTime - currentTime;
        new CountDownTimer(timerDuration, 1000)
        {

            @Override
            public void onTick(long l) {
                int days = (int)(l/1000/60/60/24);
                int l2 = (int) (l - days * 1000 * 60 * 60 * 24);
                int hours = (int)(l2/1000/60/60);
                int minutes = (int) (l2 - hours * 60 * 60 * 1000)/(1000 * 60);
                int seconds = (int) (l2 - minutes * 60 * 1000 - hours * 60 * 60 * 1000) / 1000;
                timer.setText(days+ "d " + hours + "h "+ minutes + "min " + seconds + "s");
            }

            @Override
            public void onFinish() {timer.setText("Done");
            }
        }.start();


        timerReference = firebaseDatabase.getReference("timer");
        //timerReference.child("timerDate").setValue(cal.getTimeInMillis());
      timerReference.child("timerDate").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                votingTime = dataSnapshot.getValue(Long.class);
                long currentTimeMillis = Calendar.getInstance().getTimeInMillis();

                if(currentTimeMillis > votingTime)
                {
                    while(votingTime < currentTimeMillis)
                    {
                        votingTime +=604800000;
                    }
                    final long[] eventStartTime = {votingTime - 604800000 + 86400000 + 172800000};
                    final int[] eventCount = {0};
                    timerReference.child("timerDate").setValue(votingTime);

                    databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@android.support.annotation.NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                                String postId = postSnapShot.getKey();
                                DatabaseReference postRef = firebaseDatabase.getReference().child("posts").child(postId);
                                postRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@android.support.annotation.NonNull DataSnapshot dataSnapshot) {
                                        Post post = dataSnapshot.getValue(Post.class);
                                        if (post != null) {
                                            int postVoteCount = post.getVoteCount();
                                            if (postVoteCount >= 10)
                                            {
                                                String eventID = UUID.randomUUID().toString();
                                                Calendar calendar = Calendar.getInstance();
                                                long currentEventTime = calendar.getTimeInMillis();
                                                ArrayList<String> usList = new ArrayList<String>();
                                                usList.add("Dovydas");
                                                String userEmail = post.getEmail();

                                                if(eventCount[0] != 0 && eventCount[0] % 3 == 0)
                                                {
                                                    eventStartTime[0] += 64800000;
                                                    eventCount[0]++;
                                                }
                                                else if (eventCount[0] != 0)
                                                {
                                                    eventStartTime[0] += 10800000;
                                                    eventCount[0]++;
                                                }
                                                else if(eventCount[0] == 0)
                                                {
                                                    eventCount[0]++;
                                                }

                                                event = new Event(userEmail, post.getName(), post.getLocation(), post.getUri(), post.getType(), currentEventTime, eventID, usList, eventStartTime[0], eventStartTime[0] + 10800000);
                                                String keyID = eventReference.push().getKey();
                                                eventReference.child(keyID).setValue(event);
                                                postRef.removeValue();

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
                                                                    if(user.getEmail().equals(userEmail))
                                                                    {
                                                                        userRef.child("points").setValue(user.getPoints() + 10);
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
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@android.support.annotation.NonNull DatabaseError databaseError) {
                                    }
                                });
                            }
                        }
                        @Override
                        public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Read cancelled: " + databaseError.getMessage());
            }
        });



        PostRecyclerView.setHasFixedSize(true);
        PostRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList = new ArrayList<>();

                for (DataSnapshot postsnap : snapshot.getChildren()){
                    Post post = postsnap.getValue(Post.class);
                    postList.add(post);
                    int verticalSpaceHeight = getResources().getDimensionPixelSize(R.dimen.vertical_space);
                   PostRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(1));
                }
                postAdapter = new PostAdapter(MainActivity.this,postList);
                Collections.reverse(postList);
                PostRecyclerView.setAdapter(postAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // Initialize and assign variable
        BottomNavigationView bottomNavigationView=findViewById(R.id.bottom_navigation);

        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.home);

        // Perform item selected listener
        bottomNavigationView.setOnItemSelectedListener(item -> {

            switch(item.getItemId())
            {
                case R.id.camera:
                    startActivity(new Intent(getApplicationContext(),CameraActivity.class));
                    overridePendingTransition(0,0);
                    return true;
                case R.id.home:
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
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
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToSettings();
            }
        });

        votedPostsButton = findViewById(R.id.VotedPosts);
        votedPostsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToVotedPosts();
            }
        });


        profileButton = findViewById(R.id.Profile);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToProfile();
            }
        });

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if(user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }

        // NUOTRAUKOS

        //selectedImage = findViewById(R.id.displayImageView);
        //cameraBtn = findViewById(R.id.cameraBtn);
/*
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Camera Btn is Clicked", Toast.LENGTH_SHORT).show();
            }
        }); */

        /*
        Post tempPost;
        email = user.getEmail();
        String eventID = UUID.randomUUID().toString();
        Calendar calendar2 = Calendar.getInstance();
        long currentEventTime = calendar2.getTimeInMillis();
        //Log.d("tag", String.valueOf("Laikas" + currentEventTime));
        tempPost = new Post(email, "Test1223", "Location", "https://firebasestorage.googleapis.com/v0/b/siuksliu-programele.appspot.com/o/images%2FJPEG_20230319_192028.jpg?alt=media&token=ec74a9ca-5983-453e-8c9f-93b0c7f6f656", 1, currentEventTime);
        String keyID = databaseReference.push().getKey();
        databaseReference.child(keyID).setValue(tempPost); */
    }
    private void switchToSettings() {
        Intent switchActivityIntent = new Intent(this, settingsActivity.class);
        startActivity(switchActivityIntent);
    }
    private void switchToProfile() {
        Intent switchActivityIntent = new Intent(this, profileActivity.class);
        startActivity(switchActivityIntent);
    }

    private void switchToVotedPosts() {
        Intent switchActivityIntent = new Intent(this, VotedPosts.class);
        startActivity(switchActivityIntent);
    }
}