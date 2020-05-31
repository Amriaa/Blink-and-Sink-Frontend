package me.nankeen.blinkandsink;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import android.util.Log;

public class SdpAdapter implements SdpObserver {


    private String tag;

    public SdpAdapter(String tag) {
        this.tag = "Blink and Sink" + tag;
    }

    private void log(String s) {
        Log.d(tag, s);
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        log("onCreateSuccess " + sessionDescription);
    }

    @Override
    public void onSetSuccess() {
        log("onSetSuccess ");
    }

    @Override
    public void onCreateFailure(String s) {
        log("onCreateFailure " + s);
    }

    @Override
    public void onSetFailure(String s) {
        log("onSetFailure " + s);
    }
}