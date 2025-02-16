package com.example.siukslesv1;

import java.util.List;
import java.util.UUID;

public class User {
    private String email;
    private String password;
    private String username;
    private String title;
    private List<String> titles;
    private int points;

    public User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(String email, String password, String username, String title, List<String> titles, int points) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.title = title;
        this.titles = titles;
        this.points = points;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getTitles() {return titles;}

    public int getPoints() {
        return points;
    }
}
