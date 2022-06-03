package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.servlets.DefaultServlet;
import cn.how2j.diytomcat.servlets.InvokerServlet;
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.SessionManager;
import cn.how2j.diytomcat.util.WebXMLUtil;
import cn.how2j.diytomcat.webappservlet.HelloServlet;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import http.Request;
import http.Response;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

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

            String servletClassname = context.getServletClassName(uri);
            if(null !=servletClassname){
                InvokerServlet.getInstance().service(request,response);
            }else {
                DefaultServlet.getInstance().service(request,response);
            }
            if(Constant.CODE_200 == response.getStatus()){
                handle200(s,response);
                return;
            }
            if(Constant.CODE_404 == response.getStatus()){
                handle404(s,uri);
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


    public static void handle200(Socket s, Response response) throws IOException {
        String contentType = response.getContentType();
        String headText = Constant.response_head_200;
        String cookiesHeader = response.getCookiesHeader();
        headText = StrUtil.format(headText, contentType, cookiesHeader);
        //headText = StrUtil.format(headText,contentType);
        byte[] head = headText.getBytes();

        byte[] body = response.getBody();

        byte[] responseBytes = new byte[head.length+body.length];
        ArrayUtil.copy(head,0,responseBytes,0,head.length);
        ArrayUtil.copy(body,0,responseBytes,head.length,body.length);

        OutputStream os = s.getOutputStream();
        os.write(responseBytes);
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
