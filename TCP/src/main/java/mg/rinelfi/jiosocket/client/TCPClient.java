package mg.rinelfi.jiosocket.client;

import mg.rinelfi.jiosocket.Events;
import mg.rinelfi.jiosocket.TCPCallback;
import mg.rinelfi.jiosocket.TCPEvent;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPClient {
    private Socket socket;
    private boolean connected;
    private String target;
    private int port;
    private List<TCPEvent> events;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    
    public TCPClient(String target, int port) {
        this.target = target;
        this.port = port;
        this.events = new ArrayList<>();
    }
    
    public synchronized void connect() throws IOException {
        this.socket = new Socket(target, port);
        this.connected = this.socket != null && this.socket.isConnected();
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    this.inputStream = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()));
                    Object object = this.inputStream.readObject();
                    String[] input = (String[]) object;
                    String listenEvent = input[0];
                    String json = input[1];
                    this.events.forEach(consumer -> {
                        String event = consumer.getEvent();
                        TCPCallback callback = consumer.getCallback();
                        if (listenEvent.equals(Events.CONNECT) && event.equals(Events.CONNECT)) {
                            this.connected = true;
                            callback.update(null);
                        } else if (listenEvent.equals(event)) {
                            callback.update(json);
                        }
                    });
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    public TCPClient emit(String event, String json) {
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                this.outputStream = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                this.outputStream.writeObject(new String[]{event, json});
                this.outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    public synchronized TCPClient on(String event, TCPCallback callback) {
        this.events.add(new TCPEvent(event, callback));
        return this;
    }
    
    public TCPClient onConnection(TCPCallback callback) {
        if(this.connected) callback.update(null);
        else this.on(Events.CONNECT, callback);
        return this;
    }
}
