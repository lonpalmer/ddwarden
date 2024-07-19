package com.lon.ipc.message;

import com.lon.ipc.IDdwMessage;
import lombok.Getter;

@Getter
public class KeepAliveMessage implements IDdwMessage {

    private final DdwMessageType messageType = DdwMessageType.KEEP_ALIVE;


}
