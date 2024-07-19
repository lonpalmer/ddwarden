package com.lon.ipc.net;

import com.lon.App;
import com.lon.AppModule;
import com.lon.NetIpcModule;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {AppModule.class, NetIpcModule.class})
public interface AppNetTestComponent {
    App app();
}
