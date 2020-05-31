package me.nankeen.blinkandsink;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import android.util.Log;

class ProxyVideoSink implements VideoSink {
    private VideoSink target;

    @Override
    synchronized public void onFrame(VideoFrame frame) {
        // TODO: Process video frame here!
        Log.d("ProxyVideoSink", "Videos and shit!");
        if (target == null) {
            Log.d("ProxyVideoSink", "Dropping frame in proxy because target is null.");
            return;
        }

        target.onFrame(frame);
    }

    synchronized public void setTarget(VideoSink target) {
        this.target = target;
    }
}
