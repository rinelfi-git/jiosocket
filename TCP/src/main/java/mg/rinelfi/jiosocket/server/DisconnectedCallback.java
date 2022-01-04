package mg.rinelfi.jiosocket.server;

public interface DisconnectedCallback {
    void consumeRequest(String json, DisconnectedTCPClientHandler client);
}
