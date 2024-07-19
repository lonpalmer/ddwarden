package com.lon.ipc.net;

import com.lon.ipc.IDdwIpc;
import com.lon.ipc.IDdwMessage;
import com.lon.ipc.IDdwMessageHandler;
import com.lon.ipc.IMessageCoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class NetIpc implements IDdwIpc {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<Socket> currentSocket = new AtomicReference<>(null);
    private final IMessageCoder messageCoder;
    private final int port;

    public NetIpc(IMessageCoder messageCoder, int port) {
        this.messageCoder = messageCoder;
        this.port = port;
    }

    private void stop() {
        running.set(false);
        if (currentSocket.get() != null) {
            try {
                currentSocket.get().close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void send(IDdwMessage msg) {
        Thread.ofVirtual().start(() -> {
            try {
                var socket = currentSocket.get();
                if (socket == null || !socket.isConnected()) {
                    log.error("No socket available to send message");
                    return;
                }
                var dataOut = socket.getOutputStream();
                dataOut.write(messageCoder.encode(msg));
            } catch (IOException e) {
                log.error("Error transmitting message", e);
            }
        });
    }

    @Override
    public void receive(IDdwMessageHandler handler) {
        running.set(true);
        while (running.get()) {
            try (ServerSocket serverSocket = new ServerSocket(port, 0)) {
                var socket = serverSocket.accept();
                currentSocket.set(socket);
                var dataIn = socket.getInputStream();
                int read;
                while ((read = dataIn.read()) != -1) {
                    var msg = messageCoder.decode(read);
                    msg.ifPresent(handler::handle);
                }
            } catch (IOException e) {
                log.error("Socket Error", e);
            }
        }
    }
}
