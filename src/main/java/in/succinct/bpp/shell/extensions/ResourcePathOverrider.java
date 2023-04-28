package in.succinct.bpp.shell.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;

import java.util.Arrays;
import java.util.List;

public class ResourcePathOverrider implements Extension {
    static {
        Registry.instance().registerExtension("swf.before.routing",new ResourcePathOverrider());
    }
    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Object... context) {
        List<String> pathElements = (List<String>) context[0];
        if (pathElements.isEmpty()){
            return;
        }

        if (pathElements.size() == 1 && pathElements.get(0).lastIndexOf(".") > 0){
            pathElements.add(0, "resources");
        }

    }
}
