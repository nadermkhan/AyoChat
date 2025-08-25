package com.nader.ayochat.utils;

public class Constants {
    public static final String BASE_URL = "https://api.naderr.link";
    public static final String SOCKET_URL = "https://api.naderr.link";

    // SharedPreferences keys
    public static final String PREF_NAME = "AyoChatPrefs";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_COUNTRY_FLAG = "country_flag";
    public static final String KEY_CURRENT_ROOM = "current_room";

    // Socket events
    public static final String EVENT_AUTHENTICATE = "authenticate";
    public static final String EVENT_AUTHENTICATED = "authenticated";
    public static final String EVENT_JOIN = "join";
    public static final String EVENT_JOINED = "joined";
    public static final String EVENT_SEND_MESSAGE = "send_message";
    public static final String EVENT_NEW_MESSAGE = "new_message";
    public static final String EVENT_MESSAGE_HISTORY = "message_history";
    public static final String EVENT_USER_JOINED = "user_joined";
    public static final String EVENT_USER_LEFT = "user_left";
    public static final String EVENT_TYPING = "typing";
    public static final String EVENT_TYPING_UPDATE = "typing_update";
    public static final String EVENT_ERROR = "error";
    public static final String EVENT_SYSTEM_MESSAGE = "system_message";
    public static final String EVENT_DELETE_MESSAGE = "delete_message";
    public static final String EVENT_MESSAGE_DELETED = "message_deleted";
    public static final String EVENT_ROOM_CREATED = "room_created";
    public static final String EVENT_CHANGE_ROOM = "change_room";
    public static final String EVENT_ROOM_CHANGED = "room_changed";
    public static final String EVENT_CREATE_ROOM = "create_room";
}