package com.example.googlewebrtc.socket.listener;

import android.util.Log;

import com.example.googlewebrtc.socket.SocketEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

public class EventEmitterListener extends BaseEmitterListener {
    private static final String TAG = "EventEmitterListener";

    public EventEmitterListener(String event) {
        super(event);
    }


    @Override
    public void call(String event, Object... args) {
        super.call(event, args);
        //
        Object object = args[0];
        String json = null;
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) args[0];
            json = jsonObject.toString();
        } else if (object instanceof String) {
            json = (String) args[0];
        } else {
            Log.e(TAG, "call: zfq error  ");
        }
        EventBus.getDefault().post(SocketEvent.create(event, json));
    }

    @Override
    public void callOnUi(String event, Object... args) {

    }
}
