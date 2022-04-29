package cn.how2j.diytomcat.servlets;

import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.util.Constant;
import cn.hutool.core.util.ReflectUtil;
import http.Request;
import http.Response;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class InvokerServlet extends HttpServlet {
    private static InvokerServlet instance = new InvokerServlet();

    public static synchronized InvokerServlet getInstance(){
        return instance;
    }
    private InvokerServlet(){

    }

    public void service (HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse) throws IOException , ServletException {

        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;

        String uri = request.getUri();
        Context context = request.getContext();
        String servletClassName = context.getServletClassName(uri);

        Object servletObject = ReflectUtil.newInstance(servletClassName);

        ReflectUtil.invoke(servletObject,"doGet",request,response);
        response.setStatus(Constant.CODE_200);
    }

}
