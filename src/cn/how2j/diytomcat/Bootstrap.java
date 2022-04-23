package cn.how2j.diytomcat;


import cn.how2j.diytomcat.catalina.*;
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.MiniBrowser;
import cn.how2j.diytomcat.util.ServerXMLUtil;
import cn.how2j.diytomcat.util.ThreadPoolUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import http.Request;
import http.Response;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Bootstrap {


    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}

