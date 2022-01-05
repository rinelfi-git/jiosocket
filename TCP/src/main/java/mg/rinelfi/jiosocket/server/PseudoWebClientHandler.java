package mg.rinelfi.jiosocket.server;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class PseudoWebClientHandler implements Runnable {
    private final Socket socket;
    private Map<String, PseudoWebCallbackConsumer> events;
    private String currentEvent;
    
    public PseudoWebClientHandler(Socket client) {
        this.events = new HashMap<>();
        this.socket = client;
    }
    
    public void on(String event, PseudoWebCallbackConsumer callback) {
        this.events.put(event, callback);
    }
    
    public void send(String json) {
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                outputStream.writeObject(new String[]{currentEvent, json});
                outputStream.flush();
                outputStream.close();
                this.socket.close();
            }
        } catch (IOException e) {}
    }
    
    @Override
    public void run() {
        this.listen();
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
                this.events.get(listenEvent).consume(new JSONObject(json), this);
                this.currentEvent = listenEvent;
            }
        } catch (IOException | ClassNotFoundException e) {
            /**
             * If connection is closed by user
             * then notify the server manager to remove the handler
             */
        }
    }
    
    public void setEvents(Map<String, PseudoWebCallbackConsumer> events) {
        if (this.events.size() > 0) this.events.putAll(events);
        else this.events = events;
    }
}