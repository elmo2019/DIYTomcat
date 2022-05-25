package http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class StandardServletConfig implements ServletConfig {
    private ServletContext servletContext;
    private Map<String,String> initParameters;
    private String servletName;

    public StandardServletConfig(ServletContext servletContext, String servletName,
                                 Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.servletName = servletName;
        this.initParameters = initParameters;
        if (null == this.initParameters)
            this.initParameters = new HashMap<>();
    }


    @Override
    public String getServletName() {
        return null;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public String getInitParameter(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return null;
    }
}
