package sosNamayangar;

import static rescuecore2.misc.java.JavaTools.instantiate;
import gis2.GisScenario;
import gis2.ScenarioException;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import rescuecore2.Constants;
import rescuecore2.Timestep;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.log.LogException;
import rescuecore2.log.Logger;
import rescuecore2.log.PerceptionRecord;
import rescuecore2.messages.Command;
import rescuecore2.messages.Message;
import rescuecore2.messages.control.KVTimestep;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.misc.gui.ConfigTree;
import rescuecore2.misc.gui.ListModelList;
import rescuecore2.registry.Registry;
import rescuecore2.score.CompositeScoreFunction;
import rescuecore2.score.ScoreFunction;
import rescuecore2.standard.components.StandardViewer;
import rescuecore2.standard.entities.AmbulanceCentre;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.kernel.BuriedAgentsCommandFilter;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.messages.StandardMessageFactory;
import rescuecore2.standard.score.DiscoveryScoreFunction;
import rescuecore2.standard.score.DistanceTravelledScoreFunction;
import rescuecore2.standard.score.HealthScoreFunction;
import rescuecore2.view.EntityInspector;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.SOSImageIcon;
import rescuecore2.view.SOSViewListener;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewLayer;
import rescuecore2.view.ViewListener;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import sosNamayangar.layers.ATPanelLayer;
import sosNamayangar.layers.CustomLayer;
import sosNamayangar.layers.SOSAbstractSelectedLayer;
import sosNamayangar.layers.SOSAbstractToolsLayer;
import sosNamayangar.sos_score.SearchScoreFunction;
import sosNamayangar.comparators.*;
import sosNamayangar.estimators.SimpleDeathTime;

/**
 * A simple viewer.
 */
public class NewSOSViewer extends StandardViewer {

	private static ImageIcon ICON = new SOSImageIcon(SOSViewer.class.getClassLoader().getResource("sosNamayangar/viewer.png"));
	private static final int DEFAULT_FONT_SIZE = 20;
	private static final int PRECISION = 3;

	private static final String FONT_SIZE_KEY = "viewer.font-size";
	private static final String MAXIMISE_KEY = "viewer.maximise";
	private static final String TEAM_NAME_KEY = "viewer.team-name";
	// private static final String VIEWERS_KEY = "log.viewers";

	private NewSOSFileLogReader logReader;
	private ScoreFunction scoreFunction;
	ArrayList<ScoreFunction> sosScores = new ArrayList<ScoreFunction>();

	private SOSAnimatedWorldModelViewer viewer;
	private JLabel timeLabel;
	JTextField m_timeTextField = new JTextField("-1", 3);
	private JLabel scoreLabel;
	private JLabel teamLabel;
	private NumberFormat format;
	private EntityInspector inspector;
	// private ArrayList<ViewComponent> viewers;
	private ListModelList<Command> commands;
	private JList commandsList;
	private ListModelList<Entity> updates;
	private JList updatesList;
	private StandardEntity selectedObject = null;
	private int currentTime;
	protected boolean isPaused = false;
	private JSlider intervalEachCycleSlider;
	private KVTimestep kvTimestep;
	HashMap<EntityID, AgentWorldModelViewer> agent_world = new HashMap<EntityID, AgentWorldModelViewer>();
	static final int VIEWER_WIDTH = 1024;
	static final int VIEWER_HEIGHT = 768;

	private static Splash splash;

	BufferedWriter log = null;
	JList<String> m_totalList = new JList<String>();
	JList<String> cv_infoList = new JList<String>(); // sinash
	JList<String> ubd_cv_infoList = new JList<String>(); // sinash //UnBuried
															// Damaged Civilians
	JList<String> loaded_cv_infoList = new JList<String>(); // sinash
	JList<String> deathTime_cv_infoList = new JList<String>(); // sinash

	private JToggleButton multipleSelection;
	private JButton showWorldModelButton;
	private JPopupMenu popupMenu;
	private JPanel rightPanel;
	private JSplitPane split;;
	private JPanel m_commandBox = new JPanel();
	private JPanel leftPanel;
	private JSplitPane split1;
	private ATPanelLayer aTPanelLayer;

	public static void showSplash() {
		splash = new Splash(1000);
		// splash.showSplash();
	}

	public NewSOSViewer() {
	}

	public NewSOSViewer(Config config) {
		try {
			showSplash();
			String logname = config.getValue("kernel.logname");
			logReader = new NewSOSFileLogReader(logname, Registry.SYSTEM_REGISTRY);
			for (int i = 0; i < 10; i++) {
				if (logReader.getWorldModel(0) != null)
					break;
				System.out.println("Waiting for time step 0 worldmodel");
				Thread.sleep(400);
			}
			if (logReader.getWorldModel(0) == null)
				throw new Exception("Nothing fetched from file ");
			this.config = logReader.getConfig();
			this.config.setValue("kernel.logname", logname);
			model = logReader.getWorldModel(0);
			postConnectImp();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					m_playTimeButton.doClick();
				}
			});
			// Thread.sleep(1000);

			// Thread.sleep(1000);

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Connecting to SOS Namayangar have Error " + e.getMessage(), "Error", 0);
		}
		if (splash != null)
			splash.exit();
		splash = null;
		// initialiseLog();

	}

	private void initialiseLog() {
		try {
			log = new BufferedWriter(new FileWriter("log.ini"));
			log.write("id time HP Damage Buriedness\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void postConnect() {
		try {
			showSplash();
			try {
				logReader = new SOSRunTimeFileLogReader(/*
														 * "boot/" +
														 */config.getValue("kernel.logname"), Registry.SYSTEM_REGISTRY);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LogException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			super.postConnect();
			postConnectImp();
			m_playTimeButton.doClick();
			updateInformation(new Timestep(0));
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Connecting to SOS Namayangar have Error", "Error", 0);
		}
		if (splash != null)
			splash.exit();
		splash = null;
	}

	protected void postConnectImp() {
		setToolkit();
		int fontSize = config.getIntValue(FONT_SIZE_KEY, DEFAULT_FONT_SIZE);
		String teamName = config.getValue(TEAM_NAME_KEY, "");
		scoreFunction = makeScoreFunction();
		format = NumberFormat.getInstance();
		format.setMaximumFractionDigits(PRECISION);
		JFrame frame = new JFrame("S.O.S Namayangar");
		frame.setIconImage(ICON.getImage());
		currentTime = -1;

		inspector = new EntityInspector();
		registerViewers(config);
		frame.add(controlPanel(), BorderLayout.SOUTH);
		split1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel=getLists(), viewer);
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, split1, rightPanel=rightPanel());

		frame.add(split, BorderLayout.CENTER);

		// final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
		// viewer, rightPanel());
		// CHECKSTYLE:OFF:MagicNumber
		// viewer.setPreferredSize(new Dimension(500, 500));
		// CHECKSTYLE:ON:MagicNumber
		if(!(logReader instanceof SOSRunTimeFileLogReader))
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		timeLabel = new JLabel("Time: Not started", JLabel.CENTER);
		JPanel timePanel = getTimePanel();
		teamLabel = new JLabel("       "+teamName, JLabel.CENTER);
		scoreLabel = new JLabel("Score: Unknown"+"     ", JLabel.CENTER);
		timeLabel.setBackground(Color.WHITE);
		timeLabel.setOpaque(true);
		timeLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, fontSize));
		teamLabel.setBackground(Color.WHITE);
		teamLabel.setOpaque(true);
		teamLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, fontSize));
		scoreLabel.setBackground(Color.WHITE);
		scoreLabel.setOpaque(true);
		scoreLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, fontSize));
		// frame.add(viewer, BorderLayout.CENTER);
		// CHECKSTYLE:OFF:MagicNumber
		JPanel labels = new JPanel(new BorderLayout());
		// CHECKSTYLE:ON:MagicNumber
		labels.add(teamLabel,BorderLayout.WEST);
		labels.add(timePanel,BorderLayout.CENTER);
		labels.add(scoreLabel,BorderLayout.EAST);

		showTimestep(0);
		frame.add(labels, BorderLayout.NORTH);
		frame.pack();
		if (config.getBooleanValue(MAXIMISE_KEY, false)) {
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		}

		frame.setVisible(true);
		enableFullscreen(frame);
		split.setDividerLocation((int) (GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth() * .8d));
		split1.setDividerLocation(.1d);

		// viewer.addViewListener(new ViewListener() {
		// @Override
		// public void objectsClicked(ViewComponent view,
		// List<RenderedObject> objects) {
		// for (RenderedObject next : objects) {
		// System.out.println(next.getObject());
		// }
		// }
		//
		// @Override
		// public void objectsRollover(ViewComponent view,
		// List<RenderedObject> objects) {
		// }
		// });

	}

	private void writeScenario() {
		try {
			StandardWorldModel model = logReader.getWorldModel(0);
			if (model != null) {
				GisScenario scenario = new GisScenario();
				for (StandardEntity entity : model) {
					if (entity instanceof AmbulanceCentre)
						scenario.addAmbulanceCentre(entity.getID().getValue());
					if (entity instanceof AmbulanceTeam)
						scenario.addAmbulanceTeam(((Human) entity).getPosition().getValue());
					if (entity instanceof Civilian)
						scenario.addCivilian(((Human) entity).getPosition().getValue());

					if (entity instanceof FireBrigade)
						scenario.addFireBrigade(((Human) entity).getPosition().getValue());
					if (entity instanceof FireStation)
						scenario.addFireStation(entity.getID().getValue());
					if (entity instanceof PoliceForce)
						scenario.addPoliceForce(((Human) entity).getPosition().getValue());
					if (entity instanceof PoliceOffice)
						scenario.addPoliceOffice(entity.getID().getValue());
					if (entity instanceof Refuge)
						scenario.addRefuge(entity.getID().getValue());
				}
				StandardWorldModel model1 = logReader.getWorldModel(1);
				for (StandardEntity entity : model1) {
					if (entity instanceof Building && ((Building) entity).getFieryness() != 0)
						scenario.addFire(entity.getID().getValue());
				}
				JFileChooser jfc = new JFileChooser(".");
				jfc.setSelectedFile(new File("scenario.xml"));
				if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
					Document doc = DocumentHelper.createDocument();
					scenario.write(doc);
					File saveFile = jfc.getSelectedFile();
					try {
						if (!saveFile.exists()) {
							File parent = saveFile.getParentFile();
							if (!parent.exists()) {
								if (!saveFile.getParentFile().mkdirs()) {
									throw new ScenarioException("Couldn't create file " + saveFile.getPath());
								}
							}
							if (!saveFile.createNewFile()) {
								throw new ScenarioException("Couldn't create file " + saveFile.getPath());
							}
						}
						XMLWriter writer = new XMLWriter(new FileOutputStream(saveFile), OutputFormat.createPrettyPrint());
						writer.write(doc);
						writer.flush();
						writer.close();
					} catch (IOException | ScenarioException e) {
						throw new ScenarioException(e);
					}
				}
				JOptionPane.showMessageDialog(null, "now enter config file");
				jfc.setSelectedFile(new File("kernel.cfg"));
				if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
					PrintWriter ps = new PrintWriter(jfc.getSelectedFile());
					config.setIntValue("ignition.gas_station.explosion.range", 100000);
					config.setBooleanValue("senario.human.random-id", true);
					config.setIntValue("resq-fire.water_hydrant_refill_rate", 1500);
					config.setIntValue("fire.tank.refill_hydrant_rate", 1500);
					config.setIntValue("clear.repair.rad", 1250);

					config.write(ps);
					ps.flush();
					ps.close();
				}

			}
		} catch (LogException | ScenarioException | IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "can not get step 3" + e.getMessage());
		}

	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void enableFullscreen(Window window) {
	    try {
	        Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
	        Class params[] = new Class[]{Window.class, Boolean.TYPE};
	         Method method = util.getMethod("setWindowCanFullScreen", params);
	        method.invoke(util, window, true);
	    } catch (Exception e) {
	    }
	}

	private JPanel rightPanel() {
		JPanel panel = new JPanel();

		panel.setPreferredSize(new Dimension(200, 100));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JTabbedPane tabbedPane = new JTabbedPane();
		JTabbedPane civiliansPane = new JTabbedPane(); // sinash

		tabbedPane.addTab("Information", addInfoPanel());
		tabbedPane.addTab("Option", addOptionPanel());
		tabbedPane.addTab("SOSOption", addSOSOptionPanel());
		tabbedPane.addTab("MapConfig", addMapConfigPanel());
		tabbedPane.addTab("AT Panel", addATPanel());
		
		// sinash
		civiliansPane.addTab("Buriedness & Damage", addCiviliansInfoPanel());
		civiliansPane.addTab("UB-D Civilians", addUnburiedDamagedCiviliansPanel()); // UnBuried
																					// Damaged
																					// Civilians
		civiliansPane.addTab("Loaded Civilians", addLoadedCiviliansInfoPanel());
		civiliansPane.addTab("DeathTime", addDeathTimeCiviliansInfoPanel());

		tabbedPane.add("Civilians Info", civiliansPane); // sinash

		panel.add(tabbedPane);

		panel.setVisible(false);
		return panel;
	}

	private Component addATPanel() {
		return aTPanelLayer.getPanel();
	}

	private Component addMapConfigPanel() {
		ConfigTree configTree = new ConfigTree(config);
		configTree.setEditable(false);
		return configTree;
	}

	private JPanel addInfoPanel() {
		JScrollPane jsp = new JScrollPane(inspector);
		JPanel totalPanel = new JPanel();

		totalPanel.setLayout(new BoxLayout(totalPanel, BoxLayout.Y_AXIS));
		totalPanel.add(new JLabel("Properties"));
		totalPanel.add(jsp);
		showWorldModelButton = new JButton("Show World Model");
		showWorldModelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (selectedObject != null && selectedObject instanceof Human && !(selectedObject instanceof Civilian)) {
					AgentWorldModelViewer world = agent_world.get(selectedObject.getID());
					if (world == null) {
						world = new AgentWorldModelViewer(config, selectedObject.getID(), selectedObject + "");
						agent_world.put(selectedObject.getID(), world);
					}
					world.show();
				} else
					JOptionPane.showMessageDialog(null, "Please select an agent");

			}
		});
		totalPanel.add(showWorldModelButton);
		totalPanel.add(new JLabel("Total"));

		totalPanel.add(new JScrollPane(m_totalList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		totalPanel.add(new JLabel("Other"));

		return totalPanel;
	}

	// sinash
	private JPanel addCiviliansInfoPanel() {

		JPanel cvInfoPanel = new JPanel();
		cvInfoPanel.setLayout(new BoxLayout(cvInfoPanel, BoxLayout.Y_AXIS));

		cvInfoPanel.add(new JScrollPane(cv_infoList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		return cvInfoPanel;

	}

	// sinash
	private JPanel addUnburiedDamagedCiviliansPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.add(new JScrollPane(ubd_cv_infoList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		return panel;

	}

	// sinash
	private JPanel addLoadedCiviliansInfoPanel() {

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.add(new JScrollPane(loaded_cv_infoList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		return panel;
	}

	private JPanel addDeathTimeCiviliansInfoPanel() {
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.add(new JScrollPane(deathTime_cv_infoList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		return panel;
	}

	private JPanel getLists() {
		JPanel lists = new JPanel(new GridLayout(0, 1));

		commands = new ListModelList<Command>() {
			@Override
			public Command get(int index) {
				// TODO Auto-generated method stub
				return super.get(index);
			}
		};
		commandsList = new JList(commands);
		updates = new ListModelList<Entity>();
		updatesList = new JList(updates);
		JScrollPane s = new JScrollPane(commandsList);
		s.setBorder(BorderFactory.createTitledBorder("Commands"));
		s.setPreferredSize(commandsList.getPreferredScrollableViewportSize());
		lists.add(s);
		s = new JScrollPane(updatesList);
		s.setBorder(BorderFactory.createTitledBorder("Updates"));
		s.setPreferredSize(updatesList.getPreferredScrollableViewportSize());
		lists.add(s);
		lists.setVisible(false);
		return lists;
	}

	private void registerViewers(Config config) {
		viewer = new SOSAnimatedWorldModelViewer();
		viewer.initialise(config);
		viewer.view(model);
		viewer.addLayer(aTPanelLayer=new ATPanelLayer());
		// viewers.add(viewer);
		// tabs.addTab(viewer.getViewerName(),viewer);
		viewer.addViewListener(new SOSViewListener() {

			@Override
			public void objectsRollover(ViewComponent view, List<RenderedObject> objects) {
			}

			@Override
			public void objectsRollover(ViewComponent view, List<RenderedObject> objects, MouseEvent e) {

			}

			@Override
			public void objectsClicked(ViewComponent view, List<RenderedObject> objects) {
			}

			@Override
			public void objectsClicked(ViewComponent view, List<RenderedObject> objects, MouseEvent e) {

				if (objects.size() > 0) {
					if ((e.getButton() == MouseEvent.BUTTON3))
						showMenuSelectObject(view, objects, e);
					else {

						for (RenderedObject sso : objects) {
							if (sso.getObject() instanceof rescuecore2.standard.entities.Area) {
								setSelectedObject(sso.getObject());
								break;
							}
						}
						for (RenderedObject sso : objects) {
							if (sso.getObject() instanceof Human) {
								setSelectedObject(sso.getObject());
								break;
							}
						}
					}

				} else {
					setSelectedObject(null);
				}

			}
		});

	}

	class SOSMenuItem extends JMenuItem {
		private final RenderedObject sosSelectedObj;

		public SOSMenuItem(RenderedObject sosSelectedObj) {
			super(sosSelectedObj.getObject().toString());
			this.sosSelectedObj = sosSelectedObj;
		}

		public RenderedObject getSOSSelectedObj() {
			return sosSelectedObj;
		}

		private static final long serialVersionUID = 1L;

	}

	private synchronized void showMenuSelectObject(ViewComponent view, Collection<RenderedObject> objects, MouseEvent e) {

		if (popupMenu != null) {
			popupMenu.setVisible(false);
		}
		popupMenu = new JPopupMenu();
		for (RenderedObject renderObject : objects) {

			final SOSMenuItem menuItem = new SOSMenuItem(renderObject);
			menuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					setSelectedObject(menuItem.getSOSSelectedObj().getObject());
				}
			});
			popupMenu.add(menuItem);
		}
		popupMenu.show(viewer, e.getX(), e.getY());// viewer, objs.get(0)., y)
		// if (popupMenu.isPopupTrigger(e))
		viewer.repaint();
	}

	private JPanel controlPanel() {
		JPanel superPanel = new JPanel(new BorderLayout());
		superPanel.add(m_commandBox);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setSize(new Dimension(VIEWER_WIDTH, 20));
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		addIntervalEachCycleSlider(panel);
		addIdTextField(panel);
//		addShowCommandBox(panel);
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new GridLayout());
		addZoomPanel(rightPanel);
		mainPanel.add(panel, BorderLayout.WEST);
		JPanel centerPanel=new JPanel();
		int channelCount=config.getIntValue("comms.channels.count")-1;//-1 for say
		int bandwidth = getAvailableBandwidth();
        String commType = channelCount+" channels-bandwidth:"+bandwidth+" byte";
        if(channelCount==0)
        	commType="No Comm";
        else if(bandwidth<1000)
        	commType="Low Comm "+bandwidth;
		centerPanel.add(new JLabel("Probably: "+commType));
		mainPanel.add(centerPanel,BorderLayout.CENTER);

		mainPanel.add(rightPanel, BorderLayout.EAST);
		superPanel.add(mainPanel,BorderLayout.SOUTH);
		return superPanel;
	}
	private int getAvailableBandwidth() {
		int channelCount = config.getIntValue("comms.channels.count");
		int maxValid = config.getIntValue("comms.channels.max.platoon");
		int size = 0;
		for (int i = 1; i < Math.min(maxValid+1, channelCount); i++) {
			 String channelKey = "comms.channels." + i;
			 int bandwidth=config.getIntValue(channelKey + ".bandwidth");
			 size+=bandwidth;
		}
		return size;
	}

	private void addShowCommandBox(JPanel panel) {
		final JToggleButton showCommandBox = new JToggleButton("Show Command Box", false);
		m_commandBox.setVisible(showCommandBox.isSelected());
		showCommandBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_commandBox.setVisible(showCommandBox.isSelected());
			}
		});
		viewer.addLayer(new CustomLayer(m_commandBox));

		panel.add(showCommandBox);
	}

	private static final int TIME_INTERVAL_ZARIB = 3000 / 10;

	private void addIntervalEachCycleSlider(JPanel panel) {
		final JToggleButton m_showLeftPanel = new JToggleButton("Show", false);
		m_showLeftPanel.setBackground(Color.white);
		m_showLeftPanel.setToolTipText("Diable/Enable left panel");
		m_showLeftPanel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				leftPanel.setVisible(m_showLeftPanel.isSelected());
				split1.setDividerLocation(.1);
			}
		});
		panel.add(m_showLeftPanel);
		panel.add(new JLabel(" Interval "));
		intervalEachCycleSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, 3);
		intervalEachCycleSlider.setPreferredSize(new Dimension(100, 20));
		intervalEachCycleSlider.setMaximumSize(new Dimension(100, 20));
		intervalEachCycleSlider.setToolTipText(getIntervalEachCycle() + " [m sec/cycle]");

		ToolTipManager tipMan = ToolTipManager.sharedInstance();
		tipMan.setInitialDelay(100);
		tipMan.setDismissDelay(2000);
		panel.add(intervalEachCycleSlider);
		intervalEachCycleSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				intervalEachCycleSlider.setToolTipText(getIntervalEachCycle() + " [m sec/cycle]");
			}
		});
		panel.add(intervalEachCycleSlider);

	}

	public int getIntervalEachCycle() {
		return intervalEachCycleSlider.getValue() * TIME_INTERVAL_ZARIB;
	}

	private void addZoomPanel(JPanel panel) {
		final JTextField m_zoomTextField = new JTextField("100");
		m_zoomTextField.setSize(30, 20);
		m_zoomTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewer.getTransform().setZoomLevel((double) Integer.parseInt(m_zoomTextField.getText()) / 100d);
				m_zoomTextField.setText((int) (viewer.getTransform().getZoomLevel() * 100) + "");
				viewer.repaint();
			}
		});
		JButton m_increaseZoomButton = new JButton("+");
		m_increaseZoomButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewer.getTransform().zoomIn();
				m_zoomTextField.setText((int) (viewer.getTransform().getZoomLevel() * 100) + "");
				viewer.repaint();
			}
		});
		JButton m_decreaseZoomButton = new JButton("-");
		m_decreaseZoomButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewer.getTransform().zoomOut();
				m_zoomTextField.setText((int) (viewer.getTransform().getZoomLevel() * 100) + "");
				viewer.repaint();
			}
		});
		JButton m_resetZoomButton = new JButton("R");
		m_resetZoomButton.setBackground(Color.white);
		m_resetZoomButton.setToolTipText("Reset map changed");
		m_resetZoomButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewer.getTransform().resetZoom();
				m_zoomTextField.setText((int) (viewer.getTransform().getZoomLevel() * 100) + "");
				viewer.repaint();
			}
		});
		final JToggleButton m_enableDrag = new JToggleButton("D", true);
		m_enableDrag.setBackground(Color.white);
		m_enableDrag.setToolTipText("Diable/Enable draging");
		m_enableDrag.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewer.setEnableDrag(m_enableDrag.isSelected());
			}
		});
		final JToggleButton m_showRightPanel = new JToggleButton("Show", false);
		m_showRightPanel.setBackground(Color.white);
		m_showRightPanel.setToolTipText("Diable/Enable righy panel");
		m_showRightPanel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rightPanel.setVisible(m_showRightPanel.isSelected());
				split.setDividerLocation(.8);
			}
		});

		panel.add(new JLabel(" Zoom:"));
		panel.add(m_decreaseZoomButton);
		panel.add(m_zoomTextField);
		panel.add(m_increaseZoomButton);
		panel.add(m_resetZoomButton);
		panel.add(m_enableDrag);
		panel.add(m_showRightPanel);
	}

	private void addIdTextField(JPanel panel) {
		panel.add(new JLabel(" Select ID:"));
		final JTextField idTextField = new JTextField(10);
		idTextField.setSize(100, 20);
		idTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSelectedObject(null);
				setSelectedObject((StandardEntity) model.getEntity(new EntityID(Integer.parseInt(idTextField.getText()))));
			}
		});
		panel.add(idTextField);

	}

	public void showTimestep(int time) {
		try {
			int maxTime = logReader.getMaxTimestep();
			if (/* (!(kvTimestep!=null&&time==kvTimestep.getTime()))&& */(time < 0 || time > maxTime)) {
				if (time < 0)
					time = 0;
				else
					time = maxTime;

				// return;
			}
			Timestep timestep = new Timestep(time);
			currentTime = time;

			m_timeTextField.setText(time + "");

			Collection<Command> newCommands = null;
			/*
			 * if(kvTimestep!=null&&time==kvTimestep.getTime()){
			 * newCommand=kvTimestep.getCommands(); commands.addAll(newCommand);
			 * }else
			 */

			if (logReader.getCommands(time) != null) {
				newCommands = logReader.getCommands(time).getCommands();
				timestep.setCommands(newCommands);
				ArrayList<Command> selectedCommands = new ArrayList<Command>();
				for (Command com : newCommands) {
					if (((!(model.getEntity(com.getAgentID()) instanceof Civilian) || com instanceof AKSpeak) && !(selectedObject instanceof Human))
							|| (selectedObject instanceof Human && com.getAgentID().equals(selectedObject.getID())))
						selectedCommands.add(com);
				}
				Collections.sort(selectedCommands, new Comparator<Command>() {
					@Override
					public int compare(Command o1, Command o2) {
						return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
					}
				});
				synchronized (commands) {
					commands.clear();
					commands.addAll(selectedCommands);
				}
			}
			ChangeSet newChangeSet = null;
			/*
			 * if(kvTimestep!=null&&time==kvTimestep.getTime())
			 * newChangeSet=kvTimestep.getChangeSet(); else
			 */
			if (logReader.getUpdates(time) != null)
				newChangeSet = logReader.getUpdates(time).getChangeSet();
			timestep.setChangeSet(newChangeSet);
			model = logReader.getWorldModel(time);
			for (StandardEntity e : model
					.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE,
							StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.FIRE_STATION, StandardEntityURN.POLICE_FORCE, StandardEntityURN.POLICE_OFFICE)) {
				PerceptionRecord p = logReader.getPerception(timestep.getTime(), e.getID());
				if (p != null)
					timestep.registerPerception(e.getID(), p.getChangeSet(), p.getHearing());

			}
			if (model != null) {
				logAgents(time);

				if (selectedObject != null)
					setSelectedObject(model.getEntity(selectedObject.getID()));
				if (newChangeSet != null) {
					Collection<Entity> ent = new HashSet<Entity>();
					for (EntityID entityID : newChangeSet.getChangedEntities())
						ent.add(model.getEntity(entityID));
					synchronized (updates) {
						updates.addAll(ent);
					}
				}
			}
			// for (ViewComponent viewer : viewers) {
			viewer.view(model, newCommands, newChangeSet, timestep, this);
			// }
			scoreLabel.setText("Score: " + format.format(scoreFunction.score(model, timestep))+"     ");
			updateInformation(timestep);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void logAgents(int time) {
		/*
		 * if(time==3){ for (StandardEntity bil:
		 * model.getEntitiesOfType(StandardEntityURN.BUILDING)) { try {
		 * log.write
		 * ("<rcr:civilian rcr:location=\""+bil.getID().getValue()+"\"/>");
		 * log.newLine(); log.flush(); } catch (IOException e) {
		 * e.printStackTrace(); } } } for (StandardEntity st :
		 * model.getEntitiesOfType(StandardEntityURN.CIVILIAN)) { Civilian
		 * civ=(Civilian) st; try {
		 * log.write(st.getID().getValue()+" "+time+" "+
		 * civ.getHP()+" "+civ.getDamage()+" "+civ.getBuriedness()+"\n"); }
		 * catch (IOException e) { e.printStackTrace(); } }
		 */
	}

	private void updateInformation(Timestep timestep) {
		Vector<String> totalList = new Vector<String>();
		Vector<String> cvInfoList = new Vector<String>(); // sinash
		Vector<String> ubdInfoList = new Vector<String>();// sinash
		Vector<String> loadedInfoList = new Vector<String>();// sinash
		Vector<String> deathTimeInfoList = new Vector<String>(); // sinash

		HashMap<Integer, Integer> buriednessOccurrences = new HashMap<Integer, Integer>(); // sinash

		ArrayList<Civilian> civilians = new ArrayList<Civilian>(); // sinash
		ArrayList<Civilian> unburiedDamagedCivilians = new ArrayList<Civilian>(); // sinash
		ArrayList<Civilian> loadedCivilians = new ArrayList<Civilian>();// sinash

		totalList.add("Building:" + model.getEntitiesOfType(StandardEntityURN.BUILDING).size());
		totalList.add("Road:" + model.getEntitiesOfType(StandardEntityURN.ROAD).size());
		totalList.add("Fire Brigade:" + model.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE).size());
		totalList.add("Police Force:" + model.getEntitiesOfType(StandardEntityURN.POLICE_FORCE).size());
		totalList.add("Ambulance Team:" + model.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM).size());
		totalList.add("Centers:" + model.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.FIRE_STATION, StandardEntityURN.POLICE_OFFICE).size());
		totalList.add("Refuges:" + model.getEntitiesOfType(StandardEntityURN.REFUGE).size());

		// *******sinash
		Collection<StandardEntity> civ = model.getEntitiesOfType(StandardEntityURN.CIVILIAN);
		int dead = 0;
		boolean cvIsLoaded = false;
		for (StandardEntity standardEntity : civ) {
			dead += ((Civilian) standardEntity).getHP() == 0 ? 1 : 0;
			// sinash
			cvIsLoaded = false;
			Civilian cv = (Civilian) standardEntity;
			civilians.add(cv);

			int cvPosition = cv.getPosition().getValue();

			if (cv.isBuriednessDefined() && cv.isDamageDefined()) {
				if (cv.getDamage() > 0) {
					if (cv.getBuriedness() == 0) {

						for (StandardEntity at : model.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)) {
							if (cvPosition == ((AmbulanceTeam) at).getID().getValue()) {
								loadedCivilians.add(cv);
								cvIsLoaded = true;
								break;
							}
						}
						if (!cvIsLoaded) {
							unburiedDamagedCivilians.add(cv);
						}

					}
				}
			}
		}
		// sinash
		Collections.sort(civilians, new BuriednessCivilianComparator());

		for (Civilian civilian : civilians) {
			int buriedness = civilian.getBuriedness();
			if (!buriednessOccurrences.containsKey(buriedness)) {
				buriednessOccurrences.put(buriedness, 1);
			} else {
				int occu = buriednessOccurrences.get(buriedness) + 1;
				buriednessOccurrences.put(buriedness, occu);
			}
			cvInfoList.add("Civilian " + civilian.getID().getValue() + " --> Buriedness: " + buriedness + " -->Damage: " + civilian.getDamage());
			deathTimeInfoList.add("CV " + civilian.getID().getValue() + " --> DT: " + SimpleDeathTime.getEasyLifeTime(civilian.getHP(), civilian.getDamage(), timestep.getTime()));
		}
		cvInfoList.add("\n----  [Buriedness] = [Number Of Civilians]  ----\n");
		cvInfoList.add(buriednessOccurrences.toString());

		for (Civilian civilian : unburiedDamagedCivilians) {
			ubdInfoList.add("Civilian " + civilian.getID().getValue() + ", Position: " + civilian.getPosition() + ", HP: " + civilian.getHP());

		}

		for (Civilian civilian : loadedCivilians) {

			loadedInfoList.add("CV : " + civilian.getID().getValue() + ", Position: " + civilian.getPosition());
		}
		// ********** end sinash
		totalList.add("Alive Civilian:" + (model.getEntitiesOfType(StandardEntityURN.CIVILIAN).size() - dead));
		totalList.add("Dead Civilian:" + dead);

		if (scoreFunction instanceof CompositeScoreFunction) {
			for (ScoreFunction score : ((CompositeScoreFunction) scoreFunction).getChildFunctions()) {
				totalList.add(score.getName() + ": " + format.format(score.score(model, timestep)));
			}
		}
		for (ScoreFunction score : sosScores) {
			totalList.add(score.getName() + ": " + format.format(score.score(model, timestep)));
		}
		totalList.add("Map Scale:" + (int)model.getBounds().getWidth()+"x"+(int)model.getBounds().getHeight());
		synchronized (m_totalList) {
			m_totalList.setListData(totalList);
		}
		synchronized (cv_infoList) { // sinash
			cv_infoList.setListData(cvInfoList);
		}
		synchronized (ubd_cv_infoList) { // sinash
			ubd_cv_infoList.setListData(ubdInfoList);
		}
		synchronized (loaded_cv_infoList) {
			loaded_cv_infoList.setListData(loadedInfoList);
		}
		synchronized (deathTime_cv_infoList) {
			deathTime_cv_infoList.setListData(deathTimeInfoList);
		}
	}

	private Component addSOSOptionPanel() {
		JPanel jp = new JPanel();
		jp.setBorder(new TitledBorder(new LineBorder(Color.white, 2, true), "View Setting"));
		multipleSelection = new JToggleButton("MultipleSelection");
		jp.add(multipleSelection);
		final JToggleButton renderMove = new JToggleButton("RenderMove", true);
		renderMove.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				viewer.soscommandsLayer.setRenderMove(renderMove.isSelected());
			}
		});
		jp.add(renderMove);
		JButton selectAllFB = new JButton("selectAllFB");
		selectAllFB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				for (StandardEntity se : model.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE)) {
					for (ViewLayer layers : viewer.getLayers()) {
						if (layers instanceof SOSAbstractSelectedLayer) {
							((SOSAbstractSelectedLayer) layers).addSelected(se);
						}
					}
				}
			}
		});
		jp.add(selectAllFB);

		JButton selectAllAMB = new JButton("selectAllAmb");
		selectAllAMB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				for (StandardEntity se : model.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)) {
					for (ViewLayer layers : viewer.getLayers()) {
						if (layers instanceof SOSAbstractSelectedLayer) {
							((SOSAbstractSelectedLayer) layers).addSelected(se);
						}
					}
				}
			}
		});
		jp.add(selectAllAMB);

		JButton selectAllPolice = new JButton("selectAllPolice");
		selectAllPolice.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				for (StandardEntity se : model.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
					for (ViewLayer layers : viewer.getLayers()) {
						if (layers instanceof SOSAbstractSelectedLayer) {
							((SOSAbstractSelectedLayer) layers).addSelected(se);
						}
					}
				}
			}
		});
		jp.add(selectAllPolice);
		JButton writeScenario = new JButton("writeScenario ");
		writeScenario.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				writeScenario();
			}
		});
		jp.add(writeScenario);
		jp.setLayout(new GridLayout(jp.getComponentCount(), 1, 2, 2));
		return jp;
	}

	private JPanel addOptionPanel() {

		JPanel optionPanel = new JPanel();
		optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.PAGE_AXIS));
		JPanel jp = new JPanel();
		jp.setLayout(new GridLayout(viewer.getLayers().size(), 1, 2, 2));
		jp.setBorder(new TitledBorder(new LineBorder(Color.white, 2, true), "View Setting"));
		jp.setPreferredSize(new Dimension(150, 300));
		for (final rescuecore2.view.ViewLayer layer : viewer.getLayers()) {
			final JToggleButton jtb = new JToggleButton(layer.getName(), layer.isVisible());
			jtb.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					layer.setVisible(jtb.isSelected());
					viewer.repaint();
				}
			});
			jp.add(jtb);
		}
		optionPanel.add(jp);
		return optionPanel;
	}

	public void setSelectedObject(Object object) {
		// for (SOSAbstractSelectedLayer listener : selectedObjectListeners) {
		// listener.objectSelected(selectedObject);
		// }
		// viewer.selectedLayer.setSelectedObject(selectedObject);
		// inspector.inspect(selectedObject);
		// addToSearchValueLayerList(selectedObject);//Salim

		// if (this.selectedObject != null)
		// this.selectedObject.setSelected(false);
		//
		// if (object != null) {
		// this.selectedObject = object;
		// object.setSelected(true);
		//
		// }
		for (ViewLayer layers : viewer.getLayers()) {
			if (layers instanceof SOSAbstractSelectedLayer) {
				if (multipleSelection.isSelected())
					((SOSAbstractSelectedLayer) layers).addSelected(object);
				else
					((SOSAbstractSelectedLayer) layers).selected(object);
			}
		}
		if (object == null || object instanceof StandardEntity) {
			this.selectedObject = (StandardEntity) object;
			inspector.inspect((Entity) object);
		}
		// for (ViewComponent viewer : viewers) {
		viewer.repaint();
		// }
	}

	boolean dontChangeTime = false;
	private JToggleButton m_playTimeButton;

	private JPanel getTimePanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panel.setBackground(Color.white);
		// m_timeTextField = new JTextField("0", 3);
		m_timeTextField.setFont(m_timeTextField.getFont().deriveFont(Font.PLAIN, 20));
		m_timeTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent arg0) {
				dontChangeTime = false;
			}

			@Override
			public void focusGained(FocusEvent arg0) {
				dontChangeTime = true;

			}
		});
		JButton m_backTimeButton = new JButton("<<");
		m_backTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					int a = Integer.parseInt(m_timeTextField.getText()) - 1;
					if (a >= 0) {
						showTimestep(a);
					}
				} catch (NumberFormatException ne) {
					JOptionPane.showMessageDialog(null, "Please Enter Number", "Error", 0);
				}

			}
		});
		final JToggleButton m_stopTimeButton = new JToggleButton("||", false);
		m_playTimeButton = new JToggleButton(">", true);

		m_stopTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_playTimeButton.setSelected(false);
				m_stopTimeButton.setSelected(true);
				isPaused = true;
			}
		});

		ActionListener act;
		m_playTimeButton.addActionListener(act = new ActionListener() {
			private Thread playThread;

			public void actionPerformed(ActionEvent e) {
				try {
					setCurrentTime(Integer.parseInt(m_timeTextField.getText()));
				} catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(null, "Please Enter Number", "Error", 0);
				}
				m_stopTimeButton.setSelected(false);
				m_playTimeButton.setSelected(true);
				isPaused = false;
				play();
			}

			private void play() {
				if (playThread == null || !playThread.isAlive()) {
					playThread = new Thread(new Runnable() {
						@Override
						public void run() {
							int tmp = 0;
							while (m_playTimeButton.isSelected()) {
								try {
									Thread.sleep(getIntervalEachCycle() / 5);
								} catch (InterruptedException e1) {
									Logger.warn("Player interrupted", e1);
								}
								if (tmp++ % 5 == 0) {
									try {
										if (!m_timeTextField.getToolTipText().equals("onTextbox"))
											if (getCurrentTime() < logReader.getMaxTimestep())
												showTimestep(getCurrentTime() + 1);
									} catch (LogException e) {
										e.printStackTrace();
									}
								}
							}
						}
					});
					playThread.start();
				}
			}
		});

		JButton m_forwardTimeButton = new JButton(">>");
		m_forwardTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					showTimestep(Integer.parseInt(m_timeTextField.getText()) + 1);
				} catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(null, "Please Enter Number", "Error", 0);
				}

			}
		});
		m_timeTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					showTimestep(Integer.parseInt(m_timeTextField.getText()));
				} catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(null, "Please Enter Number", "Error", 0);
				}
				m_timeTextField.setFocusable(false);
			}
		});
		m_timeTextField.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent arg0) {
				// setM_updateTimeTextBox(true);
				// m_timeTextField.setToolTipText("");
				m_timeTextField.setFocusable(true);
			}

			public void focusGained(FocusEvent arg0) {
				// m_timeTextField.setToolTipText("onTextbox");
				m_stopTimeButton.doClick();
			}
		});

		m_backTimeButton.setToolTipText("Back");
		m_stopTimeButton.setToolTipText("Stop");
		m_timeTextField.setToolTipText("Time");
		m_playTimeButton.setToolTipText("Play");
		m_forwardTimeButton.setToolTipText("Forward");

		panel.add(m_backTimeButton);
		panel.add(m_stopTimeButton);
		panel.add(m_timeTextField);
		panel.add(new Label("/" + config.getIntValue("kernel.timesteps")));
		panel.add(m_playTimeButton);
		panel.add(m_forwardTimeButton);
		return panel;

	}

	@Override
	protected void handleTimestep(final KVTimestep t) {
		// if(currentTime==t.getTime()-1)

		// super.handleTimestep(t);
		kvTimestep = t;
		// showTimestep(t.getTime());
		// SwingUtilities.invokeLater(new Runnable() {
		// public void run() {
		// timeLabel.setText("Time: " + t.getTime());
		// m_timeTextField.setText(t.getTime()+"");
		// scoreLabel.setText("Score: "
		// + format.format(scoreFunction.score(model,
		// new Timestep(t.getTime()))));
		// viewer.view(model, t.getCommands());
		// viewer.repaint();
		// }
		// });

	}

	@Override
	protected void processMessage(Message msg) {
		// super.processMessage(msg);

	}

	public int getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(int currentTime) {
		this.currentTime = currentTime;
	}

	@Override
	public String toString() {
		return "New S.O.S Namayangar";
	}

	private ScoreFunction makeScoreFunction() {
		String className = config.getValue(Constants.SCORE_FUNCTION_KEY);
		ScoreFunction result = instantiate(className, ScoreFunction.class);
		result.initialise(model, config);
		sosScores.add(new SearchScoreFunction());
		sosScores.add(new DiscoveryScoreFunction());
		sosScores.add(new DistanceTravelledScoreFunction());
		sosScores.add(new HealthScoreFunction());
		for (ScoreFunction s : sosScores) {
			s.initialise(model, config);
		}
		return result;
	}

	public static void main(String[] args) {
		Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
		Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
		Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
		setToolkit();
		try {

			Config config = new Config();
			if (args.length == 0)
				config.setValue("kernel.logname", "/home/ali/Desktop/sos-server/boot/logs/rescue.soslog");
			args = CommandLineOptions.processArgs(args, config);

			new NewSOSViewer(config);
		} catch (ConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void setToolkit() {
		try {
			Toolkit xToolkit = Toolkit.getDefaultToolkit();
			java.lang.reflect.Field awtAppClassNameField;
			awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
			awtAppClassNameField.setAccessible(true);
			awtAppClassNameField.set(xToolkit, "LogViewer");
		} catch (Exception e1) {
		}
	}
}
