package com.lon.ipc.message;

import com.lon.ipc.IDdwMessage;
import com.lon.packfile.PackInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PackInfoMessage implements IDdwMessage {

        public PackInfoMessage(PackInfo packInfo) {
                this.packInfo = packInfo;
        }

        private DdwMessageType messageType = DdwMessageType.PACK_INFO;
        private PackInfo packInfo;
}
