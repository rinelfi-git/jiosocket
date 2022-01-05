package mg.rinelfi.jiosocket.server;

import org.json.JSONObject;

public interface SocketCallbackConsumer {
    void consume(JSONObject data);
}
