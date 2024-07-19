package com.lon;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Properties;

public class Props extends Properties {
    @Inject
    public Props() {
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream("app.properties")) {
            this.load(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
