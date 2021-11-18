package mg.rinelfi.jiosocket.server;

import mg.rinelfi.jiosocket.TCPCallback;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPServer extends Thread{
    private ServerSocket server;
    List<TCPClientHandler> handlers;
    
    public TCPServer(int port) throws IOException {
        this.server = new ServerSocket(port, 3);
    }
    
    @Override
    public void run() {
        this.handlers = new ArrayList<>();
        while(this.server != null && !this.server.isClosed()) {
            try {
                Socket client = this.server.accept();
                TCPClientHandler handler = new TCPClientHandler(client);
                this.handlers.add(handler);
                handler.triggerConnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public synchronized TCPServer on(String event, TCPCallback callback) {
        this.handlers.forEach(handler -> handler.on(event, callback));
        return this;
    }
    
    public synchronized TCPServer emit(String event, String json) {
        this.handlers.forEach(handler -> handler.emit(event, json));
        return this;
    }
}
