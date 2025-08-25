package com.nader.ayochat.models;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("username")
    private String username;

    @SerializedName("message")
    private String message;

    @SerializedName("room")
    private String room;

    @SerializedName("country_flag")
    private String countryFlag;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("is_deleted")
    private boolean isDeleted;

    private boolean isSystemMessage;
    private boolean isOwnMessage;

    // Constructors
    public Message() {}

    public Message(String message, String username, boolean isSystemMessage) {
        this.message = message;
        this.username = username;
        this.isSystemMessage = isSystemMessage;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getCountryFlag() { return countryFlag != null ? countryFlag : "🌍"; }
    public void setCountryFlag(String countryFlag) { this.countryFlag = countryFlag; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public boolean isSystemMessage() { return isSystemMessage; }
    public void setSystemMessage(boolean systemMessage) { isSystemMessage = systemMessage; }

    public boolean isOwnMessage() { return isOwnMessage; }
    public void setOwnMessage(boolean ownMessage) { isOwnMessage = ownMessage; }
}