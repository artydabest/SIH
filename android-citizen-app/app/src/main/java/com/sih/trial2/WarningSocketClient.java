package com.sih.trial2;

import android.util.Log;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Live Socket.IO link to the backend for warning and safety pushes.
 * The polling timers in MainActivity remain the safety net; this link only
 * makes updates arrive in well under a second.
 */
public class WarningSocketClient {

    private static final String TAG = "WarningSocketClient";

    /** Events arrive on the socket's own thread; hop to the UI thread in the listener. */
    public interface Listener {
        void onWarningsChanged();

        void onSafetyChanged();
    }

    private Socket socket;

    public void connect(String baseUrl, final Listener listener) {
        disconnect();
        try {
            socket = IO.socket(baseUrl);
        } catch (URISyntaxException e) {
            Log.w(TAG, "Bad live-link URL: " + baseUrl);
            return;
        }

        socket.on(Socket.EVENT_CONNECT, args -> Log.i(TAG, "Live link connected"))
                .on(Socket.EVENT_DISCONNECT, args -> Log.i(TAG, "Live link disconnected"))
                .on(Socket.EVENT_CONNECT_ERROR, args -> Log.w(TAG, "Live link error; will retry"))
                .on("warning:new", onWarningEvent(listener))
                .on("warning:update", onWarningEvent(listener))
                .on("safety:update", args -> listener.onSafetyChanged());
        socket.connect();
    }

    private Emitter.Listener onWarningEvent(final Listener listener) {
        return args -> listener.onWarningsChanged();
    }

    public void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
        }
    }
}
