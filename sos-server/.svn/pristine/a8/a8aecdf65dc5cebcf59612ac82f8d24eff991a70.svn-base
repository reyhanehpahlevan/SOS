package sosNamayangar;

import javax.swing.JSlider;
import javax.swing.Timer;

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.view.AnimatedHumanLayer;
import rescuecore2.standard.view.AreaNeighboursLayer;
import rescuecore2.standard.view.BuildingLayer;
import rescuecore2.standard.view.HumanLayer;
import rescuecore2.standard.view.PositionHistoryLayer;
import rescuecore2.standard.view.RoadBlockageLayer;
import rescuecore2.standard.view.RoadLayer;
import rescuecore2.standard.view.StandardWorldModelViewer;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewLayer;
import sosNamayangar.layers.AllHearSayCommand;
import sosNamayangar.layers.AllHumanSayLayer;
import sosNamayangar.layers.HumanDeathTimeLayer;
import sosNamayangar.layers.LineOfSight;
import sosNamayangar.layers.PreciptionLayer;
import sosNamayangar.layers.SOSCommandLayer;
import sosNamayangar.layers.SOSAnimatedHumanActualSizeLayer;
import sosNamayangar.layers.SaySelectedLayer;
import sosNamayangar.layers.SenseCivilian;
import sosNamayangar.layers.SenseSelectedLaye;
import sosNamayangar.layers.ShowSelectedLayer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;

/**
 * A viewer for StandardWorldModels.
 */
public class SOSWorldModelViewer extends StandardWorldModelViewer {
	private static final long serialVersionUID = 1L;
	private static final int FRAME_COUNT = 10;
	private static final int ANIMATION_TIME = 750;
	private static final int FRAME_DELAY = ANIMATION_TIME / FRAME_COUNT;

	private boolean done;
	//	public SaySelectedLayer sayLayer;
	//	public SenseSelectedLaye senseLayer;
	public SOSCommandLayer soscommandsLayer;

	/**
	 * Construct an animated world model viewer.
	 */
	public SOSWorldModelViewer() {
		super();
	}

	@Override
	protected Collection<RenderedObject> render(Graphics2D g, ScreenTransform transform, int width, int height) {
		Collection<RenderedObject> result = new HashSet<RenderedObject>();
        prepaint();
        for (ViewLayer next : getLayers()) {
            if (next.isVisible()/*&&!(next instanceof AnimatedHumanLayer||next instanceof SOSAnimatedHumanActualSizeLayer)*/) {
                Graphics2D copy = (Graphics2D)g.create();
                result.addAll(next.render(copy, transform, width, height));
            }
        }
//        for (ViewLayer next : getLayers()) {
//        	if (next.isVisible()&&(next instanceof AnimatedHumanLayer||next instanceof SOSAnimatedHumanActualSizeLayer)) {
//        		Graphics2D copy = (Graphics2D)g.create();
//        		result.addAll(next.render(copy, transform, width, height));
//        	}
//        }
        postpaint();
        return result;
	}
	@Override
    public void paintComponent(Graphics g) {
		try{
        super.paintComponent(g);
		}catch (Exception e) {
			e.printStackTrace();
		}

    }

	@Override
	public String getViewerName() {
		return "Animated world model viewer";
	}

	@Override
	public void addDefaultLayers() {
		addLayer(new BuildingLayer());
		addLayer(new RoadLayer());
		AreaNeighboursLayer anl = new AreaNeighboursLayer();
		anl.setVisible(false);
		addLayer(anl);
		addLayer(new RoadBlockageLayer());
		//        addLayer(new BuildingIconLayer());
		//        addLayer(new MSTLayer());
		
		
		addHumanLayers();

		soscommandsLayer = new SOSCommandLayer();
		addLayer(soscommandsLayer);
		soscommandsLayer.setRenderMove(true);

		addLayer(new PreciptionLayer());
		addLayer(new LineOfSight());
		addLayer(new PositionHistoryLayer());
		addLayer(new SaySelectedLayer());
		addLayer(new SenseSelectedLaye());
		addLayer(new AllHumanSayLayer());
		addLayer(new AllHearSayCommand());
		addLayer(new HumanDeathTimeLayer()); //sinash 2013

		addLayer(new SenseCivilian());
		addLayer(new ShowSelectedLayer());
	}

	protected void addHumanLayers() {
		addLayer(new HumanLayer());		
	}

	@Override
	public void view(Object... objects) {
		super.view(objects);
	}
}
