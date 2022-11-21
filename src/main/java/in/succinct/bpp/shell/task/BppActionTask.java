package in.succinct.bpp.shell.task;

import com.venky.swf.plugins.beckn.messaging.Subscriber;
import com.venky.swf.plugins.beckn.tasks.BecknApiCall;
import com.venky.swf.plugins.beckn.tasks.BppTask;
import in.succinct.beckn.Request;
import in.succinct.bpp.shell.controller.PushData;
import in.succinct.bpp.shell.util.BecknUtil;

import java.util.Map;

import static in.succinct.bpp.shell.util.BecknUtil.log;

public class BppActionTask extends BppTask {

    public BppActionTask(){

    }
    public BppActionTask(Request request, Map<String, String> headers) {
        super(request, headers);
    }

    @Override
    public Subscriber getSubscriber() {
        return super.getSubscriber() == null ? BecknUtil.getSubscriber() : super.getSubscriber();
    }


    @Override
    public Request generateCallBackRequest() {

        PushData data = new PushData(getRequest(),getHeaders());
        data.push();
        Request request = getRequest();
        String action = getRequest().getContext().getAction();

        Request callbackRequest =  data.getResponse();
        if (callbackRequest != null) {
            log("ToApplication",request,getHeaders(),callbackRequest,"/" + request.getContext().getAction());
        }
        return callbackRequest;
    }

    @Override
    protected BecknApiCall send(Request callbackRequest) {
        return send(callbackRequest,BecknUtil.getSchemaFile());
    }
    protected BecknApiCall send(Request callbackRequest,String schemaSource){
        BecknApiCall apiCall = super.send(callbackRequest.getContext().getBapUri() , callbackRequest,schemaSource);
        log("ToNetwork",callbackRequest,apiCall.getHeaders(),apiCall.getResponse(),apiCall.getUrl());
        return apiCall;
    }

    @Override
    protected void sendError(Throwable th) {
        super.sendError(th,BecknUtil.getSchemaFile());
    }

}
