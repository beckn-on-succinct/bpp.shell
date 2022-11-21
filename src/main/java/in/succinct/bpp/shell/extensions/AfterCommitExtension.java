package in.succinct.bpp.shell.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.plugins.background.core.agent.Agent;
import com.venky.swf.plugins.background.core.agent.PersistedTaskPollingAgent;

public class AfterCommitExtension implements Extension {
    static {
        Registry.instance().registerExtension("after.commit",new AfterCommitExtension());
    }

    @Override
    public void invoke(Object... context) {
        Agent.instance().start(PersistedTaskPollingAgent.PERSISTED_TASK_POLLER);
    }
}
