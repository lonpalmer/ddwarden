package com.lon;


import com.lon.ipc.IDdwIpc;
import com.lon.ipc.pipe.PipeIpc;
import dagger.Binds;
import dagger.Module;

import javax.inject.Singleton;

@Module
public interface PipeIpcModule {
    @Binds
    @Singleton
    IDdwIpc bindIpc(PipeIpc ipc);
}
