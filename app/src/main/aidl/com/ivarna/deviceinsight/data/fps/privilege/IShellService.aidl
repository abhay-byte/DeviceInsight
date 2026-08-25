package com.ivarna.deviceinsight.data.fps.privilege;

interface IShellService {
    void destroy() = 16777114;
    String exec(String command) = 1;
    int getUid() = 2;
}
