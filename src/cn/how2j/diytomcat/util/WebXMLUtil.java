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

public class WebXMLUtil {
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
