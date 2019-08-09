package com.example.googlewebrtc.listener;

import android.util.Log;

import org.webrtc.SdpObserver;

/**
 * Created by louisgeek on 2019/8/8.
 */
public abstract class SdpObserverListener implements SdpObserver {
    private static final String TAG = "SdpObserverListener";

    @Override
    public void onSetSuccess() {
        Log.e(TAG, "onSetSuccess: ");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.e(TAG, "onCreateFailure: "+s);
    }

    @Override
    public void onSetFailure(String s) {
        Log.e(TAG, "onSetFailure: "+s);
    }
}
