package in.succinct.bpp.shell.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.controller.OidController.OIDProvider;
import com.venky.swf.extensions.SocialLoginInfoExtractor;
import com.venky.swf.integration.JSON;
import com.venky.swf.routing.Config;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.InputStreamReader;
import java.util.Stack;

public class HumBolUserInfoExtractor extends SocialLoginInfoExtractor {
    static {
        Registry.instance().registerExtension(SocialLoginInfoExtractor.class.getName(),new HumBolUserInfoExtractor());
    }
    
    @Override
    public JSONObject extractUserInfo(OIDProvider provider, OAuthResourceResponse resourceResponse) {
        JSONObject userInfo = (JSONObject) JSONValue.parse(new InputStreamReader(resourceResponse.getBodyAsInputStream()));
        return  extractUserInfo(userInfo);
        //FormatHelper.instance(userInfo).change_key_case(KeyCase.CAMEL, KeyCase.SNAKE);
    }
    
    @SuppressWarnings("all")
    public JSONObject extractUserInfo(JSONObject userInfo) {
        if (userInfo.containsKey("User")){
            userInfo = (JSONObject) userInfo.remove("User");
        }
        cleanUpId(userInfo);
        Config.instance().getLogger(getClass().getName()).info("Found user :\n" +  userInfo);
        JSONArray companies = (JSONArray) userInfo.get("Companies");
        
        JSONObject requiredUserInfo = new JSONObject();
        for (String attr : new String[]{"Name","Email","PhoneNumber","UserEmails","UserPhones","UserRoles"}){
            requiredUserInfo.put(attr,userInfo.get(attr));
        }
        if (companies != null && !companies.isEmpty()) {
            JSONObject company = (JSONObject) companies.get(0);
            if (!ObjectUtil.equals(company.get("VerificationStatus"),"Approved")){
                throw new RuntimeException("Kyc needs to be completed");
            }
            company.remove("Applications"); //Not required to inherit.
            
            requiredUserInfo.put("ProviderId", company.get("SubscriberId"));
            if (ObjectUtil.equals(company.get("NetworkEnvironment"),"production")){
                requiredUserInfo.put("NetworkEnvironment", "production");
            }else {
                requiredUserInfo.put("NetworkEnvironment", "test");
            }
        }
        return requiredUserInfo;
    }
    private void cleanUpId(JSONObject userInfo){
        Stack<JSONObject> s = new Stack<>();
        s.push(userInfo);
        while (!s.isEmpty()){
            JSON e = new JSON(s.pop());
            e.removeAttribute("Id");
            for (String name : e.getElementAttributeNames()){
                s.push(e.getElementAttribute(name));
            }
            for (String name : e.getArrayElementNames()){
                for (JSONObject o : e.getArrayElements(name)){
                    s.push(o);
                }
            }
        }

    }
}
