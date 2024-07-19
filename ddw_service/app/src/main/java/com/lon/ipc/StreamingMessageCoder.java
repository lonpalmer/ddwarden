package com.lon.ipc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lon.ipc.message.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class StreamingMessageCoder implements IMessageCoder {

    private final ObjectMapper jackson;

    @Inject
    public StreamingMessageCoder(ObjectMapper jackson) {
        this.jackson = jackson;
    }

    private static final int[] START_WORD = new int[] { 0xd0, 0x0d };
    private int dataLen = 0;
    private int msgTypeOrdinal = 0;
    private DdwMessageType msgType;
    private ByteArrayOutputStream buffer;
    private ParseState state = ParseState.START1;

    enum ParseState {
        START1,
        START2,
        MSG_TYPE1,
        MSG_TYPE2,
        MSG_TYPE3,
        MSG_TYPE4,
        LEN1,
        LEN2,
        LEN3,
        LEN4,
        CHECK,
        DATA
    }

    public byte[] encode(IDdwMessage msg) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            var msgBytes = jackson.writeValueAsBytes(msg);
            var len = msgBytes.length;
            var msgTypeOrdinal = msg.getMessageType().ordinal();
            var check = (len ^ msgTypeOrdinal) & 0xff;


            Arrays.stream(START_WORD).forEach(outStream::write);
            outStream.write(msgTypeOrdinal & 0xff);
            outStream.write((msgTypeOrdinal >> 8) & 0xff);
            outStream.write((msgTypeOrdinal >> 16) & 0xff);
            outStream.write((msgTypeOrdinal >> 24) & 0xff);
            outStream.write(len & 0xff);
            outStream.write((len >> 8) & 0xff);
            outStream.write((len >> 16) & 0xff);
            outStream.write((len >> 24) & 0xff);
            outStream.write(check);
            outStream.writeBytes(msgBytes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return outStream.toByteArray();
    }

    public Optional<IDdwMessage> decode(int data) {
        switch (state) {
            case START1:
                if (data == START_WORD[0]) {
                    state = ParseState.START2;
                }
                break;
            case START2:
                if (data == START_WORD[1]) {
                    state = ParseState.MSG_TYPE1;
                } else {
                    state = ParseState.START1;
                }
                break;
            case MSG_TYPE1:
                msgTypeOrdinal = data;
                state = ParseState.MSG_TYPE2;
                break;
            case MSG_TYPE2:
                msgTypeOrdinal |= data << 8;
                state = ParseState.MSG_TYPE3;
                break;
            case MSG_TYPE3:
                msgTypeOrdinal |= data << 16;
                state = ParseState.MSG_TYPE4;
                break;
            case MSG_TYPE4:
                msgTypeOrdinal |= data << 24;
                msgType = DdwMessageType.values()[msgTypeOrdinal];
                state = ParseState.LEN1;
                break;
            case LEN1:
                dataLen = data;
                state = ParseState.LEN2;
                break;
            case LEN2:
                dataLen |= data << 8;
                state = ParseState.LEN3;
                break;
            case LEN3:
                dataLen |= data << 16;
                state = ParseState.LEN4;
                break;
            case LEN4:
                dataLen |= data << 24;
                state = ParseState.CHECK;
                break;
            case CHECK:
                if(data == ((dataLen ^ msgTypeOrdinal) & 0xff)) {
                    state = ParseState.DATA;
                    buffer = new ByteArrayOutputStream();
                } else {
                    state = ParseState.START1;
                }
                break;
            case DATA:
                buffer.write(data);
                if (buffer.size() == dataLen) {
                    var bytes = buffer.toByteArray();
                    buffer = null;
                    state = ParseState.START1;
                    try {
                        var msg = switch(msgType) {
                            case KEEP_ALIVE -> jackson.readValue(bytes, KeepAliveMessage.class);
                            case KEEP_ALIVE_ACK -> jackson.readValue(bytes, KeepAliveAckMessage.class);
                            case SCAN_DIRECTORY -> jackson.readValue(bytes, ScanDirectory.class);
                            case PACK_INFO -> jackson.readValue(bytes, PackInfoMessage.class);
                        };
                        return Optional.ofNullable(msg);
                    } catch (IOException e) {
                        log.error("Jackson Decode Error", e);
                        return Optional.empty();
                    }
                }
                break;
        }
        return Optional.empty();

    }




}
