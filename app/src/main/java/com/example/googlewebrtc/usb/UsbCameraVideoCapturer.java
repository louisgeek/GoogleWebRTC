package com.example.googlewebrtc.usb;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

import com.example.googlewebrtc.R;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import org.webrtc.CameraVideoCapturer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by louisgeek on 2019/7/22.
 */
public class UsbCameraVideoCapturer implements CameraVideoCapturer, USBMonitor.OnDeviceConnectListener, IFrameCallback {
    private static final String TAG = "UsbCameraVideoCapturer";
    private Context mContext;
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private SurfaceViewRenderer mSurfaceViewRenderer;
    private SurfaceTextureHelper mSurfaceTextureHelper;
    private SurfaceTexture mSurfaceTexture;
    private CapturerObserver mCapturerObserver;
    private Executor executor = Executors.newSingleThreadExecutor();

    public UsbCameraVideoCapturer(Context context, SurfaceViewRenderer surfaceViewRenderer) {
        mContext = context;
        mSurfaceViewRenderer = surfaceViewRenderer;
       /* executor.execute(new Runnable() {
            @Override
            public void run() {
                mUSBMonitor = new USBMonitor(mContext, UsbCapturer.this);
                mUSBMonitor.register();
            }
        });*/
        mSurfaceViewRenderer.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                // 当Surface可用的时候打开摄像头
                openUSUCamera();
                Log.e(TAG, "surfaceCreated: qfzqfz  openUSUCamera");
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.e(TAG, "surfaceChanged: qfzqfz  openUSUCamera");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.e(TAG, "surfaceDestroyed: qfzqfz  openUSUCamera");
            }
        });
        mUSBMonitor = new USBMonitor(mContext, UsbCameraVideoCapturer.this);
        mUSBMonitor.register();
    }

    // 请求打开USB摄像头，SystemUI已修改 不会弹出对话框等待用户确认； 直接授权
    private void openUSUCamera() {
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(mContext, R.xml.device_filter);
        List<UsbDevice> deviceList = mUSBMonitor.getDeviceList(filter);
        if (deviceList.size() > 0) {
            mUSBMonitor.requestPermission(deviceList.get(0));
        } else {
            Log.e(TAG, "onCreate: qfzqfz no camera device");
        }
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        this.mSurfaceTextureHelper = surfaceTextureHelper;
        this.mCapturerObserver = capturerObserver;
        this.mSurfaceTexture = surfaceTextureHelper.getSurfaceTexture();
    }

    @Override
    public void startCapture(int i, int i1, int i2) {
        if (mUVCCamera != null) {
            mUVCCamera.startPreview();
        }
       /* if (mCapturerObserver != null) {
            mCapturerObserver.onCapturerStarted(true);
        }*/
    }

    @Override
    public void stopCapture() throws InterruptedException {
        if (mUVCCamera != null) {
            mUVCCamera.stopPreview();
            mUVCCamera.close();
            mUVCCamera.destroy();
        }
       /*
        ## maybe case
        A/libc: Fatal signal 6 (SIGABRT), code -6 in tid 30575 (ft.mob.dw.debug)
        ############
        if (mCapturerObserver != null) {
            mCapturerObserver.onCapturerStopped();
        }*/
    }

    @Override
    public void changeCaptureFormat(int i, int i1, int i2) {

    }

    @Override
    public void dispose() {
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
            mUSBMonitor.destroy();
        }

    }

    @Override
    public boolean isScreencast() {
        return false;
//        return true;
    }

    @Override
    public void onAttach(UsbDevice device) {
//        mUSBMonitor.requestPermission(device);
        Log.d(TAG, "onAttach() called with: device = [" + device + "]");
    }

    @Override
    public void onDetach(UsbDevice device) {
        Log.d(TAG, "onDetach() called with: device = [" + device + "]");
    }


    @Override
    public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
        if (mUVCCamera != null) {
            mUVCCamera.destroy();
        }
        mUVCCamera = new UVCCamera();
        //
        executor.execute(new Runnable() {
            @Override
            public void run() {
                //
                mUVCCamera.open(ctrlBlock);
                try {
                    mUVCCamera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_YUYV);
                    //可以
//                    mUVCCamera.setPreviewDisplay(mSurfaceViewRenderer.getHolder());
                    //也可以
                    //some devices maybe need
                    mSurfaceTexture.setDefaultBufferSize(1280, 720);
                    mUVCCamera.setPreviewTexture(mSurfaceTexture);
                    //=======================
                    mUVCCamera.setFrameCallback(UsbCameraVideoCapturer.this, UVCCamera.PIXEL_FORMAT_NV21);
                    mUVCCamera.startPreview();
                    //
//                    mCapturerObserver.onCapturerStarted(true);
                } catch (final IllegalArgumentException e) {
                    e.printStackTrace();
                }
                //
            }
        });
    }

    @Override
    public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
        if (mUVCCamera != null) {
            mUVCCamera.close();
        }
    }

    @Override
    public void onCancel() {

    }


    @Override
    public void onFrame(ByteBuffer frame) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] bufferArray = new byte[frame.remaining()];
                frame.get(bufferArray);
                //
                int frameRotation = 0;
                long timestamp = SystemClock.elapsedRealtime();
                long timestampNs = TimeUnit.MILLISECONDS.toNanos(timestamp);
                //
                /*VideoFrame.I420Buffer i420Buffer = new NV21Buffer(bufferArray, 1280,
                        720, null).toI420();
                VideoFrame videoFrame = new VideoFrame(i420Buffer, frameRotation, timestampNs);
                //
                mCapturerObserver.onFrameCaptured(videoFrame);
                videoFrame.release();*/
                //
//                byte[] data, int width, int height, int rotation, long timeStamp
                mCapturerObserver.onByteBufferFrameCaptured(bufferArray, 1280, 720, frameRotation, timestamp);
            }
        });
    }

    @Override
    public void switchCamera(CameraSwitchHandler cameraSwitchHandler) {

    }

    @Override
    public void addMediaRecorderToCamera(MediaRecorder mediaRecorder, MediaRecorderHandler mediaRecorderHandler) {

    }

    @Override
    public void removeMediaRecorderFromCamera(MediaRecorderHandler mediaRecorderHandler) {

    }


}