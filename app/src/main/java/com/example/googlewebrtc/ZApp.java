package com.example.googlewebrtc;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.example.googlewebrtc.bean.base.UserModel;
import com.example.googlewebrtc.socket.SocketClient;
import com.example.googlewebrtc.socket.SocketEvent;
import com.example.googlewebrtc.socket.SocketEvents;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Ack;

/**
 * Created by louisgeek on 2019/8/8.
 */
public class ZApp extends Application {
    
    private static final String TAG = "ZApp";
    private static final String SP_DEVICE = "SP_DEVICE";
    private static final String SP_DEVICE_TAG = "SP_DEVICE_TAG";
    private static final String SOCKET_URL = "http://192.168.1.109:3001/";
    //    private static final String SOCKET_URL = "http://127.0.0.1:3001/";
    private static String deviceTag;

    @Override
    public void onCreate() {
        super.onCreate();
        //
        SharedPreferences sharedPreferences = getSharedPreferences(SP_DEVICE, Context.MODE_PRIVATE);
        deviceTag = sharedPreferences.getString(SP_DEVICE_TAG, getDeviceName());
    }

    private static UserModel mUserModel = new UserModel();


    private static String getDeviceName() {
//        String deviceName = Build.MANUFACTURER + "_" + Build.MODEL + "_" + UUID.randomUUID().toString();
        String deviceName = Build.MANUFACTURER + "_" + Build.MODEL;
        deviceName = deviceName.replace(" ", "_");
        deviceName = deviceName.replace("-", "");
        Log.e(TAG, "getDeviceName: " + deviceName);
        return deviceName;
    }

    //在线用户列表
    private static List<UserModel> mUserModelList = new ArrayList<>();

    public static List<UserModel> getUserModelList() {
        return mUserModelList;
    }

    public static UserModel getUserModel() {
        return mUserModel;
    }

    public static void socketUserLogin() {
        ZApp.getUserModel().socketId = "";
        ZApp.getUserModel().userName = deviceTag;
        if (SocketClient.get().socket() == null) {
            SocketClient.get().init(SOCKET_URL, "dsa/dad");
        }
        String json = new Gson().toJson(ZApp.getUserModel());
        SocketClient.get().socket().emit(SocketEvents.userLogin, json, new Ack() {
            @Override
            public void call(Object... args) {
                String socketId = (String) args[0];
                ZApp.getUserModel().socketId = socketId;
                Log.e(TAG, "SocketClient init call: " + socketId);
                //
//                ZApp.isIOSocketConnected = true;
                EventBus.getDefault().post(SocketEvent.create(SocketEvents.connected, null));
            }
        });
    }


    public static void socketUserLogout() {

        if (SocketClient.get().socket() == null) {
            SocketClient.get().init(SOCKET_URL, "dsa/dad");
        }
        if (ZApp.getUserModel() == null) {
            return;
        }
        //退出登录
        String json = new Gson().toJson(ZApp.getUserModel());
        Log.d("ll===", "socketUserLogout");

        SocketClient.get().socket().emit(SocketEvents.userLogout, json);
    }

}
