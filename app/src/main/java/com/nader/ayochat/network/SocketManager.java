package com.nader.ayochat.network;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.nader.ayochat.utils.Constants;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket socket;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isConnected = false;

    private SocketManager() {
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        initSocket();
    }

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    private void initSocket() {
        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay = 1000;

            socket = IO.socket(Constants.SOCKET_URL, options);

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "Socket connected");
                    isConnected = true;
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "Socket disconnected");
                    isConnected = false;
                }
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket connection error: " + args[0]);
                    isConnected = false;
                }
            });

        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket URI error: " + e.getMessage());
        }
    }

    public void connect() {
        if (socket != null && !socket.connected()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // For Android 7+
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        socket.connect();
                    }
                });
            } else {
                // For Android 5-6
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        socket.connect();
                    }
                }).start();
            }
        }
    }

    public void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
    }

    public void emit(final String event, final JSONObject data) {
        if (socket != null && socket.connected()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        socket.emit(event, data);
                    }
                });
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        socket.emit(event, data);
                    }
                }).start();
            }
        }
    }

    public void on(String event, Emitter.Listener listener) {
        if (socket != null) {
            socket.on(event, listener);
        }
    }

    public void off(String event) {
        if (socket != null) {
            socket.off(event);
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    public Socket getSocket() {
        return socket;
    }

    public void runOnUiThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    public void destroy() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        instance = null;
    }
}