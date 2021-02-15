package sosNamayangar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import rescuecore2.GUIComponent;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.view.AnimatedWorldModelViewer;
import rescuecore2.standard.view.StandardWorldModelViewer;
import rescuecore2.view.EntityInspector;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;

public class SOSWorldModelNamayangar implements GUIComponent{
    private static final int SIZE = 700;

    private StandardWorldModelViewer viewer;
    private EntityInspector inspector;
    private JTextField field;
    private JComponent view;
    private WorldModel<? extends Entity> world;

    /**
       Construct a StandardWorldModelViewerComponent.
    */
    public SOSWorldModelNamayangar(Config config,StandardWorldModel model,String title) {
        viewer = new AnimatedWorldModelViewer();
        viewer.initialise(config);
        world=model;
        
        inspector = new EntityInspector();
        field = new JTextField();
        viewer.setPreferredSize(new Dimension(SIZE, SIZE));
        viewer.addViewListener(new ViewListener() {
                @Override
                public void objectsClicked(ViewComponent v, List<RenderedObject> objects) {
                    for (RenderedObject next : objects) {
                        if (next.getObject() instanceof Entity) {
                            inspector.inspect((Entity)next.getObject());
                            field.setText("");
                            return;
                        }
                    }
                }

                @Override
                public void objectsRollover(ViewComponent v, List<RenderedObject> objects) {
                }
            });
        field.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String s = field.getText();
                    try {
                        int id = Integer.parseInt(s);
                        EntityID eid = new EntityID(id);
                        Entity e = world.getEntity(eid);
                        inspector.inspect(e);
                    }
                    catch (NumberFormatException e) {
                        field.setText("");
                    }
                }
            });
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewer, new JScrollPane(inspector));
        view = new JPanel(new BorderLayout());
        view.add(split, BorderLayout.CENTER);
        view.add(field, BorderLayout.NORTH);
        viewer.view(world);
        JFrame jf=new JFrame(title);
        jf.add(getGUIComponent());
        jf.setSize(SIZE, SIZE);
        jf.setVisible(true);
        split.setDividerLocation(0.8);
    }

    public void timestepCompleted(StandardWorldModel model, ChangeSet changeSet) {
        viewer.view(model, /*time.getCommands(),*/ changeSet);
        viewer.repaint();
    }

    @Override
    public JComponent getGUIComponent() {
        return view;
    }

    @Override
    public String getGUIComponentName() {
        return "SOS World view";
    }

}
