package com.lon.ipc.message;

import com.lon.ipc.IDdwMessage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScanDirectory implements IDdwMessage {

    private DdwMessageType messageType = DdwMessageType.SCAN_DIRECTORY;
    private String path;

}
