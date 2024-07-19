package com.lon;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {AppModule.class, NetIpcModule.class})
public interface AppComponent {
    App app();
}
