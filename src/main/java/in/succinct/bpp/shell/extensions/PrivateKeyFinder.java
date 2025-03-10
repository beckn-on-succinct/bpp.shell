package in.succinct.bpp.shell.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import in.succinct.beckn.Request;
import in.succinct.bpp.shell.util.NetworkManager;

public class PrivateKeyFinder implements Extension {
    static {
        Registry.instance().registerExtension("private.key.get.Ed25519",new PrivateKeyFinder());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invoke(Object... context) {
        String privateKey = Request.getPrivateKey(NetworkManager.getInstance().getSubscriberId(),NetworkManager.getInstance().getLatestKeyId());
        ObjectHolder<String> holder = (ObjectHolder<String>) context[0];
        holder.set(String.format("%s|%s:%s",NetworkManager.getInstance().getSubscriberId(),NetworkManager.getInstance().getCommerceAdaptor().getSubscriber().getUniqueKeyId(),privateKey));
    }
}
