package http;

import cn.how2j.diytomcat.Bootstrap;
import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.util.MiniBrowser;
import cn.hutool.core.util.StrUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class Request {
    private Context context;
    private String requestString;
    private String uri;
    private Socket socekt;
    public Request(Socket socekt) throws IOException{
        this.socekt = socekt;
        parseHttpRequest();
        if(StrUtil.isEmpty(requestString)){
            return;
        }
        parseUri();
        parseContext();
        if(!"/".equals(context.getPath())){
            uri = StrUtil.removePrefix(uri,context.getPath());
        }
    }

    private void parseContext() {
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path)
            path = "/";
        else
            path = "/" + path;

        context = Bootstrap.contextMap.get(path);
        if (null == context)
            context = Bootstrap.contextMap.get("/");
    }

    public Context getContext(){
        return context;
    }

    private void parseHttpRequest() throws IOException{
        InputStream is = this.socekt.getInputStream();
        byte[] result = MiniBrowser.readBytes(is);
        requestString = new String(result,"utf-8");
    }
    private  void parseUri() throws IOException{
        String temp;
        temp = StrUtil.subBetween(requestString," "," ");
        if(!StrUtil.contains(temp,'?')){
            uri = temp;
            return;
        }
        temp = StrUtil.subBefore(temp,'?',false);
        uri = temp;
    }
    public String getUri(){
        return uri;
    }
    public String getRequestString(){
        return requestString;
    }
}
