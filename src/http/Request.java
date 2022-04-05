package http;

import cn.how2j.diytomcat.util.MiniBrowser;
import cn.hutool.core.util.StrUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class Request {
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
