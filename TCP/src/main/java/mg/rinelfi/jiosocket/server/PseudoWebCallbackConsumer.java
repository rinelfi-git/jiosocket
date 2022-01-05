package mg.rinelfi.jiosocket.server;

import org.json.JSONObject;

public interface PseudoWebCallbackConsumer {
    void consume(JSONObject json, PseudoWebClientHandler handler);
}
