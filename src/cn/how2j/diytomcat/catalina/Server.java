package cn.how2j.diytomcat.catalina;


import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ThreadPoolUtil;
import cn.how2j.diytomcat.util.WebXMLUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import http.Request;
import http.Response;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Server {
    private Service service;
    public Server(){
        this.service = new Service(this);
    }
    public void start(){
        logJVM();
        init();
    }
    private void init() {
        try {
            int port = 18080;
            ServerSocket ss = new ServerSocket(port);

            while(true) {
                Socket s =  ss.accept();
                Runnable r = new Runnable(){
                    @Override
                    public void run() {
                        try {
                            Request request = new Request(s,service);
                            Response response = new Response();
                            String uri = request.getUri();
                            if(null==uri)
                                return;
                            System.out.println("uri:"+uri);
                            System.out.println("requst"+request.getRequestString());
                            Context context = request.getContext();

                            if("/500.html".equals(uri)){
                                throw new Exception("this is a deliberately created exception");
                            }

                            if("/".equals(uri))
                                uri = WebXMLUtil.getWelcomeFile(request.getContext());

                            String fileName = StrUtil.removePrefix(uri, "/");
                            File file = FileUtil.file(context.getDocBase(),fileName);
                            //多级目录判断 ？
                            if (!file.isFile()){//因为只有目录没有文件，所有判断不是文件就行了
                                uri=uri+"/"+WebXMLUtil.getWelcomeFile(request.getContext());
                                fileName = StrUtil.removePrefix(uri, "/");
                                file=new File(context.getDocBase(),fileName);
                            }
                            if(file.exists()){
                                String extName = FileUtil.extName(file);
                                String mimeType = WebXMLUtil.getMimeType(extName);
                                response.setContentType(mimeType);
                                //现在改为直接读取二进制文件
                                byte[] body = FileUtil.readBytes(file);
                                response.setBody(body);
                                /*
                                String fileContent = FileUtil.readUtf8String(file);
                                response.getWriter().println(fileContent);
                                 */

                                if(fileName.equals("timeConsume.html")){
                                    ThreadUtil.sleep(1000);
                                }

                            }
                            else{
                                handle404(s, uri);
                                return;
                            }
                            handle200(s, response);
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
                };

                ThreadPoolUtil.run(r);
            }
        } catch (IOException e) {
            LogFactory.get().error(e);
            e.printStackTrace();
        }
    }
    private static void logJVM() {
        Map<String,String> infos = new LinkedHashMap<>();
        infos.put("Server version", "CJS DiyTomcat/1.0.1");
        infos.put("Server built", "2022-04-21 10:20:22");
        infos.put("Server number", "1.0.1");
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));

        Set<String> keys = infos.keySet();
        for (String key : keys) {
            LogFactory.get().info(key+":\t\t" + infos.get(key));
        }
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
        headText = StrUtil.format(headText,contentType);
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
