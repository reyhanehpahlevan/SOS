package sosNamayangar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import rescuecore2.Timestep;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.log.Logger;
import rescuecore2.log.PerceptionRecord;
import rescuecore2.messages.Command;
import rescuecore2.messages.Message;
import rescuecore2.messages.control.KVTimestep;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.misc.gui.ConfigTree;
import rescuecore2.misc.gui.ListModelList;
import rescuecore2.registry.Registry;
import rescuecore2.score.ScoreFunction;
import rescuecore2.standard.components.StandardViewer;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;
import rescuecore2.view.EntityInspector;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewLayer;
import rescuecore2.view.ViewListener;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import sosNamayangar.layers.SOSAbstractSelectedLayer;

/**
 * A simple viewer.
 */
public class AgentWorldModelViewer extends StandardViewer {
	private static final int DEFAULT_FONT_SIZE = 20;
	private static final int PRECISION = 3;

	private static final String FONT_SIZE_KEY = "viewer.font-size";
	private static final String MAXIMISE_KEY = "viewer.maximise";
	private static final String TEAM_NAME_KEY = "viewer.team-name";
	// private static final String VIEWERS_KEY = "log.viewers";

	private NewSOSAgentFileLogReader logReader;
	ArrayList<ScoreFunction> sosScores = new ArrayList<ScoreFunction>();

	private SOSAnimatedWorldModelViewer viewer;
	private JLabel timeLabel;
	JTextField m_timeTextField = new JTextField("0", 3);
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

	static final int VIEWER_WIDTH = 1024;
	static final int VIEWER_HEIGHT = 768;

	private static Splash splash;

	BufferedWriter log = null;
	JList<String> m_totalList = new JList<String>();
	private JToggleButton multipleSelection;
	private final EntityID agentID;
	private JFrame frame;

	public void show() {
		frame.setVisible(true);
	}

	public static void showSplash() {
		splash = new Splash(1000);
		// splash.showSplash();
	}

	public AgentWorldModelViewer(Config config, EntityID agentID, String title) {
		this.agentID = agentID;
		try {
			showSplash();
			logReader = new NewSOSAgentFileLogReader(new File(config.getValue("kernel.logname")), Registry.SYSTEM_REGISTRY, agentID);
			for (int i = 0; i < 10; i++) {
				if (logReader.getWorldModel(0) != null)
					break;
				Thread.sleep(400);
			}
			this.config = logReader.getConfig();
			postConnectImp(title);

			showTimestep(0);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Connecting to SOS Namayangar have Error", "Error", 0);
		}
		if (splash != null)
			splash.exit();
		splash = null;

	}

	protected void postConnectImp(String title) {
		int fontSize = config.getIntValue(FONT_SIZE_KEY, DEFAULT_FONT_SIZE);
		String teamName = title;
		format = NumberFormat.getInstance();
		format.setMaximumFractionDigits(PRECISION);
		frame = new JFrame("S.O.S world Namayangar----" + title);
		currentTime = 0;

		inspector = new EntityInspector();
		registerViewers(config);
		JSplitPane split1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getLists(), viewer);
		JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, split1, rightPanel());

		frame.add(split2, BorderLayout.CENTER);

		// final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
		// viewer, rightPanel());
		// CHECKSTYLE:OFF:MagicNumber
		// viewer.setPreferredSize(new Dimension(500, 500));
		// CHECKSTYLE:ON:MagicNumber
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.add(controlPanel(), BorderLayout.SOUTH);
		timeLabel = new JLabel("Time: Not started", JLabel.CENTER);
		JPanel timePanel = getTimePanel();
		teamLabel = new JLabel(teamName, JLabel.CENTER);
		// scoreLabel = new JLabel("Score: Unknown", JLabel.CENTER);
		scoreLabel = new JLabel("-", JLabel.CENTER);
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
		JPanel labels = new JPanel(new GridLayout(1, 3));
		// CHECKSTYLE:ON:MagicNumber
		labels.add(teamLabel);
		labels.add(timePanel);
		labels.add(scoreLabel);
		frame.add(labels, BorderLayout.NORTH);
		frame.pack();
		if (config.getBooleanValue(MAXIMISE_KEY, false)) {
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		}
		frame.setVisible(true);
		split2.setDividerLocation((int) (GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth() * .8d));
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

	private JPanel rightPanel() {
		JPanel panel = new JPanel();

		panel.setPreferredSize(new Dimension(200, 100));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JTabbedPane tabbedPane = new JTabbedPane();

		tabbedPane.addTab("Information", addInfoPanel());
		tabbedPane.addTab("Option", addOptionPanel());
		tabbedPane.addTab("SOSOption", addSOSOptionPanel());
		tabbedPane.addTab("MapConfig", addMapConfigPanel());

		panel.add(tabbedPane);
		return panel;
	}

	private Component addMapConfigPanel() {
		ConfigTree configTree = new ConfigTree(config);
		configTree.setEditable(false);
		return configTree;
	}

	private JPanel addInfoPanel() {
		JScrollPane jsp = new JScrollPane(inspector);
		JPanel totalPanel = new JPanel();
		totalPanel = new JPanel();

		totalPanel.setLayout(new BoxLayout(totalPanel, BoxLayout.Y_AXIS));
		totalPanel.add(new JLabel("Properties"));
		totalPanel.add(jsp);
		totalPanel.add(new JLabel("Total"));

		totalPanel.add(new JScrollPane(m_totalList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		totalPanel.add(new JLabel("Other"));

		return totalPanel;
	}

	private JPanel getLists() {
		JPanel lists = new JPanel(new GridLayout(0, 1));
		commands = new ListModelList<Command>();
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
		return lists;
	}

	private void registerViewers(Config config) {
		viewer = new SOSAnimatedWorldModelViewer();
		viewer.initialise(config);
		viewer.view(model);
		// viewers.add(viewer);
		// tabs.addTab(viewer.getViewerName(),viewer);
		viewer.addViewListener(new ViewListener() {
			@Override
			public void objectsClicked(ViewComponent view, List<RenderedObject> objects) {
				if (objects.size() > 0) {
					if (objects.get(0).getObject() instanceof StandardEntity)
						setSelectedObject((StandardEntity) objects.get(0).getObject());
				} else {
					setSelectedObject(null);
				}
			}

			@Override
			public void objectsRollover(ViewComponent view, List<RenderedObject> objects) {
			}
		});

	}

	private JPanel controlPanel() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setSize(new Dimension(VIEWER_WIDTH, 20));
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		addIntervalEachCycleSlider(panel);
		addIdTextField(panel);
		// addShowCommandBox(panel);
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new GridLayout());
		addZoomPanel(rightPanel);
		mainPanel.add(panel, BorderLayout.WEST);
		mainPanel.add(rightPanel, BorderLayout.EAST);
		return mainPanel;
	}

	private void addIntervalEachCycleSlider(JPanel panel) {
		panel.add(new JLabel(" Interval "));
		intervalEachCycleSlider = new JSlider(JSlider.HORIZONTAL, 0, 4000, 1000);
		intervalEachCycleSlider.setPreferredSize(new Dimension(100, 20));
		intervalEachCycleSlider.setMaximumSize(new Dimension(100, 20));
		intervalEachCycleSlider.setToolTipText(intervalEachCycleSlider.getValue() + " [m sec/cycle]");

		ToolTipManager tipMan = ToolTipManager.sharedInstance();
		tipMan.setInitialDelay(100);
		tipMan.setDismissDelay(2000);

		panel.add(intervalEachCycleSlider);
		intervalEachCycleSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				intervalEachCycleSlider.setToolTipText(intervalEachCycleSlider.getValue() + " [m sec/cycle]");
			}
		});
		panel.add(intervalEachCycleSlider);

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

		panel.add(new JLabel(" Zoom:"));
		panel.add(m_decreaseZoomButton);
		panel.add(m_zoomTextField);
		panel.add(m_increaseZoomButton);
		panel.add(m_resetZoomButton);
		panel.add(m_enableDrag);
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

	public void showTimestep(final int time) {
		try {
			int maxTime = logReader.getMaxTimestep();
			if (/* (!(kvTimestep!=null&&time==kvTimestep.getTime()))&& */(time < 0 || time > maxTime)) {
				if (time < 0)
					m_timeTextField.setText(0 + "");
				else
					m_timeTextField.setText(maxTime + "");

				return;
			}
			Timestep timestep = new Timestep(time);
			currentTime = time;
			m_timeTextField.setText(time + "");
			commands.clear();
			updates.clear();
			Collection<Command> newCommands = null;
			/*
			 * if(kvTimestep!=null&&time==kvTimestep.getTime()){
			 * newCommand=kvTimestep.getCommands(); commands.addAll(newCommand);
			 * }else
			 */
			PerceptionRecord p = logReader.getPerception(time, agentID);
			if (p == null)
				return;

			ChangeSet newChangeSet = p.getChangeSet();
			timestep.registerPerception(agentID, p.getChangeSet(), p.getHearing());
			timestep.setChangeSet(newChangeSet);
			model = logReader.getWorldModel(time);

			if (model != null) {
				logAgents(time);

				if (selectedObject != null)
					setSelectedObject(model.getEntity(selectedObject.getID()));
				if (newChangeSet != null) {
					Collection<Entity> ent = new ArrayList<Entity>();
					for (EntityID entityID : newChangeSet.getChangedEntities())
						ent.add(model.getEntity(entityID));
					updates.addAll(ent);
				}
			}
			// for (ViewComponent viewer : viewers) {
			viewer.view(model, newCommands, newChangeSet, timestep);
			// }
			// scoreLabel.setText("Score: " +
			// format.format(scoreFunction.score(model, timestep)));
			// updateInformation(timestep);
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
		totalList.add("Building:" + model.getEntitiesOfType(StandardEntityURN.BUILDING).size());
		totalList.add("Road:" + model.getEntitiesOfType(StandardEntityURN.ROAD).size());
		totalList.add("Fire Brigade:" + model.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE).size());
		totalList.add("Police Force:" + model.getEntitiesOfType(StandardEntityURN.POLICE_FORCE).size());
		totalList.add("Ambulance Team:" + model.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM).size());
		totalList.add("Centers:" + model.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.FIRE_STATION, StandardEntityURN.POLICE_OFFICE).size());
		totalList.add("Refuges:" + model.getEntitiesOfType(StandardEntityURN.REFUGE).size());

		m_totalList.setListData(totalList);
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
		selectAllFB.addActionListener(new ActionListener() {

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
		selectAllFB.addActionListener(new ActionListener() {

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

	public void setSelectedObject(StandardEntity selectedObject) {
		if (this.selectedObject != null)
			this.selectedObject.setSelected(false);

		this.selectedObject = selectedObject;
		if (selectedObject != null) {
			selectedObject.setSelected(true);

		}
		for (ViewLayer layers : viewer.getLayers()) {
			if (layers instanceof SOSAbstractSelectedLayer) {
				if (multipleSelection.isSelected())
					((SOSAbstractSelectedLayer) layers).addSelected(selectedObject);
				else
					((SOSAbstractSelectedLayer) layers).selected(selectedObject);
			}
		}
		inspector.inspect(selectedObject);
		// for (ViewComponent viewer : viewers) {
		viewer.repaint();
		// }
	}

	private JPanel getTimePanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panel.setBackground(Color.white);
		// m_timeTextField = new JTextField("0", 3);
		m_timeTextField.setFont(m_timeTextField.getFont().deriveFont(Font.PLAIN, 20));

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
		final JToggleButton m_playTimeButton = new JToggleButton(">", true);

		m_stopTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_playTimeButton.setSelected(false);
				m_stopTimeButton.setSelected(true);
				isPaused = true;
			}
		});

		ActionListener act;
		m_playTimeButton.addActionListener(act = new ActionListener() {
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
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						int tmp = 0;
						while (m_playTimeButton.isSelected()) {
							try {
								Thread.sleep(intervalEachCycleSlider.getValue() / 5);
							} catch (InterruptedException e1) {
								Logger.warn("Player interrupted", e1);
							}
							if (++tmp % 5 == 0)
								showTimestep(getCurrentTime() + 1);
						}
					}
				});
				t.start();
			}
		});
		act.actionPerformed(null);
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
				m_timeTextField.setFocusable(true);
			}

			public void focusGained(FocusEvent arg0) {
				// setM_updateTimeTextBox(false);

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
		panel.add(m_playTimeButton);
		panel.add(m_forwardTimeButton);
		frame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent arg0) {
			}

			@Override
			public void windowIconified(WindowEvent arg0) {
			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
				m_playTimeButton.setSelected(false);
			}

			@Override
			public void windowClosing(WindowEvent arg0) {
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
			}

			@Override
			public void windowActivated(WindowEvent arg0) {
			}
		});
		return panel;

	}

	@Override
	protected void handleTimestep(final KVTimestep t) {
		// if(currentTime==t.getTime()-1)

		// super.handleTimestep(t);
		// kvTimestep = t;
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

	public static void main(String[] args) {
		Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
		Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
		Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
		try {
			Config config = new Config(new File("boot/config"));
			args = CommandLineOptions.processArgs(args, config);
			config.setValue("kernel.logname", "/home/ali/Desktop/scriptrunlogs/maps2011/Kobe3/rescue.soslog");
			new AgentWorldModelViewer(config, new EntityID(35431), "test");
		} catch (ConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
