package com.hv.calllib.websocket;

/**
 * Created by liuhongyu on 2017/4/30.
 */

import android.content.Context;
import android.util.Log;

import org.java_websocket.WebSocketFactory;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.client.WebSocketClient.WebSocketClientFactory;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Map;


import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.hv.calllib.LooperExecutor;

public class WebSocketConnection {

    private static final String TAG = "WebSocketConnection";
    private static final int CLOSE_TIMEOUT = 1000;

    private WebSocketConnectionState connectionState;
    private ConnectionHandler events;
    private ExtendedWebSocketClient client;
    private LooperExecutor executor;
    private final Object closeEventLock = new Object();
    private boolean closeEvent;

    private SSLManager               mSSLManager = null;

    protected WebSocketClientFactory webSocketClientFactory = null;
    private Map<String,String> mapWebSocketHeaders = null;

    public enum WebSocketConnectionState {
        CONNECTED, CLOSED, ERROR
    }

    public interface ConnectionHandler {
        public void onOpen();
        public void onClose(int code, String reason);
        public void onTextMessage(String payload);
        public void onRawTextMessage(byte[] payload);
        public void onBinaryMessage(byte[] payload);
    }

    private class SSLManager {
        private KeyStore keyStore;
        private boolean  usingSelfSigned = true;

        protected SSLManager( String cert, boolean selfSigned, Context context ){
            try {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);

                InitSSLCertificate( cert,selfSigned,context );

            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
                e.printStackTrace();
            }
        }

        public boolean isUsingSelfSigned() {
            return usingSelfSigned;
        }

        public void setUsingSelfSigned(boolean usingSelfSigned) {
            this.usingSelfSigned = usingSelfSigned;
        }

        public KeyStore getKeyStore() {
            return keyStore;
        }

        public void InitSSLCertificate(String cert, boolean selfSigned, Context context ) {
            CertificateFactory cf;
            try {
                cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream( context.getAssets().open(cert) );//"groupcall.cer"  context.getAssets().open(cert)
                Certificate ca = cf.generateCertificate(caInput);
                addTrustedCertificate("ca", ca);

                setUsingSelfSigned( selfSigned );

            } catch (CertificateException |IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * This methods can be used to add a self-signed SSL certificate to be trusted when establishing
         * connection.
         * @param alias is a unique alias for the certificate
         * @param cert is the certificate object
         */
        @SuppressWarnings("unused")
        public void addTrustedCertificate(String alias, Certificate cert){
            try {
                keyStore.setCertificateEntry(alias, cert);
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
        }
    }

    private class ExtendedWebSocketClient extends WebSocketClient {

        public ExtendedWebSocketClient( URI serverUri, ConnectionHandler events ) {
            super(serverUri, new Draft_17(),mapWebSocketHeaders,0);
        }

        @Override
        public void onOpen(final ServerHandshake handshakedata) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    connectionState = WebSocketConnectionState.CONNECTED;
                    events.onOpen();
                }
            });
        }

        @Override
        public void onClose(final int code, final String reason, final boolean remote) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (connectionState != WebSocketConnectionState.CLOSED) {
                        connectionState = WebSocketConnectionState.CLOSED;
                        events.onClose(code, reason);
                    }
                }
            });
        }

        @Override
        public void onError(final Exception e) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (connectionState != WebSocketConnectionState.ERROR) {
                        connectionState = WebSocketConnectionState.ERROR;
                        events.onClose(-1,"");
                    }
                }
            });
        }

        @Override
        public void onMessage(final String message) {

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (connectionState == WebSocketConnectionState.CONNECTED) {
                        events.onTextMessage( message );
                    }
                }
            });
        }

        public boolean isConnected()
        {
            return (connectionState == WebSocketConnectionState.CONNECTED)?true:false;
        }
    }

    public WebSocketConnection() {
        this.connectionState = WebSocketConnectionState.CLOSED;
        this.events          = null;
        this.executor        = new LooperExecutor();;
        this.client          = null;

        executor.requestStart();
    }

    public void connect( Map<String,String> mapHeaders,Context ctx,final String wsUri,final String cert,boolean selfSigned,ConnectionHandler wsHandler ) {
        String scheme;
        this.events          = wsHandler;

        mapWebSocketHeaders  = mapHeaders;
        try {
            scheme = new URI(wsUri).getScheme();
            if (scheme.equals("https") || scheme.equals("wss")) {
                // Create an SSLContext that uses our or default TrustManager
                SSLContext sslContext = SSLContext.getInstance("TLS");
                mSSLManager = new SSLManager( cert,selfSigned,ctx );

                if (mSSLManager.isUsingSelfSigned()) {
                    // Create a TrustManager that trusts the CAs in our KeyStore
                    String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                    tmf.init(mSSLManager.getKeyStore());
                    sslContext.init(null, tmf.getTrustManagers(), null);
                } else {
                    sslContext.init(null, null, null);
                }
                webSocketClientFactory = new DefaultSSLWebSocketClientFactory(sslContext);
            }
        }catch(URISyntaxException e) {
            e.printStackTrace();
        }catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }catch(KeyStoreException e) {
            e.printStackTrace();
        }catch(KeyManagementException e) {
            e.printStackTrace();
        }

        try{
            //serverUri, draft, null, 0
            client = new ExtendedWebSocketClient(new URI(wsUri), events);

            if (webSocketClientFactory != null) {
                client.setWebSocketFactory(webSocketClientFactory);
            }

            executor.execute(new Runnable() {
                public void run() {
                    closeEvent = false;

                    //mWebSocketHeaders
                    client.connect();
                }
            });

        }catch(Exception e ) {
            Log.e(TAG, "connect", e);
        }

    }

    public boolean isConnected()
    {
        return client.isConnected();
    }

    public void disconnect( final boolean waitForComplete) {
        executor.execute(new Runnable() {
            public void run() {
                if (client.getConnection().isOpen()) {
                    client.close();
                    connectionState = WebSocketConnectionState.CLOSED;

                    if (waitForComplete) {
                        synchronized (closeEventLock) {
                            while (!closeEvent) {
                                try {
                                    closeEventLock.wait(CLOSE_TIMEOUT);
                                    break;
                                } catch (InterruptedException e) {
                                    Log.e(TAG, "WebSocket wait error: " + e.toString());
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public WebSocketConnectionState getConnectionState(){
        return connectionState;
    }

    public final void setWebSocketFactory( WebSocketClientFactory wsf ) {
        client.setWebSocketFactory(wsf);
    }

    public final WebSocketFactory getWebSocketFactory() {
        return client.getWebSocketFactory();
    }

    public void sendTextMessage( final String payload){

        executor.execute(new Runnable() {
            public void run() {
                if(connectionState == WebSocketConnectionState.CONNECTED ) {
                    client.send( payload );
                }
                else{
                    Log.e(TAG, "connectionState: " + connectionState);
                }
            }
        });
    }
}
