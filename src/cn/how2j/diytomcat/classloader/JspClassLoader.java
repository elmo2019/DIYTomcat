package cn.how2j.diytomcat.classloader;

import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.util.Constant;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class JspClassLoader extends URLClassLoader {
    //用map来保存JSP文件和JspClassLoader之间的关系
    private static Map<String,JspClassLoader> map = new HashMap<>();

    //让Jsp 和 JspClassLoader取消关联
    public static void invalidJspClassLoader(String uri, Context context){
        String key = context.getPath() + "/" + uri;
        map.remove(key);
    }

    //获取jsp 对应的 JspClassLoader , 如果没有就新创建一个
    public static JspClassLoader getJspClassLoader(String uri, Context context){
        String key = context.getPath() + "/" + uri;
        JspClassLoader loader = map.get(key);
        if(null == loader){
            loader = new JspClassLoader(context);
            map.put(key,loader);
        }
        return loader;
    }

    //构造方法， JspClassLoader 会基于 WebClassLoader 来创建
    private JspClassLoader(Context context){
        super(new URL[]{},context.getWebClassLoader());
        try{
            String subFolder;
            String path = context.getPath();
            if("/".equals(path))
                subFolder="_";
            else
                subFolder = StrUtil.subAfter(path,'/',false);
            File classesFolder = new File(Constant.workFolder, subFolder);
            URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            this.addURL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
