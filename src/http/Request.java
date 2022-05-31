package http;

import cn.how2j.diytomcat.Bootstrap;
import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.catalina.Engine;
import cn.how2j.diytomcat.catalina.Host;
import cn.how2j.diytomcat.catalina.Service;
import cn.how2j.diytomcat.util.MiniBrowser;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.util.*;

public class Request extends BaseRequest {
    private String method;
    private Context context;
    private String requestString;
    private String uri;
    private Socket socket;
    //private Host host;
    //private Engine engine;
    private Service service;

    //实现获取参数功能
    private String queryString;
    private Map<String, String[]> parameterMap;

    public Request(Socket socket, Service service) throws IOException {
        this.socket = socket;
        this.service = service;
        this.parameterMap = new HashMap<>();
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
        parseParameters();

        System.out.println("显示方法     "+this.getMethod());
        //System.out.println("显示请求字符串     "+queryString);
    }
    //解析Do Get 方法的参数
    private void parseParameters() {
        if ("GET".equals(this.getMethod())) {
            String url = StrUtil.subBetween(requestString, " ", " ");
            if (StrUtil.contains(url, '?')) {
                queryString = StrUtil.subAfter(url, '?', false);
            }
        }
        if ("POST".equals(this.getMethod())) {
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }
        if (null == queryString)
            return;
        queryString = URLUtil.decode(queryString);
        String[] parameterValues = queryString.split("&");
        if (null != parameterValues) {
            for (String parameterValue : parameterValues) {
                String[] nameValues = parameterValue.split("=");
                String name = nameValues[0];
                String value = nameValues[1];
                String values[] = parameterMap.get(name);
                if (null == values) {
                    values = new String[] { value };
                    parameterMap.put(name, values);
                } else {
                    values = ArrayUtil.append(values, value);
                    parameterMap.put(name, values);
                }
            }
        }
    }

    //重写获取参数方法等一系列相关方法
    public String getParameter(String name) {
        String values[] = parameterMap.get(name);
        if (null != values && 0 != values.length)
            return values[0];
        return null;
    }
    public Map getParameterMap() {
        return parameterMap;
    }
    public Enumeration getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
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

    @Override
    public String getMethod() {
        return method;
    }

    public ServletContext getServletContext() {
        return context.getServletContext();
    }
    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

}
