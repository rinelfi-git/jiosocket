package mg.rinelfi.jiosocket.server;

import mg.rinelfi.jiosocket.SocketEvents;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

class SocketClientHandler implements Runnable {
    private final Socket socket;
    private Map<String, SocketCallbackConsumer> events;
    private SocketCloseListener socketCloseListener;
    private boolean connected;
    
    public SocketClientHandler(Socket client) {
        this.connected = false;
        this.events = new HashMap<>();
        this.socket = client;
    }
    
    public SocketClientHandler on(String event, SocketCallbackConsumer callback) {
        this.events.put(event, callback);
        return this;
    }
    
    public void onDisconnect(SocketCloseListener listener) {
        this.socketCloseListener = listener;
        this.events.put(SocketEvents.DISCONNECT, null);
    }
    
    public SocketClientHandler send(String event, String json) {
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                outputStream.writeObject(new String[]{event, json});
                outputStream.flush();
                System.out.println("sending : " + json);
            }
        } catch (IOException e) {
        }
        return this;
    }
    
    @Override
    public void run() {
        this.connected = true;
        while (this.connected) {
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
                if (listenEvent.equals(SocketEvents.DISCONNECT)) this.socket.close();
                else this.events.get(listenEvent).consume(new JSONObject(json));
            }
        } catch (IOException | ClassNotFoundException e) {
            /**
             * If connection is closed by user
             * then notify the server manager to remove the handler
             */
            this.connected = false;
            this.socketCloseListener.trigger();
        }
    }
    
    public void setEvents(Map<String, SocketCallbackConsumer> events) {
        if (this.events.size() > 0) this.events.putAll(events);
        else this.events = events;
    }
}
