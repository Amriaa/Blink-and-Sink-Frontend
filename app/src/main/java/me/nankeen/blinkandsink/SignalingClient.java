package me.nankeen.blinkandsink;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import android.util.Log;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

class SignalingClient extends WebSocketListener {
    SignalingClientListener listener;
    private Gson gson = new Gson();
    public SignalingClient(SignalingClientListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosing(webSocket, code, reason);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        super.onMessage(webSocket, text);
        JsonObject jsonObject = gson.fromJson(text, JsonObject.class);
        if (jsonObject.has("serverUrl")) {
            listener.onIceCandidateReceived(gson.fromJson(jsonObject, IceCandidate.class));
        } else if (jsonObject.has("type") && jsonObject.get("type").getAsString() == "OFFER") {
            listener.onOfferReceived(gson.fromJson(jsonObject, SessionDescription.class));
        } else if (jsonObject.has("type") && jsonObject.get("type").getAsString() == "ANSWER") {
            listener.onAnswerReceived(gson.fromJson(jsonObject, SessionDescription.class));
        }
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        super.onMessage(webSocket, bytes);
        Log.d(SignalingClient.class.getSimpleName(), "Received bytes! ignoring...");
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        super.onOpen(webSocket, response);
    }
}
