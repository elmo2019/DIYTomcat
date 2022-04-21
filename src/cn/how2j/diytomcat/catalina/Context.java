package cn.how2j.diytomcat.catalina;
import cn.how2j.diytomcat.util.Constant;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;

public class Context {
    private String path;    //path相当于文件路径，uri中的路径
    private String docBase;  //docBase是在文件系统中（服务器存储中的路径）

    public Context(String path, String docBase) {
        TimeInterval timeInterval = DateUtil.timer();
        this.path=path;
        this.docBase=docBase;
        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms", this.docBase,timeInterval.intervalMs());
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
}
