package com.nader.ayochat.models;

import com.google.gson.annotations.SerializedName;

public class Room {
    @SerializedName("name")
    private String name;

    @SerializedName("slug")
    private String slug;

    @SerializedName("locked")
    private boolean locked;

    @SerializedName("userCount")
    private int userCount;

    @SerializedName("isPrivate")
    private boolean isPrivate;

    // Constructors
    public Room() {}

    public Room(String name, boolean locked) {
        this.name = name;
        this.locked = locked;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public int getUserCount() { return userCount; }
    public void setUserCount(int userCount) { this.userCount = userCount; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }
}