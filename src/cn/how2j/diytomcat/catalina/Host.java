package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ServerXMLUtil;


import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Host {
    private String name;
    private Map<String, Context> contextMap;

    public Host(){
        this.contextMap = new HashMap<>();
        this.name = ServerXMLUtil.getHostName();

        scanContextsOnWebAppsFolder(); //扫描WebApp文件夹，把所有应用添加到映射，方便对照uri进行查找
        scanContextsInServerXML(); //扫描配置文件

    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Context getContext(String path) {
        return contextMap.get(path);
    }


    //通过解析配置文件的方式添加多应用
    private void scanContextsInServerXML(){
        List<Context> contexts = ServerXMLUtil.getContext();
        for(Context context : contexts){
            contextMap.put(context.getPath(),context);
        }
    }

    //扫描WebAPP目录，把项目添加到Map映射
    private void scanContextsOnWebAppsFolder() {
        File[] folders = Constant.webappsFolder.listFiles();
        for (File folder : folders) {
            if (!folder.isDirectory())
                continue;
            loadContext(folder);
        }
    }

    //设置新的path路径
    private void loadContext(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;

        String docBase = folder.getAbsolutePath();
        Context context = new Context(path,docBase);

        contextMap.put(context.getPath(), context);
    }

}
