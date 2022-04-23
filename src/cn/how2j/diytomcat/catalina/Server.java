package cn.how2j.diytomcat.catalina;


import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ThreadPoolUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
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
    public void init(){
        try {

            //logJVM();
            //Host host = new Host();
            //Engine engine = new Engine();
            //Service service = new Service();
            int port = 18080;
/*
            if(!NetUtil.isUsableLocalPort(port)) {
                System.out.println(port +" 端口已经被占用了，排查并关闭本端口的办法hahahaha");
                return;
            }

 */
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
                            System.out.println(uri);
                            //一个错误点，必须是先检查uri是否存在才能去获取context对象，否则存在context对象为null
                            Context context = request.getContext();
                            if("/".equals(uri)){
                                String html = "Hello DIY Tomcat from cjs";
                                response.getWriter().println(html);
                            }
                            else{
                                String fileName = StrUtil.removePrefix(uri, "/");
                                File file = FileUtil.file(context.getDocBase(),fileName);
                                if(file.exists()){
                                    String fileContent = FileUtil.readUtf8String(file);
                                    response.getWriter().println(fileContent);

                                    if(fileName.equals("timeConsume.html")){
                                        ThreadUtil.sleep(1000);
                                    }

                                }
                                else{
                                    //response.getWriter().println("File Not Found");
                                    handle404(s,uri);
                                    return;
                                }
                            }
                            handle200(s, response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        finally{
                            try {
                                if(!s.isClosed()){
                                    s.close();
                                }
                            } catch (IOException e){
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
    public static void handle404(Socket s,String uri) throws IOException{
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


}