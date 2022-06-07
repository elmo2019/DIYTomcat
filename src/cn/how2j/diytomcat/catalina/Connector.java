package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ThreadPoolUtil;
import cn.how2j.diytomcat.util.WebXMLUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import http.Request;
import http.Response;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Connector implements Runnable{
    int port;
    private Service service;
    //实现gzip功能，增加四个属性
    private String compression;
    private int compressionMinSize;
    private String noCompressionUserAgents;
    private String compressableMimeType;
    public String getCompression() {
        return compression;
    }
    public void setCompression(String compression) {
        this.compression = compression;
    }
    public int getCompressionMinSize() {
        return compressionMinSize;
    }
    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }
    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }
    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }
    public String getCompressableMimeType() {
        return compressableMimeType;
    }
    public void setCompressableMimeType(String compressableMimeType) {
        this.compressableMimeType = compressableMimeType;
    }




    public Connector(Service service){
        this.service = service;
    }
    public Service getService(){
        return service;
    }
    public void setPort(int port){
        this.port=port;
    }
    //因为继承了Runnable 接口，要重写 run 方法
    public void run(){
        try {
            //int port = 18080;
            ServerSocket ss = new ServerSocket(port);

            while(true) {
                Socket s =  ss.accept();
                Runnable r = new Runnable(){
                    @Override
                    public void run() {
                        try {
                            Request request = new Request(s,Connector.this);
                            Response response = new Response();
                            HttpProcessor httpProcessor = new HttpProcessor();
                            httpProcessor.execute(s,request,response);
                        } catch (IOException e) {
                            e.printStackTrace();
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
    //做出日志风格
    public void init(){
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]",port);
    }
    //启动监听该线程
    public void start(){
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]",port);
        new Thread(this).start();
    }



}
