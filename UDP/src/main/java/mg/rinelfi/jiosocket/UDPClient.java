package mg.rinelfi.jiosocket;

import java.io.IOException;
import java.net.*;

public class UDPClient {
    private DatagramSocket socket;
    
    public UDPClient() {
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    
    public void ping(String host, int port) {
        try {
            DatagramPacket packet = new DatagramPacket(new byte[0], 0, InetAddress.getByName(host), port);
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
