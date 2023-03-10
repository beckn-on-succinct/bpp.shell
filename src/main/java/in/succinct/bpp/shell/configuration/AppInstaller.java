package in.succinct.bpp.shell.configuration;

import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.background.core.DbTask;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.sql.Select;
import in.succinct.bpp.shell.util.BecknUtil;

import java.util.List;

public class AppInstaller implements Installer {

    public void install() {
        Database.getInstance().resetIdGeneration();

        installCity();
        TaskManager.instance().executeAsync((DbTask)()->{
            BecknUtil.subscribe();
            Registry.instance().callExtensions( "in.succinct.bpp.search.extension.installer",BecknUtil.getNetworkAdaptor(),BecknUtil.getCommerceAdaptor());
        },false);

    }


    private void installCity() {
        String country2 = BecknUtil.getCountry(2);
        String country3 = BecknUtil.getCountry(3);
        String name = BecknUtil.getCountryName();

        Country country = Database.getTable(Country.class).newRecord();
        country.setIsoCode(country3);
        country = Database.getTable(Country.class).getRefreshed(country);
        if (country.getRawRecord().isNewRecord()) {
            country.setName(name);
            country.setIsoCode2(country2);
            country.save();
        }
        List<State> states = new Select().from(State.class).execute(1);
        State state = states.isEmpty() ? Database.getTable(State.class).newRecord() : states.get(0);
        if (state.getRawRecord().isNewRecord()){
            state.setName("Karnataka");
            state.setCountryId(country.getId());
            state.setCode("KA");
            state.save();
        }

        String code = BecknUtil.getCity();
        if (code != null && !ObjectUtil.equals(code,BecknUtil.getWildCardCharacter())){
            City city = Database.getTable(City.class).newRecord();
            city.setCode(code);
            city = Database.getTable(City.class).getRefreshed(city);
            if (city.getRawRecord().isNewRecord()){
                city.setName(code);
                city.setStateId(state.getId());
                city.save();
            }
        }

    }


}

