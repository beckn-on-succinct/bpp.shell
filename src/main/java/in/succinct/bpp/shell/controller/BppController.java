package in.succinct.bpp.shell.controller;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.security.Crypt;
import com.venky.core.string.StringUtil;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.beckn.tasks.BecknApiCall;
import com.venky.swf.plugins.beckn.tasks.BecknTask;
import com.venky.swf.plugins.collab.db.model.CryptoKey;
import com.venky.swf.routing.Config;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import in.succinct.beckn.Acknowledgement;
import in.succinct.beckn.Acknowledgement.Status;
import in.succinct.beckn.Error;
import in.succinct.beckn.Error.Type;
import in.succinct.beckn.Request;
import in.succinct.beckn.Response;
import in.succinct.beckn.Subscriber;
import in.succinct.bpp.shell.extensions.BecknPublicKeyFinder;
import in.succinct.bpp.shell.task.BppActionTask;
import in.succinct.bpp.shell.util.BecknUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

public class BppController extends Controller {
    public BppController(Path path) {
        super(path);
    }

    public void subscribe() {
        Request request = new Request(BecknUtil.getSubscriptionJson());
        String hostName = Config.instance().getHostName();
        TaskManager.instance().executeAsync((Task) () -> {
            Config.instance().setHostName(hostName);
            JSONObject response = new Call<JSONObject>().url(BecknUtil.getRegistryUrl() , "subscribe").method(HttpMethod.POST).input(request.getInner()).inputFormat(InputFormat.JSON).
                    header("Content-Type", MimeType.APPLICATION_JSON.toString()).
                    header("Accept", MimeType.APPLICATION_JSON.toString()).
                    header("Authorization", request.generateAuthorizationHeader(BecknUtil.getSubscriberId(), Objects.requireNonNull(BecknUtil.getSelfKey()).getAlias())).
                    getResponseAsJson();
        }, false);
    }

    @SuppressWarnings("unchecked")
    private BppActionTask createTask(String action, Request request, Map<String,String> headers){
        try {
            return (BppActionTask)BecknUtil.getSubscriber().getTaskClass(action).getConstructor(Request.class,Map.class).newInstance(request,headers);
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }


    private View act(){
        Request request = null;
        try {

            request = new Request(StringUtil.read(getPath().getInputStream()));
            request.getContext().setBppId(BecknUtil.getSubscriberId());
            request.getContext().setBppUri(BecknUtil.getSubscriberUrl());
            request.getContext().setAction(getPath().action());

            String callbackUrl = request.getContext().getBapUri();


            Map<String,String> headers =  new IgnoreCaseMap<>();
            headers.put("Content-Type", MimeType.APPLICATION_JSON.toString());
            headers.put("Accept",MimeType.APPLICATION_JSON.toString());
            if (getPath().getHeader("Authorization") != null) {
                headers.put("Authorization",getPath().getHeader("Authorization"));
            }
            if (getPath().getHeader("Proxy-Authorization") != null) {
                headers.put("Proxy-Authorization",getPath().getHeader("Proxy-Authorization"));
            }
            if (getPath().getHeader("X-Gateway-Authorization") != null) {
                headers.put("X-Gateway-Authorization",getPath().getHeader("X-Gateway-Authorization"));
            }
            Config.instance().getLogger(getClass().getName()).log(Level.INFO,request.toString());

            BecknApiCall.build().schema(BecknUtil.getSchemaFile()).url(getPath().getOriginalRequestUrl()).path("/"+getPath().action()).headers(headers).request(request).validateRequest();

            BppActionTask task = createTask(getPath().action(),request,headers);

            Set<String> importantActions = new HashSet<String>(){{
                add("init");
                add("confirm");
                add("cancel");
                add("update");
            }};

            if (isRequestAuthenticated(task,request)){
                boolean persistentTask = importantActions.contains(getPath().action());

                TaskManager.instance().executeAsync(task, false);
                return ack(request);
            }else {
                return nack(request,new AccessDeniedException(),request.getContext().getBapId());
            }
        }catch (Exception ex){
            if (request == null){
                throw new RuntimeException(ex);
            }
            Request response  = new Request();
            Error error = new Error();
            response.setContext(request.getContext());
            response.setError(error);
            error.setCode(ex.getMessage());
            error.setMessage(ex.getMessage());

            BecknUtil.log("FromNetwork",request,getPath().getHeaders(),response,getPath().getOriginalRequestUrl());
            return new BytesView(getPath(),response.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    @RequireLogin(false)
    public View search() {
        return act();
    }
    @RequireLogin(false)
    public View select(){
        return act();
    }
    @RequireLogin(false)
    public View cancel(){
        return act();
    }

    @RequireLogin(false)
    public View init(){
        return act();
    }
    @RequireLogin(false)
    public View confirm(){
        return act();
    }

    @RequireLogin(false)
    public View status(){
        return act();
    }

    @RequireLogin(false)
    public View update(){
        return act();
    }


    @RequireLogin(false)
    public View track(){
        return act();
    }

    @RequireLogin(false)
    public View rating(){
        return act();
    }

    @RequireLogin(false)
    public View support(){
        return act();
    }

    @RequireLogin(false)
    public View get_cancellation_reasons(){
        return act();
    }

    @RequireLogin(false)
    public View get_return_reasons(){
        return act();
    }

    @RequireLogin(false)
    public View get_rating_categories(){
        return act();
    }
    @RequireLogin(false)
    public View get_feedback_categories(){
        return act();
    }


    /**/
    public Response nack(Request request, Throwable th){
        Response response = new Response(new Acknowledgement(Status.NACK));
        if (th != null){
            Error error = new Error();
            response.setError(error);
            error.setMessage(th.toString());
            error.setType( th.getClass().getName().startsWith("org.openapi4j") ?  Type.JSON_SCHEMA_ERROR: Type.DOMAIN_ERROR);
            error.setCode(th.toString());
        }
        //BecknUtil.log("FromNetwork",request,getPath().getHeaders(),response,getPath().getOriginalRequestUrl());
        return response;
    }

    public View nack(Request request, Throwable th, String realm){

        Response response = nack(request,th);

        return new BytesView(getPath(),
                response.getInner().toString().getBytes(StandardCharsets.UTF_8),
                MimeType.APPLICATION_JSON,"WWW-Authenticate","Signature realm=\""+realm+"\"",
                "headers=\"(created) (expires) digest\""){
            @Override
            public void write() throws IOException {
                if (th instanceof AccessDeniedException){
                    super.write(HttpServletResponse.SC_UNAUTHORIZED);
                }else {
                    super.write(HttpServletResponse.SC_BAD_REQUEST);
                }
                //TODO wanto return 200 in 1.0
            }
        };
    }
    public View ack(Request request){
        Response response = new Response(new Acknowledgement(Status.ACK));

        //BecknUtil.log("FromNetwork",request,getPath().getHeaders(),response,getPath().getOriginalRequestUrl());

        return new BytesView(getPath(),response.getInner().toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
    }

    protected boolean isRequestAuthenticated(BecknTask task, Request request){
        if ( Config.instance().getBooleanProperty("beckn.auth.enabled", false)) {
            if (getPath().getHeader("X-Gateway-Authorization") != null) {
                task.registerSignatureHeaders("X-Gateway-Authorization");
            }
            if (getPath().getHeader("Proxy-Authorization") != null){
                task.registerSignatureHeaders("Proxy-Authorization");
            }
            if (getPath().getHeader("Authorization") != null) {
                task.registerSignatureHeaders("Authorization");
            }
            return task.verifySignatures(false);
        }else {
            return true;
        }
    }

    @RequireLogin(value = false)
    public View on_subscribe() throws Exception{
        String payload = StringUtil.read(getPath().getInputStream());
        JSONObject object = (JSONObject) JSONValue.parse(payload);

        JSONObject lookupJSON = new JSONObject();
        lookupJSON.put("subscriber_id",Config.instance().getProperty("in.succinct.bpp.shell.registry.id"));
        lookupJSON.put("domain",Config.instance().getProperty("in.succinct.bpp.domain"));
        lookupJSON.put("type", Subscriber.SUBSCRIBER_TYPE_LOCAL_REGISTRY);
        JSONArray array = BecknPublicKeyFinder.lookup(lookupJSON);
        String signingPublicKey = null;
        String encrPublicKey = null;
        if (array.size() == 1){
            JSONObject registrySubscription = ((JSONObject)array.get(0));
            signingPublicKey = (String)registrySubscription.get("signing_public_key");
            encrPublicKey = (String)registrySubscription.get("encr_public_key");
        }
        if (signingPublicKey == null || encrPublicKey == null){
            throw new RuntimeException("Cannot verify Signature, Could not find registry keys for " + lookupJSON);
        }


        if (!Request.verifySignature(getPath().getHeader("Signature"), payload, signingPublicKey)){
            throw new RuntimeException("Cannot verify Signature");
        }

        PrivateKey privateKey = Crypt.getInstance().getPrivateKey(Request.ENCRYPTION_ALGO,
                CryptoKey.find(BecknUtil.getCryptoKeyId(),CryptoKey.PURPOSE_ENCRYPTION).getPrivateKey());

        PublicKey registryPublicKey = Request.getEncryptionPublicKey(encrPublicKey);

        KeyAgreement agreement = KeyAgreement.getInstance(Request.ENCRYPTION_ALGO);
        agreement.init(privateKey);
        agreement.doPhase(registryPublicKey,true);

        SecretKey key = agreement.generateSecret("TlsPremasterSecret");

        JSONObject output = new JSONObject();
        output.put("answer", Crypt.getInstance().decrypt((String)object.get("challenge"),"AES",key));

        return new BytesView(getPath(),output.toString().getBytes(),MimeType.APPLICATION_JSON);
    }

}
