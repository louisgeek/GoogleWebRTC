package com.example.googlewebrtc.bean.info;


import com.example.googlewebrtc.bean.base.UserModel;

/**
 * fromUserModel 指的是发送消息的人
 * toUserModel 指的是接收消息的人
 */
public class VideoChatSdpInfoModel {
    public UserModel fromUserModel;
    public UserModel toUserModel;
    //
    public String type;
    public String description;
}
