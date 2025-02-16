package com.example.siukslesv1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.net.URI;

public class PostDetailsActivity extends AppCompatActivity {
    private TextView mTitleTextView;
    private TextView mVotesTextView;
    private TextView locationTextView;

    private ImageView imagePostView;
    FirebaseDatabase firebaseDatabase;
    Button voteButton;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        // Get the post data from the Intent
        String postTitle = getIntent().getStringExtra("post_title");
        String postVotes = getIntent().getStringExtra("post_votes");
        String postLocation = getIntent().getStringExtra("post_location");
        String imagePost = getIntent().getStringExtra("post_image");

        // Initialize the views in the layout
        mTitleTextView = findViewById(R.id.row_post_title);
        mVotesTextView = findViewById(R.id.votes);
        locationTextView = findViewById(R.id.location);
        imagePostView = findViewById(R.id.row_post_image);

        // Set the views with the post data
        mTitleTextView.setText(postTitle);
        mVotesTextView.setText(postVotes);
        locationTextView.setText(postLocation);
        Picasso.get().load(imagePost).into(imagePostView);
        voteButton = findViewById(R.id.vote_button);

        voteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseDatabase = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
                DatabaseReference postRef = firebaseDatabase.getReference().child("posts");
                FirebaseAuth auth = FirebaseAuth.getInstance();
                String userId = auth.getCurrentUser().getUid();

                Query query = postRef.orderByChild("name").equalTo(postTitle);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String postId = dataSnapshot.getChildren().iterator().next().getKey();
                            DatabaseReference postVoteRef = firebaseDatabase.getReference().child("post_votes").child(postId);
                            postVoteRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.exists()) {
                                        postVoteRef.child(userId).setValue(true);
                                        DatabaseReference postRef = firebaseDatabase.getReference().child("posts").child(postId);
                                        postRef.runTransaction(new Transaction.Handler() {
                                            @androidx.annotation.NonNull
                                            @Override
                                            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                                                Integer votes = mutableData.child("voteCount").getValue(Integer.class);
                                                if (votes == null) {
                                                    // If the vote count is null (which should not happen), set it to 1
                                                    mutableData.child("voteCount").setValue(1);
                                                } else {
                                                    // Otherwise, increment the vote count by 1
                                                    mutableData.child("voteCount").setValue(votes + 1);
                                                }

                                                return Transaction.success(mutableData);
                                            }

                                            @Override
                                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                                            }
                                        });

                                    } else {
                                        // If the user has already voted, show an error message or disable the vote button
                                        //
                                        Toast.makeText(getApplicationContext(), "You have already voted for this post", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                        // Handle errors
                    }

                });
            }
        });


                // Initialize and assign variable
                BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

                // Set Home selected
                bottomNavigationView.setSelectedItemId(R.id.home);

                // Perform item selected listener
                bottomNavigationView.setOnItemSelectedListener(item -> {

                    switch (item.getItemId()) {
                        case R.id.camera:
                            startActivity(new Intent(getApplicationContext(), CameraActivity.class));
                            overridePendingTransition(0, 0);
                            return true;
                        case R.id.home:
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            overridePendingTransition(0, 0);
                            return true;
                        case R.id.map:
                            startActivity(new Intent(getApplicationContext(), mapActivity.class));
                            overridePendingTransition(0, 0);
                            return true;
                    }
                    return false;
                });
            }
        }