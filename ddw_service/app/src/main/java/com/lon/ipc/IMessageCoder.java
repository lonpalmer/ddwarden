package com.lon.ipc;

import java.util.Optional;

public interface IMessageCoder {

    byte[] encode(IDdwMessage msg);
    Optional<IDdwMessage> decode(int data);
}
