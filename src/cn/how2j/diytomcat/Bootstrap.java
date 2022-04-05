package cn.how2j.diytomcat;


import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.MiniBrowser;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import http.Request;
import http.Response;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Bootstrap {

    public static void main(String[] args) {

        try {
            int port = 18080;

            if(!NetUtil.isUsableLocalPort(port)) {
                System.out.println(port +" 端口已经被占用了，排查并关闭本端口的办法请用：\r\nhttps://how2j.cn/k/tomcat/tomcat-portfix/545.html");
                return;
            }
            ServerSocket ss = new ServerSocket(port);

            while(true) {
                Socket s =  ss.accept();
                Request request = new Request(s);
                /* 初始处理request请求的方法，直接对流进行单一简单的处理
                InputStream is= s.getInputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                is.read(buffer);
                byte[] buffer = MiniBrowser.readBytes(is);
                String requestString = new String(buffer,"utf-8");
                 */
                //改进处理方法，对长度过于长的流进行判断处理
                String requestString = request.getRequestString();
                System.out.println("浏览器的输入信息： \r\n" + requestString);
                String uri =  request.getUri();
                System.out.println("Uri： \r\n" + uri);

                /*初始的服务器进行响应的办法，直接输出流
                OutputStream os = s.getOutputStream();
                String response_head = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n\r\n";
                String responseString = "Hello DIY Tomcat from how2j.cn";
                responseString = response_head + responseString;
                os.write(responseString.getBytes());
                os.flush();
                s.close();

                 */
                //改进的response处理方法，抽象出response类，类内部对消息进行拼装
                Response response = new Response();
                /*
                //直接将要输出内容写进内存，再由返回处理
                String html = "Hello DIY Tomcat from how2j.cn";
                response.getWriter().println(html);

                 */
                if(uri==null){
                    continue;
                }
                System.out.println(uri);
                if("/".equals(uri)){
                    String html = "Hello DIY Tomcat from how2j.cn";
                    response.getWriter().println(html);
                }else {
                    String fileName = StrUtil.removePrefix(uri,"/");
                    File file = FileUtil.file(Constant.rootFolder,fileName);
                    if(file.exists()){
                        String fileContent = FileUtil.readUtf8String(file);
                        response.getWriter().println(fileContent);
                    }else{
                        response.getWriter().println("File Not Found");
                    }
                }

                handle200(s,response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void handle200(Socket s, Response response) throws IOException {
        String contentType = response.getContentType();
        String headText = Constant.response_head_200;
        headText = StrUtil.format(headText,contentType);
        byte[] head = headText.getBytes();

        byte[] body = response.getBody();

        byte[] responseBytes = new byte[head.length+body.length];
        ArrayUtil.copy(head,0,responseBytes,0,head.length);
        ArrayUtil.copy(body,0,responseBytes,head.length,body.length);

        OutputStream os = s.getOutputStream();
        os.write(responseBytes);
        s.close();
    }
}

