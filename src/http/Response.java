package http;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;

public class Response extends BaseResponse {
    private StringWriter stringWriter;
    private PrintWriter writer;
    private String contentType;
    private byte[] body;
    public Response(){
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.contentType = "text/html";
    }

    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public PrintWriter getWriter() {
        return writer;
    }
    //直接获取二进制
    public byte[] getBody() throws UnsupportedEncodingException {
        if(null==body) {
            String content = stringWriter.toString();
            body = content.getBytes("utf-8");
        }
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

}
