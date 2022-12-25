package in.succinct.bpp.shell.util;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.CommerceAdaptorFactory;
import in.succinct.bpp.core.registry.BecknRegistry;
import in.succinct.bpp.core.tasks.BppActionTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BecknUtil {
    public static String getSubscriberId(){
        return Config.instance().getProperty("in.succinct.bpp.shell.subscriber.id",Config.instance().getHostName());
    }

    public static String getCryptoKeyId(){
        List<CryptoKey> latest = new Select().from(CryptoKey.class).
                where(new Expression(ModelReflector.instance(CryptoKey.class).getPool(),"PURPOSE",
                        Operator.EQ ,CryptoKey.PURPOSE_SIGNING)).
                orderBy("ID DESC").execute(1);
        if (latest.isEmpty()){
            return BecknUtil.getSubscriberId() + ".k0" ;
        }else {
            return latest.get(0).getAlias();
        }
    }


    public static String getRegistryUrl(){
        String url = Config.instance().getProperty("in.succinct.bpp.shell.registry.url");
        if (ObjectUtil.isVoid(url)){
            throw new RuntimeException("\"in.succinct.bpp.shell.registry.url\" not set.");
        }
        return url;
    }
    public static String getSubscriberUrl(){
        return String.format("%s/bpp",Config.instance().getServerBaseUrl());
    }

    public static String getNetworkRole() {
        return in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BPP;
    }
    public static String getCountry(){
        return getCountry(3);
    }
    public static String getCountry(int numChars) {
        return Config.instance().getProperty(String.format("in.succinct.bpp.shell.country.iso.%d",numChars) , numChars == 2 ? "IN" : "IND");
    }
    public static String getCountryName(){
        return Config.instance().getProperty("in.succinct.bpp.shell.country.name",getCountry(3));
    }

    public static String getWildCardCharacter() {
        return Config.instance().getProperty("in.succinct.bpp.shell.registry.wild.card.character" , "");
    }

    public static String getCity() {
        return Config.instance().getProperty("in.succinct.bpp.shell.city" , getWildCardCharacter()); //Default all cities. ondc needs * some other networks don't expect the attr to be passed.
    }

    public static String getNicCode() {
        return getDomain();
    }

    public static String getDomain(){
        return Config.instance().getProperty("in.succinct.bpp.domain","nic2004:52110"); // Retail!!
    }


    public static String getSchemaFile() {
        return "/config/schema.yaml";
    }

    public static BecknRegistry getRegistry(){
        return registry;
    }
    public static Subscriber getSubscriber(){
        return bSubscriber;
    }
    public static void subscribe(){
        getRegistry().subscribe(getSubscriber());
    }
    private static BecknRegistry registry = new BecknRegistry(getRegistryUrl(),getSchemaFile()){
        public boolean isSelfRegistrationSupported() {
            return Config.instance().getBooleanProperty("in.succinct.bpp.shell.registry.auto.register");
        }
    };
    private static Subscriber bSubscriber = new Subscriber(){
        {
            setSubscriberId(BecknUtil.getSubscriberId());
            setUniqueKeyId(BecknUtil.getCryptoKeyId());
            setCity(BecknUtil.getCity());
            setType(BecknUtil.getNetworkRole());
            setCountry(BecknUtil.getCountry());
            setDomain(BecknUtil.getDomain());
            setSubscriberUrl(BecknUtil.getSubscriberUrl());
            registry.getSubscriptionJson(this);
        }
        @Override
        public Class<BppActionTask> getTaskClass(String action) {
            return BppActionTask.class;
        }
    };


    public static CommerceAdaptor getCommerceAdaptor(){
        return CommerceAdaptorFactory.getInstance().createAdaptor(getAdaptorConfig(),BecknUtil.getSubscriber(),BecknUtil.getRegistry());
    }

    public static Map<String,String> getAdaptorConfig(){
        List<String> keys = Config.instance().getPropertyKeys("in.succinct.bpp." + Config.instance().getProperty("in.succinct.bpp.shell.adaptor")+".*");
        Map<String,String> properties = new HashMap<>();
        for (String k : keys){
            properties.put(k,Config.instance().getProperty(k));
        }
        return properties;
    }
}
