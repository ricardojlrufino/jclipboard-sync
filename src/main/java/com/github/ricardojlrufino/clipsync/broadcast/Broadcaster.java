package com.github.ricardojlrufino.clipsync.broadcast;


import com.github.ricardojlrufino.clipsync.clipboard.ClipboardHandler;

public interface Broadcaster {

    void send(int type, byte[] data);

    void setClipboardHandler(ClipboardHandler clipboardHandler);
}
