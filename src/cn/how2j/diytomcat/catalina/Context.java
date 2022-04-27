package cn.how2j.diytomcat.catalina;
import cn.how2j.diytomcat.exception.WebConfigDuplicatedException;
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ContextXMLUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.*;

public class Context {
    private String path;    //path相当于文件路径，uri中的路径
    private String docBase;  //docBase是在文件系统中（服务器存储中的路径）
    private File contextWebXmlFile;
    //建议servlet信息的一对一的Map映射
    private Map<String, String> url_servletClassName;
    private Map<String, String> url_servletName;
    private Map<String, String> servletName_className;
    private Map<String, String> className_servletName;

    public Context(String path, String docBase) {
        TimeInterval timeInterval = DateUtil.timer();
        this.path=path;
        this.docBase=docBase;

        //解析servlet
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResourse());
        this.url_servletClassName = new HashMap<>();
        this.url_servletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();
        deploy();
    }

    public String getPath(){
        return  path;
    }
    public void setPath(String path){
        this.path=path;
    }
    public String getDocBase(){
        return docBase;
    }
    public void setDocBase(String docBase){
        this.docBase=docBase;
    }

    //解析context.xml文件，得到servlet配置信息
    private void parseServletMapping(Document d){
        // url_ServletName
        Elements mappingurlElements = d.select("servlet-mapping url-pattern");
        for(Element mappingurlElement : mappingurlElements){
            String urlPattern = mappingurlElement.text();
            String servletName = mappingurlElement.parent().select("servlet-name").first().text();
            url_servletName.put(urlPattern,servletName);
        }

        //servletName_calssName  / className_servletName
        Elements servletNameElements = d.select("servlet servlet-name");
        for(Element servletNameElement : servletNameElements){
            String servletName = servletNameElement.text();
            String servletclass = servletNameElement.parent().select("servlet-class").first().text();
            servletName_className.put(servletName,servletclass);
            className_servletName.put(servletclass,servletName);
        }

        //url_servletClassName
        Set<String> urls = url_servletName.keySet();
        for(String url : urls){
            String servletName = url_servletName.get(url);
            String servletClassName = servletName_className.get(servletName);
            url_servletClassName.put(url, servletClassName);
        }
    }
    //查看是否重复加载
    private void checkDuplicated(Document d,String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements = d.select(mapping);
        //将信息放入一个集合，排序之后看相邻两个元素是否相同
        List<String> contents = new ArrayList<>();
        for(Element e :elements){
            contents.add(e.text());
        }
        Collections.sort(contents);
        for(int i =0;i<contents.size()-1;i++){
            String contentPre = contents.get(i);
            String contentNext =contents.get(i+1);
            if(contentPre.equals(contentNext)){
                throw new WebConfigDuplicatedException(StrUtil.format(desc,contentPre));
            }
        }
    }
    private void checkDuplicated() throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        checkDuplicated(d, "servlet-mapping url-pattern", "servlet url 重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-name", "servlet 名称重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-class", "servlet 类名重复,请保持其唯一性:{} ");
    }
    //初始化，调用解析方法。解析之前判断是否存在和重复
    private void init(){
        if(!contextWebXmlFile.exists()){
            return;
        }
        try{
            checkDuplicated();
        } catch (WebConfigDuplicatedException e){
            e.printStackTrace();
            return;
        }
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        parseServletMapping(d);
    }
    //主要是为了打印日志
    private void deploy(){
        TimeInterval timeInterval = DateUtil.timer();
        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        init();
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms",this.getDocBase(),timeInterval.intervalMs());
    }

    public String getServletClassName(String uri) {
        return url_servletClassName.get(uri);
    }


}
