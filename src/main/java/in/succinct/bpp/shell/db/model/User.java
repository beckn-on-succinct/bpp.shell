package in.succinct.bpp.shell.db.model;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;

public interface User extends com.venky.swf.plugins.collab.db.model.user.User {
    @IS_NULLABLE
    @UNIQUE_KEY(value = "PROVIDER_ID", allowMultipleRecordsWithNull = true)
    String getProviderId();
    void setProviderId(String providerId);
    
    public static User findProvider(String providerId){
        User user  = Database.getTable(User.class).newRecord();
        user.setProviderId(providerId);
        user = Database.getTable(User.class).getRefreshed(user);
        return user;
    }
    
    String getCredentialJson();
    void setCredentialJson(String credentialJson);
    
}
