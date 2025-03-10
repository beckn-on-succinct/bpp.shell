package in.succinct.bpp.shell.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.CryptoKey;

public class BecknPrivateKeyFinder implements Extension {
    static {
        Registry.instance().registerExtension("beckn.private.key.get", new BecknPrivateKeyFinder());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invoke(Object... context) {
        String subscriber_id = (String) context[0];
        String uniqueKeyId = (String) context[1];
        ObjectHolder<String> privateKeyHolder = (ObjectHolder<String>) context[2];
        if (privateKeyHolder.get() != null) {
            return;
        }
        CryptoKey key = CryptoKey.find(uniqueKeyId, CryptoKey.PURPOSE_SIGNING);
        if (!key.getRawRecord().isNewRecord()) {
            privateKeyHolder.set(key.getPrivateKey());
        }
    }
}
