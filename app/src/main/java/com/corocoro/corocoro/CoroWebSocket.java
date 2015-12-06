package com.corocoro.corocoro;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * Created by ladicle on 12/5/15.
 */
public class CoroWebSocket extends WebSocketClient {

    public CoroWebSocket(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        Log.i(Context.LOG_TAG, "onOpen");
    }

    @Override
    public void onMessage(String s) {
        Log.i(Context.LOG_TAG, "onMessage\n" + s);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        Log.i(Context.LOG_TAG, "onClose");
    }

    @Override
    public void onError(Exception e) {
        Log.i(Context.LOG_TAG, "onError\n" + e.getMessage());
    }
}
