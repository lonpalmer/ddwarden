package com.lon.ipc.pipe;


import com.lon.App;
import com.lon.AppModule;
import com.lon.PipeIpcModule;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {AppModule.class, PipeIpcModule.class})
public interface AppPipeTestComponent {
    App app();
}
