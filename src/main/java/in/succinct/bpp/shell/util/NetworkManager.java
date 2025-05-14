package in.succinct.bpp.shell.util;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import com.venky.swf.plugins.beckn.tasks.BecknTask;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.beckn.Request;

import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.CommerceAdaptorFactory;
import in.succinct.bpp.core.db.model.ProviderConfig;
import in.succinct.bpp.core.tasks.BppActionTask;
import in.succinct.onet.core.adaptor.NetworkAdaptor;
import in.succinct.onet.core.adaptor.NetworkAdaptor.Domain;
import in.succinct.onet.core.adaptor.NetworkAdaptor.DomainCategory;
import in.succinct.onet.core.adaptor.NetworkAdaptorFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import javax.xml.crypto.Data;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class NetworkManager {
    private static volatile NetworkManager sSoleInstance;
    
    //private constructor.
    private NetworkManager() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }
    
    public static NetworkManager getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (NetworkManager.class) {
                if (sSoleInstance == null) sSoleInstance = new NetworkManager();
            }
        }
        
        return sSoleInstance;
    }
    
    //Make singleton from serialize and deserialize operation.
    protected NetworkManager readResolve() {
        return getInstance();
    }
    
    public String getNetworkId(){
        return Config.instance().getProperty("in.succinct.onet.name","beckn_open");
    }
    
    public void subscribe(String role){
        TaskManager.instance().executeAsync((Task) () ->
                getNetworkAdaptor().
                        subscribe(getSubscriber(role)),false);
    }
    public NetworkAdaptor getNetworkAdaptor(){
        return NetworkAdaptorFactory.getInstance().getAdaptor(getNetworkId());
    }
    
    public String getLatestKeyId(){
        String selfKeyId;
        CryptoKey latestSigning = getLatestKey(Request.SIGNATURE_ALGO);
        if (latestSigning != null){
            selfKeyId = latestSigning.getAlias();
        }else {
            selfKeyId = "%s.k1".formatted(Config.instance().getHostName());
        }
        return selfKeyId;
    }
    public String getSubscriberId(){
        return Config.instance().getHostName();
    }
    public String getSubscriberUrl(String role){
        String relativeUrl = ObjectUtil.equals(role,Subscriber.SUBSCRIBER_TYPE_BAP) ? "bap" :
                ObjectUtil.equals(role,Subscriber.SUBSCRIBER_TYPE_LOCAL_REGISTRY)? "subscribers":
                        ObjectUtil.equals(role,Subscriber.SUBSCRIBER_TYPE_BPP)? "bpp" : "bg";

        return String.format("%s/%s",
                Config.instance().getServerBaseUrl(),
                relativeUrl);
    }
    
    public Subscriber getSubscriber(String role){
        String selfKeyId = getLatestKeyId();
        DomainCategory category = getDomainCategory();
        return new Subscriber(){
            {
                setSubscriberId(Config.instance().getHostName());
                setAppId(getSubscriberId());
                setPubKeyId(selfKeyId);
                setUniqueKeyId(selfKeyId);
                setNonce(Base64.getEncoder().encodeToString(String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)));
                setSubscriberUrl(NetworkManager.getInstance().getSubscriberUrl(role));
                setType(Subscriber.SUBSCRIBER_TYPE_BPP);
                Domains domains = new Domains();
                for (Domain domain : getNetworkAdaptor().getDomains()) {
                    if (domain.getDomainCategory() == category){
                        domains.add(domain.getId());
                    }
                }
                setDomains(domains);
                getNetworkAdaptor().getSubscriptionJson(this);
            }
            
            @SuppressWarnings("unchecked")
            @Override
            public <T extends BecknTask> Class<T> getTaskClass(String action) {
                return (Class<T>)BppActionTask.class;
            }
        };
    }
    
    public URL getSchemaURL(String domain) {
        return getInstance().getNetworkAdaptor().getDomains().get(domain).getSchemaURL();
        //return "/config/schema.yaml";
    }
    
    ProviderConfig providerConfig = null;
    public DomainCategory getDomainCategory(){
        if (providerConfig == null) {
            providerConfig = new ProviderConfig(getAdaptorConfig().get(getAdapterKey() + ".provider.config"));
        }
        return providerConfig.getDomainCategory();
    }
    
    
    public CryptoKey getLatestKey(String purpose){
        List<CryptoKey> latest = new Select().from(CryptoKey.class).
                where(new Expression(ModelReflector.instance(CryptoKey.class).getPool(), Conjunction.AND).
                        add(new Expression(ModelReflector.instance(CryptoKey.class).getPool(), "PURPOSE",Operator.EQ, purpose)).
                        add(new Expression(ModelReflector.instance(CryptoKey.class).getPool(), "ALIAS",Operator.LK, Config.instance().getHostName() + "%"))).
                
                orderBy("ID DESC").execute(1);
        if (!latest.isEmpty()) {
            return latest.get(0);
        }
        return null;
    }
    public CommerceAdaptor getCommerceAdaptor(){
        return CommerceAdaptorFactory.getInstance().createAdaptor(getAdaptorConfig(),getSubscriber("BPP"));
    }
    public String getAdapterKey(){
        return "in.succinct.bpp." + getAdaptorName() ;
    }
    public String getAdaptorName(){
        List<String> names = Config.instance().getPropertyValueList("in.succinct.bpp.shell.adaptor");
        String name = new StringTokenizer(Config.instance().getHostName(),".").nextToken();
        if (names.size() == 1){
            name = names.get(0);
        }
        return name;
    }
    public  Map<String,String> getAdaptorConfig(){
        String adaptorKey = getAdapterKey();
        String config = Config.instance().getProperty(adaptorKey);
        JSONObject adaptorJSON;
        try {
            if (config != null) {
                adaptorJSON  = (JSONObject) JSONValue.parseWithException(config);
            }else {
                adaptorJSON  = new JSONObject();
            }
        }catch(ParseException ex){
            throw new RuntimeException(ex);
        }
        
        Map<String,String> properties = new HashMap<>();
        String adaptorName = getAdaptorName();
        for (Object o : adaptorJSON.keySet()) {
            String k = (String)o;
            properties.put(String.format("in.succinct.bpp.%s.%s",adaptorName,k), StringUtil.valueOf(adaptorJSON.get(k)));
        }
        
        List<String> keys = Config.instance().getPropertyKeys(adaptorKey+".*");
        
        for (String k : keys){
            properties.put(k,Config.instance().getProperty(k));
        }
        return properties;
    }
}
