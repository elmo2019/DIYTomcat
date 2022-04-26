package cn.how2j.diytomcat.util;

import cn.how2j.diytomcat.catalina.*;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Range;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;

public class ServerXMLUtil {

    //该方法用来解析获取Connector属性
    public static List<Connector> getConnectors(Service service){
        List<Connector> results = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("Connector");
        for(Element e : es){
            int port = Convert.toInt(e.attr("port"));
            Connector c = new Connector(service);
            c.setPort(port);
            results.add(c);
        }
        return results;
    }

    //该方法用来解析XML配置文件中的Context属性
    public static List<Context> getContext(){
        List<Context> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("Context");
        for(Element e : es){
            String path = e.attr("path");
            String docBase = e.attr("docBase");
            Context context = new Context(path,docBase);
            result.add(context);
        }
        return result;
    }

    //解析获取Service
    public static String getServiceName(){
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Element host = d.select("Service").first();
        return host.attr("name");
    }


    //获取默认host的值
    public static String getEngineDefaultHost() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Element host = d.select("Engine").first();
        return host.attr("defaultHost");
    }

    //该方法用来解析 Host属性
    public static String getHostName(){
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Element host = d.select("Host").first();
        return host.attr("name");
    }

    //获取host列表
    public static List<Host> getHosts(Engine engine){
        List<Host> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("Host");
        for(Element e : es){
            String name = e.attr("name");
            Host host = new Host(name,engine);
            result.add(host);
        }
        return result;
    }


}
