package mg.rinelfi.jiosocket.server;

import mg.rinelfi.jiosocket.TCPCallback;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TCPServer extends Thread{
    private final ServerSocket server;
    private Map<String, TCPCallback> events;
    List<TCPClientHandler> handlers;
    
    public TCPServer(int port) throws IOException {
        this.events = new HashMap<>();
        this.server = new ServerSocket(port, 3);
    }
    
    @Override
    public void run() {
        this.handlers = new ArrayList<>();
        while(!this.server.isClosed()) {
            try {
                Socket client = this.server.accept();
                TCPClientHandler handler = new TCPClientHandler(client);
                handler.setEvents(this.events);
                handler.triggerConnect();
                handler.onDisconnect(data -> this.handlers.remove(handler));
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();
                this.handlers.add(handler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public TCPServer on(String event, TCPCallback callback) {
        this.events.put(event, callback);
        this.handlers.forEach(handler -> handler.on(event, callback));
        return this;
    }
    
    public TCPServer emit(String event, String json) {
        this.handlers.forEach(handler -> handler.emit(event, json));
        return this;
    }
}
