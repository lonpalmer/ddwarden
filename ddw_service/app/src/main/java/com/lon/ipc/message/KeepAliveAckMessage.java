package com.lon.ipc.message;

import com.lon.ipc.IDdwMessage;
import lombok.Getter;

@Getter
public class KeepAliveAckMessage implements IDdwMessage {
    private final DdwMessageType messageType = DdwMessageType.KEEP_ALIVE_ACK;
}
