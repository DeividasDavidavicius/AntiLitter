package com.example.siukslesv1;

import java.util.Date;
import java.util.UUID;

public class Post {
    private String email;
    private String name;
    private String location;
    private String uri;
    private int type;
    private int voteCount;
    private String postid;

    private long creationTime;


    public Post() {
    }

    public Post(String email, String name, String location, String uri, int type, long creationTime) {
        this.email = email;
        this.name = name;
        this.location = location;
        this.uri = uri;
        this.type = type;
        this.voteCount = 0;
        this.creationTime = creationTime;
    }

    public String getEmail() {
        return email;
    }
    public String getName() { return name; }
    public String getUri() {return uri; };
    public String getLocation() { return location; }
    public int getType() { return type; }
    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }
    public int getVoteCount() { return voteCount; }
    public String getPostid() { return postid; }
    public long getTime(){return creationTime;}
}

