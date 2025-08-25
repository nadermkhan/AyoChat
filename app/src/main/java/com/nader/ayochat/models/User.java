package com.nader.ayochat.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private int id;

    @SerializedName("username")
    private String username;

    @SerializedName("countryFlag")
    private String countryFlag;

    @SerializedName("country_flag")
    private String country_flag;

    // Constructors
    public User() {}

    public User(int id, String username, String countryFlag) {
        this.id = id;
        this.username = username;
        this.countryFlag = countryFlag;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCountryFlag() {
        if (countryFlag != null) return countryFlag;
        if (country_flag != null) return country_flag;
        return "🌍";
    }
    public void setCountryFlag(String countryFlag) {
        this.countryFlag = countryFlag;
        this.country_flag = countryFlag;
    }
}