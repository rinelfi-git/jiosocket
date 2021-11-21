package mg.rinelfi.jiosocket.server;

import mg.rinelfi.jiosocket.Events;
import mg.rinelfi.jiosocket.TCPCallback;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

class TCPClientHandler implements Runnable {
    private String identity;
    private final Socket socket;
    private Map<String, TCPCallback> events;
    private SocketCloseListener socketCloseListener;
    
    public TCPClientHandler(Socket client) {
        this.events = new HashMap<>();
        this.socket = client;
    }
    
    public String getIdentity() {
        return identity;
    }
    
    public void setIdentity(String identity) {
        this.identity = identity;
    }
    
    public TCPClientHandler on(String event, TCPCallback callback) {
        if (!event.equals(Events.DISCONNECT))
            this.events.put(event, callback);
        return this;
    }
    
    public void onDisconnect(SocketCloseListener listener) {
        this.socketCloseListener = listener;
    }
    
    public TCPClientHandler emit(String event, String json) {
        new Thread(() -> {
            try {
                if (this.socket != null && !this.socket.isClosed()) {
                    ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                    outputStream.writeObject(new String[]{event, json});
                    outputStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return this;
    }
    
    private void triggerDisconnect() {
    
    }
    
    public void triggerConnect() {
        this.emit(Events.CONNECT, null);
    }
    
    @Override
    public void run() {
        while (this.socket.isConnected() && !this.socket.isClosed()) {
            this.listen();
        }
    }
    
    private void listen() {
        try {
            /**
             * listen on input stream
             * wether there is a packet or not
             */
            ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()));
            Object object = inputStream.readObject();
            String[] input = (String[]) object;
            /**
             * loop on registered events
             * and trigger callback on event
             */
            String listenEvent = input[0],
                json = input[1];
            if (this.events.containsKey(listenEvent)) {
                if (listenEvent.equals(Events.DISCONNECT)) this.socket.close();
                this.events.get(listenEvent).update(json);
            }
        } catch (IOException | ClassNotFoundException e) {
            // e.printStackTrace();
            /**
             * If connection is closed by user
             * then notify the server manager to remove the handler
             */
            if (this.socket.isClosed() || !this.socket.isConnected()) {
                this.socketCloseListener.trigger();
            }
        }
    }
    
    public void setEvents(Map<String, TCPCallback> events) {
        this.events = events;
    }
}
