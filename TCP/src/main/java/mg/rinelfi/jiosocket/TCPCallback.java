package mg.rinelfi.jiosocket;

import com.sun.istack.internal.Nullable;

public interface TCPCallback {
    void update(@Nullable String data);
}
