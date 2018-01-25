package com.library.common;

/**
 * Created by android1 on 2018/1/25.
 */

public interface WriteFileCallback {
    void success(String path);

    void failure(String err);
}
