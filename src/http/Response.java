package http;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class Response {

    private StringWriter stringWriter; //理解为String类型的缓冲区，存在于缓存中
    private PrintWriter writer; //理解为输出流，将获取到的输出
    private String contentType;
    private byte[] body;
    public Response(){
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter); //理te解到Printwrit将接收到的数据输出到 StringWrite对象的缓冲区
        this.contentType = "text/html";
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType(){
        return  contentType;
    }
    public PrintWriter getWriter(){
        return writer;
    }
    //修改直接获取二进制
    public byte[] getBody() throws UnsupportedEncodingException{
        if(body==null){
            String content = stringWriter.toString();
            body = content.getBytes("utf-8");
        }
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

}
