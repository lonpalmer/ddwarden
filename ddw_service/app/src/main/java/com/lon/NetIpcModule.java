package com.lon;

import com.lon.ipc.IDdwIpc;
import com.lon.ipc.IMessageCoder;
import com.lon.ipc.net.NetIpc;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import java.util.Properties;

@Module
public abstract class NetIpcModule {
    @Provides
    @Singleton
    static IDdwIpc provideIpc(IMessageCoder messageCoder, Props props) {
        var port = Integer.parseInt(props.getProperty("ipc.net.port", "60123"));
        return new NetIpc(messageCoder, port);
    }


}
