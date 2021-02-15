package sosNamayangar;

import javax.swing.JSlider;
import javax.swing.Timer;

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.view.AnimatedHumanLayer;
import rescuecore2.standard.view.AreaNeighboursLayer;
import rescuecore2.standard.view.BuildingLayer;
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
public class SOSAnimatedWorldModelViewer extends SOSWorldModelViewer {
	private static final long serialVersionUID = 1L;
	private static final int FRAME_COUNT = 10;
	private static final int ANIMATION_TIME = 750;
	private static final int FRAME_DELAY = ANIMATION_TIME / FRAME_COUNT;

	private AnimatedHumanLayer humans;
	private SOSAnimatedHumanActualSizeLayer humansActualSize;
	private Timer timer;
	private final Object lock = new Object();
	private boolean done;
	//	public SaySelectedLayer sayLayer;
	//	public SenseSelectedLaye senseLayer;
	public SOSCommandLayer soscommandsLayer;

	/**
	 * Construct an animated world model viewer.
	 */
	public SOSAnimatedWorldModelViewer() {
		super();
		timer = new Timer(FRAME_DELAY, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				synchronized (lock) {
					if (done) {
						return;
					}
					done = true;
					if (humans.isVisible()&&humans.nextFrame()) {
						done = false;
						repaint();
					}
					if (humansActualSize.isVisible()&&humansActualSize.nextFrame()) {
						done = false;
						repaint();
					}
				}
			}

		});
		timer.setRepeats(true);

		timer.start();
	}


	@Override
	public String getViewerName() {
		return "Animated world model viewer";
	}

	@Override
	protected void addHumanLayers() {
		humans = new AnimatedHumanLayer();
		addLayer(humans);
		humansActualSize = new SOSAnimatedHumanActualSizeLayer();
		humansActualSize.setVisible(false);
		addLayer(humansActualSize);
	}
	@Override
	public void view(Object... objects) {
		super.view(objects);
		int intervalEachCycle=config.getIntValue("kernel.agents.think-time");
		for (Object object : objects) {
			if(object instanceof NewSOSViewer)
				intervalEachCycle=((NewSOSViewer) object).getIntervalEachCycle();
			if(object instanceof SOSViewer)
				intervalEachCycle=((SOSViewer) object).getIntervalEachCycle();
		}
		System.out.println(timer.getInitialDelay());
		timer.setDelay(intervalEachCycle/ FRAME_COUNT);
//		System.out.println(timer.getDelay());

		synchronized (lock) {
			done = false;
			if(humans.isVisible())
				humans.computeAnimation(FRAME_COUNT);
			if(humansActualSize.isVisible())
				humansActualSize.computeAnimation(FRAME_COUNT);
		}
	}
}
