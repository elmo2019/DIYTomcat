package http;

import cn.how2j.diytomcat.Bootstrap;
import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.catalina.Engine;
import cn.how2j.diytomcat.catalina.Host;
import cn.how2j.diytomcat.catalina.Service;
import cn.how2j.diytomcat.util.MiniBrowser;
import cn.hutool.core.util.StrUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public class Request extends BaseRequest {
    private String method;
    private Context context;
    private String requestString;
    private String uri;
    private Socket socket;
    //private Host host;
    //private Engine engine;
    private Service service;
    public Request(Socket socket, Service service) throws IOException {
        this.socket = socket;
        this.service = service;
        parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;
        parseUri();
        parseContext();
        parseMethod();
        if(!"/".equals(context.getPath())){
            uri = StrUtil.removePrefix(uri, context.getPath());
            if(StrUtil.isEmpty(uri))
                uri = "/";
        }

    }
    private void parseMethod(){
        method = StrUtil.subBefore(requestString," ",false);
    }

    private void parseContext() {
        Engine engine = service.getEngine();
        context = engine.getDefaultHost().getContext(uri);
        if(null!=context)
            return;
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path)
            path = "/";
        else
            path = "/" + path;
        context = engine.getDefaultHost().getContext(path);
        if (null == context)
            context = engine.getDefaultHost().getContext("/");
    }

    private void parseHttpRequest() throws IOException {
        InputStream is = this.socket.getInputStream();
        byte[] bytes = MiniBrowser.readBytes(is,false);
        requestString = new String(bytes, "utf-8");
    }

    private void parseUri() {
        String temp;

        temp = StrUtil.subBetween(requestString, " ", " ");
        if (!StrUtil.contains(temp, '?')) {
            uri = temp;
            return;
        }
        temp = StrUtil.subBefore(temp, '?', false);
        uri = temp;
    }

    public Context getContext() {
        return context;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString(){
        return requestString;
    }


}
