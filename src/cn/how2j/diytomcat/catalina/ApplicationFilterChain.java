package cn.how2j.diytomcat.catalina;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import cn.hutool.core.util.ArrayUtil;

public class ApplicationFilterChain implements FilterChain{

    private Filter[] filters;
    private Servlet servlet;
    int pos;

    public ApplicationFilterChain(List<Filter> filterList,Servlet servlet){
        this.filters = ArrayUtil.toArray(filterList,Filter.class);
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        //System.out.println("过滤器长度"+filters.length);
        if(pos < filters.length) {
            Filter filter= filters[pos++];
            //System.out.println("进入 filter");
            filter.doFilter(request, response, this);
        }
        else {
            servlet.service(request, response);
            //System.out.println("进入执行else");
        }
    }

}
