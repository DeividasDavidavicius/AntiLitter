package com.example.siukslesv1;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
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

import java.util.Calendar;
import java.util.List;
import java.util.logging.Handler;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.MyViewHolder> {
    Context mContext;
    List<Post> mData;
    FirebaseDatabase firebaseDatabase;

private android.os.Handler handlerr = new android.os.Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            firebaseDatabase = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
            DatabaseReference postRef = firebaseDatabase.getReference().child("posts");
            postRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                        String postId = postSnapShot.getKey();
                        PostAdapter.this.deletePostIfExpiredAndLowVotes(postId);
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


    public PostAdapter (Context mContext, List<Post> mData)
    {
        this.mContext = mContext;
        this.mData = mData;
    }

    public MyViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType){
        View row = LayoutInflater.from(mContext).inflate(R.layout.row_post_item,parent,false);

        return new MyViewHolder(row);
    }

    @Override
    public void onBindViewHolder (@NonNull MyViewHolder holder, int position){
        // Set a click listener on the ConstraintLayout to open the PostDetailActivity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create an Intent to launch the PostDetailActivity
                Intent intent = new Intent(mContext, PostDetailsActivity.class);

                // Add the post data to the Intent
                intent.putExtra("post_title", mData.get(position).getName());
                intent.putExtra("post_votes", String.valueOf(mData.get(position).getVoteCount()));
                intent.putExtra("post_image", mData.get(position).getUri());
                intent.putExtra("post_location", mData.get(position).getLocation());

                // Launch the PostDetailActivity
                mContext.startActivity(intent);
            }
        });

        holder.tvTitle.setText(mData.get(position).getName());
        holder.votes.setText(String.valueOf(mData.get(position).getVoteCount()));
        Glide.with(mContext).load(mData.get(position).getUri()).into(holder.imgPost);
        int clickedPosition = -1;
        holder.voteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseDatabase = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
                DatabaseReference postRef = firebaseDatabase.getReference().child("posts");
                Query query = postRef.orderByChild("name").equalTo(mData.get(position).getName());
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String postId = dataSnapshot.getChildren().iterator().next().getKey();
                            startRepeatingTask();
                            DatabaseReference postVoteRef = firebaseDatabase.getReference().child("post_votes").child(postId);
                            postVoteRef.child(holder.getCurrentUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.exists()) {
                                        // If the user has not voted yet, increment the vote count and mark the user as voted
                                        postVoteRef.child(holder.getCurrentUserId()).setValue(true);
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
                                        Toast.makeText(mContext, "You have already voted on this post", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    // Handle errors
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
    }

    @Override
    public int getItemCount(){
        return mData.size();
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ImageView imgPost;

        TextView votes;

        Button voteButton;


        public MyViewHolder(View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.row_post_title);
            imgPost = itemView.findViewById(R.id.row_post_image);
            votes = itemView.findViewById(R.id.votes);
            voteButton = itemView.findViewById(R.id.VoteFor);
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

        private void deletePostIfExpiredAndLowVotes(String postId) {
            firebaseDatabase = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
            DatabaseReference postRef = firebaseDatabase.getReference().child("posts").child(postId);
            postRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Post post = dataSnapshot.getValue(Post.class);
                    if (post != null) {
                        Calendar calendar = Calendar.getInstance();
                        long currentTimeMillis = calendar.getTimeInMillis();
                        long postTimeMillis = post.getTime();
                        int postVoteCount = post.getVoteCount();
                        if (currentTimeMillis - postTimeMillis < 604800000
                        && postVoteCount < 5) {
                           postRef.removeValue(); // Delete the post from Firebase
                        }
                    }
             }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }


    }

