package mg.rinelfi.jiosocket;

import java.io.*;
import java.net.Socket;

public class TCPClient {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    
    public TCPClient(String target, int port) throws IOException {
        this.socket = new Socket(target, port);
    }
    
    public TCPClient emit(String event, String json) {
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                this.outputStream = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                this.outputStream.writeObject(new String[]{event, json});
                this.outputStream.flush();
                this.outputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    public synchronized TCPClient on(String event, TCPCallback callback) {
        try {
            while (true) {
                this.inputStream = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()));
                String[] input = (String[]) this.inputStream.readObject();
                if (input[0].equals(event)) callback.update(input[1]);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this;
    }
}
