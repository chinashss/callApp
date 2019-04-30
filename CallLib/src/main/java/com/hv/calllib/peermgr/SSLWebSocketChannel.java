package com.hv.calllib.peermgr;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.hv.calllib.websocket.WebSocketConnection;


/**
 * Created by liuhongyu on 2017/5/1.
 */

public class SSLWebSocketChannel {
    private static final String TAG = "WSChannelPeerClient"+"CallManager";

    private static final int CLOSE_TIMEOUT                   = 1000;
    private final SSLWebSocketChannel.WebSocketChannelEvents events ;
    private final Handler handler;
    private Context       mApplicationContext                = null;

    private WebSocketConnection ws                           = null;
    private SSLWebSocketChannel.WebSocketObserver wsObserver = null;

    private String wsServerUrl             = "";
    private String clientID                = "";

    private SSLWebSocketChannel.WebSocketConnectionState state;

    private final Object closeEventLock = new Object();
    private boolean closeEvent;

    // WebSocket的发送队列
    // 对于p2p来讲，只有注册后才能发送后续消息，否则都是缓存消息，直到注册成功后，再刷新消息队列
    private final LinkedList<String> wsSendQueue;

    /**
     * Possible WebSocket connection states.
     */
    public enum WebSocketConnectionState { NEW, CONNECTED, CLOSED, ERROR }

    /**
     * Callback interface for messages delivered on WebSocket.
     * All events are dispatched from a looper executor thread.
     */
    public interface WebSocketChannelEvents {
        void onWebSocketOpen();
        void onWebSocketMessage(final String message);
        void onWebSocketClose();
        void onWebSocketError(final String description);
    }

    public SSLWebSocketChannel( Context ctx,Handler handler, SSLWebSocketChannel.WebSocketChannelEvents events) {
        mApplicationContext = ctx;
        this.handler        = handler;
        this.events         = events;
        //roomID            = null;
        clientID            = null;
        wsSendQueue         = new LinkedList<String>();

        state = SSLWebSocketChannel.WebSocketConnectionState.NEW;
    }

    public SSLWebSocketChannel.WebSocketConnectionState getState() {
        return state;
    }

    public void connect( final String wsUrl,boolean forceOrigin ) {
        checkIfCalledOnValidThread();
        if (state != SSLWebSocketChannel.WebSocketConnectionState.NEW&&state != SSLWebSocketChannel.WebSocketConnectionState.CLOSED) {
            Log.e(TAG, "WebSocket is already connected.");
            return;
        }
        wsServerUrl = wsUrl;
        closeEvent = false;

        Log.i(TAG, "Connecting WebSocket to: " + wsUrl );
        ws = new WebSocketConnection();

        wsObserver = new SSLWebSocketChannel.WebSocketObserver();

        //try
        {
            //wsServerUrl = "ws://192.168.11.121:8089/ws";
            //forceOrigin = true;
            if( forceOrigin == true ) {
                Map<String,String> headers = new HashMap<String,String>();
                headers.put("Origin","http://192.168.11.18");
                ws.connect( headers,mApplicationContext,wsServerUrl,"groupcall.cer",true, wsObserver);
            }else {
                //wsServerUrl = "ws://192.168.11.121:8089/ws";
                ws.connect( null,mApplicationContext,wsServerUrl,"groupcall.cer",true, wsObserver);
            }

        } /*catch (WebSocketException e) {
            reportError("WebSocket connection error: " + e.getMessage());
        }*/
    }

    public void send(String message) {
        checkIfCalledOnValidThread();

        if( state == SSLWebSocketChannel.WebSocketConnectionState.CONNECTED ) {
            Log.i(TAG, "C->WSS: " + message);
            ws.sendTextMessage(message);
        }else {
            Log.e(TAG, "WebSocket send() in error or closed state : " + message);
        }
    }

    //  发送到待发送队列里
    public void SendMessageQueue(String message) {
        checkIfCalledOnValidThread();
        Log.i(TAG, "WS ACC: " + message);
        wsSendQueue.add(message);
    }
    /**
     *  强制刷新消息队列的消息发送出去
     */
    public void flush() {
        checkIfCalledOnValidThread();
        // Send any previously accumulated messages.
        for (String sendMessage : wsSendQueue) {
            send(sendMessage);
        }

        wsSendQueue.clear();
    }

    public void disconnect(boolean waitForComplete) {
        checkIfCalledOnValidThread();
        Log.i(TAG, "Disconnect WebSocket. State: " + state);

        // Close WebSocket in CONNECTED or ERROR states only.
        if (state == SSLWebSocketChannel.WebSocketConnectionState.CONNECTED || state == SSLWebSocketChannel.WebSocketConnectionState.ERROR) {
            ws.disconnect( true );
            state = SSLWebSocketChannel.WebSocketConnectionState.CLOSED;

            // Wait for websocket close event to prevent websocket library from
            // sending any pending messages to deleted looper thread.
            if (waitForComplete) {
                synchronized (closeEventLock) {
                    while (!closeEvent) {
                        try {
                            closeEventLock.wait(CLOSE_TIMEOUT);
                            break;
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Wait error: " + e.toString());
                        }
                    }
                }
            }
        }
        Log.i(TAG, "Disconnecting WebSocket done.");
    }

    protected void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (state != SSLWebSocketChannel.WebSocketConnectionState.ERROR) {
                    state = SSLWebSocketChannel.WebSocketConnectionState.ERROR;
                    events.onWebSocketError(errorMessage);
                }
            }
        });
    }

    // Helper method for debugging purposes. Ensures that WebSocket method is
    // called on a looper thread.
    private void checkIfCalledOnValidThread() {
        if (Thread.currentThread() != handler.getLooper().getThread()) {
            throw new IllegalStateException("WebSocket method is not called on valid thread");
        }
    }

    /*
        websocket 事件监听
     */
    private class WebSocketObserver implements WebSocketConnection.ConnectionHandler {
        @Override
        public void onOpen() {
            Log.i(TAG, "WebSocket connection opened to: " + wsServerUrl);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    state = SSLWebSocketChannel.WebSocketConnectionState.CONNECTED;
                    // Check if we have pending register request.
                    events.onWebSocketOpen();
                }
            });
        }

        @Override
        public void onClose( final int code, final String reason) {
            Log.i(TAG, "WebSocket connection closed. Code: " + code + ". Reason: " + reason + ". State: "
                    + state);
            synchronized (closeEventLock) {
                closeEvent = true;
                closeEventLock.notify();
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (state != SSLWebSocketChannel.WebSocketConnectionState.CLOSED) {
                        state = SSLWebSocketChannel.WebSocketConnectionState.CLOSED;
                        if( events!= null ) {
                            if (code < 0 ) {
                                events.onWebSocketError(reason);
                            } else {
                                events.onWebSocketClose();
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onTextMessage(String payload) {
            Log.i(TAG, "WSS->C: " + payload);
            final String message = payload;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (state == SSLWebSocketChannel.WebSocketConnectionState.CONNECTED ) {
                        events.onWebSocketMessage(message);
                    }
                }
            });
        }

        @Override
        public void onRawTextMessage(byte[] payload) {}

        @Override
        public void onBinaryMessage(byte[] payload) {}
    }
}
