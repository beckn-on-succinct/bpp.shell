package in.succinct.bpp.shell.controller;

import com.venky.extension.Registry;
import in.succinct.beckn.Request;
import in.succinct.bpp.shell.util.BecknUtil;

import java.util.Map;

public class PushData {
    Request request;
    Map<String, String> headers;
    Request response;

    public PushData(Request request, Map<String, String> headers) {
        this.request = request;
        this.headers = headers;
    }

    public Request getResponse() {
        return response;
    }

    public void push() {
        response = new Request();
        Registry.instance().callExtensions("in.succinct.bpp.shell.extension", headers, request, response);
        BecknUtil.createReplyContext(request,response);
    }
}
