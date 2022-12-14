package in.succinct.bpp.shell.util;

import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.beckn.messaging.Mq;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import com.venky.swf.plugins.collab.db.model.CryptoKey;
import com.venky.swf.plugins.sequence.db.model.SequentialNumber;
import com.venky.swf.routing.Config;
import in.succinct.beckn.BecknAware;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.Context;
import in.succinct.beckn.Request;

import in.succinct.bpp.shell.controller.BppController;
import in.succinct.bpp.shell.extensions.BecknPublicKeyFinder;
import in.succinct.bpp.shell.task.BppActionTask;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

public class BecknUtil {
    public static String getSubscriberId(){
        return Config.instance().getProperty("in.succinct.bpp.shell.subscriber.id",Config.instance().getHostName());
    }



    public static String getCryptoKeyId(){
        return BecknUtil.getSubscriberId() + ".k" + BecknUtil.getCurrentKeyNumber();
    }


    public static CryptoKey getSelfEncryptionKey(){
        CryptoKey encryptionKey = CryptoKey.find(getCryptoKeyId(),CryptoKey.PURPOSE_ENCRYPTION);
        if (encryptionKey.getRawRecord().isNewRecord()){
            return null;
        }
        return encryptionKey;
    }
    public static CryptoKey getSelfKey(){
        CryptoKey key = CryptoKey.find(getCryptoKeyId() ,CryptoKey.PURPOSE_SIGNING);
        if (key.getRawRecord().isNewRecord()){
            return null;
        }
        return key;
    }

    public static long  getCurrentKeyNumber(){
        String sKeyNumber =  SequentialNumber.get("KEYS").getCurrentNumber();
        return Long.parseLong(sKeyNumber);
    }

    public static long getNextKeyNumber(){
        String sKeyNumber = SequentialNumber.get("KEYS").next();
        return Long.parseLong(sKeyNumber);
    }

    public static String getRegistryUrl(){
        String url = Config.instance().getProperty("in.succinct.bpp.shell.registry.url");
        if (ObjectUtil.isVoid(url)){
            throw new RuntimeException("\"in.succinct.bpp.shell.registry.url\" not set.");
        }
        return url;
    }

    public static long getKeyValidityMillis(){
        return Config.instance().getLongProperty("in.succinct.bpp.shell.key.validity.days",(long) (10L * 365.25D)) * 24L * 60L * 60L * 1000L;
    }

    public static void rotateKeys() {
        CryptoKey existingKey = CryptoKey.find(BecknUtil.getCryptoKeyId(), CryptoKey.PURPOSE_SIGNING);
        if (existingKey.getRawRecord().isNewRecord() || existingKey.getUpdatedAt().getTime() + getKeyValidityMillis() <= System.currentTimeMillis() ){
            KeyPair signPair = Crypt.getInstance().generateKeyPair(Request.SIGNATURE_ALGO, Request.SIGNATURE_ALGO_KEY_LENGTH);
            KeyPair encPair = Crypt.getInstance().generateKeyPair(Request.ENCRYPTION_ALGO, Request.ENCRYPTION_ALGO_KEY_LENGTH);


            BecknUtil.getNextKeyNumber(); //Just increment.
            CryptoKey signKey = CryptoKey.find(BecknUtil.getCryptoKeyId(), CryptoKey.PURPOSE_SIGNING); //Create new key
            signKey.setAlgorithm(Request.SIGNATURE_ALGO);
            signKey.setPrivateKey(Crypt.getInstance().getBase64Encoded(signPair.getPrivate()));
            signKey.setPublicKey(Crypt.getInstance().getBase64Encoded(signPair.getPublic()));
            signKey.save();

            CryptoKey encryptionKey = CryptoKey.find(BecknUtil.getCryptoKeyId(), CryptoKey.PURPOSE_ENCRYPTION);
            encryptionKey.setAlgorithm(Request.ENCRYPTION_ALGO);
            encryptionKey.setPrivateKey(Crypt.getInstance().getBase64Encoded(encPair.getPrivate()));
            encryptionKey.setPublicKey(Crypt.getInstance().getBase64Encoded(encPair.getPublic()));
            encryptionKey.save();
        }

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

    public static JSONObject getSubscriptionJson() {
        BecknUtil.rotateKeys();
        CryptoKey skey = BecknUtil.getSelfKey();
        CryptoKey ekey = BecknUtil.getSelfEncryptionKey();

        long validFrom = skey.getUpdatedAt().getTime();
        long validTo = (validFrom + BecknUtil.getKeyValidityMillis());

        JSONObject object = new JSONObject();
        object.put("subscriber_id", BecknUtil.getSubscriberId());
        object.put("subscriber_url", BecknUtil.getSubscriberUrl());
        object.put("type",BecknUtil.getNetworkRole());

        object.put("domain", BecknUtil.getNicCode());
        object.put("signing_public_key", Request.getRawSigningKey(skey.getPublicKey()));
        object.put("encr_public_key", Request.getRawEncryptionKey(ekey.getPublicKey()));
        object.put("valid_from", BecknObject.TIMESTAMP_FORMAT.format(new Date(validFrom)));
        object.put("valid_until", BecknObject.TIMESTAMP_FORMAT.format(new Date(validTo)));
        object.put("country", BecknUtil.getCountry());
        if (!ObjectUtil.isVoid(BecknUtil.getCity())){
            object.put("city", BecknUtil.getCity());
        }
        object.put("created",BecknObject.TIMESTAMP_FORMAT.format(skey.getCreatedAt()));
        object.put("updated",BecknObject.TIMESTAMP_FORMAT.format(skey.getUpdatedAt()));


        object.put("unique_key_id", skey.getAlias());
        object.put("pub_key_id", skey.getAlias());
        object.put("nonce", Base64.getEncoder().encodeToString(String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)));


        return object;
    }

    public static String getSchemaFile() {
        return "/config/schema.yaml";
    }

    /*
    public static String getDestinationUrl() {
        return Config.instance().getProperty("in.succinct.bpp.shell.destination.url" );
    }*/

    public static void createReplyContext(Request from , Request to){
        Context newContext = ObjectUtil.clone(from.getContext());
        String  action = from.getContext().getAction();
        newContext.setAction(action.startsWith("get_") ? action.substring(4) : "on_" + action);
        newContext.setBppId(BecknUtil.getSubscriberId());
        newContext.setBppUri(BecknUtil.getSubscriberUrl());
        to.setContext(newContext);
    }

    public static void log(String direction,
                           Request request, Map<String, String> headers, BecknAware response,
                           String url) {
        Map<String,String> maskedHeaders = new HashMap<>();
        headers.forEach((k,v)->{
            maskedHeaders.put(k,Config.instance().isDevelopmentEnvironment()? v : "***");
        });
        Config.instance().getLogger(BecknUtil.class.getName()).log(Level.INFO,String.format("%s|%s|%s|%s|%s",direction,request,headers,response,url));

    }

    public static Subscriber getSubscriber(){
        return subscriber;
    }
    private static final Subscriber subscriber = new Subscriber() {
        @Override
        public String getSubscriberUrl() {
            return BecknUtil.getSubscriberUrl();
        }

        @Override
        public String getSubscriberId() {
            return BecknUtil.getSubscriberId();
        }

        @Override
        public String getPubKeyId() {
            return BecknUtil.getSelfKey().getAlias();
        }

        @Override
        public String getDomain() {
            return BecknUtil.getDomain();
        }

        @Override
        public Mq getMq() {
            return null;
        }

        @Override
        public Set<String> getSupportedActions() {
            return in.succinct.beckn.Subscriber.BPP_ACTION_SET;
        }

        @Override
        public Class<BppActionTask> getTaskClass(String action) {
            return BppActionTask.class;
        }
    };

    public static void subscribe() {
        BecknUtil.rotateKeys();
        Request request = new Request(BecknUtil.getSubscriptionJson());
        String hostName = Config.instance().getHostName();
        TaskManager.instance().executeAsync((Task) () -> {
            Config.instance().setHostName(hostName);
            JSONArray registered_subscribers = BecknPublicKeyFinder.lookup(BecknUtil.getSubscriberId());

            List<String> apis = new ArrayList<>();
            if (Config.instance().getBooleanProperty("in.succinct.bpp.shell.registry.auto.register")
                    && registered_subscribers.isEmpty()){
                apis.add("register");
            }
            apis.add("subscribe");

            for (String api : apis){
                Call<JSONObject> call = new Call<JSONObject>().url(BecknUtil.getRegistryUrl() , api).method(HttpMethod.POST).input(request.getInner()).inputFormat(InputFormat.JSON).
                        header("Content-Type", MimeType.APPLICATION_JSON.toString()).
                        header("Accept", MimeType.APPLICATION_JSON.toString());

                if (api.equals("subscribe")){
                    call.header("Authorization", request.generateAuthorizationHeader(BecknUtil.getSubscriberId(), Objects.requireNonNull(BecknUtil.getSelfKey()).getAlias()));
                }
                JSONObject response = call.getResponseAsJson();
                Config.instance().getLogger(BecknUtil.class.getName()).info(api + "-" + response.toString());
            }


        }, false);
    }
}
