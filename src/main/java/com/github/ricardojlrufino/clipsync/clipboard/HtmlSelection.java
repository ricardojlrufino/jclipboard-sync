package com.github.ricardojlrufino.clipsync.clipboard;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

// This class is used to hold HTML on the clipboard.
public class HtmlSelection implements Transferable {

    private final String text;
    private final String html;

    public HtmlSelection(String html, String text) {
        this.html = html;
        this.text = text;
    }

    // Returns supported flavors
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.allHtmlFlavor, DataFlavor.stringFlavor};
    }

    // Returns true if flavor is supported
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.allHtmlFlavor.equals(flavor) || DataFlavor.stringFlavor.equals(flavor);
    }

    // Returns image
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (DataFlavor.allHtmlFlavor.equals(flavor)) {
            return html;
        } else if (DataFlavor.stringFlavor.equals(flavor)) {
            return text;
        }
        throw new UnsupportedFlavorException(flavor);
    }
}