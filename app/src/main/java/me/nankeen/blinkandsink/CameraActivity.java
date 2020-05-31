package me.nankeen.blinkandsink;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CameraActivity extends AppCompatActivity {

    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private RTCVideoChatClient rtcClient;
    private SurfaceViewRenderer remoteVideoView;
    private SurfaceViewRenderer localVideoView;
    private ProgressBar remoteLoading;

    private SignalingClient signalingClient;
    private WebSocket signalingClientWS;
    private OkHttpClient okHttpClient;
    private SdpObserver sdpObserver = new SdpAdapter("SDP");

    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        remoteVideoView = findViewById(R.id.remoteVideoView);
        localVideoView = findViewById(R.id.localVideoView);
        remoteLoading = findViewById(R.id.remoteLoading);
        okHttpClient = new OkHttpClient();
        checkCameraPermissions();
    }

    private void checkCameraPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, REQUEST_CAMERA_PERMISSION);
        } else {
            onCameraPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        } else {
            onCameraPermissionDenied();
        }
    }

    private void onCameraPermissionDenied() {
        Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void onCameraPermissionGranted() {
        rtcClient = new RTCVideoChatClient(
            getApplication(),
            new PeerConnectionAdapter("local connection") {
                @Override
                public void onIceCandidate(IceCandidate iceCandidate) {
                    super.onIceCandidate(iceCandidate);
                    signalingClientWS.send(gson.toJson(iceCandidate));
                    rtcClient.addIceCandidate(iceCandidate);
                }

                @Override
                public void onAddStream(MediaStream mediaStream) {
                    super.onAddStream(mediaStream);
                    VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                    runOnUiThread(() -> {
                        remoteVideoTrack.addSink(remoteVideoView);
                    });
                }
            });
        rtcClient.initializeSurfaceView(remoteVideoView);
        rtcClient.initializeSurfaceView(localVideoView);
        rtcClient.startLocalCapture(localVideoView);
        // TODO: Signalling client change the backend
        Request request = new Request.Builder().url("ws://echo.websocket.org").build();
        signalingClient = new SignalingClient(new SignalingClientListener() {
            @Override
            public void onConnectionEstablished() {
                // TODO: Add stuff to this?
            }

            @Override
            public void onOfferReceived(SessionDescription description) {
                rtcClient.onRemoteSessionReceived(description);
                rtcClient.answer(sdpObserver);
                remoteLoading.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnswerReceived(SessionDescription description) {
                rtcClient.onRemoteSessionReceived(description);
                remoteLoading.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onIceCandidateReceived(IceCandidate iceCandidate) {
                rtcClient.addIceCandidate(iceCandidate);
            }
        });

        signalingClientWS = okHttpClient.newWebSocket(request, signalingClient);
        // TODO: Call!
    }

}