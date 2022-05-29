package cn.how2j.diytomcat.test;



import cn.how2j.diytomcat.util.MiniBrowser;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import org.jsoup.helper.DataUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestTomcat {
    private static int port = 18080;
    private static String ip = "127.0.0.1";
    @BeforeClass
    public static void beforeClass() {
        //所有测试开始前看diy tomcat 是否已经启动了
        if(NetUtil.isUsableLocalPort(port)) {
            System.err.println("请先启动 位于端口: " +port+ " 的diy tomcat，否则无法进行单元测试");
            System.exit(1);
        }
        else {
            System.out.println("检测到 diy tomcat已经启动，开始进行单元测试");
        }
    }

    @Test
    public void testHelloTomcat() {
        String html = getContentString("/");
        Assert.assertEquals(html,"Hello DIY Tomcat from how2j.cn");
    }


    //添加耗时任务的时间检测，创建三个访问线程
    @Test
    public void testTimeConsumeHtml() throws InterruptedException {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(20,20,60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(10));
        TimeInterval timeInterval = DateUtil.timer();

        for(int i=0;i<3;i++){
            threadPool.execute(new Runnable(){
                public void run(){
                    getContentString("/timeConsume.html");
                }
            });
        }
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);

        long duration = timeInterval.intervalMs();

        Assert.assertTrue(duration < 3000);
    }
    //实现web类加载器功能时判断
    /*
    @Test
    public void testJavawebHello() {
        String html = getContentString("/javaweb/hello");
        Assert.assertEquals(html,"Hello DIY Tomcat from HelloServlet@javaweb");
    }

     */
    //实现单例模式后的web项目测试
    @Test
    public void testJavawebHelloSingleton() {
        String html1 = getContentString("/javaweb/hello");
        String html2 = getContentString("/javaweb/hello");
        Assert.assertEquals(html1,html2);
    }
    //判断servlet功能雏形是否正常
    @Test
    public void testhello(){
        String html = getContentString("/j2ee/hello");
        Assert.assertEquals(html,"hello DIY Tomcat from HelloServlet");
    }

    @Test
    public void testPNG() {
        byte[] bytes = getContentBytes("/logo.png");
        int pngFileLength = 1672;
        Assert.assertEquals(pngFileLength, bytes.length);
    }
    @Test
    public void testPDF() {
        byte[] bytes = getContentBytes("/etf.pdf");
        int pngFileLength = 3590775;
        Assert.assertEquals(pngFileLength, bytes.length);
    }

    //检查是否能获取解析其他文件类型
    @Test
    public void testaTxt() {
        String response  = getHttpString("/a.txt");
        containAssert(response, "Content-Type: text/plain");
    }
    @Test
    public void test500() {
        String response  = getHttpString("/500.html");
        containAssert(response, "HTTP/1.1 500 Internal Server Error");
    }
    //检查是否有404
    @Test
    public void test404(){
        String response = getHttpString("/not_exist.html");
        containAssert(response,"HTTP/1.1 404 Not Found");
    }
    @Test
    public void testaIndex() {
        String html = getContentString("/a");
        Assert.assertEquals(html,"Hello DIY Tomcat from index.html@a");
    }

    @Test
    public void testbIndex() {
        String html = getContentString("/b");
        Assert.assertEquals(html,"Hello DIY Tomcat from index.html@b");
    }

    //和文本文件获取方式不同，用另一个方法获取二进制文件
    private byte[] getContentBytes(String uri) {
        return getContentBytes(uri,false);
    }
    private byte[] getContentBytes(String uri,boolean gzip) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentBytes(url,false);
    }

    //获取http响应的内容
    private String getContentString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        String content = MiniBrowser.getContentString(url);
        return content;
    }
    //获取http响应
    private String getHttpString(String uri){
        String url = StrUtil.format("http://{}:{}{}",ip,port,uri);
        String http = MiniBrowser.getHttpString(url);
        return http;
    }
    //怎加一个 Assert，来判断是否包含
    private void containAssert(String html,String string){
        boolean match = StrUtil.containsAny(html,string);
        Assert.assertTrue(match);
    }
}

