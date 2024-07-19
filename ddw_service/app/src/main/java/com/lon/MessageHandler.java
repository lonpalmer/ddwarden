package com.lon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lon.ipc.IDdwIpc;
import com.lon.ipc.IDdwMessage;
import com.lon.ipc.IDdwMessageHandler;
import com.lon.ipc.message.KeepAliveAckMessage;
import com.lon.ipc.message.KeepAliveMessage;
import com.lon.ipc.message.PackInfoMessage;
import com.lon.ipc.message.ScanDirectory;
import com.lon.packfile.InvalidPackFileException;
import com.lon.packfile.PackInfo;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

@Slf4j
public class MessageHandler implements IDdwMessageHandler {

    private final IDdwIpc ipc;    // needed for sending
    private final ObjectMapper jackson;

    @Inject
    public MessageHandler(IDdwIpc ipc, ObjectMapper jackson) {
        this.ipc = ipc;
        this.jackson = jackson;
    }

    @Override
    public void handle(IDdwMessage message) {

        switch(message.getMessageType()) {
            case KEEP_ALIVE:
                ipc.send(new KeepAliveAckMessage());
                break;
            case SCAN_DIRECTORY:
                handleScanDirectory((ScanDirectory) message);
                break;
            default:
                System.out.println("Unknown message type: " + message.getMessageType());
        }
    }

    private void handleScanDirectory(ScanDirectory message) {

        var file = new File(message.getPath());
        final var procLimit = new Semaphore(10); // TODO: make this configurable

        if(!file.exists()) {
            log.error("Directory does not exist: {}", message.getPath());
            return;
        }

        if(!file.isDirectory()) {
            log.error("Path is not a directory: {}", message.getPath());
            return;
        }

        var files = List.of(Objects.requireNonNull(file.listFiles((dir, name) -> name.endsWith(".dungeondraft_pack"))));

        var tasks = new LinkedList<Thread>();
        for(var f : files) {
            procLimit.acquireUninterruptibly();
            tasks.add(Thread.ofVirtual().start(() -> {
                try {
                    var packInfo = new PackInfo(f.getAbsolutePath(),jackson);
                    packInfo.scan();
                    ipc.send(new PackInfoMessage(packInfo));
                } catch (InvalidPackFileException e) {
                    log.error("Invalid pack file: {}", f.getAbsolutePath());
                } finally {
                    procLimit.release();
                }
            }));
        }

        tasks.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException ignore) {

            }
        });

    }
}
