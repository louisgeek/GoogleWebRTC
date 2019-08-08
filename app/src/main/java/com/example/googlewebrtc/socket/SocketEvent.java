package com.example.googlewebrtc.socket;

public class SocketEvent {
    public String event;
    public String json;

    private SocketEvent(String event, String json) {
        this.event = event;
        this.json = json;
    }

    public static SocketEvent create(String event, String json) {
        return new SocketEvent(event, json);
    }
}
