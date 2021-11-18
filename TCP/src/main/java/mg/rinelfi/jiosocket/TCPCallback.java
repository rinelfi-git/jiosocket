package mg.rinelfi.jiosocket;

import com.sun.istack.internal.Nullable;

interface TCPCallback {
    void update(@Nullable String data);
}
