package com.lon.packfile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileInfo {
    private String path;
    private long size;
    private long offset;
    private byte[] md5Hash;

    @Override
    public String toString() {
        return "FileInfo{" +
                "path='" + path + '\'' +
                ", size=" + size +
                ", offset=" + offset +
                '}';
    }
}
