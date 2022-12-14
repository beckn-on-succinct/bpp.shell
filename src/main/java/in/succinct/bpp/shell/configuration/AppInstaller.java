package in.succinct.bpp.shell.configuration;

import com.venky.swf.configuration.Installer;
import in.succinct.bpp.shell.util.BecknUtil;

public class AppInstaller implements Installer {

    public void install() {
        BecknUtil.subscribe();
    }


}

