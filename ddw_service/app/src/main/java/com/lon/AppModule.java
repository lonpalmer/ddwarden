package com.lon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lon.ipc.IDdwMessageHandler;
import com.lon.ipc.IMessageCoder;
import com.lon.ipc.StreamingMessageCoder;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public abstract class AppModule {

    // use dagger 2 to provide the IDdwMessageHandler using the IDdwIpc singleton
    @Binds
    @Singleton
    abstract IDdwMessageHandler bindMessageHandler(MessageHandler handler);

    @Binds
    @Singleton
    abstract IMessageCoder bindMessageCoder(StreamingMessageCoder coder);

    @Provides
    @Singleton
    static ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }


}
