package com.github.ricardojlrufino.clipsync.clipboard;


import com.github.ricardojlrufino.clipsync.AppConfig;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClipboardHandler  extends Thread implements ClipboardOwner {

    public interface ClipboardListener {
        void onClipboardData(int type, byte[] data);
    }

    private final AppConfig config;
    private final Clipboard clipboard;
    private final static Logger logger = Logger.getLogger(ClipboardHandler.class.getName());
    private ClipboardListener listener;

    public ClipboardHandler(AppConfig config) {
        this.config = config;
        this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    public void setListener(ClipboardListener listener) {
        this.listener = listener;
    }

    @Override
    public void lostOwnership(Clipboard c, Transferable t) {
        logger.finest("lostOwnership");

        try {
            sleep(200); // wait other software to update clipboard
        } catch (Exception e) {
        }

        try {
            Transferable contents = c.getContents(this);
            processContents(contents);
            regainOwnership(c, contents);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void processContents(Transferable t) {
        try {

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor) && config.isEnableFiles()) {
                    List<File> fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    File file = fileList.get(0);
                    if(file.length() < 1024L * 1024L * config.getFileSizeMB() ){
                        baos.write(ClipboardType.FILE);
                        byte[] bytes = Files.readAllBytes(file.toPath());
//                        baos.write(bytes);
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(file);
                        oos.writeObject(bytes);
                        oos.flush();
                        logger.info("Sending file ... " + file);
                    }else{
                        logger.info("[ingore] large file ... " + file);
                    }

                } else if (t.isDataFlavorSupported(DataFlavor.allHtmlFlavor) && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    baos.write(ClipboardType.HTML);
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(((String) t.getTransferData(DataFlavor.allHtmlFlavor)).getBytes(StandardCharsets.UTF_8));
                    oos.writeObject(((String) t.getTransferData(DataFlavor.stringFlavor)).getBytes(StandardCharsets.UTF_8));
                    oos.flush();
                } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    baos.write(ClipboardType.STRING);
                    baos.writeBytes(((String) t.getTransferData(DataFlavor.stringFlavor)).getBytes(StandardCharsets.UTF_8));
                } else if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    baos.write(ClipboardType.PICTURE);
                    ImageIO.write((BufferedImage) t.getTransferData(DataFlavor.imageFlavor), "png", baos);
                }
                if (baos.size() > 0) {
                    byte[] data = baos.toByteArray();

                    // we alert our entry listener
                    if (listener != null) {
                        listener.onClipboardData(data[0], data);
                    }

                    baos.flush();
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }

        } catch (Exception e) {
        }
    }

    public void regainOwnership(Clipboard c, Transferable t) {
        logger.finest("regainOwnership");
        c.setContents(t, this);
    }

    public void run() {
        Transferable transferable = clipboard.getContents(this);
        regainOwnership(clipboard, transferable);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                sleep(200);
            } catch (Exception ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void updateClipboard(byte[] data) {
        // StringSelection content = new StringSelection(new String(data));
        // clipboard.setContents(content, this);
        Transferable t = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        int type = bais.read();

        try {
            switch (type) {
                case ClipboardType.HTML:
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    byte[] html = (byte[]) ois.readObject();
                    byte[] text = (byte[]) ois.readObject();
                    String ht = new String(html, StandardCharsets.UTF_8);
                    String te = new String(text, StandardCharsets.UTF_8);
                    //System.out.println(ht);
                    t = new HtmlSelection(ht, te);
                    break;
                case ClipboardType.STRING:
                    String st = new String(bais.readAllBytes(), StandardCharsets.UTF_8);
                    // To test with single client
                    if(logger.isLoggable(Level.FINEST)){
                        st = "<" + st + ">";
                    }
                    t = new StringSelection(st);
                    break;
                case ClipboardType.PICTURE:
                    t = new ImageSelection(ImageIO.read(bais));
                    break;
                case ClipboardType.FILE:
                    ObjectInputStream fois = new ObjectInputStream(bais);
                    File file = (File) fois.readObject();
                    byte[] contents = (byte[]) fois.readObject();

                    logger.info("Receiving file: " + file + ", size: " + contents.length + ", payload: " + data.length);

                    Path tmp = Path.of(System.getProperty("java.io.tmpdir"), "clipboard");
                    tmp.toFile().mkdirs();
                    Path tempFile = tmp.resolve(file.getName());

                    logger.info("Saving to: " + tempFile);
                    Files.write(tmp.resolve(file.getName()), contents);
                    t = new FileTransferable(tempFile.toFile());

                    JOptionPane.showMessageDialog(null, "<html>File Received ! Use Ctrl+V in any folder to save ! <br> Or copy: " + tempFile + "</html>");

                    break;
            }

            if(t != null){
                clipboard.setContents(t, this);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
