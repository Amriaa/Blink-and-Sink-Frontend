package me.nankeen.blinkandsink;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.webrtc.Camera2Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;

import androidx.annotation.Nullable;

import static org.webrtc.EglBase.create;

public class RTCVideoChatClient {
    private static final String LOCAL_TRACK_ID = "local_track";
    private @Nullable PeerConnectionFactory peerConnectionFactory;
    private @Nullable VideoCapturer videoCapturer;
    private @Nullable VideoSource localVideoSource;
    private @Nullable  SurfaceViewRenderer localVideoOutput;
    private EglBase rootEglBase = create();

    public RTCVideoChatClient(Application context, SurfaceViewRenderer localVideoOutput) {
        this.localVideoOutput = localVideoOutput;
        initializePeerConnectionFactory(context);
        initializeSurfaceView(localVideoOutput);

        peerConnectionFactory = buildPeerConnectionFactory();
        videoCapturer = getVideoCapturer(context);
        localVideoSource = peerConnectionFactory.createVideoSource(false);
    }

    private void initializePeerConnectionFactory(Application context) {
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
    }

    private PeerConnectionFactory buildPeerConnectionFactory() {
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(), true, true);

        return PeerConnectionFactory
                .builder()
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setOptions(new PeerConnectionFactory.Options())
                .createPeerConnectionFactory();
    }

    private VideoCapturer getVideoCapturer(Context context) {
        Camera2Enumerator emumerator = new Camera2Enumerator(context);
        Log.v("RTCVideoChatClient", emumerator.getDeviceNames()[0]);
        return emumerator.createCapturer(emumerator.getDeviceNames()[0], null);
    }

    private void initializeSurfaceView(SurfaceViewRenderer view) {
        view.setMirror(true);
        view.setEnableHardwareScaler(true);
        view.init(rootEglBase.getEglBaseContext(), null);
    }

    void startLocalCapture() {
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), rootEglBase.getEglBaseContext());
        videoCapturer.initialize(surfaceTextureHelper, localVideoOutput.getContext(), localVideoSource.getCapturerObserver());
    }
}
