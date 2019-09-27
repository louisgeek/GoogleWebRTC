package com.example.googlewebrtc.socket;


import com.example.googlewebrtc.socket.listener.EventEmitterListener;

import java.util.HashMap;
import java.util.Map;

public class SocketEvents {
    public static final String connected = "connected";
    //全局监听
    public static final String userLogin = "userLogin";
    public static final String userLogout = "userLogout";
    //
    public static final String roomUserChange = "roomUserChange";
    //================================================================
    public static final String videoChatInvite = "videoChatInvite";
    public static final String videoChatCancel = "videoChatCancel";
    //
    public static final String videoChatAgree = "videoChatAgree";
    public static final String videoChatReject = "videoChatReject";
    //局部监听 视频通信界面
    public static final String offer = "offer";
    public static final String answer = "answer";
    public static final String candidate = "candidate";
    ///================================================================
    //局部监听
    //
    public static final String userChat = "userChat";
    public static Map<String, EventEmitterListener> eventMap = new HashMap<>();

    static {
        eventMap.put(userLogin, new EventEmitterListener(userLogin));
        eventMap.put(userLogout, new EventEmitterListener(userLogout));
        eventMap.put(roomUserChange, new EventEmitterListener(roomUserChange));
        //
        eventMap.put(videoChatInvite, new EventEmitterListener(videoChatInvite));
        eventMap.put(videoChatCancel, new EventEmitterListener(videoChatCancel));
        eventMap.put(videoChatAgree, new EventEmitterListener(videoChatAgree));
        eventMap.put(videoChatReject, new EventEmitterListener(videoChatReject));
        //
        eventMap.put(offer, new EventEmitterListener(offer));
        eventMap.put(answer, new EventEmitterListener(answer));
        eventMap.put(candidate, new EventEmitterListener(candidate));
        //
        eventMap.put(userChat, new EventEmitterListener(userChat));

    }
}
