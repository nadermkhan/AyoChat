package com.nader.ayochat.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nader.ayochat.R;
import com.nader.ayochat.adapters.MessageAdapter;
import com.nader.ayochat.models.Message;
import com.nader.ayochat.models.Room;
import com.nader.ayochat.models.User;
import com.nader.ayochat.network.SocketManager;
import com.nader.ayochat.utils.Constants;
import com.nader.ayochat.utils.DeviceUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private TextView tvRoomName;
    private TextView tvOnlineCount;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;

    private SocketManager socketManager;
    private SharedPreferences preferences;
    private Gson gson;

    private String currentRoom = "general";
    private String username;
    private int userId;
    private boolean isTyping = false;
    private Timer typingTimer;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        initViews();

        // Initialize utilities
        preferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        gson = new Gson();
        handler = new Handler();
        socketManager = SocketManager.getInstance();

        // Setup
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        // Connect and authenticate
        connectToServer();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        tvRoomName = findViewById(R.id.tvRoomName);
        tvOnlineCount = findViewById(R.id.tvOnlineCount);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        tvRoomName.setText("Room: " + currentRoom);
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ayo Chat");
            getSupportActionBar().setSubtitle("Connecting...");
        }
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);

        messageAdapter.setOnMessageLongClickListener(new MessageAdapter.OnMessageLongClickListener() {
            @Override
            public void onMessageLongClick(Message message, int position) {
                showDeleteMessageDialog(message);
            }
        });
    }

    private void setupListeners() {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && !isTyping) {
                    sendTypingStatus(true);
                } else if (s.length() == 0 && isTyping) {
                    sendTypingStatus(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadMoreMessages();
            }
        });
    }

    private void connectToServer() {
        progressBar.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            new ConnectTask().execute();
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    performConnection();
                }
            }).start();
        }
    }

    private void performConnection() {
        socketManager.connect();
        setupSocketListeners();

        // Wait for connection
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (socketManager.isConnected()) {
                    authenticate();
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show();
                }
            }
        }, 2000);
    }

    private void setupSocketListeners() {
        // Authenticated event
        socketManager.on(Constants.EVENT_AUTHENTICATED, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            JSONObject userObj = data.getJSONObject("user");

                            User user = gson.fromJson(userObj.toString(), User.class);
                            userId = user.getId();
                            username = user.getUsername();

                            // Save to preferences
                            preferences.edit()
                                    .putString(Constants.KEY_USERNAME, username)
                                    .putInt(Constants.KEY_USER_ID, userId)
                                    .putString(Constants.KEY_COUNTRY_FLAG, user.getCountryFlag())
                                    .apply();

                            // Join default room
                            joinRoom(currentRoom);

                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setSubtitle(username + " " + user.getCountryFlag());
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing authenticated data", e);
                        }
                    }
                });
            }
        });

        // Joined room event
        socketManager.on(Constants.EVENT_JOINED, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        try {
                            JSONObject data = (JSONObject) args[0];
                            currentRoom = data.getString("room");
                            tvRoomName.setText("Room: " + currentRoom);

                            // Update online users count
                            if (data.has("onlineUsers")) {
                                JSONArray users = data.getJSONArray("onlineUsers");
                                tvOnlineCount.setText("Online: " + users.length());
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing joined data", e);
                        }
                    }
                });
            }
        });

        // Message history
        socketManager.on(Constants.EVENT_MESSAGE_HISTORY, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONArray messagesArray = (JSONArray) args[0];
                            Type listType = new TypeToken<List<Message>>(){}.getType();
                            List<Message> messages = gson.fromJson(messagesArray.toString(), listType);

                            // Mark own messages
                            for (Message msg : messages) {
                                msg.setOwnMessage(msg.getUserId() == userId);
                            }

                            messageAdapter.setMessages(messages);
                            recyclerView.scrollToPosition(messages.size() - 1);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing message history", e);
                        }
                    }
                });
            }
        });

        // New message
        socketManager.on(Constants.EVENT_NEW_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject messageObj = (JSONObject) args[0];
                            Message message = gson.fromJson(messageObj.toString(), Message.class);
                            message.setOwnMessage(message.getUserId() == userId);

                            messageAdapter.addMessage(message);
                            recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing new message", e);
                        }
                    }
                });
            }
        });

        // System message
        socketManager.on(Constants.EVENT_SYSTEM_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            String text = data.getString("message");

                            Message systemMessage = new Message(text, "System", true);
                            systemMessage.setSystemMessage(true);

                            messageAdapter.addMessage(systemMessage);
                            recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing system message", e);
                        }
                    }
                });
            }
        });

        // Message deleted
        socketManager.on(Constants.EVENT_MESSAGE_DELETED, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            int messageId = data.getInt("messageId");
                            messageAdapter.removeMessage(messageId);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing message deleted", e);
                        }
                    }
                });
            }
        });

        // Error event
        socketManager.on(Constants.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            String errorMsg = data.getString("message");
                            Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();

                            if (data.has("type") && "BAN".equals(data.getString("type"))) {
                                // User is banned, show dialog
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Banned")
                                        .setMessage(errorMsg)
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing error message", e);
                        }
                    }
                });
            }
        });

        // User joined/left events
        socketManager.on(Constants.EVENT_USER_JOINED, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            if (data.has("onlineUsers")) {
                                JSONArray users = data.getJSONArray("onlineUsers");
                                tvOnlineCount.setText("Online: " + users.length());
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing user joined", e);
                        }
                    }
                });
            }
        });

        socketManager.on(Constants.EVENT_USER_LEFT, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            if (data.has("onlineUsers")) {
                                JSONArray users = data.getJSONArray("onlineUsers");
                                tvOnlineCount.setText("Online: " + users.length());
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing user left", e);
                        }
                    }
                });
            }
        });
    }

    private void authenticate() {
        String deviceId = DeviceUtils.getDeviceId(this);
        preferences.edit().putString(Constants.KEY_DEVICE_ID, deviceId).apply();

        try {
            JSONObject data = new JSONObject();
            data.put("deviceId", deviceId);
            socketManager.emit(Constants.EVENT_AUTHENTICATE, data);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating auth data", e);
        }
    }

    private void joinRoom(String room) {
        try {
            JSONObject data = new JSONObject();
            data.put("room", room);
            socketManager.emit(Constants.EVENT_JOIN, data);
        } catch (JSONException e) {
            Log.e(TAG, "Error joining room", e);
        }
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (message.isEmpty()) return;

        try {
            JSONObject data = new JSONObject();
            data.put("message", message);
            data.put("room", currentRoom);
            socketManager.emit(Constants.EVENT_SEND_MESSAGE, data);

            etMessage.setText("");
            sendTypingStatus(false);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending message", e);
        }
    }

    private void sendTypingStatus(boolean typing) {
        isTyping = typing;

        try {
            JSONObject data = new JSONObject();
            data.put("isTyping", typing);
            data.put("room", currentRoom);
            socketManager.emit(Constants.EVENT_TYPING, data);

            if (typing) {
                if (typingTimer != null) {
                    typingTimer.cancel();
                }
                typingTimer = new Timer();
                typingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendTypingStatus(false);
                    }
                }, 3000);
            } else {
                if (typingTimer != null) {
                    typingTimer.cancel();
                    typingTimer = null;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error sending typing status", e);
        }
    }

    private void showDeleteMessageDialog(final Message message) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMessage(message);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMessage(Message message) {
        try {
            JSONObject data = new JSONObject();
            data.put("messageId", message.getId());
            socketManager.emit(Constants.EVENT_DELETE_MESSAGE, data);
        } catch (JSONException e) {
            Log.e(TAG, "Error deleting message", e);
        }
    }

    private void loadMoreMessages() {
        // Implement pagination here
        swipeRefresh.setRefreshing(false);
    }

    private void showRoomDialog() {
        Intent intent = new Intent(this, RoomListActivity.class);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String newRoom = data.getStringExtra("room");
            if (newRoom != null && !newRoom.equals(currentRoom)) {
                changeRoom(newRoom);
            }
        }
    }

    private void changeRoom(String newRoom) {
        try {
            JSONObject data = new JSONObject();
            data.put("newRoom", newRoom);
            socketManager.emit(Constants.EVENT_CHANGE_ROOM, data);
            currentRoom = newRoom;
            tvRoomName.setText("Room: " + currentRoom);
        } catch (JSONException e) {
            Log.e(TAG, "Error changing room", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_rooms) {
            showRoomDialog();
            return true;
        } else if (id == R.id.action_create_room) {
            showCreateRoomDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCreateRoomDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_room, null);
        final EditText etRoomName = dialogView.findViewById(R.id.etRoomName);
        final EditText etPassword = dialogView.findViewById(R.id.etPassword);

        new AlertDialog.Builder(this)
                .setTitle("Create New Room")
                .setView(dialogView)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String roomName = etRoomName.getText().toString().trim();
                        String password = etPassword.getText().toString().trim();

                        if (!roomName.isEmpty()) {
                            createRoom(roomName, password);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createRoom(String roomName, String password) {
        try {
            JSONObject data = new JSONObject();
            data.put("roomName", roomName);
            if (!password.isEmpty()) {
                data.put("password", password);
            }
            socketManager.emit(Constants.EVENT_CREATE_ROOM, data);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating room", e);
        }
    }

    // AsyncTask for Android 7+
    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            performConnection();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Connection initiated
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (typingTimer != null) {
            typingTimer.cancel();
        }
        socketManager.disconnect();
    }
}