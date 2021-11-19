package mg.rinelfi.jiosocket.client;

import mg.rinelfi.jiosocket.Events;
import mg.rinelfi.jiosocket.TCPCallback;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TCPClient {
    private Socket socket;
    private boolean connected;
    private final String target;
    private final int tcpPort;
    private int udpPort;
    private int minDatagramPort;
    private int maxDatagramPort;
    private final Map<String, TCPCallback> events;
    private ObjectInputStream inputStream;
    private final List<String[]> eventsStacks;
    
    public TCPClient(String target, int tcpPort) {
        this.target = target;
        this.tcpPort = tcpPort;
        this.minDatagramPort = 49152;
        this.maxDatagramPort = 65535;
        this.events = new HashMap<>();
        this.eventsStacks = new ArrayList<>();
        
    }
    
    public void connect() {
        final int timeout = 3000;
        Thread t = new Thread(() -> {
            while (!this.connected) {
                try {
                    this.socket = new Socket(target, tcpPort);
                    this.connected = true;
                    this.listen();
                } catch (IOException e) {
                    // e.printStackTrace();
                    System.out.println("[WARNING] Erreur de connexion; tentative dans " + (timeout / 1000) + " seconde(s)");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            /**
             * check if there is stored events before socket is initialized
             */
            if (this.eventsStacks.size() > 0) {
                this.eventsStacks.forEach(eventsStack -> this.emit(eventsStack[0], eventsStack[1]));
                this.eventsStacks.clear();
            }
        });
        t.setDaemon(true);
        t.start();
    }
    
    private void listen() {
        Thread thread = new Thread(() -> {
            try {
                while (this.connected) {
                    this.inputStream = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()));
                    Object object = this.inputStream.readObject();
                    String[] input = (String[]) object;
                    String listenEvent = input[0],
                        json = input[1];
                    if (this.events.containsKey(listenEvent)) {
                        if (listenEvent.equals(Events.CONNECT)) {
                            this.connected = true;
                            this.events.get(listenEvent).update(null);
                        } else {
                            this.events.get(listenEvent).update(json);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                // e.printStackTrace();
                this.connect();
                if (this.socket.isClosed() || !this.socket.isConnected()) {
                    System.out.println("[WARNING] Fin de la connexion ou erreur de conversion");
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    public synchronized TCPClient emit(String event, String json) {
        if (this.socket != null && !this.socket.isClosed()) {
            new Thread(() -> {
                try {
                    ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                    outputStream.writeObject(new String[]{event, json});
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            /**
             * wait for socket disponibility
             * and store it in an arraylist
             */
            this.eventsStacks.add(new String[]{event, json});
        }
        return this;
    }
    
    public TCPClient on(String event, TCPCallback callback) {
        this.events.put(event, callback);
        return this;
    }
    
    public TCPClient onConnection(TCPCallback callback) {
        if (this.connected) callback.update(null);
        else this.on(Events.CONNECT, callback);
        return this;
    }
    
    public void setMinDatagramPort(int port) {
        this.minDatagramPort = port;
    }
    
    public void setMaxDatagramPort(int port) {
        this.maxDatagramPort = port;
    }
    
    public void getDatagramPort() {
        for (int iteration = this.minDatagramPort; iteration <= this.maxDatagramPort && this.udpPort == 0; iteration++) {
            try {
                ServerSocket test = new ServerSocket(iteration);
                test.close();
                this.udpPort = iteration;
            } catch (IOException e) {
                System.out.println(String.format("[WARNING] Le port %5d n'est pas disponible\n", iteration));
            }
        }
        this.emit(Events.UDP_PORT, String.valueOf(this.udpPort));
    }
}