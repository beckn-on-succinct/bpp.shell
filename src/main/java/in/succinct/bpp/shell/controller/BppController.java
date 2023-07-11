package in.succinct.bpp.shell.controller;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.security.Crypt;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.db.model.SWFHttpResponse;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.beckn.tasks.BecknApiCall;
import com.venky.swf.plugins.beckn.tasks.BecknTask;
import com.venky.swf.routing.Config;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import in.succinct.beckn.Acknowledgement;
import in.succinct.beckn.Acknowledgement.Status;
import in.succinct.beckn.BecknException;
import in.succinct.beckn.Error;
import in.succinct.beckn.Error.Type;
import in.succinct.beckn.Request;
import in.succinct.beckn.Response;
import in.succinct.beckn.SellerException;
import in.succinct.beckn.SellerException.GenericBusinessError;
import in.succinct.beckn.SellerException.InvalidRequestError;
import in.succinct.beckn.SellerException.InvalidSignature;
import in.succinct.beckn.Subscriber;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.api.NetworkApiAdaptor;
import in.succinct.bpp.core.tasks.BppActionTask;
import in.succinct.bpp.shell.util.BecknUtil;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;

public class BppController extends Controller {
    private CommerceAdaptor adaptor ;
    public BppController(Path path) {
        super(path);
        adaptor = BecknUtil.getCommerceAdaptor();
    }

    public View subscribe() {
        BecknUtil.subscribe();
        return new BytesView(getPath(),"Subscription initiated!".getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
    }

    public View register(){
        BecknUtil.getNetworkAdaptor().register(BecknUtil.getCommerceAdaptor().getSubscriber());
        return new BytesView(getPath(),"Registration initiated!".getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
    }

    @RequireLogin(false)
    public View subscriber_json(){
        return new BytesView(getPath(),BecknUtil.getCommerceAdaptor().getSubscriber().toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
    }

    @RequireLogin(false)
    public View reindex(){
        Registry.instance().callExtensions( "in.succinct.bpp.search.extension.reinstall",BecknUtil.getNetworkAdaptor(),BecknUtil.getCommerceAdaptor());
        return IntegrationAdaptor.instance(SWFHttpResponse.class, JSONObject.class).createStatusResponse(getPath(),null);
    }

    public View sign() throws  Exception{
        String sign = Request.generateSignature(StringUtil.read(getPath().getInputStream()), Request.getPrivateKey(BecknUtil.getSubscriberId(),BecknUtil.getCryptoKeyId()));
        return new BytesView(getPath(),sign.getBytes(StandardCharsets.UTF_8),MimeType.TEXT_PLAIN);
    }

    @SuppressWarnings("unchecked")
    private BppActionTask createTask(String action, Request request, Map<String,String> headers){
        try {
            return (BppActionTask)BecknUtil.getCommerceAdaptor().getSubscriber().getTaskClass(action).
                    getConstructor(NetworkApiAdaptor.class,CommerceAdaptor.class,Request.class,Map.class).newInstance(BecknUtil.getNetworkAdaptor().getApiAdaptor(),adaptor,request,headers);
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }


    private View act(){
        Request request = null;
        try {

            request = new Request(StringUtil.read(getPath().getInputStream()));
            // Go Ahead or reject context based on subsciber!
            request.getContext().setBppId(BecknUtil.getSubscriberId());
            request.getContext().setBppUri(BecknUtil.getSubscriberUrl());
            request.getContext().setAction(getPath().action());

            //String callbackUrl = request.getContext().getBapUri();


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

                in.succinct.beckn.Subscriber bg = getSubscriber(request.extractAuthorizationParams("X-Gateway-Authorization",headers));
                if (bg != null) {
                    request.getExtendedAttributes().set(Request.CALLBACK_URL, bg.getSubscriberUrl());
                }
            }
            Config.instance().getLogger(getClass().getName()).log(Level.INFO,request.toString());

            BecknApiCall.build().schema(BecknUtil.getSchemaURL()).url(getPath().getOriginalRequestUrl()).path("/"+getPath().action()).headers(headers).request(request).validateRequest();

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
                return nack(request,new SellerException.InvalidSignature(),request.getContext().getBapId()); // See if throw. !!
            }
        }catch (Exception ex){
            if (request == null){
                throw new RuntimeException(ex);
            }
            Request response  = new Request();
            Error error = new Error();
            response.setContext(request.getContext());
            response.setError(error);
            if (ex instanceof BecknException) {
                error.setCode(((BecknException)ex).getErrorCode());
            }else{
                error.setCode(new SellerException.GenericBusinessError().getErrorCode());
            }
            error.setMessage(ex.getMessage());

            BecknUtil.getNetworkAdaptor().getApiAdaptor().log("FromNetwork",request,getPath().getHeaders(),response,getPath().getOriginalRequestUrl());
            return new BytesView(getPath(),response.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private Subscriber getSubscriber(Map<String, String> authParams) {
        in.succinct.beckn.Subscriber bg = null;
        if (!authParams.isEmpty()){
            String keyId = authParams.get("keyId");
            StringTokenizer keyTokenizer = new StringTokenizer(keyId,"|");
            String subscriberId = keyTokenizer.nextToken();

            List<Subscriber> subscriber = BecknUtil.getNetworkAdaptor().lookup(subscriberId,true);
            if (!subscriber.isEmpty()){
                bg = subscriber.get(0);
            }
        }
        return bg;

    }

    /* web hook */
    @RequireLogin(false)
    public View hook(){
        JSONObject out = new JSONObject();

        try {
            Registry.instance().callExtensions("in.succinct.bpp.shell."+getPath().action(), adaptor, BecknUtil.getNetworkAdaptor(), getPath());
            out.put("status","OK");
        }catch (RuntimeException e){
            StringWriter w = new StringWriter();
            ExceptionUtil.getRootCause(e).printStackTrace(new PrintWriter(w));
            out.put("status","ERROR");
            out.put("message",w.toString());
        }

        return new BytesView(getPath(),out.toString().getBytes(StandardCharsets.UTF_8),
                MimeType.APPLICATION_JSON){
            @Override
            public void write() throws IOException {
                if (ObjectUtil.equals("OK",out.get("status"))) {
                    super.write(HttpServletResponse.SC_OK);
                }else {
                    super.write(HttpServletResponse.SC_EXPECTATION_FAILED);
                }
            }
        };
    }

    @RequireLogin(false)
    public View issue(){
        return act();
    }

    @RequireLogin(false)
    public View issue_status(){
        return act();
    }

    @RequireLogin(false)
    public View receiver_recon(){
        return act();
    }



    @RequireLogin(false)
    public View order_hook(String event){
        //event accessible via path.parameter()
        return hook();
    }
    @RequireLogin(false)
    public View igm_hook(){
        return hook();
    }

    @RequireLogin(false)
    public View recon_hook(String event){
        //event = on_receiver_recon
        //event accessible via path.parameter()
        return hook();
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
            if (th.getClass().getName().startsWith("org.openapi4j")){
                InvalidRequestError sellerException = new InvalidRequestError();
                error.setType(Type.JSON_SCHEMA_ERROR);
                error.setCode(sellerException.getErrorCode());
                error.setMessage(sellerException.getMessage());
            }else if (th instanceof BecknException){
                BecknException bex = (BecknException) th;
                error.setType(Type.DOMAIN_ERROR);
                error.setCode(bex.getErrorCode());
                error.setMessage(bex.getMessage());
            }else {
                error.setMessage(th.toString());
                error.setCode(new GenericBusinessError().getErrorCode());
                error.setType(Type.DOMAIN_ERROR);
            }
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
                if (th instanceof InvalidSignature){
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

        Subscriber registry = BecknUtil.getNetworkAdaptor().getRegistry();

        if (registry.getEncrPublicKey() == null){
            throw new RuntimeException("Could not find registry keys for " + registry);
        }



        PrivateKey privateKey = Crypt.getInstance().getPrivateKey(Request.ENCRYPTION_ALGO,
                CryptoKey.find(BecknUtil.getCryptoKeyId(),CryptoKey.PURPOSE_ENCRYPTION).getPrivateKey());

        PublicKey registryPublicKey = Request.getEncryptionPublicKey(registry.getEncrPublicKey());

        KeyAgreement agreement = KeyAgreement.getInstance(Request.ENCRYPTION_ALGO);
        agreement.init(privateKey);
        agreement.doPhase(registryPublicKey,true);

        SecretKey key = agreement.generateSecret("TlsPremasterSecret");

        JSONObject output = new JSONObject();
        output.put("answer", Crypt.getInstance().decrypt((String)object.get("challenge"),"AES",key));

        return new BytesView(getPath(),output.toString().getBytes(),MimeType.APPLICATION_JSON);
    }

}
