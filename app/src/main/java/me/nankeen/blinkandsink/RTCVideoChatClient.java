package me.nankeen.blinkandsink;

import android.app.Application;
import android.content.Context;

import org.webrtc.Camera2Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

import static org.webrtc.EglBase.create;

public class RTCVideoChatClient {
    private static final String LOCAL_TRACK_ID = "local_track";
    private PeerConnectionFactory peerConnectionFactory;
    private VideoCapturer videoCapturer;
    private VideoSource localVideoSource;
    private ProxyVideoSink localVideoProxy;
    private PeerConnection peerConnection;
    private EglBase rootEglBase = create();
    private List<PeerConnection.IceServer> iceServer = new ArrayList<PeerConnection.IceServer>(){{
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
    }};

    public RTCVideoChatClient(Application context, PeerConnection.Observer observer) {
        initializePeerConnectionFactory(context);

        peerConnectionFactory = buildPeerConnectionFactory();
        videoCapturer = getVideoCapturer(context);
        localVideoSource = peerConnectionFactory.createVideoSource(false);
        peerConnection = buildPeerConnection(observer);
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
        Camera2Enumerator enumerator = new Camera2Enumerator(context);
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    public void initializeSurfaceView(SurfaceViewRenderer view) {
        view.setMirror(true);
        view.setEnableHardwareScaler(true);
        view.init(rootEglBase.getEglBaseContext(), null);
    }

    public void startLocalCapture(SurfaceViewRenderer localVideoOutput) {
        localVideoProxy = new ProxyVideoSink();
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), rootEglBase.getEglBaseContext());
        videoCapturer.initialize(surfaceTextureHelper, localVideoOutput.getContext(), localVideoSource.getCapturerObserver());
        videoCapturer.startCapture(320, 240, 60);
        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource);
        localVideoTrack.addSink(localVideoProxy);
        localVideoProxy.setTarget(localVideoOutput);
    }

    private PeerConnection buildPeerConnection(PeerConnection.Observer observer) {
        return peerConnectionFactory.createPeerConnection(iceServer, observer);
    }

    void onRemoteSessionReceived(SessionDescription description) {
        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
            }

            @Override
            public void onSetSuccess() {
            }

            @Override
            public void onCreateFailure(String s) {
            }

            @Override
            public void onSetFailure(String s) {
            }
        }, description);
    }

    void answer(SdpObserver observer) {
        peerConnection.createAnswer(observer, new MediaConstraints());
    }

    void addIceCandidate(IceCandidate iceCandidate) {
        peerConnection.addIceCandidate(iceCandidate);
    }
}
