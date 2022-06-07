package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.servlets.DefaultServlet;
import cn.how2j.diytomcat.servlets.InvokerServlet;
import cn.how2j.diytomcat.servlets.JspServlet;
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.SessionManager;
import cn.how2j.diytomcat.util.WebXMLUtil;
import cn.how2j.diytomcat.webappservlet.HelloServlet;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import com.sun.tools.internal.ws.wsdl.document.Output;
import http.Request;
import http.Response;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

//原Connector 类中既要监听接收socket信息，又要处理消息，将业务分离出来，让消息单独处理
public class HttpProcessor {
    public void execute(Socket s , Request request, Response response){
        try {
            String uri = request.getUri();
            if(null==uri)
                return;
            //System.out.println("uri:"+uri);
            //System.out.println("requst"+request.getRequestString());
            prepareSession(request,response);
            Context context = request.getContext();

            String servletClassName = context.getServletClassName(uri);
            HttpServlet workingServlet;
            if(null!=servletClassName)
                workingServlet = InvokerServlet.getInstance();
            else if(uri.endsWith(".jsp"))
                workingServlet = JspServlet.getInstance();
            else
                workingServlet = DefaultServlet.getInstance();

            //System.out.println("request.getRequestURI()   "+request.getRequestURI());
            List<Filter> filters = request.getContext().getMatchedFilters(request.getRequestURI());
            //System.out.println("过滤器如下   "+request.getContext().getfiltPool());
            //System.out.println("gugugugugugugugugu   "+filters);
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
            filterChain.doFilter(request, response);
            //如果是服务端跳转那就不用接下来的处理，否则会导致多次关闭
            if(request.isForwarded())
                return;


            if(Constant.CODE_200 == response.getStatus()){
                handle200(s,request  ,response);
                return;
            }
            if(Constant.CODE_404 == response.getStatus()){
                handle404(s,uri);
                return;
            }
            if(Constant.CODE_302 == response.getStatus()){
                handle302(s,response);
                return;
            }

            } catch (Exception e) {
            LogFactory.get().error(e);
            handle500(s,e);
        }
        finally{
            try {
                if(!s.isClosed())
                    s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //302响应处理
    private void handle302(Socket s, Response response) throws IOException{
        OutputStream os = s.getOutputStream();
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.response_head_302;
        String header = StrUtil.format(head_text,redirectPath);
        byte[] responseBytes = header.getBytes("utf-8");
        os.write(responseBytes);
    }

    //判断是否需要压缩
    private boolean isGzip(Request request,byte[] body, String mimeType){
        String acceptEncodings = request.getHeader("Accept-Encoding");
        //游览器是否支持Gzip
        if(!StrUtil.containsAny(acceptEncodings,"gzip"))
            return false;

        Connector connector = request.getConnector();
        if(mimeType.contains(";"))
            mimeType = StrUtil.subBefore(mimeType,";",false);
        //判断服务端端口是否支持压缩
        if (!"on".equals(connector.getCompression()))
            return false;
        //信息长度是否要压缩
        if (body.length < connector.getCompressionMinSize())
            return false;
        //浏览器型号是否是支持压缩
        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(",");
        for (String eachUserAgent : eachUserAgents) {
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            if (StrUtil.containsAny(userAgent, eachUserAgent))
                return false;
        }
        //要发送的文件类型是否要进行压缩
        String mimeTypes = connector.getCompressableMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for (String eachMimeType : eachMimeTypes) {
            if (mimeType.equals(eachMimeType))
                return true;
        }
        return false;
    }

    //准备session
    public void prepareSession(Request request,Response response){
        String jsessionid = request.getJSessionFromCookie();
        HttpSession session = SessionManager.getSession(jsessionid,request,response);
        request.setSession(session);
    }

    //处理404返回
    protected static void handle404(Socket s,String uri) throws IOException{
        OutputStream os = s.getOutputStream();
        String responseText = StrUtil.format(Constant.response_head_404,uri,uri);
        responseText = Constant.response_head_404 + responseText;
        byte[] responseByte = responseText.getBytes("utf-8");
        os.write(responseByte);
    }


    public void handle200(Socket s, Request request,Response response) throws IOException {
        OutputStream os = s.getOutputStream();
        String contentType = response.getContentType();
        byte[] body = response.getBody();
        String cookiesHeader = response.getCookiesHeader();
        boolean gzip = isGzip(request, body, contentType);
        String headText;
        if (gzip)
            headText = Constant.response_head_200_gzip;
        else
            headText = Constant.response_head_200;
        headText = StrUtil.format(headText, contentType, cookiesHeader);
        if (gzip)
            body = ZipUtil.gzip(body);
        byte[] head = headText.getBytes();
        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);
        os.write(responseBytes,0,responseBytes.length);
        os.flush();
        os.close();
        //s.close();
    }

    protected void handle500(Socket s, Exception e) {
        try {
            OutputStream os = s.getOutputStream();
            StackTraceElement stes[] = e.getStackTrace();
            StringBuffer sb = new StringBuffer();
            sb.append(e.toString());
            sb.append("\r\n");
            for (StackTraceElement ste : stes) {
                sb.append("\t");
                sb.append(ste.toString());
                sb.append("\r\n");
            }

            String msg = e.getMessage();

            if (null != msg && msg.length() > 20)
                msg = msg.substring(0, 19);

            String text = StrUtil.format(Constant.textFormat_500, msg, e.toString(), sb.toString());
            text = Constant.response_head_500 + text;
            byte[] responseBytes = text.getBytes("utf-8");
            os.write(responseBytes);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}
