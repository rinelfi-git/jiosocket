package mg.rinelfi.jiosocket.server;

import mg.rinelfi.jiosocket.SocketEvents;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SocketServer extends Server {
    private final Map<String, SocketCallbackConsumer> events;
    private final Map<String, SocketClientHandler> handlers;
    
    public SocketServer(int port) throws IOException {
        super(port);
        this.events = new HashMap<>();
        this.handlers = new HashMap<>();
    }
    
    @Override
    public void run() {
        while (!this.server.isClosed()) {
            try {
                Socket client = this.server.accept();
                SocketClientHandler handler = new SocketClientHandler(client);
                long currentTime = System.currentTimeMillis();
                String hash = BCrypt.hashpw(Long.toString(currentTime), BCrypt.gensalt(12));
                // Payloads for new user
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("address", client.getInetAddress().getCanonicalHostName());
                jsonObject.put("port", client.getPort());
                jsonObject.put("identifier", hash);
                
                // setting identity for the thread
                this.handlers.put(hash, handler);
                
                // sending connection status done
                handler.send(SocketEvents.CONNECT, jsonObject.toString());
                
                // prepare to broadcast the connected users
                broadcastIdentity();
                
                handler.setEvents(this.events);
                
                handler.onDisconnect(() -> {
                    this.handlers.remove(hash);
                    broadcastIdentity();
                });
                
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();
                
                // predefined
                handler.on(SocketEvents.BROADCAST_IDENTITY, json -> {
                    this.broadcastIdentity();
                });
                
                handler.on(SocketEvents.RELOAD_IDENTITY, data -> {
                    String old = data.getString("old");
                    String current = data.getString("current");
                    this.handlers.put(current, handler);
                    this.handlers.remove(old);
                    this.broadcastIdentity();
                    handler.onDisconnect(() -> {
                        this.handlers.remove(current);
                        broadcastIdentity();
                    });
                });
            } catch (IOException e) {
            }
        }
    }
    
    public SocketServer on(String event, SocketCallbackConsumer consumer) {
        this.events.put(event, consumer);
        this.handlers.forEach((hash, handler) -> handler.on(event, consumer));
        return this;
    }
    
    public SocketServer emit(String event, String json) {
        this.handlers.forEach((hash, handler) -> handler.send(event, json));
        return this;
    }
    
    public SocketServer emit(String event, String json, String destination) {
        this.handlers.get(destination).send(event, json);
        return this;
    }
    
    private void broadcastIdentity() {
        JSONArray identities = new JSONArray();
        this.handlers.keySet().forEach((key) -> {
            identities.put(key);
        });
        this.emit(SocketEvents.BROADCAST_IDENTITY, new JSONObject().put("identities", identities).toString());
    }
}
