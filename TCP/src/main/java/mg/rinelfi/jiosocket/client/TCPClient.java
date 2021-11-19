package mg.rinelfi.jiosocket.client;

import mg.rinelfi.jiosocket.Events;
import mg.rinelfi.jiosocket.TCPCallback;
import mg.rinelfi.jiosocket.TCPEvent;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPClient {
    private Socket socket;
    private boolean connected;
    private final String target;
    private final int tcpPort;
    private int udpPort;
    private int minDatagramPort;
    private int maxDatagramPort;
    private final List<TCPEvent> events;
    private ObjectInputStream inputStream;
    
    public TCPClient(String target, int tcpPort) {
        this.target = target;
        this.tcpPort = tcpPort;
        this.events = new ArrayList<>();
        this.minDatagramPort = 49152;
        this.maxDatagramPort = 65535;
    }
    
    public synchronized void connect() {
        final int timeout = 3000;
        Thread t = new Thread(() -> {
            while(!this.connected) {
                try {
                    this.socket = new Socket(target, tcpPort);
                    this.connected = true;
                    this.listen();
                    System.out.println("[INFO] Connexion au server succès");
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
        });
        t.setDaemon(true);
        t.start();
    }
    
    public void listen() {
        Thread thread = new Thread(() -> {
            try {
                while (this.connected) {
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
                if(this.socket.isClosed() || !this.socket.isConnected()) {
                    System.out.println("[WARNING] Fin de la connexion");
                    this.connect();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    public TCPClient emit(String event, String json) {
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                outputStream.writeObject(new String[]{event, json});
                outputStream.flush();
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