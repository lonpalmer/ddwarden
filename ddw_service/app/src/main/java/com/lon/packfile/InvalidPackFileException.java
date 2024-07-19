package com.lon.packfile;

public class InvalidPackFileException extends Throwable {
    public InvalidPackFileException(String packPath) {
        super("Invalid pack file: " + packPath);
    }
}
