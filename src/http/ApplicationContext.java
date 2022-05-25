package http;

import cn.how2j.diytomcat.catalina.Context;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

//实现ServletContext功能


public class ApplicationContext extends BaseServletContext{
    private Map<String,Object> attributesMap;
    private Context context;

    public ApplicationContext(Context context) {
        this.attributesMap = new HashMap<>();
        this.context = context;
    }

    public void removeAttribute(String name){
        attributesMap.remove(name);
    }
    public void serAttribute(String name,Object value){
        attributesMap.put(name,value);
    }
    public Object getAttribute(String name){
        return attributesMap.get(name);
    }
    public Enumeration<String> getAttributeNames(){
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);

    }

    public String getRealPath(String path){
        return new File(context.getDocBase(),path).getAbsolutePath();
    }
}
