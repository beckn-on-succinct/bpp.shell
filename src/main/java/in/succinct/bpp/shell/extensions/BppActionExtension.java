package in.succinct.bpp.shell.extensions;

import com.venky.extension.Extension;
import in.succinct.beckn.Request;
import in.succinct.bpp.shell.util.BecknUtil;

import java.lang.reflect.Method;
import java.util.Map;

public abstract class BppActionExtension implements Extension {

    @SuppressWarnings("unchecked")
    @Override
    public void invoke(Object... context) {
        Map<String,String> headers = (Map<String,String>) context[0];
        Request request = (Request) context[1];
        Request response = (Request) context[2];
        BecknUtil.createReplyContext(request,response);
        try {
            Method method = getClass().getMethod(request.getContext().getAction(), Request.class, Request.class);
            method.invoke(this, request, response);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    public abstract void search(Request request,Request reply);
    public abstract void select(Request request,Request reply);
    public abstract void init(Request request,Request reply);
    public abstract void confirm(Request request,Request reply);
    public abstract void track(Request request,Request reply);
    public abstract void cancel(Request request,Request reply);
    public abstract void update(Request request,Request reply);
    public abstract void status(Request request,Request reply);
    public abstract void rating(Request request,Request reply);
    public abstract void support(Request request,Request reply);
    public abstract void get_cancellation_reasons(Request request,Request reply);
    public abstract void get_return_reasons(Request request,Request reply);
    public abstract void get_rating_categories(Request request,Request reply);
    public abstract void get_feedback_categories(Request request,Request reply);


}
