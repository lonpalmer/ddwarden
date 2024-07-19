package com.lon.ipc;

public interface IDdwIpc {

    String DDW_IPC_PIPE_NAME = "ddw_pipe";

    void send(IDdwMessage msg);
    void receive(IDdwMessageHandler handler);

}
