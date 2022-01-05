package mg.rinelfi.jiosocket.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PseudoWebServer extends Server {
    private final Map<String, PseudoWebCallbackConsumer> events;
    private final List<PseudoWebClientHandler> handlers;
    
    public PseudoWebServer(int port) throws IOException {
        super(port);
        this.events = new HashMap<>();
        this.handlers = new ArrayList<>();
    }
    
    @Override
    public void run() {
        while(!this.server.isClosed()) {
            try {
                Socket client = this.server.accept();
                PseudoWebClientHandler handler = new PseudoWebClientHandler(client);
                handler.setEvents(this.events);
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public synchronized Server on(String event, PseudoWebCallbackConsumer callback) {
        this.events.put(event, callback);
        this.handlers.forEach(handler -> handler.on(event, callback));
        return this;
    }
}
