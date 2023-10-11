package com.hawolt.lcu;

import com.hawolt.logger.Logger;
import com.hawolt.ui.SocketServer;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.*;

public class LCUWebSocket extends WebSocketClient {
    private static List<LCUWebSocket> openWebSockets = new ArrayList<>();
    private LeagueClient client;

    public LCUWebSocket(URI serverUri, Map<String, String> headers) {
        super(serverUri, new Draft_6455(), headers);
    }

    public static void launch() throws IOException, URISyntaxException {
        List<LeagueClient> clients = WMIC.retrieve();
        for (LeagueClient c : clients) {
            if (LCUWebSocket.isAdded(c)) continue;

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(("riot:" + c.getLeagueAuth()).getBytes()));
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");

            LCUWebSocket webSocket = new LCUWebSocket(new URI(
                    "wss://127.0.0.1:" + c.getLeaguePort()), headers);
            webSocket.client = c;
            LCUWebSocket.trustAllCerts(webSocket);
            webSocket.connect();
            openWebSockets.add(webSocket);
        }
    }

    public static void disconnect() {
        for (LCUWebSocket webSocket : LCUWebSocket.openWebSockets) {
            webSocket.close();
        }
        LCUWebSocket.openWebSockets.clear();
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        Logger.debug("Started LCU WebSocket");
        send("[5, \"OnJsonApiEvent\"]");
    }

    @Override
    public void onMessage(String s) {
        if (s.isEmpty())
            return;
        JSONObject response = new JSONArray(s).getJSONObject(2);
        JSONObject object = new JSONObject();
        object.put("type", "lcu");
        object.put("out", response);
        SocketServer.forward(object.toString());
    }

    public static boolean isAdded(LeagueClient c) {
        for (LCUWebSocket webSocket : LCUWebSocket.openWebSockets) {
            if (c.getLeaguePort().equals(webSocket.client.getLeaguePort()))
                return true;
        }
        return false;
    }

    public static void trustAllCerts(WebSocketClient webSocket){
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
        try {
            SSLSocketFactory factory;
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init((KeyManager[]) null, trustAllCerts, (SecureRandom) null);
            webSocket.setSocketFactory(sslContext.getSocketFactory());
        }
        catch (Exception e) {
            Logger.error(e);
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        Logger.debug("LCU WebSocket disconnected");
    }

    @Override
    public void onError(Exception e) {
        Logger.error(e);
    }
}
