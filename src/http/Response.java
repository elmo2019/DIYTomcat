package http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import org.apache.tools.ant.taskdefs.Local;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class Response extends BaseResponse {
    private int status;
    private StringWriter stringWriter;
    private PrintWriter writer;
    private String contentType;
    private byte[] body;
    //实现cookie功能
    private List<Cookie> cookies;

    public Response(){
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.contentType = "text/html";
        this.cookies = new ArrayList<>();
    }
    //把Cookie集合转化成 cookie Header
    public String getCookiesHeader() {
        if(null==cookies)
            return "";
        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
        StringBuffer sb = new StringBuffer();
        for (Cookie cookie : getCookies()) {
            sb.append("\r\n");
            sb.append("Set-Cookie: ");
//			System.out.println(cookie.getName() + "=" + cookie.getValue() + "; ");
            sb.append(cookie.getName() + "=" + cookie.getValue() + "; ");
            if (-1 != cookie.getMaxAge()) { //-1 mean forever
                sb.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                sb.append(sdf.format(expire));
                sb.append("; ");
            }
            if (null != cookie.getPath()) {
                sb.append("Path=" + cookie.getPath());
            }
        }
        return sb.toString();
    }

    //cookie的get set 方法
    public void addCookie(Cookie cookie){
        cookies.add(cookie);
    }
    public List<Cookie> getCookies(){
        return this.cookies;
    }


    public void setStatus(int status){
        this.status = status;
    }
    public int getStatus(){
        return status;
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
