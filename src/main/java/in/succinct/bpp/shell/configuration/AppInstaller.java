package in.succinct.bpp.shell.configuration;

import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;

public class AppInstaller implements Installer {

    public void install() {
        Database.getInstance().resetIdGeneration();

        /*
        bpp.search not needed. Can cache in bg!
        TaskManager.instance().executeAsync((DbTask)()->{
            //NetworkManager.getInstance().subscribe("BPP");
            Registry.instance().callExtensions( "in.succinct.bpp.search.extension.installer", NetworkManager.getInstance().getNetworkAdaptor(),NetworkManager.getInstance().getCommerceAdaptor());
        },false);
        */
    }




}

