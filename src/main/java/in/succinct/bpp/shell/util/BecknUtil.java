package in.succinct.bpp.shell.util;

import com.venky.core.string.StringUtil;
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

import in.succinct.bpp.core.tasks.BppActionTask;
import in.succinct.onet.core.adaptor.NetworkAdaptor;
import in.succinct.onet.core.adaptor.NetworkAdaptorFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BecknUtil {
    public static String getSubscriberId(){
        return Config.instance().getProperty("in.succinct.bpp.shell.subscriber.id",Config.instance().getHostName());
    }

    public static String getCryptoKeyId(){

        String keyId = Config.instance().getProperty("in.succinct.bpp.shell.subscriber.key.id");

        if (ObjectUtil.isVoid(keyId)) {
            List<CryptoKey> latest = new Select().from(CryptoKey.class).
                    where(new Expression(ModelReflector.instance(CryptoKey.class).getPool(), "PURPOSE",
                            Operator.EQ, CryptoKey.PURPOSE_SIGNING)).
                    orderBy("ID DESC").execute(1);
            if (latest.isEmpty()) {
                keyId = BecknUtil.getSubscriberId() + ".k0";
            } else {
                keyId =latest.get(0).getAlias();
            }
        }
        return keyId;
    }


    public static String getRegistryUrl(){
        return getNetworkAdaptor().getRegistryUrl();
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
        return Config.instance().getProperty("in.succinct.bpp.shell.domain","nic2004:52110"); // Retail!!
    }


    public static URL getSchemaURL() {
        return networkAdaptor.getDomains().get(getCommerceAdaptor().getSubscriber().getDomain()).getSchemaURL();
        //return "/config/schema.yaml";
    }

    public static NetworkAdaptor getNetworkAdaptor(){
        return networkAdaptor;
    }
    public static Subscriber getSubscriber(){
        return bSubscriber;
    }
    public static void subscribe(){
        CommerceAdaptor adaptor = getCommerceAdaptor();
        getNetworkAdaptor().subscribe(adaptor.getSubscriber());
    }
    private static NetworkAdaptor networkAdaptor = NetworkAdaptorFactory.getInstance().getAdaptor(Config.instance().getProperty("in.succinct.bpp.shell.network.name","beckn_open"));
    private static Subscriber bSubscriber = new Subscriber(){
        {
            setAppId(BecknUtil.getSubscriberId());
            setSubscriberId(BecknUtil.getSubscriberId());
            setUniqueKeyId(BecknUtil.getCryptoKeyId());
            setAlias(BecknUtil.getCryptoKeyId());
            setCity(BecknUtil.getCity());
            setType(BecknUtil.getNetworkRole());
            setCountry(BecknUtil.getCountry());
            setDomain(BecknUtil.getDomain());
            setSubscriberUrl(BecknUtil.getSubscriberUrl());
            networkAdaptor.getSubscriptionJson(this);
        }
        @Override
        public Class<BppActionTask> getTaskClass(String action) {
            return BppActionTask.class;
        }
    };


    public static CommerceAdaptor getCommerceAdaptor(){
        return CommerceAdaptorFactory.getInstance().createAdaptor(getAdaptorConfig(),BecknUtil.getSubscriber());
    }

    public static Map<String,String> getAdaptorConfig(){
        String providerConfigKey = "in.succinct.bpp." + Config.instance().getProperty("in.succinct.bpp.shell.adaptor") + ".provider.config";
        String config = Config.instance().getProperty(providerConfigKey);
        JSONObject adaptorJSON = config == null ? new JSONObject() :(JSONObject) JSONValue.parse(config);

        Map<String,String> properties = new HashMap<>();
        for (Object o : adaptorJSON.keySet()) {
            String k = (String)o;
            properties.put(String.format("in.succinct.bpp.%s.%s",Config.instance().getProperty("in.succinct.bpp.shell.adaptor"),k), StringUtil.valueOf(adaptorJSON.get(k)));
        }

        List<String> keys = Config.instance().getPropertyKeys("in.succinct.bpp." + Config.instance().getProperty("in.succinct.bpp.shell.adaptor")+".*");
        keys.remove(providerConfigKey);

        for (String k : keys){
            properties.put(k,Config.instance().getProperty(k));
        }
        return properties;
    }
}
