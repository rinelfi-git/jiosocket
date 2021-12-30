package mg.rinelfi.jiosocket.server;

import mg.rinelfi.jiosocket.SocketEvents;
import mg.rinelfi.jiosocket.TCPCallback;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import sun.awt.X11.XKeyboardFocusManagerPeer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TCPServer extends Thread{
    private final ServerSocket server;
    private Map<String, TCPCallback> events;
    private Map<String, TCPClientHandler> handlers;
    
    public TCPServer(int port) throws IOException {
        this.events = new HashMap<>();
        this.handlers = new HashMap<>();
        this.server = new ServerSocket(port, 3);
    }
    
    @Override
    public void run() {
        while(!this.server.isClosed()) {
            try {
                Socket client = this.server.accept();
                TCPClientHandler handler = new TCPClientHandler(client);
                long currentTime = System.currentTimeMillis();
                String hash = BCrypt.hashpw(Long.toString(currentTime), BCrypt.gensalt(12));
                // Payloads for new user
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("address", client.getInetAddress().getCanonicalHostName());
                jsonObject.put("port", client.getPort());
                jsonObject.put("identifier", hash);
                
                // setting identity for the thread
                handler.setIdentity(hash);
                this.handlers.put(hash, handler);
                
                // sending connection status done
                handler.emit(SocketEvents.CONNECT, jsonObject.toString());
    
                // prepare to broadcast the connected users
                System.out.println("broadcast");
                JSONArray identities = new JSONArray();
                for (String key : this.handlers.keySet()) identities.put(key);
                this.emit(SocketEvents.BROADCAST_IDENTITY, new JSONObject().put("identities", identities).toString());
                
                handler.setEvents(this.events);
                
                handler.onDisconnect(() -> {
                    this.handlers.remove(hash);
                });
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();
                Thread.sleep(500);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public TCPServer on(String event, TCPCallback callback) {
        this.events.put(event, callback);
        this.handlers.forEach((hash, handler) -> handler.on(event, callback));
        return this;
    }
    
    public TCPServer emit(String event, String json) {
        this.handlers.forEach((hash, handler) -> handler.emit(event, json));
        return this;
    }
}
