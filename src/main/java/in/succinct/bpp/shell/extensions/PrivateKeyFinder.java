package in.succinct.bpp.shell.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import in.succinct.beckn.Request;
import in.succinct.bpp.shell.util.BecknUtil;

public class PrivateKeyFinder implements Extension {
    static {
        Registry.instance().registerExtension("private.key.get.Ed25519",new PrivateKeyFinder());
    }

    @Override
    public void invoke(Object... context) {
        String privateKey = Request.getPrivateKey(BecknUtil.getSubscriberId(),BecknUtil.getCryptoKeyId());
        ObjectHolder<String> holder = (ObjectHolder<String>) context[0];
        holder.set(String.format("%s|%s:%s",BecknUtil.getSubscriberId(),BecknUtil.getCommerceAdaptor().getSubscriber().getUniqueKeyId(),privateKey));
    }
}
