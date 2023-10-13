package in.succinct.bpp.shell.extensions;

import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.Event;
import com.venky.swf.plugins.background.core.DbTask;
import com.venky.swf.plugins.background.core.TaskManager;
import in.succinct.beckn.Note.RepresentativeAction;
import in.succinct.bpp.core.db.model.Subscriber;
import in.succinct.bpp.core.db.model.igm.Note;
import in.succinct.bpp.core.db.model.igm.Representative;
import in.succinct.bpp.core.extensions.SuccinctIssueTracker;

public class BeforeValidateNote extends BeforeModelValidateExtension<Note> {
    static {
        registerExtension(new BeforeValidateNote());
    }
    @Override
    public void beforeValidate(Note model) {
        RepresentativeAction action = RepresentativeAction.valueOf(model.getAction());
        Representative loggedByRepresentor = model.getLoggedByRepresentor();
        Subscriber subscriber = loggedByRepresentor.getSubscriber();
        Application application = subscriber.getApplication();

        Event event = Event.find("on_create_issue_note");
        if (event != null && application != null){
            TaskManager.instance().executeAsync((DbTask) () ->
                    event.raise(application, SuccinctIssueTracker.getBecknIssue(model.getIssue())), false);
        }
    }
}
