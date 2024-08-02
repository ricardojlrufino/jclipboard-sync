package com.github.ricardojlrufino.clipsync.broadcast;


import com.github.ricardojlrufino.clipsync.clipboard.ClipboardHandler;
import com.github.ricardojlrufino.clipsync.clipboard.ClipboardHandler.ClipboardListener;

import javax.crypto.Cipher;
import java.util.Random;

public abstract class AbstractBroadcaster implements Broadcaster, ClipboardListener {
    private Cipher cipherIn;
    private Cipher cipherOut;
    private ClipboardHandler clipboardHandler;

    public void setCipherIn(Cipher cipherIn) {
        this.cipherIn = cipherIn;
    }

    public void setCipherOut(Cipher cipherOut) {
        this.cipherOut = cipherOut;
    }

    @Override
    public void setClipboardHandler(ClipboardHandler clipboardHandler) {
        this.clipboardHandler = clipboardHandler;
        this.clipboardHandler.setListener(this);
    }

    abstract protected void broadcast(int type, byte[] data);

    @Override
    public void send(int type, byte[] data) {
        this.broadcast(type, encrypt(data));
    }

    @Override
    public void onClipboardData(int type, byte[] data) {
        this.send(type, data);
    }

    protected void updateClipboard(byte[] data){
        clipboardHandler.updateClipboard(data);
    }

    protected byte[] encrypt(byte[] data) {
        try {
            return this.cipherOut.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected byte[] decrypt(byte[] data) {
        try {
            return this.cipherIn.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected String randonString(int length) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();
    }
}
