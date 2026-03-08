package com.appcontrolx;

interface IShellService {
    String openSession(String packageName);
    void closeSession(String sessionToken);
    String exec(String sessionToken, String command);
    int execReturnCode(String sessionToken, String command);
}
