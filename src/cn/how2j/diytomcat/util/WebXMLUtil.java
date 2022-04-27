package cn.how2j.diytomcat.util;

import static cn.how2j.diytomcat.util.Constant.webXmlFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cn.how2j.diytomcat.catalina.Context;
import cn.hutool.core.io.FileUtil;

//解析 MimeType
public class WebXMLUtil {
    //创建一个map映射去保存，文件扩展名和返回文件类型信息的映射
    private static Map<String,String> mimeTypeMapping = new HashMap<>();


    //获取对应的返回类型
    public static synchronized String getMimeType(String extName) {
        if (mimeTypeMapping.isEmpty())
            initMimeType();
        String mimeType = mimeTypeMapping.get(extName);
        if (null == mimeType)
            return "text/html";
        return mimeType;
    }

    //初始化，解析xml，将其对应
    private static void initMimeType(){
        String xml = FileUtil.readUtf8String(webXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("mime-mapping");

        for(Element e : es){
            String extName = e.select("extension").first().text();
            String mimeType = e.select("mime-type").first().text();
            mimeTypeMapping.put(extName,mimeType);
        }
    }

    //根据context，找该context对应的欢迎文件，逻辑为 新建一个文件绝对路径，判断是否存在
    public static String getWelcomeFile(Context context) {
        String xml = FileUtil.readUtf8String(webXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("welcome-file");
        for (Element e : es) {
            String welcomeFileName = e.text();
            File f = new File(context.getDocBase(), welcomeFileName);
            if (f.exists())
                return f.getName();
        }
        return "index.html";
    }
}
