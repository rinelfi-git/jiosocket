package mg.rinelfi.jiosocket.server;

import mg.rinelfi.jiosocket.TCPCallback;
import mg.rinelfi.jiosocket.TCPEvent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPServer extends Thread{
    private final ServerSocket server;
    private List<TCPEvent> events;
    List<TCPClientHandler> handlers;
    
    public TCPServer(int port) throws IOException {
        this.events = new ArrayList<>();
        this.server = new ServerSocket(port, 3);
    }
    
    @Override
    public void run() {
        this.handlers = new ArrayList<>();
        while(!this.server.isClosed()) {
            try {
                Socket client = this.server.accept();
                System.out.println("new client : " + client);
                TCPClientHandler handler = new TCPClientHandler(client);
                this.handlers.add(handler);
                handler.triggerConnect();
                handler.onDisconnect(data -> this.handlers.remove(handler));
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public synchronized TCPServer on(String event, TCPCallback callback) {
        this.events.add(new TCPEvent(event, callback));
        this.handlers.forEach(handler -> handler.on(event, callback));
        return this;
    }
    
    public synchronized TCPServer emit(String event, String json) {
        this.handlers.forEach(handler -> handler.emit(event, json));
        return this;
    }
}
