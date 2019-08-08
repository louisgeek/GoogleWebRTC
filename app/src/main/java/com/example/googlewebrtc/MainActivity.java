package com.example.googlewebrtc;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
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
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private static final String TAG = "MainActivity";
    private EglBase mEglBase;
    private List<PeerConnection.IceServer> iceServers = new ArrayList();
    private AudioTrack mLocalAudioTrack;
    private VideoCapturer mLocalVideoCapturer;
    private VideoSource mLocalVideoSource;
    private VideoTrack mLocalVideoTrack;
    private SurfaceViewRenderer mLocalSurfaceViewRenderer;
    private SurfaceViewRenderer mRemoteSurfaceViewRenderer;
    /* access modifiers changed from: private */
    public Gson mGson = new Gson();
    /* access modifiers changed from: private */
    public String offerIp;
    private PeerConnectionFactory mPeerConnectionFactory;
    /* access modifiers changed from: private */
    public PeerConnection remotePeer;
    private SurfaceTextureHelper mSurfaceTextureHelper;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRemoteSurfaceViewRenderer = findViewById(R.id.remote_gl_surface_view);
        mLocalSurfaceViewRenderer = findViewById(R.id.glview_call);
        //
        init();
    }

    private void init() {
        mEglBase = EglBase.create();
        //
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
        //
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions
                .builder(mContext.getApplicationContext())
                .setEnableInternalTracer(true)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                mEglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext());
        mPeerConnectionFactory = new PeerConnectionFactory(options, defaultVideoEncoderFactory, defaultVideoDecoderFactory);
        //
        mLocalSurfaceViewRenderer.init(this.mEglBase.getEglBaseContext(), null);
        mLocalSurfaceViewRenderer.setZOrderMediaOverlay(true);
        mRemoteSurfaceViewRenderer.init(this.mEglBase.getEglBaseContext(), null);
        mRemoteSurfaceViewRenderer.setZOrderMediaOverlay(true);

        mSurfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", mEglBase.getEglBaseContext());
        mLocalVideoCapturer = createVideoCapturer();
        mLocalVideoCapturer.initialize(mSurfaceTextureHelper, mContext.getApplicationContext(),
                mLocalVideoSource.getCapturerObserver());
        mLocalVideoCapturer.startCapture(480, 320, 15);
        //
        mLocalVideoSource = mPeerConnectionFactory.createVideoSource(mLocalVideoCapturer);
        AudioSource localAudioSource = mPeerConnectionFactory.createAudioSource(new MediaConstraints());
        //mLocalVideoCapturer.isScreencast()
        mLocalVideoTrack = mPeerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, mLocalVideoSource);
        mLocalVideoTrack.setEnabled(true);
        mLocalAudioTrack = mPeerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, localAudioSource);
        mLocalAudioTrack.setEnabled(true);
        mLocalVideoTrack.addSink(this.mLocalSurfaceViewRenderer);
        //
        UDPHelper.openUDPPort(19999, new UDPMessageListener() {
            public void onMessageArrived(String message) {
                Log.i("MainActivity", message);
                if (MainActivity.this.remotePeer == null) {
                    MainActivity.this.start();
                }
                Message msg = (Message) MainActivity.this.mGson.fromJson(message, Message.class);
                MainActivity.this.offerIp = msg.offerip;
                if (1 == msg.tag) {
                    MainActivity.this.remotePeer.addIceCandidate((IceCandidate) MainActivity.this.mGson.fromJson(msg.json, IceCandidate.class));
                } else if (2 == msg.tag) {
                    MainActivity.this.remotePeer.setRemoteDescription(new CustomSdpObserver("remoteSetRemoteDesc"), (SessionDescription) MainActivity.this.mGson.fromJson(msg.json, SessionDescription.class));
                    MainActivity.this.startAnswer();
                }
            }

            public void onError(Exception e) {
            }
        });
    }


    private VideoCapturer createVideoCapturer() {
        CameraEnumerator cameraEnumerator = new Camera2Enumerator(this);
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


    public void start() {
        this.remotePeer = this.peerConnectionFactory.createPeerConnection(new RTCConfiguration(this.iceServers), (PeerConnection.Observer) new CustomPeerConnectionObserver("remotePeerCreation") {
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Message message = new Message();
                message.tag = 1;
                message.json = MainActivity.this.mGson.toJson((Object) iceCandidate);
                UDPHelper.sendUDPMessage(MainActivity.this.mGson.toJson((Object) message), MainActivity.this.offerIp, 19999);
            }

            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                MainActivity.this.gotRemoteStream(mediaStream);
            }
        });
        this.remotePeer.addTrack(this.localAudioTrack);
    }

    /* access modifiers changed from: private */
    public void startAnswer() {
        this.remotePeer.createAnswer(new CustomSdpObserver("remoteCreateAnswer") {
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                MainActivity.this.remotePeer.setLocalDescription(new CustomSdpObserver("remoteSetLocalDesc"), sessionDescription);
                Message message = new Message();
                message.tag = 2;
                message.json = MainActivity.this.mGson.toJson((Object) sessionDescription);
                UDPHelper.sendUDPMessage(MainActivity.this.mGson.toJson((Object) message), MainActivity.this.offerIp, 19999);
            }
        }, new MediaConstraints());
    }

    /* access modifiers changed from: private */
    public void gotRemoteStream(MediaStream stream) {
        for (AudioTrack audioTrack : stream.audioTracks) {
            audioTrack.setEnabled(true);
        }
    }

}
