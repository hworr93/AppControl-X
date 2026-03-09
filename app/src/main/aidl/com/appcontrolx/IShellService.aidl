package com.appcontrolx;

interface IShellService {
    String exec(String command);
    int execReturnCode(String command);
}
