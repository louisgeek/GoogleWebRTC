package com.example.googlewebrtc.socket;

import android.text.TextUtils;
import android.util.Log;

import com.example.googlewebrtc.ZApp;
import com.example.googlewebrtc.socket.listener.EventEmitterListener;

import org.greenrobot.eventbus.EventBus;

import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class SocketClient {
    private static final String TAG = "SocketClient";

    private SocketClient() {
    }

    private static class Inner {
        private static final SocketClient SOCKET_CLIENT = new SocketClient();
    }

    public static SocketClient get() {
        return Inner.SOCKET_CLIENT;
    }

    private Socket IOSocket;

    public void init(String url, String nsp) {
        //切换socket地址需要先断开连接
        releaseSocket();

        Log.e(TAG, "init:ll url " + url);
        if (!TextUtils.isEmpty(nsp)) {
            if (!nsp.startsWith("/")) {
                nsp = String.format(Locale.CHINA, "/%s", nsp);
            }
            // FIXME: 2019/3/12
//            url = String.format(Locale.CHINA, "%s%s", url, nsp);
        }
        try {
            IOSocket = IO.socket(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        //统一处理
        for (Map.Entry<String, EventEmitterListener> mapEntry : SocketEvents.eventMap.entrySet()) {
            String event = mapEntry.getKey();
            EventEmitterListener listener = mapEntry.getValue();
            IOSocket.on(event, listener);
        }
        IOSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "call: EVENT_CONNECT ");
                ZApp.isIOSocketConnected = true;
                //
                EventBus.getDefault().post(SocketEvent.create(Socket.EVENT_CONNECT, null));
            }
        });
        IOSocket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "call: EVENT_CONNECT_TIMEOUT ");
                ZApp.isIOSocketConnected = false;
                //
                EventBus.getDefault().post(SocketEvent.create(Socket.EVENT_CONNECT_TIMEOUT, null));
            }
        });
        IOSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "call: EVENT_CONNECT_ERROR ");
                ZApp.isIOSocketConnected = false;
                //
                EventBus.getDefault().post(SocketEvent.create(Socket.EVENT_CONNECT_ERROR, null));
            }
        });
        IOSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "call: EVENT_DISCONNECT ");
                //
                ZApp.isIOSocketConnected = false;
                //
                EventBus.getDefault().post(SocketEvent.create(Socket.EVENT_DISCONNECT, null));
            }
        });
        //
        IOSocket.connect();
    }

    public Socket socket() {
        return IOSocket;
    }

    public void releaseSocket() {
        if (IOSocket != null) {
            IOSocket.disconnect();
            IOSocket = null;
        }
    }
}
