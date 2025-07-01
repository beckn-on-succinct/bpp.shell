package in.succinct.bpp.shell.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class OidUserPreInit implements Extension {
    static {
        Registry.instance().registerExtension( "oid.user.pre.init",new OidUserPreInit());
    }
    
    @Override
    public void invoke(Object... context) {
        JSONObject userInfo = (JSONObject) context[0];
        JSONArray companies = (JSONArray) userInfo.get("Companies");
        if (companies != null && !companies.isEmpty()) {
            JSONObject company = (JSONObject) companies.get(0);
            if (!ObjectUtil.equals(company.get("VerificationStatus"),"Approved")){
                throw new RuntimeException("Kyc needs to be completed");
            }
            userInfo.put("ProviderId", company.get("SubscriberId")); //This is the only channel user can see.
            if (ObjectUtil.equals(company.get("NetworkEnvironment"),"production")){
                userInfo.put("NetworkEnvironment", "production");
            }else {
                userInfo.put("NetworkEnvironment", "test");
            }
        }
    }
}
