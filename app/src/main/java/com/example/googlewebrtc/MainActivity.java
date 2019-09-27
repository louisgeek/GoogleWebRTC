package com.example.googlewebrtc;

import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.googlewebrtc.bean.ChatInfoModel;
import com.example.googlewebrtc.bean.ChatInfoModelParam;
import com.example.googlewebrtc.bean.base.UserModel;
import com.example.googlewebrtc.bean.info.VideoChatInfoModel;
import com.example.googlewebrtc.bean.info.VideoChatSdpInfoModel;
import com.example.googlewebrtc.bean.simple.VideoChatModel;
import com.example.googlewebrtc.listener.PeerConnectionObserverListener;
import com.example.googlewebrtc.listener.SdpObserverListener;
import com.example.googlewebrtc.socket.SocketClient;
import com.example.googlewebrtc.socket.SocketEvent;
import com.example.googlewebrtc.socket.SocketEvents;
import com.example.googlewebrtc.usb.UVCCamerarHelper;
import com.example.googlewebrtc.usb.UsbCameraVideoCapturer;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CapturerObserver;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private EglBase mEglBase = EglBase.create();
    private List<PeerConnection.IceServer> mIceServerList = new ArrayList();
    private AudioSource mLocalAudioSource;
    private AudioTrack mLocalAudioTrack;
    private VideoCapturer mLocalVideoCapturer;
    private VideoSource mLocalVideoSource;
    private SurfaceTextureHelper mSurfaceTextureHelper;
    private CapturerObserver mCapturerObserver;
    private VideoTrack mLocalVideoTrack;
    private SurfaceViewRenderer mLocalSurfaceViewRenderer;
    private SurfaceViewRenderer mRemoteSurfaceViewRenderer;
    public Gson mGson = new Gson();
    private PeerConnectionFactory mPeerConnectionFactory;
    public PeerConnection mPeerConnection;
    private Context mContext;
    private MediaConstraints mSdpMediaConstraints;
    private UserModel callerUserModel;
    private UserModel calleeUserModel;
    private boolean isCaller;
    private Button id_btn_hangup;
    private AudioManager mAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        EventBus.getDefault().register(this);
        //
        initViews();
        //
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mAudioManager.setSpeakerphoneOn(true);
        //
        init();
        //
        isCaller = getIntent().getBooleanExtra("isCaller", false);
        //
        if (isCaller) {
            callerUserModel = ZApp.getUserModel();
            //
            String calleeUserModelJson = getIntent().getStringExtra("calleeUserModelJson");
            calleeUserModel = mGson.fromJson(calleeUserModelJson, UserModel.class);
        } else {
            calleeUserModel = ZApp.getUserModel();
            //
            String videoChatModelJson = getIntent().getStringExtra("videoChatModelJson");
            VideoChatModel videoChatModel = mGson.fromJson(videoChatModelJson, VideoChatModel.class);
            callerUserModel = videoChatModel.inviteVideoChatUserModel;
            //
            //B被邀请
            new AlertDialog.Builder(mContext)
                    .setMessage(videoChatModel.inviteVideoChatUserModel.userName
                            + "邀请你视频对讲")
                    .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //被邀请 直接使用发过来的 videoChatModel
                            String json = new Gson().toJson(videoChatModel);
                            SocketClient.get().socket().emit(SocketEvents.videoChatAgree, json);
                        }
                    })
                    .setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
//                            dialogInterface.dismiss();
                        }
                    }).create().show();
        }
        //
    }


    private void initViews() {
        mLocalSurfaceViewRenderer = findViewById(R.id.id_svr_local);
        mRemoteSurfaceViewRenderer = findViewById(R.id.id_svr_remote);
        //
        mLocalSurfaceViewRenderer.init(this.mEglBase.getEglBaseContext(), null);
        mLocalSurfaceViewRenderer.setZOrderMediaOverlay(true);
        mLocalSurfaceViewRenderer.setMirror(true);
        //
        mRemoteSurfaceViewRenderer.init(this.mEglBase.getEglBaseContext(), null);
        mRemoteSurfaceViewRenderer.setZOrderMediaOverlay(true);
        mRemoteSurfaceViewRenderer.setMirror(true);
        //
        id_btn_hangup = findViewById(R.id.id_btn_hangup);
        id_btn_hangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //
                //B do【A打给B，B挂断】
                ChatInfoModel chatInfoModel = new ChatInfoModel();
                chatInfoModel.fromUserModel = isCaller ? callerUserModel : calleeUserModel;
                chatInfoModel.toUserModel = isCaller ? calleeUserModel : callerUserModel;
                chatInfoModel.chatInfo = ChatInfoModelParam.ChatInfo_ChatEnd;
                String json = mGson.toJson(chatInfoModel);
                SocketClient.get().socket().emit(SocketEvents.userChat, json);
                //
                finish();
            }
        });
    }


    private void init() {
        //
        mSdpMediaConstraints = new MediaConstraints();
        mSdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mSdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
//        mSdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", ZApp.isVideo ? "true" : "false"));
        mSdpMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));


        //1 initialize Factory
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(mContext.getApplicationContext())
                        .setEnableInternalTracer(true)
//                        .setEnableVideoHwAcceleration(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        //2 new Factory
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        VideoEncoderFactory videoEncoderFactory = new DefaultVideoEncoderFactory(mEglBase.getEglBaseContext(), true, true);
        VideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext());
//        mPeerConnectionFactory = new PeerConnectionFactory(options, videoEncoderFactory, videoDecoderFactory);
        //PeerConnectionFactory.Builder
        PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
//                .setAudioDeviceModule(audioDeviceModule)
//                .setAudioDeviceModule(null)
                .setOptions(options);
        mPeerConnectionFactory = builder.createPeerConnectionFactory();
        //3 new VideoCapturer
        if (UVCCamerarHelper.hasUVCCamera(mContext)) {
            mLocalVideoCapturer = new UsbCameraVideoCapturer(mContext, mLocalSurfaceViewRenderer);
        } else {
            //普通摄像头
            mLocalVideoCapturer = createVideoCapturer();
        }
        //4 new AudioSource
        mLocalAudioSource = mPeerConnectionFactory.createAudioSource(new MediaConstraints());
        //5 new AudioTrack
        mLocalAudioTrack = mPeerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, mLocalAudioSource);
        mLocalAudioTrack.setEnabled(true);
        //
        //6 new VideoSource
        mLocalVideoSource = mPeerConnectionFactory.createVideoSource(false);
        mCapturerObserver = mLocalVideoSource.getCapturerObserver();
        mSurfaceTextureHelper = SurfaceTextureHelper.create("VideoCapturerThread", mEglBase.getEglBaseContext());
        mLocalVideoCapturer.initialize(mSurfaceTextureHelper, mContext.getApplicationContext(), mCapturerObserver);
        //7 new VideoTrack
        mLocalVideoTrack = mPeerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, mLocalVideoSource);
        mLocalVideoTrack.setEnabled(true);
        //8 VideoTrack add SurfaceViewRenderer
        mLocalVideoTrack.addSink(this.mLocalSurfaceViewRenderer);
        //
        //9 new PeerConnection 可以延后
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(mIceServerList);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        mPeerConnection = mPeerConnectionFactory.createPeerConnection(rtcConfig,
                new PeerConnectionObserverListener() {
                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        VideoChatInfoModel videoChatInfoModel = new VideoChatInfoModel();
                        videoChatInfoModel.fromUserModel = isCaller ? callerUserModel : calleeUserModel;
                        //caller createOffer 后回调的情况 toUserModel is callee
                        //callee createAnswer 后回调的情况 toUserModel is caller
                        videoChatInfoModel.toUserModel = isCaller ? calleeUserModel : callerUserModel;
                        //
                        videoChatInfoModel.sdpMLineIndex = String.valueOf(iceCandidate.sdpMLineIndex);
                        videoChatInfoModel.sdpMid = iceCandidate.sdpMid;
                        videoChatInfoModel.sdp = iceCandidate.sdp;
                        //
                        String json = mGson.toJson(videoChatInfoModel);
                        SocketClient.get().socket().emit(SocketEvents.candidate, json);
                    }

                    @Override
                    public void onAddRemoteVideoTrack(VideoTrack remoteVideoTrack) {
                        //
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ////UI线程执行
                                //构建远端view
                                SurfaceViewRenderer remoteSurfaceViewRenderer = mRemoteSurfaceViewRenderer;
                                //控件布局
                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT);
//                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(360, 360);
//                            layoutParams.topMargin = 20;
//                            id_ll_remote.addView(remoteSurfaceViewRenderer, layoutParams);
//@@            setupRemoteSurfaceViewRendererLayout().addView(remoteSurfaceViewRenderer, layoutParams);
//@@             mRemoteSurfaceViewRendererList.add(remoteSurfaceViewRenderer);
                                //添加数据
                                //VideoTrack videoTrack = mediaStream.videoTracks.get(0);
                                remoteVideoTrack.addSink(remoteSurfaceViewRenderer);
                            }
                        });
                        //
                    }
                });
        //10 new MediaStream
        MediaStream localMediaStream = mPeerConnectionFactory.createLocalMediaStream("102");
        localMediaStream.addTrack(mLocalAudioTrack);
        localMediaStream.addTrack(mLocalVideoTrack);
        //11 PeerConnection add MediaStream
        mPeerConnection.addStream(localMediaStream);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLocalVideoCapturer != null) {
//            mLocalVideoCapturer.startCapture(480, 320, 15);
            mLocalVideoCapturer.startCapture(1024, 720, 30);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (mLocalVideoCapturer != null) {
                mLocalVideoCapturer.stopCapture();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private void callerOffer() {
        //
        mPeerConnection.createOffer(new SdpObserverListener() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                //caller do【offer 成功后回调 然后发给 callee】
                String type = sessionDescription.type.canonicalForm();
                //2 通过 setLocalDescription()方法，将 caller 的 SDP 描述符交给 caller 的 PC 实例
                mPeerConnection.setLocalDescription(this, sessionDescription);
                //3 caller 将 offer 信令通过服务器发送给 callee
                VideoChatSdpInfoModel videoChatSdpInfoModel = new VideoChatSdpInfoModel();
                videoChatSdpInfoModel.fromUserModel = callerUserModel;
                //createOffer 时候 toUserModel is callee
                videoChatSdpInfoModel.toUserModel = calleeUserModel;
                videoChatSdpInfoModel.description = sessionDescription.description;
                videoChatSdpInfoModel.type = type;
                //
                String json = mGson.toJson(videoChatSdpInfoModel);
//                    SocketClient.get().socket().emit("offer".equals(type) ? SocketEvents.offer : SocketEvents.answer, json);
                SocketClient.get().socket().emit(SocketEvents.offer, json);

            }
        }, mSdpMediaConstraints);
    }

    /**
     *
     */
    private void calleeAnswer() {
        mPeerConnection.createAnswer(new SdpObserverListener() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                //callee do【answer 成功后回调 然后发给 caller】
                String type = sessionDescription.type.canonicalForm();
                //2 通过 setLocalDescription()方法，将 callee 的 SDP 描述符交给 callee 的 PC 实例
                mPeerConnection.setLocalDescription(this, sessionDescription);
                //3 callee 将 answer 信令通过服务器发送给 caller
                VideoChatSdpInfoModel videoChatSdpInfoModel = new VideoChatSdpInfoModel();
                videoChatSdpInfoModel.fromUserModel = calleeUserModel;
                //createAnswer 时候 toUserModel is caller
                videoChatSdpInfoModel.toUserModel = callerUserModel;
                videoChatSdpInfoModel.description = sessionDescription.description;
                videoChatSdpInfoModel.type = type;
                //
                String json = mGson.toJson(videoChatSdpInfoModel);
//                    SocketClient.get().socket().emit("offer".equals(type) ? SocketEvents.offer : SocketEvents.answer, json);
                SocketClient.get().socket().emit(SocketEvents.answer, json);

            }
        }, mSdpMediaConstraints);
    }


    /**
     * 普通摄像头
     *
     * @return
     */
    private VideoCapturer createVideoCapturer() {
        CameraEnumerator cameraEnumerator;
        if (Camera2Enumerator.isSupported(mContext)) {
            cameraEnumerator = new Camera2Enumerator(this);
            Log.e(TAG, "createVideoCapturer: use Camera2Enumerator");
        } else {
            cameraEnumerator = new Camera1Enumerator(false);
            Log.e(TAG, "createVideoCapturer: use Camera1Enumerator");
        }
        String[] deviceNames = cameraEnumerator.getDeviceNames();
        Logging.d("MainActivity", "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (cameraEnumerator.isFrontFacing(deviceName)) {
                Logging.d("MainActivity", "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = cameraEnumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        Logging.d("MainActivity", "Looking for other cameras.");
        for (String deviceName2 : deviceNames) {
            if (!cameraEnumerator.isFrontFacing(deviceName2)) {
                Logging.d("MainActivity", "Creating other camera capturer.");
                VideoCapturer videoCapturer2 = cameraEnumerator.createCapturer(deviceName2, null);
                if (videoCapturer2 != null) {
                    return videoCapturer2;
                }
            }
        }
        return null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSubscribe(SocketEvent socketEvent) {
        String json = socketEvent.json;
        String event = socketEvent.event;
        Log.e(TAG, "onSubscribe: event; " + event);
        if (SocketEvents.userChat.equals(event)) {
            String chatInfoModelJson = json;
            ChatInfoModel chatInfoModel = mGson.fromJson(chatInfoModelJson, ChatInfoModel.class);
            if (ChatInfoModelParam.ChatInfo_ChatEnd.equals(chatInfoModel.chatInfo)) {
              /*  if (mVideoChatInterface != null) {
                    mVideoChatInterface.onVideoChatEnd(chatInfoModel);
                }*/
                Toast.makeText(mContext, chatInfoModel.fromUserModel.userName + " 已挂断！", Toast.LENGTH_SHORT).show();
                finish();
            } else if (ChatInfoModelParam.ChatInfo_Timeout.equals(chatInfoModel.chatInfo)) {
               /*  if (mVideoChatInterface != null) {
                   mVideoChatInterface.onVideoChatTimeout(chatInfoModel);
                }*/
            }
        } else if (SocketEvents.videoChatAgree.equals(event)) {
            callerOffer();
        } else if (SocketEvents.offer.equals(event)) {
            //被呼叫端 callee do【callee 收到 offer】
            VideoChatSdpInfoModel videoChatSdpInfoModel = mGson.fromJson(json, VideoChatSdpInfoModel.class);
            UserModel callerUserModel = videoChatSdpInfoModel.fromUserModel;
            UserModel calleeUserModel = videoChatSdpInfoModel.toUserModel;
            //4 B 通过服务器收到 A 的offer信令
            SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER, videoChatSdpInfoModel.description);
            //5 B 将 A 的offer信令中所包含的的SDP描述符提取出来，通过PC所提供的setRemoteDescription()方法交给B 的PC实例
            mPeerConnection.setRemoteDescription(new SdpObserverListener() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.e(TAG, "B setRemoteDescription onCreateSuccess: " + sessionDescription);
                }
            }, sessionDescription);
            //
            //6 B 通过PC所提供的createAnswer()方法建立一个包含B 的SDP描述符answer信令
            calleeAnswer();
            //被呼叫端 显示挂断
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//
                }
            });
        } else if (SocketEvents.answer.equals(event)) {
            //呼叫端 caller do【caller 收到 answer】
            VideoChatSdpInfoModel videoChatSdpInfoModel = mGson.fromJson(json, VideoChatSdpInfoModel.class);
            UserModel calleeUserModel = videoChatSdpInfoModel.fromUserModel;
            UserModel callerUserModel = videoChatSdpInfoModel.toUserModel;
            //9  A 通过服务器收到B 的answer信令
            SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, videoChatSdpInfoModel.description);
            //10  A 接收到B 的answer信令后，将其中B 的SDP描述符提取出来，调用setRemoteDescripttion()方法交给 A 自己的PC实例
//        peerConnection.setRemoteDescription(new SimpleSdpObserver(), sessionDescription);
            mPeerConnection.setRemoteDescription(new SdpObserverListener() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.e(TAG, "A setRemoteDescription onCreateSuccess: " + sessionDescription);
                }
            }, sessionDescription);
            //
            //呼叫端 显示挂断
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //
                }
            });
        } else if (SocketEvents.candidate.equals(event)) {
            VideoChatInfoModel videoChatInfoModel = mGson.fromJson(json, VideoChatInfoModel.class);
//            UserModel xxUserModel = videoChatInfoModel.fromUserModel;
            //
            String sdpMid = videoChatInfoModel.sdpMid;
            String _sdpMLineIndex = videoChatInfoModel.sdpMLineIndex;
            String sdp = videoChatInfoModel.sdp;
            //
            int sdpMLineIndex = Integer.valueOf(_sdpMLineIndex);
            IceCandidate remoteIceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
            mPeerConnection.addIceCandidate(remoteIceCandidate);
        }
    }


    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        release();
        super.onDestroy();
    }

    private void release() {
        if (mPeerConnection != null) {
            mPeerConnection.dispose();
            mPeerConnection = null;
        }
        //
        if (mLocalAudioSource != null) {
            mLocalAudioSource.dispose();
            mLocalAudioSource = null;
        }
        if (mLocalVideoSource != null) {
            mLocalVideoSource.dispose();
            mLocalVideoSource = null;
        }
        if (mLocalAudioTrack != null) {
            try {
                mLocalAudioTrack.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mLocalAudioTrack = null;
        }
        if (mLocalVideoTrack != null) {
            try {
                mLocalVideoTrack.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mLocalVideoTrack = null;
        }
        if (mEglBase != null) {
            mEglBase.release();
            mEglBase = null;
        }
        if (mLocalSurfaceViewRenderer != null) {
            mLocalSurfaceViewRenderer.clearImage();
            mLocalSurfaceViewRenderer.release();
            mLocalSurfaceViewRenderer = null;
        }
        if (mRemoteSurfaceViewRenderer != null) {
            mRemoteSurfaceViewRenderer.clearImage();
            mRemoteSurfaceViewRenderer.release();
            mRemoteSurfaceViewRenderer = null;
        }
        if (mLocalVideoCapturer != null) {
            try {
                mLocalVideoCapturer.stopCapture();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mLocalVideoCapturer.dispose();
            mLocalVideoCapturer = null;
        }

        if (mPeerConnectionFactory != null) {
            mPeerConnectionFactory.dispose();
            mPeerConnectionFactory = null;
        }
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();
    }
}
