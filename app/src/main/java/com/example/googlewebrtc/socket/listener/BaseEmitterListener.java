package com.example.googlewebrtc.socket.listener;

import android.os.Handler;
import android.os.Looper;

import io.socket.emitter.Emitter;

public abstract class BaseEmitterListener implements Emitter.Listener {
    private String event;

    public BaseEmitterListener(String event) {
        this.event = event;
    }

    @Override
    public void call(Object... args) {
        call(event, args);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                callOnUi(event, args);
            }
        });
    }
    public  void call(String event, Object... args){

    }
    public abstract void callOnUi(String event, Object... args);
}
