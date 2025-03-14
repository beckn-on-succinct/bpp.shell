package in.succinct.bpp.shell.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import in.succinct.bpp.core.adaptor.api.NetworkApiAdaptor;
import in.succinct.onet.core.adaptor.NetworkAdaptor;


public class ApiAdaptorCreator implements Extension {
    static {
        Registry.instance().registerExtension(NetworkApiAdaptor.class.getName(),new ApiAdaptorCreator());
    }
    @Override
    public void invoke(Object... objects) {
        NetworkAdaptor adaptor= (NetworkAdaptor) objects[0];
        @SuppressWarnings("unchecked")
        ObjectHolder<NetworkApiAdaptor> h = (ObjectHolder<NetworkApiAdaptor>) objects[1];
        NetworkApiAdaptor networkApiAdaptor = new NetworkApiAdaptor(adaptor){};
        h.set(networkApiAdaptor);
    }
}
