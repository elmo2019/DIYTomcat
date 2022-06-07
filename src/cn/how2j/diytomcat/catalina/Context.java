package cn.how2j.diytomcat.catalina;
import cn.how2j.diytomcat.classloader.WebappClassLoader;
import cn.how2j.diytomcat.exception.WebConfigDuplicatedException;
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ContextXMLUtil;
import cn.how2j.diytomcat.watcher.ContextFileChangeWatcher;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;

import http.ApplicationContext;
import http.StandardServletConfig;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import javax.servlet.*;
import javax.servlet.http.HttpServlet;
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

    //为每一个context类 （web多应用）新建一个自己的类对象
    private WebappClassLoader webappClassLoader;

    //实现热加载，增加属性
    private Host host;
    private boolean reloadable;
    private ContextFileChangeWatcher contextFileChangeWatcher;

    //增加servletContext属性
    private ServletContext servletContext;

    //实现Servlet单例
    private Map<Class<?>, HttpServlet> servletPool;

    //实现生命周期功能
    private Map<String, Map<String, String>> servlet_className_init_params;
    //实现自启动
    private List<String> loadOnStartupServletClassNames;

    //实现Filter的加载和初始化
    private Map<String, List<String>> url_filterClassName;
    private Map<String, List<String>> url_FilterNames;
    private Map<String, String> filterName_className;
    private Map<String, String> className_filterName;
    private Map<String, Map<String, String>> filter_className_init_params;

    private Map<String, Filter> filterPool;

    //实现contex的生命周期监听事件
    private List<ServletContextListener> listeners;


    public Context(String path, String docBase,Host host, boolean reloadable) {
        this.servletContext = new ApplicationContext(this);

        TimeInterval timeInterval = DateUtil.timer();
        this.host = host;
        this.reloadable=reloadable;

        this.path = path;
        this.docBase = docBase;
        //解析servlet
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());
        this.url_servletClassName = new HashMap<>();
        this.url_servletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase,commonClassLoader);

        this.servletPool = new HashMap<>();

        this.servlet_className_init_params = new HashMap<>();

        this.loadOnStartupServletClassNames = new ArrayList<>();

        this.url_filterClassName = new HashMap<>();
        this.url_FilterNames = new HashMap<>();
        this.filterName_className = new HashMap<>();
        this.className_filterName = new HashMap<>();
        this.filter_className_init_params = new HashMap<>();
        this.filterPool = new HashMap<>();

        listeners = new ArrayList<ServletContextListener>();

        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        deploy();
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms", this.docBase,timeInterval.intervalMs());

    }
    //fireEvent 方法
    private void fireEvent(String type) {
        ServletContextEvent event = new ServletContextEvent(servletContext);
        for (ServletContextListener servletContextListener : listeners) {
            if("init".equals(type))
                servletContextListener.contextInitialized(event);
            if("destroy".equals(type))
                servletContextListener.contextDestroyed(event);
        }
    }

    //从web.xml中扫描监听器
    private void loadListeners(){
        try {
            if(!contextWebXmlFile.exists())
                return;
            String xml = FileUtil.readUtf8String(contextWebXmlFile);
            Document d = Jsoup.parse(xml);

            Elements es = d.select("listener listener-class");
            for (Element e : es) {
                String listenerClassName = e.text();

                Class<?> clazz= this.getWebClassLoader().loadClass(listenerClassName);
                ServletContextListener listener = (ServletContextListener) clazz.newInstance();
                addListener(listener);

            }
        } catch (IORuntimeException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    //添加监听器
    public void addListener(ServletContextListener listener){
        listeners.add(listener);
    }

    //获取匹配了的过滤器集合
    public List<Filter> getMatchedFilters(String uri){
        List<Filter> filters = new ArrayList<>();
        Set<String> patterns = url_filterClassName.keySet();
        //System.out.println("检查MAP数据是否为空"+patterns);
        Set<String> matchedPatterns = new HashSet<>();
        for(String pattern : patterns){
            if(match(pattern,uri)){
                matchedPatterns.add(pattern);
            }
        }
        Set<String> matchedFilterClassNames = new HashSet<>();
        for(String pattern : matchedPatterns){
            List<String> filterClassName = url_filterClassName.get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }
        for (String filterClassName : matchedFilterClassNames) {
            Filter filter = filterPool.get(filterClassName);
            filters.add(filter);
        }
        return filters;
    }

    //实现Filter 三种匹配模式
    private boolean match(String pattern,String uri){
        // 完全匹配
        if(StrUtil.equals(pattern, uri))
            return true;
        // /* 模式
        if(StrUtil.equals(pattern, "/*"))
            return true;
        // 后缀名 /*.jsp
        if(StrUtil.startWith(pattern, "/*.")) {
            String patternExtName = StrUtil.subAfter(pattern, '.', false);
            String uriExtName = StrUtil.subAfter(uri, '.', false);
            if(StrUtil.equals(patternExtName, uriExtName))
                return true;
        }

        return false;
    }

    //初始化Filter
    private void initFilter(){
        Set<String> classNames = className_filterName.keySet();
        for(String className : classNames){
            try{
                Class clazz = this.getWebappClassLoader().loadClass(className);
                Map<String ,String> initParameters = filter_className_init_params.get(className);
                String filterName = className_filterName.get(className);
                FilterConfig filterConfig = new StandardFilterConfig(servletContext,filterName,initParameters);
                Filter filter = filterPool.get(clazz);
                if(null==filter){
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    filterPool.put(className, filter);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    //解析Filter参数
    private void parseFilterInitParams(Document d) {
        Elements filterClassNameElements = d.select("filter-class");
        for (Element filterClassNameElement : filterClassNameElements) {
            String filterClassName = filterClassNameElement.text();

            Elements initElements = filterClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;


            Map<String, String> initParams = new HashMap<>();

            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }

            filter_className_init_params.put(filterClassName, initParams);

        }
    }

    //解析Filter的对应关系
    public void parseFilterMapping(Document d) {
        // filter_url_name
        Elements mappingurlElements = d.select("filter-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String filterName = mappingurlElement.parent().select("filter-name").first().text();

            List<String> filterNames= url_FilterNames.get(urlPattern);
            if(null==filterNames) {
                filterNames = new ArrayList<>();
                url_FilterNames.put(urlPattern, filterNames);
            }
            filterNames.add(filterName);
        }
        // class_name_filter_name
        Elements filterNameElements = d.select("filter filter-name");
        for (Element filterNameElement : filterNameElements) {
            String filterName = filterNameElement.text();
            String filterClass = filterNameElement.parent().select("filter-class").first().text();
            filterName_className.put(filterName, filterClass);
            className_filterName.put(filterClass, filterName);
        }
        // url_filterClassName

        Set<String> urls = url_FilterNames.keySet();
        for (String url : urls) {
            List<String> filterNames = url_FilterNames.get(url);
            if(null == filterNames) {
                filterNames = new ArrayList<>();
                url_FilterNames.put(url, filterNames);
            }
            for (String filterName : filterNames) {
                String filterClassName = filterName_className.get(filterName);
                List<String> filterClassNames = url_filterClassName.get(url);
                if(null==filterClassNames) {
                    filterClassNames = new ArrayList<>();
                    url_filterClassName.put(url, filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
    }
    //实现JspClassLoader
    public WebappClassLoader getWebClassLoader(){
        return webappClassLoader;
    }

    //对需要做自启动的类做自启动
    public void handleLoadOnStartup() {
        for (String loadOnStartupServletClassName : loadOnStartupServletClassNames) {
            try {
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                getServlet(clazz);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ServletException e) {
                e.printStackTrace();
            }
        }
    }

    //从web.xml 解析自启动参数
    public void parseLoadOnStartup(Document d){
        Elements es = d.select("load-on-startup");
        for (Element e : es) {
            String loadOnStartupServletClassName = e.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
        }
    }
    //从web.xml 解析Servlet生命周期的初始化参数
    private void parseServletInitParams(Document d) {

        Elements servletClassNameElements = d.select("servlet-class");
        for (Element servletClassNameElement : servletClassNameElements) {
            String servletClassName = servletClassNameElement.text();
            Elements initElements = servletClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;
            Map<String, String> initParams = new HashMap<>();

            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }

            servlet_className_init_params.put(servletClassName, initParams);
        }

		System.out.println("class_name_init_params:" + servlet_className_init_params);
    }
    //实现Servlet池子
    public synchronized HttpServlet  getServlet(Class<?> clazz)
            throws InstantiationException, IllegalAccessException, ServletException {
        HttpServlet servlet = servletPool.get(clazz);
        if (null == servlet) {
            servlet = (HttpServlet) clazz.newInstance();
            ServletContext servletContext = this.getServletContext();
            String className = clazz.getName();
            String servletName = className_servletName.get(className);
            Map<String, String> initParameters = servlet_className_init_params.get(className);
            System.out.println(initParameters);
            ServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParameters);
            servlet.init(servletConfig);
            servletPool.put(clazz, servlet);
        }
        return servlet;
    }

    //获取servletContext属性
    public ServletContext getServletContext(){
        return servletContext;
    }

    //提供热加载属性的方法
    public boolean isReloadable(){
        return reloadable;
    }
    public void setReloadable(boolean reloadable){
        this.reloadable = reloadable;
    }



    //获取当前context类（web应用）的加载器
    public WebappClassLoader getWebappClassLoader(){
        return webappClassLoader;
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
    private void parseServletMapping(Document d) {
        // url_ServletName
        Elements mappingurlElements = d.select("servlet-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String servletName = mappingurlElement.parent().select("servlet-name").first().text();
            url_servletName.put(urlPattern, servletName);
        }
        // servletName_className / className_servletName
        Elements servletNameElements = d.select("servlet servlet-name");
        for (Element servletNameElement : servletNameElements) {
            String servletName = servletNameElement.text();
            String servletClass = servletNameElement.parent().select("servlet-class").first().text();
            servletName_className.put(servletName, servletClass);
            className_servletName.put(servletClass, servletName);
        }
        // url_servletClassName
        Set<String> urls = url_servletName.keySet();
        for (String url : urls) {
            String servletName = url_servletName.get(url);
            String servletClassName = servletName_className.get(servletName);
            url_servletClassName.put(url, servletClassName);
        }
    }
    //查看是否重复加载
    private void checkDuplicated(Document d, String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements = d.select(mapping);
        // 判断逻辑是放入一个集合，然后把集合排序之后看两临两个元素是否相同
        List<String> contents = new ArrayList<>();
        for (Element e : elements) {
            contents.add(e.text());
        }

        Collections.sort(contents);

        for (int i = 0; i < contents.size() - 1; i++) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre));
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
    private void init() {
        fireEvent("init");
        if (!contextWebXmlFile.exists())
            return;

        try {
            checkDuplicated();
        } catch (WebConfigDuplicatedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        parseServletMapping(d);
        parseServletInitParams(d);
        parseLoadOnStartup(d);
        handleLoadOnStartup();
        parseFilterMapping(d);
        parseFilterInitParams(d);
        initFilter();
    }

    //销毁所有的servlets
    private void destroyServlets() {
        Collection<HttpServlet> servlets = servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }
    //主要是为了打印日志
    private void deploy() {
        //初始化监听器
        loadListeners();
        init();
        if(reloadable){
            contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }
        JspC c = new JspC();
        new JspRuntimeContext(servletContext,c);

    }
    //停止方法，把类加载器和热加载监听器关闭
    public void stop(){
        fireEvent("destroy");
        webappClassLoader.stop();
        contextFileChangeWatcher.stop();
        destroyServlets();
    }
    //重载该context类
    public void reload(){
        host.reload(this);
    }

    public String getServletClassName(String uri) {
        return url_servletClassName.get(uri);
    }


}
