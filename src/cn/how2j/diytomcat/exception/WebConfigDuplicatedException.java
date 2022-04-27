package cn.how2j.diytomcat.exception;

//自定义错误，返回servlet重复配置
public class WebConfigDuplicatedException extends Exception{
    public WebConfigDuplicatedException(String msg) {
        super(msg);
    }
}
