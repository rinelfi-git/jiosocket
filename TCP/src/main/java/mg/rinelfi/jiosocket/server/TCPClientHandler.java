package mg.rinelfi.jiosocket.server;

import mg.rinelfi.jiosocket.Events;
import mg.rinelfi.jiosocket.TCPCallback;
import mg.rinelfi.jiosocket.TCPEvent;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

class TCPClientHandler {
    private String id;
    private Socket socket;
    private List<TCPEvent> events;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    
    public TCPClientHandler(Socket client) {
        this.events = new ArrayList<>();
        this.socket = client;
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    this.inputStream = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()));
                    Object object = this.inputStream.readObject();
                    String[] input = (String[]) object;
                    this.events.forEach(consumer -> {
                        String event = consumer.getEvent();
                        TCPCallback callback = consumer.getCallback();
                        String listenEvent = input[0];
                        String json = input[1];
                        if (listenEvent.equals(event)) {
                            callback.update(json);
                        }
                    });
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                if(this.socket.isClosed() || !this.socket.isConnected()) {
            
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public synchronized TCPClientHandler on(String event, TCPCallback callback) {
        this.events.add(new TCPEvent(event, callback));
        return this;
    }
    
    public TCPClientHandler emit(String event, String json) {
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
    
    public void onDisconnect(TCPCallback callback) {
        callback.update(null);
    }
    
    public void triggerConnect() {
        this.emit(Events.CONNECT, null);
    }
}
