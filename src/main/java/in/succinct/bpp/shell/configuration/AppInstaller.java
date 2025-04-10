package in.succinct.bpp.shell.configuration;

import com.venky.extension.Registry;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.background.core.DbTask;
import com.venky.swf.plugins.background.core.TaskManager;
import in.succinct.bpp.shell.util.NetworkManager;

public class AppInstaller implements Installer {

    public void install() {
        Database.getInstance().resetIdGeneration();

        TaskManager.instance().executeAsync((DbTask)()->{
            //NetworkManager.getInstance().subscribe("BPP");
            Registry.instance().callExtensions( "in.succinct.bpp.search.extension.installer", NetworkManager.getInstance().getNetworkAdaptor(),NetworkManager.getInstance().getCommerceAdaptor());
        },false);

    }




}

