package com.example.googlewebrtc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.googlewebrtc.adapter.MyBaseAdapter;
import com.example.googlewebrtc.bean.base.UserModel;
import com.example.googlewebrtc.bean.simple.VideoChatModel;
import com.example.googlewebrtc.socket.SocketClient;
import com.example.googlewebrtc.socket.SocketEvent;
import com.example.googlewebrtc.socket.SocketEvents;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private Gson mGson = new Gson();
    private MyBaseAdapter mMyBaseAdapter;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mContext = this;
        //
        EventBus.getDefault().register(this);
        //
        init();
        //
        ZApp.socketUserLogin();
    }
    private void inviteVideoChat(UserModel calleeUserModel) {
        //caller 邀请 callee 对讲
        VideoChatModel videoChatModel = new VideoChatModel();
        videoChatModel.inviteVideoChatUserModel = ZApp.getUserModel();
        videoChatModel.otherUserModel = calleeUserModel;
        String videoChatModelJson = mGson.toJson(videoChatModel);
        SocketClient.get().socket().emit(SocketEvents.videoChatInvite, videoChatModelJson);
    }

    private void init() {
        ListView id_lv = findViewById(R.id.id_lv);
        mMyBaseAdapter = new MyBaseAdapter();
        id_lv.setAdapter(mMyBaseAdapter);
        id_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                UserModel calleeUserModel = (UserModel) adapterView.getAdapter().getItem(i);
                if (ZApp.getUserModel().socketId.equals(calleeUserModel.socketId)){
                    Toast.makeText(mContext, "不能邀请本机！", Toast.LENGTH_SHORT).show();
                    return;
                }
                //
                inviteVideoChat(calleeUserModel);
                //
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.putExtra("isCaller", true);
                intent.putExtra("calleeUserModelJson", mGson.toJson(calleeUserModel));
                startActivity(intent);
                //

            }

        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        ZApp.socketUserLogout();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSubscribe(SocketEvent socketEvent) {
        String json = socketEvent.json;
        String event = socketEvent.event;
        Log.e(TAG, "onSubscribe: event; " + event);
        if (SocketEvents.userChange.equals(event)) {
            // socket.id & user Json
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> userModelMap = mGson.fromJson(json, type);
            //
            ZApp.getUserModelList().clear();
            for (Map.Entry<String, String> keyValueEntry : userModelMap.entrySet()) {
//                String socketid = keyValueEntry.getKey();
                String socketBeanJson = keyValueEntry.getValue();
                UserModel user = mGson.fromJson(socketBeanJson, UserModel.class);
                user.userName = ZApp.getUserModel().socketId.equals(user.socketId)
                        ? user.userName + "[本机]" : user.userName;
                ZApp.getUserModelList().add(user);
                //
                mMyBaseAdapter.refreshDataList(ZApp.getUserModelList());
            }
            for (UserModel userModel : ZApp.getUserModelList()) {
                Log.e(TAG, "检测到 onUserChange userName: " + userModel.userName + " socketId " + userModel.socketId);
            }

        } else if (SocketEvents.videoChatInvite.equals(event)) {
            String videoChatModelJson = json;
            Intent intent = new Intent(mContext, MainActivity.class);
            intent.putExtra("isCaller", false);
            intent.putExtra("videoChatModelJson", videoChatModelJson);
            startActivity(intent);
        }
    }


}
