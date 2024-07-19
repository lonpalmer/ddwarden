package com.lon.ipc.pipe;

import com.lon.ipc.IDdwIpc;
import com.lon.ipc.IDdwMessage;
import com.lon.ipc.IDdwMessageHandler;
import com.lon.ipc.IMessageCoder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class PipeIpc implements IDdwIpc {

    private final String RX_PIPE;
    private final String TX_PIPE;
    private final boolean isWindows;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final IMessageCoder messageCoder;
    @Inject
    public PipeIpc(IMessageCoder messageCoder) {
        this.messageCoder = messageCoder;

        // Detect the OS and set the pipe names accordingly
        var os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            RX_PIPE = "\\\\.\\pipe\\" + IDdwIpc.DDW_IPC_PIPE_NAME + "_RX";
            TX_PIPE = "\\\\.\\pipe\\" + IDdwIpc.DDW_IPC_PIPE_NAME + "_TX";
            isWindows = true;
            log.info("Windows OS named pipe");
        } else {
            RX_PIPE = "/tmp/" + IDdwIpc.DDW_IPC_PIPE_NAME + "_RX";
            TX_PIPE = "/tmp/" + IDdwIpc.DDW_IPC_PIPE_NAME + "_TX";
            isWindows = false;
            log.info("MacOS / Linux named pipe");
        }

        log.info("RX: {}, TX: {}", RX_PIPE, TX_PIPE);
    }

    @Override
    public void send(IDdwMessage msg) {
        // Create a new virtual thread to send the message
        Thread.ofVirtual().start(() -> {
            try (var dataOut = new FileOutputStream(TX_PIPE)) {
                log.info("Sending message: {}", msg.getMessageType().name());
                dataOut.write(messageCoder.encode(msg));
            } catch (IOException e) {
                log.error("Error transmitting message", e);
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void createPipeFile() {

        if (isWindows) {
            return;
        }

        var rxFile = new File(RX_PIPE);
        if(rxFile.exists()) {
            rxFile.delete();
        }

        try {
            new ProcessBuilder().command("mkfifo", RX_PIPE).start().waitFor();
        } catch (InterruptedException | IOException e) {
            log.error("Error while creating RX pipe", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void receive(IDdwMessageHandler handler) {

        createPipeFile();

        if (!running.compareAndSet(false, true)) {
            return;
        }

        // Note, opening the pipe file will block until something connects to it.
        try (var dataIn = new FileInputStream(RX_PIPE)) {

            while (running.get()) {
                try {
                    log.trace("Waiting for message");
                    int available = dataIn.available();
                    if(available == 0) {
                        Thread.yield();
                    } else {
                        while (available > 0) {
                            var msg = messageCoder.decode(dataIn.read());
                            msg.ifPresent(handler::handle);
                            available--;
                        }
                    }
                } catch (IOException io) {
                    log.error("Error while decoding message", io);
                }
            }
        } catch (IOException e) {
            log.error("Error while opening RX pipe", e);
        }


    }
}
