package mg.rinelfi.jiosocket.server;

import mg.rinelfi.jiosocket.Events;
import mg.rinelfi.jiosocket.TCPCallback;
import mg.rinelfi.jiosocket.TCPEvent;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

class TCPClientHandler implements Runnable{
    private String id;
    private Socket socket;
    private List<TCPEvent> events;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    
    public TCPClientHandler(Socket client) {
        this.events = new ArrayList<>();
        this.socket = client;
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
    
    private void triggerDisconnect() {
    
    }
    
    public void onDisconnect(TCPCallback callback) {
        callback.update(null);
    }
    
    public void triggerConnect() {
        this.emit(Events.CONNECT, null);
    }
    
    @Override
    public void run() {
        System.out.println("listening");
        try {
            while (true) {
                /**
                 * listen on input stream
                 * wether there is a packet or not
                 */
                this.inputStream = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()));
                Object object = this.inputStream.readObject();
                String[] input = (String[]) object;
                /**
                 * loop on registered events
                 * and trigger callback on event
                 */
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
            // e.printStackTrace();
            /**
             * If connection is closed by user
             * then notify the server manager to remove the handler
             */
            if(this.socket.isClosed() || !this.socket.isConnected()) {
                this.triggerDisconnect();
            }
        }
    }
}
