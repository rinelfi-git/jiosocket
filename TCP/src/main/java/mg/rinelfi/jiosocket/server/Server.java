package mg.rinelfi.jiosocket.server;

import java.io.IOException;
import java.net.ServerSocket;

public abstract class Server extends Thread {
    
    protected final ServerSocket server;
    
    public Server(int port) throws IOException {
        this.server = new ServerSocket(port, 3);
    }
}
