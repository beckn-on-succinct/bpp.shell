package in.succinct.bpp.shell.extensions;

import com.venky.cache.Cache;
import com.venky.core.util.ObjectHolder;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.Controller.CacheOperation;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.View;

import java.util.Map;
import java.util.regex.Pattern;

public class URLCacheExtension implements Extension {
    private static URLCacheExtension urlCacheExtension = null;
    public static URLCacheExtension getInstance(){
        if (urlCacheExtension == null) {
            synchronized (URLCacheExtension.class) {
                if (urlCacheExtension == null) {
                    urlCacheExtension = new URLCacheExtension();
                }
            }
        }
        return urlCacheExtension;
    }
    static {
        Registry.instance().registerExtension(Controller.GET_CACHED_RESULT_EXTENSION,getInstance());
        Registry.instance().registerExtension(Controller.SET_CACHED_RESULT_EXTENSION,getInstance());
        Registry.instance().registerExtension(Controller.CLEAR_CACHED_RESULT_EXTENSION,getInstance());
    }
    private Map<String,Map<String,View>> cache = new Cache<String, Map<String, View>>(100,0.3) {
        @Override
        protected Map<String, View> getValue(String s) {
            return new Cache<String, View>(100,.3) {
                @Override
                protected View getValue(String s) {
                    return null;
                }
            };
        }
    };

    @Override
    public void invoke(Object... context) {
        if (((_IPath)context[1]).action().equals("reset_router")){
            Config.instance().getLogger(getClass().getName()).warning("ignoreing cache for " + ((_IPath)context[1]).getRequest().getRequestURI());
            return;
        }
        Path path = (Path)context[1];
        ObjectHolder<View> holder = (ObjectHolder<View>)context[2];

        switch ((CacheOperation)context[0]){
            case GET:
                get(path,holder);
                break;
            case SET:
                put(path,holder);
                break;
            case CLEAR:
                cache.clear();
                break;
        }

    }
    /*public static void main(String [] args){
        System.out.println(getInstance().isCacheable("/services/destroy"));
    }*/


    private boolean isCacheable(String path){
        Pattern[] cacheablePatterns = new Pattern[]{
                    Pattern.compile("^("+ (Config.instance().isDevelopmentEnvironment()? "/resources/scripts/node_modules" : "" )+ ".*)\\.(jpg|jpeg|png|gif|ico|ttf|eot|svg|woff|woff2|css|js|map|html|md|scss|txt|webp|json)$"),
                        Pattern.compile("^("+ String.format("/resources/%s/",Config.instance().getHostName())+ ".*)\\.(jpg|jpeg|png|gif|ico|ttf|eot|svg|woff|woff2|css|js|map|html|md|scss|txt|webp|json)$"),

        };
        for (Pattern p : cacheablePatterns){
            if (p.matcher(path).matches()){
                return true;
            }
        }
        return false;
    }
    private String getURL(Path path) {
        final StringBuilder requestURL = new StringBuilder(); // Need to check based on target and not request path. Or else there is an infinite loop with forwarding.
        path.getPathElements().forEach(e->{
            requestURL.append("/");
            requestURL.append(e);
        });
        String queryString = path.getRequest() == null ? null : path.getRequest().getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }

    private boolean isCacheable(Path path){
        if (path.getRequest() != null && !path.getRequest().getMethod().equalsIgnoreCase(HttpMethod.GET.toString())){
            return false;
        }

        return isCacheable(getURL(path));
    }
    public void get(Path path,ObjectHolder<View> holder){
        if (!isCacheable(path)){
            return;
        }
        View result = cache.get(getURL(path)).get(path.getReturnProtocol().toString());
        if (result != null){
            Config.instance().getLogger(getClass().getName()).info("{CachedUrl:"+ getURL(path) + ",  ContentType:" + path.getReturnProtocol() + "}");
        }
        holder.set(result);
    }
    public void put(Path path,ObjectHolder<View> holder){
        if (!isCacheable(path)){
            return;
        }
        String mimeType = path.getReturnProtocol().toString();
        View v = holder.get();
        if (v instanceof HtmlView ){
            mimeType = MimeType.TEXT_HTML.toString();
        }else if (v instanceof BytesView){
            mimeType = ((BytesView)v).getContentType();
        }
        cache.get(getURL(path)).put(mimeType,holder.get());
    }
}
