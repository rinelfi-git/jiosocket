package mg.rinelfi.jiosocket;

import mg.rinelfi.jiosocket.TCPCallback;

public class TCPEvent {
    private String event;
    private TCPCallback callback;
    
    public TCPEvent(String event, TCPCallback callback) {
        this.event = event;
        this.callback = callback;
    }
    
    public String getEvent() {
        return event;
    }
    
    public TCPCallback getCallback() {
        return callback;
    }
}
