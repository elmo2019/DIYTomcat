package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ServerXMLUtil;
import cn.how2j.diytomcat.watcher.WarFileWatcher;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;


import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Host {
    private String name;
    private Map<String, Context> contextMap;
    private Engine engine;

    public Host(String name, Engine engine){
        this.contextMap = new HashMap<>();
        this.name = ServerXMLUtil.getHostName();
        this.engine=engine;

        scanContextsOnWebAppsFolder(); //扫描WebApp文件夹，把所有应用添加到映射，方便对照uri进行查找
        scanContextsInServerXML(); //扫描配置文件
        scanWarOnWebAppsFolder(); // 配置war文件
        new WarFileWatcher(this).start();
    }
    //扫描webapps目录，处理所有war
    private void scanWarOnWebAppsFolder() {
        File folder = FileUtil.file(Constant.webappsFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if(!file.getName().toLowerCase().endsWith(".war"))
                continue;
            loadWar(file);
        }
    }

    //把 war 文件解压为目录，并把文件夹加载为 Context
    public void loadWar(File warFile) {
        String fileName =warFile.getName();
        String folderName = StrUtil.subBefore(fileName,".",true);
        //看看是否已经有对应的 Context了
        Context context= getContext("/"+folderName);
        if(null!=context)
            return;
        //先看是否已经有对应的文件夹
        File folder = new File(Constant.webappsFolder,folderName);
        if(folder.exists())
            return;
        //移动war文件，因为jar 命令只支持解压到当前目录下
        File tempWarFile = FileUtil.file(Constant.webappsFolder, folderName, fileName);
        File contextFolder = tempWarFile.getParentFile();
        contextFolder.mkdir();
        FileUtil.copyFile(warFile, tempWarFile);
        //解压
        String command = "jar xvf " + fileName;
//		System.out.println(command);
        Process p =RuntimeUtil.exec(null, contextFolder, command);
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //解压之后删除临时war
        tempWarFile.delete();
        //然后创建新的 Context
        load(contextFolder);
    }

    //实现静态部署war功能，把一个文件夹加载为Context
    public void load(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path, docBase, this, false);
        contextMap.put(context.getPath(), context);
    }

    //实现热加载功能的host中的重载功能
    public void reload(Context context){
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath());
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();
        //stop
        context.stop();
        //remove
        contextMap.remove(path);
        //allocate new context
        Context newContext = new Context(path,docBase,this,reloadable);
        //assign it to map
        contextMap.put(newContext.getPath(),newContext);
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());
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
        List<Context> contexts = ServerXMLUtil.getContext(this);
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
    private void loadContext(File folder){
        String path = folder.getName();
        if("ROOT".equals(path))
            path="/";
        else
            path ="/"+path;
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path,docBase,this,true);
        contextMap.put(context.getPath(),context);
    }

}
