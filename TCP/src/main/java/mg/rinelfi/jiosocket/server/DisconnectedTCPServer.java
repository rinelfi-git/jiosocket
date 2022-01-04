package mg.rinelfi.jiosocket.server;

import mg.rinelfi.jiosocket.ConnectedCallback;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisconnectedTCPServer extends TCPServer{
    private final Map<String, DisconnectedCallback> events;
    private final List<DisconnectedTCPClientHandler> handlers;
    
    public DisconnectedTCPServer(int port) throws IOException {
        super(port);
        this.events = new HashMap<>();
        this.handlers = new ArrayList<>();
    }
    
    @Override
    public void run() {
        while(!this.server.isClosed()) {
            try {
                Socket client = this.server.accept();
                DisconnectedTCPClientHandler handler = new DisconnectedTCPClientHandler(client);
                handler.setEvents(this.events);
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public synchronized TCPServer on(String event, DisconnectedCallback callback) {
        this.events.put(event, callback);
        this.handlers.forEach(handler -> handler.on(event, callback));
        return this;
    }
}
