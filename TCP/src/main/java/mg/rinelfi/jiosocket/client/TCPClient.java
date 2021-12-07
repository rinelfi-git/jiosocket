package mg.rinelfi.jiosocket.client;

import mg.rinelfi.jiosocket.Events;
import mg.rinelfi.jiosocket.TCPCallback;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TCPClient {
    private Socket socket;
    private boolean connected, reconnect;
    private final String target;
    private final int tcpPort;
    private int udpPort, timeout;
    private final Map<String, TCPCallback> events;
    private ObjectInputStream inputStream;
    private final List<String[]> eventsStacks;
    
    public TCPClient(String target, int tcpPort) {
        this.target = target;
        this.tcpPort = tcpPort;
        this.reconnect = true;
        this.timeout = 3000;
        this.events = new HashMap<>();
        this.eventsStacks = new ArrayList<>();
        
    }
    
    public void connect() {
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
                        Thread.sleep(timeout);
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
                    new Thread(() -> {
                        String[] input = (String[]) object;
                        String listenEvent = input[0],
                            json = input[1];
                        if (this.events.containsKey(listenEvent)) {
                            if (listenEvent.equals(Events.CONNECT)) {
                                this.connected = true;
                                this.events.get(listenEvent).update(json);
                            } else {
                                this.events.get(listenEvent).update(json);
                            }
                        }
                    }).start();
                }
            } catch (IOException | ClassNotFoundException e) {
                // e.printStackTrace();
                if (this.socket.isClosed() || !this.socket.isConnected()) {
                    /**
                     * Check if client want the socket to reconnect after its link has broken
                     */
                    if(this.reconnect) this.connect();
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
    
    public TCPClient setAutoreconnection(boolean reconnection) {
        this.reconnect = reconnection;
        return this;
    }
    
    public TCPClient setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
    
    public String getRemoteAddress() {
        String remoteAddress;
        if(this.socket == null) remoteAddress = "localhost";
        else remoteAddress = this.socket.getInetAddress().getCanonicalHostName();
        return remoteAddress;
    }
    
    public int getRemotePort() {
        int remotePort;
        if(this.socket == null) remotePort = -1;
        else remotePort = this.socket.getPort();
        return remotePort;
    }
    
    public String getLocalAddress() {
        String localAddress;
        if(this.socket == null) localAddress = "localhost";
        else localAddress = this.socket.getLocalAddress().getCanonicalHostName();
        return localAddress;
    }
    
    public int getLocalPort() {
        int localPort;
        if(this.socket == null) localPort = -1;
        else localPort = this.socket.getLocalPort();
        return localPort;
    }
}