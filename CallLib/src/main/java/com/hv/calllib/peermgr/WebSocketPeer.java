
/**
 * Created by liuhongyu on 2016/12/17.
 * 从此信令端可以获取不同的信令接口
 * getP2PPeer
 * getConferencePeer
 * getBroadcastPeer
 * getGroupPeer
 */

package com.hv.calllib.peermgr;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;


import java.util.HashMap;

import com.hv.calllib.live.LiveCommand;
import com.hv.calllib.live.WebSocketLiveComand;
import com.hv.calllib.peermgr.group.GroupCommand;
import com.hv.calllib.peermgr.group.WebSocketGroupComand;

public class WebSocketPeer implements SSLWebSocketChannel.WebSocketChannelEvents {

    private static final String TAG         = "WebSocketPeer";
    private GroupCommand mGroupComandPeer = null;
    private LiveCommand mLiveComandPeer = null;

    private String hostUserID   = null;         //  本端用户id

    public enum ConnectionState { NEW, CONNECTING,CONNECTED, REGISTERING,REGISTERED, CLOSED, ERROR }
    public enum ClientPeerState{ NONE,P2P,ONECALLONE,GROUP,CONFERENCE,LIVEVIDEO }//  对应p2p(纯粹的p2p)，1v1(过流媒体）,群组，会议，直播

    private WebSocketPeer.SignalingEvents          events;
    private final Handler                          handler;
    private SSLWebSocketChannel                    wssChannel;

    private WebSocketPeer.ConnectionState          mPeerState;
    private ClientPeerState                        mClientState;

    private static HashMap<String,WebSocketPeer>   mWebSocketPeerMgr = new HashMap<String,WebSocketPeer>();

    private Context context = null;
    /**
     *  单例实现websocket 的信令端（一个应用只维持一个信令连接
     * @return
     */
    public static WebSocketPeer GetWebSocketPeer(Context context, String peer ) {
        WebSocketPeer wsPeer = null;

        if ( mWebSocketPeerMgr == null ) {
            mWebSocketPeerMgr = new HashMap<String,WebSocketPeer>();
        }

        wsPeer = mWebSocketPeerMgr.get(peer);

        if( wsPeer == null ) {
            wsPeer = new WebSocketPeer(context);

            mWebSocketPeerMgr.put(peer,wsPeer);
        }

        return wsPeer;
    }

    public static void ReleaseWebSocketPeer( String peer ) {
        WebSocketPeer wsPeer = null;
        if ( mWebSocketPeerMgr != null ) {
            wsPeer = mWebSocketPeerMgr.get(peer);
            if( wsPeer != null ) {
                mWebSocketPeerMgr.remove(peer);
                wsPeer.Release();
                wsPeer = null;
            }
        }
    }

    public void RegisterEvent( WebSocketPeer.SignalingEvents events ) {
        this.events = events;
    }

    public String GetHostUID() {
        return hostUserID;
    }

    private WebSocketPeer(Context context) {

        this.context = context;

        mClientState = ClientPeerState.NONE;

        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        mPeerState = WebSocketPeer.ConnectionState.NEW;



        if( wssChannel == null ) {
            wssChannel = new SSLWebSocketChannel(context, handler, this);
            mPeerState = ConnectionState.NEW;
        }
    }

    public void Release() {
        if( mGroupComandPeer != null ){
            mGroupComandPeer = null;
        }

        if( wssChannel!= null ) {
            //wssChannel.disconnect(true);
            wssChannel = null;
            /*
            handler.post(new Runnable() {
                @Override
                public void run() {
                    wssChannel.disconnect(true);
                    wssChannel = null;
                }
            });
            */
        }
    }

    public SSLWebSocketChannel GetSSLWebSocketChannel() {
        return wssChannel;
    }

    public WebSocketPeer.ConnectionState GetConnectState() {
        return this.mPeerState;
    }

    public void ConnectTestServer(final String uid, final String wssUrl ) {

        hostUserID = uid;


        handler.post(new Runnable() {
            @Override
            public void run() {
                // Connect and register WebSocket client.
                if( mPeerState == ConnectionState.NEW || mPeerState ==  ConnectionState.CLOSED  ) {
                    mPeerState = ConnectionState.CONNECTING;
                    wssChannel.connect(wssUrl,false);
                }
            }
        });
    }

    @Override
    public void onWebSocketMessage(String message) {

        if ( mPeerState != ConnectionState.CONNECTED  ) {
            Log.e(TAG, "Got WebSocket message in non registered state.");
            return;
        }

        ProcessDefault( message );
    }

    private void ProcessDefault( String message )
    {
        if(mClientState == ClientPeerState.GROUP) {
            if( mGroupComandPeer!= null ){
                mGroupComandPeer.onSocketMessage( message );
            }
        }else if(mClientState == ClientPeerState.LIVEVIDEO) {
            if( mLiveComandPeer!= null ){
                mLiveComandPeer.onSocketMessage( message );
            }
        }
    }

    @Override
    public void onWebSocketClose() {
        mPeerState =  ConnectionState.CLOSED;
        events.onWebSocketClose();
    }

    @Override
    public void onWebSocketError(String description) {
        //mPeerState =  ConnectionState.CLOSED;
        reportError("WebSocket error: " + description);
    }

    @Override
    public void onWebSocketOpen() {

        mPeerState = ConnectionState.CONNECTED;
        if (events != null) {
            events.onRegistered(true);
        }
    }

    public Handler GetHandler() {
        return this.handler;
    }

    public GroupCommand getGroupCommand() {
        if( mClientState == ClientPeerState.NONE || mClientState == ClientPeerState.GROUP ) {
            if (mGroupComandPeer == null) {
                mGroupComandPeer = new WebSocketGroupComand( handler,wssChannel );
            }

            mClientState = ClientPeerState.GROUP;

            mGroupComandPeer.SetNetState(mPeerState);

            return mGroupComandPeer;
        }else {
            return null;
        }
    }
    //


    public LiveCommand getLiveCommand() {
        if( mClientState == ClientPeerState.NONE || mClientState == ClientPeerState.LIVEVIDEO ) {
            if (mLiveComandPeer == null) {
                mLiveComandPeer = new WebSocketLiveComand( handler,wssChannel );
            }

            mClientState = ClientPeerState.LIVEVIDEO;

            mLiveComandPeer.SetNetState(mPeerState);

            return mLiveComandPeer;
        }else {
            return null;
        }
    }

    // Helper functions.
    private void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mPeerState != WebSocketPeer.ConnectionState.ERROR) {
                    mPeerState = WebSocketPeer.ConnectionState.ERROR;
                    events.onWebSocketError(errorMessage);
                }
            }
        });
    }

    public boolean isRegistered() {
        return ( mPeerState == ConnectionState.REGISTERED )?true:false;
    }
    /**
     * Callback interface for messages delivered on signaling channel.
     *
     * <p>Methods are guaranteed to be invoked on the UI thread of |activity|.
     */
    public interface SignalingEvents {
        /**
         * 注册回调
         * @param status
         */
        public void onRegistered( boolean status);

        /**
         * 来电呼叫（包括p2p/conference/group都会回调这个函数
         */
        public void OnIncomingCall(ClientPeerState type,String from );

        /**
         *  远端用户状态变化
         *  在线／离线／掉线
         */
        public void onPeerStatusChange( int status ) ;

        /**
         * 通道被关闭
         */
        public void onWebSocketClose();

        /**
         * 通道发生错误
         */
        public void onWebSocketError(final String description);
    }
}
