package sosNamayangar.layers;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.Action;
import javax.swing.AbstractAction;

import java.util.Comparator;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;

import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.config.Config;
import rescuecore2.view.Icons;
import rescuecore2.log.Logger;

import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.view.StandardEntityViewLayer;

/**
 * A view layer that renders humans.
 */
public class SOSHumanActualSizeLayer extends StandardEntityViewLayer<Human> {
	/**
	 * @author Ali
	 */
	private static final int RESCUE_AGENT_RADIUS = 500;
	private static final int CIVILIAN_RADIUS = 200;
	// /////////////////////
	private static final int HP_MAX = 10000;
	private static final int HP_INJURED = 7500;
	private static final int HP_CRITICAL = 1000;

	private static final String ICON_SIZE_KEY = "view.standard.human.icons.size";
	private static final String USE_ICONS_KEY = "view.standard.human.icons.use";
	private static final int DEFAULT_ICON_SIZE = 32;

	private static final HumanSorter HUMAN_SORTER = new HumanSorter();

	private static final Color CIVILIAN_COLOUR = Color.GREEN;
	private static final Color FIRE_BRIGADE_COLOUR = Color.RED;
	private static final Color POLICE_FORCE_COLOUR = Color.BLUE;
	private static final Color AMBULANCE_TEAM_COLOUR = Color.WHITE;
	private static final Color DEAD_COLOUR = Color.BLACK;

	private int iconSize;
	private Map<String, Map<State, Icon>> icons;
	private boolean useIcons;
	private Action useIconsAction;

	/**
	 * Construct a human view layer.
	 * 
	 * @param screenTransform
	 */
	public SOSHumanActualSizeLayer() {
		super(Human.class);
		iconSize = DEFAULT_ICON_SIZE;
	}

	@Override
	public void initialise(Config config) {
		iconSize = config.getIntValue(ICON_SIZE_KEY, DEFAULT_ICON_SIZE);
		icons = new HashMap<String, Map<State, Icon>>();
		useIcons = config.getBooleanValue(USE_ICONS_KEY, false);
		icons.put(StandardEntityURN.FIRE_BRIGADE.toString(), generateIconMap("FireBrigade"));
		icons.put(StandardEntityURN.AMBULANCE_TEAM.toString(), generateIconMap("AmbulanceTeam"));
		icons.put(StandardEntityURN.POLICE_FORCE.toString(), generateIconMap("PoliceForce"));
		icons.put(StandardEntityURN.CIVILIAN.toString() + "-Male", generateIconMap("Civilian-Male"));
		icons.put(StandardEntityURN.CIVILIAN.toString() + "-Female", generateIconMap("Civilian-Female"));
		useIconsAction = new UseIconsAction();
	}

	@Override
	public String getName() {
		return "SOS Humans Actual Size";
	}

	@Override
	public Shape render(Human h, Graphics2D g, ScreenTransform t) {
		// Don't draw humans in ambulances
		if (h.isPositionDefined() && (world.getEntity(h.getPosition()) instanceof AmbulanceTeam)) {
			return null;
		}
		Pair<Integer, Integer> location = getLocation(h);
		if (location == null) {
			return null;
		}
		int x = t.xToScreen(location.first());
		int y = t.yToScreen(location.second());
		Shape shape;
		Icon icon = useIcons ? getIcon(h) : null;
		if (icon == null) {
			int radius;
			if (h instanceof Civilian) {
				radius = CIVILIAN_RADIUS;
			} else {
				radius = RESCUE_AGENT_RADIUS;
			}
			double agentX = x;
			double agentY = y;
			double ellipseX1 = agentX - radius;
			double ellipseY1 = agentY + radius;
			double ellipseX2 = agentX + radius;
			double ellipseY2 = agentY - radius;

			int x1 = t.xToScreen(ellipseX1);
			int y1 = t.yToScreen(ellipseY1);
			int x2 = t.xToScreen(ellipseX2);
			int y2 = t.yToScreen(ellipseY2);
			int ellipseWidth = x2 - x1;
			int ellipseHeight = y2 - y1;

			shape = new Ellipse2D.Double(x - ellipseWidth / 2, y - ellipseHeight / 2, ellipseWidth, ellipseHeight);
			g.setColor(adjustColour(getColour(h), h.getHP()));
			g.fill(shape);
			g.setColor(Color.BLACK);
			g.draw(shape);
		} else {
			x -= icon.getIconWidth() / 2;
			y -= icon.getIconHeight() / 2;
			shape = new Rectangle2D.Double(x, y, icon.getIconWidth(), icon.getIconHeight());
			icon.paintIcon(null, g, x, y);
		}
		return shape;
	}

	@Override
	public List<JMenuItem> getPopupMenuItems() {
		List<JMenuItem> result = new ArrayList<JMenuItem>();
		result.add(new JMenuItem(useIconsAction));
		return result;
	}

	@Override
	protected void postView() {
		Collections.sort(entities, HUMAN_SORTER);
	}

	/**
	 * Get the location of a human.
	 * 
	 * @param h
	 *            The human to look up.
	 * @return The location of the human.
	 */
	protected Pair<Integer, Integer> getLocation(Human h) {
		return h.getLocation(world);
	}

	private Map<State, Icon> generateIconMap(String type) {
		Map<State, Icon> result = new EnumMap<State, Icon>(State.class);
		for (State state : State.values()) {
			String resourceName = "rescuecore2/standard/view/" + type + "-" + state.toString() + "-" + iconSize + "x" + iconSize + ".png";
			URL resource = SOSHumanActualSizeLayer.class.getClassLoader().getResource(resourceName);
			if (resource == null) {
				Logger.warn("Couldn't find resource: " + resourceName);
			} else {
				result.put(state, new ImageIcon(resource));
			}
		}
		return result;
	}

	private Color getColour(Human h) {
		switch (h.getStandardURN()) {
		case CIVILIAN:
			return CIVILIAN_COLOUR;
		case FIRE_BRIGADE:
			return FIRE_BRIGADE_COLOUR;
		case AMBULANCE_TEAM:
			return AMBULANCE_TEAM_COLOUR;
		case POLICE_FORCE:
			return POLICE_FORCE_COLOUR;
		default:
			throw new IllegalArgumentException("Don't know how to draw humans of type " + h.getStandardURN());
		}
	}

	private Color adjustColour(Color c, int hp) {
		if (hp == 0) {
			return DEAD_COLOUR;
		}
		if (hp < HP_CRITICAL) {
			c = c.darker();
		}
		if (hp < HP_INJURED) {
			c = c.darker();
		}
		if (hp < HP_MAX) {
			c = c.darker();
		}
		return c;
	}

	private Icon getIcon(Human h) {
		State state = getState(h);
		Map<State, Icon> iconMap = null;
		switch (h.getStandardURN()) {
		case CIVILIAN:
			boolean male = h.getID().getValue() % 2 == 0;
			if (male) {
				iconMap = icons.get(StandardEntityURN.CIVILIAN.toString() + "-Male");
			} else {
				iconMap = icons.get(StandardEntityURN.CIVILIAN.toString() + "-Female");
			}
			break;
		default:
			iconMap = icons.get(h.getStandardURN().toString());
		}
		if (iconMap == null) {
			return null;
		}
		return iconMap.get(state);
	}

	private State getState(Human h) {
		int hp = h.getHP();
		if (hp <= 0) {
			return State.DEAD;
		}
		if (hp <= HP_CRITICAL) {
			return State.CRITICAL;
		}
		if (hp <= HP_INJURED) {
			return State.INJURED;
		}
		return State.HEALTHY;
	}

	private enum State {
		HEALTHY {
			@Override
			public String toString() {
				return "Healthy";
			}
		},
		INJURED {
			@Override
			public String toString() {
				return "Injured";
			}
		},
		CRITICAL {
			@Override
			public String toString() {
				return "Critical";
			}
		},
		DEAD {
			@Override
			public String toString() {
				return "Dead";
			}
		};
	}

	private static final class HumanSorter implements Comparator<Human>, java.io.Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(Human h1, Human h2) {
			if (h1 instanceof Civilian && !(h2 instanceof Civilian)) {
				return -1;
			}
			if (h2 instanceof Civilian && !(h1 instanceof Civilian)) {
				return 1;
			}
			return h1.getID().getValue() - h2.getID().getValue();
		}
	}

	private final class UseIconsAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public UseIconsAction() {
			super("Use icons");
			putValue(Action.SELECTED_KEY, Boolean.valueOf(useIcons));
//			putValue(Action.SMALL_ICON, useIcons ? Icons.TICK : Icons.CROSS);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			useIcons = !useIcons;
			putValue(Action.SELECTED_KEY, Boolean.valueOf(useIcons));
//			putValue(Action.SMALL_ICON, useIcons ? Icons.TICK : Icons.CROSS);
			component.repaint();
		}
	}
}
