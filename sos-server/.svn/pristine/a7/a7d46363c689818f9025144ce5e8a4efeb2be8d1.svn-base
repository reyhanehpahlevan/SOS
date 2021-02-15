package rescuecore2.view;

import java.awt.event.MouseEvent;
import java.util.List;


/**
   A listener for view events.
 */
public interface SOSViewListener extends ViewListener{
	void objectsRollover(ViewComponent view, List<RenderedObject> objects,MouseEvent e);
	void objectsClicked(ViewComponent view, List<RenderedObject> objects,MouseEvent e);
    /**
       Notification that a set of objects were clicked.
       @param view The ViewComponent that was clicked.
       @param objects The list of objects that were under the click point.
     */
    void objectsClicked(ViewComponent view, List<RenderedObject> objects);

    /**
       Notification that a set of objects were rolled over.
       @param view The ViewComponent that was rolled over.
       @param objects The list of objects that were under the mouse point.
     */
    void objectsRollover(ViewComponent view, List<RenderedObject> objects);
}
