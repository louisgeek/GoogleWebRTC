package com.example.googlewebrtc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

        SharedPreferences sharedPreferences = getSharedPreferences("room_user", Context.MODE_PRIVATE);
        String userName = sharedPreferences.getString("userName", "");
        String roomId = sharedPreferences.getString("roomId", "");
        String roomName = sharedPreferences.getString("roomName", "");

        if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(roomId) || TextUtils.isEmpty(roomName)) {
            showDA();
        } else {
            //
            initAll();
        }


    }

    private void initAll() {
        //
        init();
        //
        ZApp.socketUserLogin(this);
    }

    private void showDA() {
        SharedPreferences sharedPreferences = getSharedPreferences("room_user", Context.MODE_PRIVATE);
        //
        LinearLayout linearLayout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        //
        EditText editText_userName = new EditText(this);
        editText_userName.setHint("userName");
        editText_userName.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        linearLayout.addView(editText_userName);
        //
        EditText editText_roomId = new EditText(this);
        editText_roomId.setHint("roomId");
        editText_roomId.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        linearLayout.addView(editText_roomId);
        //
        EditText editText_roomName = new EditText(this);
        editText_roomName.setHint("roomName");
        editText_roomName.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        linearLayout.addView(editText_roomName);
        new AlertDialog.Builder(this)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //
                        String editText_userNameTxt = editText_userName.getText().toString();
                        String editText_roomIdTxt = editText_roomId.getText().toString();
                        String editText_roomNameTxt = editText_roomName.getText().toString();
                        sharedPreferences.edit()
                                .putString("userName", editText_userNameTxt)
                                .putString("roomId", editText_roomIdTxt)
                                .putString("roomName", editText_roomNameTxt)
                                .apply();
                        //
                        initAll();
                    }
                })
                .setView(linearLayout)
                .create().show();
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
                if (ZApp.getUserModel().socketId.equals(calleeUserModel.socketId)) {
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
        if (SocketEvents.roomUserChange.equals(event)) {
            // socket.id & user Json
            Type type = new TypeToken<Map<String, UserModel>>() {
            }.getType();
            Map<String, UserModel> userModelMap = mGson.fromJson(json, type);
            //
            ZApp.getUserModelList().clear();
            for (Map.Entry<String, UserModel> keyValueEntry : userModelMap.entrySet()) {
//                String socketid = keyValueEntry.getKey();
                UserModel user = keyValueEntry.getValue();
//                UserModel user = mGson.fromJson(socketBeanJson, UserModel.class);
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
