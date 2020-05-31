package me.nankeen.blinkandsink;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

interface SignalingClientListener {
    void onConnectionEstablished();
    void onOfferReceived(SessionDescription description);
    void onAnswerReceived(SessionDescription description);
    void onIceCandidateReceived(IceCandidate iceCandidate);
}
