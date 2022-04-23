package http;

import cn.how2j.diytomcat.Bootstrap;
import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.catalina.Host;
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
    private Host host;

    public Request(Socket socekt,Host host) throws IOException{
        this.socekt = socekt;
        this.host = host;
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

        context = host.getContext(path);
        if (null == context)
            context = host.getContext("/");
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
