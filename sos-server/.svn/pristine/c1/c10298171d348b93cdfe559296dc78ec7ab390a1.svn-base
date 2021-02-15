package sosNamayangar;

import static rescuecore2.misc.java.JavaTools.instantiate;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ToolTipManager;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.log.CommandsRecord;
import rescuecore2.log.FileLogReader;
import rescuecore2.log.LogException;
import rescuecore2.log.LogReader;
import rescuecore2.log.UpdatesRecord;
import rescuecore2.messages.Command;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.misc.gui.ListModelList;
import rescuecore2.misc.java.LoadableTypeProcessor;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.StandardMessageFactory;
import rescuecore2.view.EntityInspector;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;

/**
 * A class for viewing log files.
 */
public class SOSLogViewerOld extends JFrame implements ViewListener {
	private static final long serialVersionUID = 1L;

	private static final String VIEWERS_KEY = "log.viewers";

	private LogReader log;
	private EntityInspector inspector;
	private JList commandsList;
	private JList updatesList;
	private ListModelList<Command> commands;
	private ListModelList<Entity> updates;
	private ArrayList<ViewComponent> viewers;
	private int maxTime;
	private StandardEntity selectedObject;
	// /////////S.O.S////////////////
	private static final int FONT_SIZE = 20;
	static final int VIEWER_WIDTH = 1024;
	static final int VIEWER_HEIGHT = 768;
	// ////////S.O.S/////////
	private boolean m_updateTimeTextBox = true;
	private JLabel m_statusLabel = new JLabel();
	// private JCheckBox m_closeCheckBox = new JCheckBox("Close at the end",
	// false);
	private JTextField m_timeTextField = new JTextField("0", 3);
	private JToggleButton m_viewSay = new JToggleButton("Say");
	private JToggleButton m_viewSense = new JToggleButton("Sense", true);
	private JToggleButton m_viewLiveCivilian = new JToggleButton("LiveCiv", true);
	private JToggleButton m_viewDeadCivilian = new JToggleButton("DeadCiv", true);
	private JToggleButton m_viewPoliceForce = new JToggleButton("PF", true);
	private JToggleButton m_viewPoliceOffice = new JToggleButton("PO", true);
	private JToggleButton m_viewFireBrigade = new JToggleButton("FB", true);
	private JToggleButton m_viewFireStation = new JToggleButton("FS", true);
	private JToggleButton m_viewAmbulanceTeam = new JToggleButton("AT", true);
	private JToggleButton m_viewAmbulanceCenter = new JToggleButton("AC", true);
	private JToggleButton m_viewBlockades = new JToggleButton("BL", true);

	private JToggleButton m_viewMotionLessSense = new JToggleButton("MSense", false);
	private JToggleButton m_viewMotionLessSay = new JToggleButton("MSay", false);

	private JToggleButton m_viewCluster = new JToggleButton("VC", true);

	public JTextField m_viewClusterNum = new JTextField();
	private JPanel m_commandBox = new JPanel();
	private JProgressBar m_cycleProgressBar;
	JList m_totalList = new JList();

	private JTextArea commandsPane = new JTextArea(4, 0);
	private JTextField commandLine = new JTextField();

	// ////

	private ViewComponent viewer;
	private JLabel timeLabel;
	private static Splash splash;

	public static void showSplash() {
		splash = new Splash(1000);
		splash.showSplash("S.O.S Log Namayangar");
	}

	// ////////////////////////////////////////////////////
	HashMap<Integer, StandardWorldModel> timesModel = new HashMap<Integer, StandardWorldModel>();
	HashMap<Integer, CommandsRecord > timesCommand = new HashMap<Integer, CommandsRecord>();
	HashMap<Integer, UpdatesRecord> timesUpdate = new HashMap<Integer, UpdatesRecord>();
	int timeHasBeenRead=0;
	/**
	 * Construct a LogViewer.
	 * 
	 * @param reader
	 *            The LogReader to read.
	 * @param config
	 *            The system configuration.
	 * @param string
	 * @throws LogException
	 *             If there is a problem reading the log.
	 * @throws InterruptedException
	 */
	public SOSLogViewerOld(LogReader reader, Config config, String title) throws LogException, InterruptedException {
		super(title);
		try {
			this.log = reader;
			maxTime = log.getMaxTimestep();
			timeLabel = new JLabel("Time: Not started", JLabel.CENTER);
			timeLabel.setBackground(Color.WHITE);
			timeLabel.setOpaque(true);
			timeLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, FONT_SIZE));

			inspector = new EntityInspector();
			registerViewers(config);

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

			JTabbedPane tabs = new JTabbedPane();
			for (ViewComponent next : viewers) {
				tabs.addTab(next.getViewerName(), next);
				next.addViewListener(new ViewListener() {
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
				viewer = next;
			}
			JSplitPane split1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lists, tabs);
			JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, split1, rightPanel());

			add(split2, BorderLayout.CENTER);

			setDefaultCloseOperation(EXIT_ON_CLOSE);

			add(split2, BorderLayout.CENTER);
			add(statusPanel(), BorderLayout.NORTH);
			add(bottomPanel(), BorderLayout.SOUTH);

			setSize(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(), GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight() - 50);
			for (ViewComponent viewer : viewers) {
				viewer.addViewListener(this);
			}

			pack();
			setExtendedState(JFrame.MAXIMIZED_BOTH);
			setVisible(true);

			split1.setDividerLocation(.2);
			split2.setDividerLocation(.8);

			new Thread(new Runnable() {
				public void run() {
					try {
						for (int time = 0; time < 100; time++) {
							StandardWorldModel model=StandardWorldModel.createStandardWorldModel((WorldModel<? extends Entity>)log.getWorldModel(time));
							timesModel.put(time,model);
							timesCommand.put(time, log.getCommands(time));
							timesUpdate.put(time,log.getUpdates(time));
							timeHasBeenRead=time;
							System.out.println(time);
						}
					} catch (LogException e) {
						JOptionPane.showMessageDialog(null, e, "Error", JOptionPane.ERROR_MESSAGE);
						e.printStackTrace();
					}
				}
			}).start();
			System.out.println("S");
//			m_cycleProgressBar.setValue(0);
			splash.exit();
			splash = null;
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Connecting to SOS Namayangar have Error", "Error", 0);
			if (splash != null)
				splash.exit();
		}

	}

	JToggleButton m_stopTimeButton;
	JToggleButton m_playTimeButton;
	private JSlider intervalEachCycleSlider;

	private WorldModel<? extends Entity> model;

	private JPanel statusPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panel.setBackground(Color.white);
		m_statusLabel.setFont(m_statusLabel.getFont().deriveFont(Font.PLAIN, 20));
		m_timeTextField.setFont(m_statusLabel.getFont().deriveFont(Font.PLAIN, 20));

		// m_closeCheckBox.setBackground(Color.white);
		JButton m_backTimeButton = new JButton("<<");
		m_backTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int value = m_cycleProgressBar.getValue() - 1;
				if (value >= 0) {
					m_cycleProgressBar.setValue(value);
				}
			}
		});
		m_stopTimeButton = new JToggleButton("||", true);
		m_playTimeButton = new JToggleButton(">", false);

		m_stopTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_playTimeButton.setSelected(false);
				m_stopTimeButton.setSelected(true);
			}
		});

		m_playTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleTimestep();
				m_stopTimeButton.setSelected(false);
				m_playTimeButton.setSelected(true);

			}
		});
		JButton m_forwardTimeButton = new JButton(">>");
		m_forwardTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int value = m_cycleProgressBar.getValue() + 1;
				if (value <= maxTime) {
					m_cycleProgressBar.setValue(value);
				}
			}
		});
		m_timeTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					int value = Integer.parseInt(m_timeTextField.getText());
					if (value < 0)
						value = 0;
					if (value > maxTime)
						value = maxTime;
					m_cycleProgressBar.setValue(value);
				} catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(null, "Enter numbers only");
				}

				m_timeTextField.setFocusable(false);
			}
		});
		m_timeTextField.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				setM_updateTimeTextBox(true);
				m_timeTextField.setFocusable(true);
			}

			public void focusGained(FocusEvent e) {
				setM_updateTimeTextBox(false);

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
		panel.add(m_statusLabel);
		// panel.add(m_closeCheckBox);

		return panel;
	}

	public void setM_updateTimeTextBox(boolean m_updateTimeTextBox) {
		this.m_updateTimeTextBox = m_updateTimeTextBox;
	}

	public boolean isM_updateTimeTextBox() {
		return m_updateTimeTextBox;
	}

	private Component bottomPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(commandBox(), BorderLayout.CENTER);
		panel.add(controlPanel(), BorderLayout.SOUTH);
		return panel;
	}

	private JPanel commandBox() {
		m_commandBox.setLayout(new BorderLayout());
		m_commandBox.setBorder(new TitledBorder(new LineBorder(Color.white, 2, true), "Command Box"));
		commandsPane.setAutoscrolls(true);
		commandsPane.setEditable(false);
		commandLine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!commandLine.getText().trim().equals("")) {
					commandsPane.setText(commandsPane.getText() + "\n" + commandLine.getText());
					commandLine.setText("");
				}
			}
		});

		m_commandBox.add(new JScrollPane(commandsPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
		m_commandBox.add(commandLine, BorderLayout.SOUTH);
		return m_commandBox;
	}

	private JPanel controlPanel() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setSize(new Dimension(VIEWER_WIDTH, 20));
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		addCycleProgressBar(panel);
		addIntervalEachCycleSlider(panel);
		addIdTextField(panel);
		addShowCommandBox(panel);
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new GridLayout());
		addZoomPanel(rightPanel);
		mainPanel.add(panel, BorderLayout.WEST);
		mainPanel.add(rightPanel, BorderLayout.EAST);
		return mainPanel;
	}

	private void addCycleProgressBar(JPanel panel) {
		m_cycleProgressBar = new JProgressBar(0, maxTime);

		m_cycleProgressBar.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						showTimestep(m_cycleProgressBar.getValue());	
					}
				}).start();
				if (isM_updateTimeTextBox())
					m_timeTextField.setText(m_cycleProgressBar.getValue() + "");
				m_cycleProgressBar.setString(m_cycleProgressBar.getValue() + "/" + maxTime);
				m_cycleProgressBar.setToolTipText(m_cycleProgressBar.getValue() + "/" + maxTime);
			}
		});
		m_cycleProgressBar.setStringPainted(true);

		panel.add(m_cycleProgressBar);

	}

	private void addIntervalEachCycleSlider(JPanel panel) {
		panel.add(new JLabel(" Interval "));
		intervalEachCycleSlider = new JSlider(JSlider.HORIZONTAL, 0, 2000, 100);
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

	private void addShowCommandBox(JPanel panel) {
		final JToggleButton showCommandBox = new JToggleButton("Show Command Box", false);
		m_commandBox.setVisible(showCommandBox.isSelected());
		showCommandBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_commandBox.setVisible(showCommandBox.isSelected());
			}
		});
		panel.add(showCommandBox);
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

	private JPanel rightPanel() {
		JPanel panel = new JPanel();

		panel.setPreferredSize(new Dimension(200, 100));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JTabbedPane tabbedPane = new JTabbedPane();

		tabbedPane.addTab("Information", addInfoPanel());
		tabbedPane.addTab("Option", addOptionPanel());

		panel.add(tabbedPane);
		return panel;
	}

	private JPanel addOptionPanel() {

		JPanel optionPanel = new JPanel();
		JPanel jp = new JPanel();
		jp.setLayout(new GridLayout(7, 2, 2, 2));
		jp.setBorder(new TitledBorder(new LineBorder(Color.white, 2, true), "View Setting"));
		jp.setPreferredSize(new Dimension(150, 300));

		ActionListener mvActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewer.repaint();
			}
		};
		ActionListener mlActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewer.repaint();
			}
		};
		m_viewSay.addActionListener(mvActionListener);
		m_viewSense.addActionListener(mvActionListener);
		m_viewMotionLessSay.addActionListener(mvActionListener);
		m_viewMotionLessSense.addActionListener(mvActionListener);
		m_viewPoliceForce.addActionListener(mvActionListener);
		m_viewPoliceOffice.addActionListener(mlActionListener);
		m_viewFireBrigade.addActionListener(mvActionListener);
		m_viewFireStation.addActionListener(mlActionListener);
		m_viewAmbulanceTeam.addActionListener(mvActionListener);
		m_viewAmbulanceCenter.addActionListener(mlActionListener);
		m_viewLiveCivilian.addActionListener(mvActionListener);
		m_viewDeadCivilian.addActionListener(mvActionListener);
		m_viewCluster.addActionListener(mlActionListener);
		m_viewClusterNum.addActionListener(mlActionListener);

		jp.add(m_viewSay);
		jp.add(m_viewSense);
		jp.add(m_viewPoliceForce);
		jp.add(m_viewPoliceOffice);
		jp.add(m_viewFireBrigade);
		jp.add(m_viewFireStation);
		jp.add(m_viewAmbulanceTeam);
		jp.add(m_viewAmbulanceCenter);
		jp.add(m_viewLiveCivilian);
		jp.add(m_viewDeadCivilian);
		jp.add(m_viewBlockades);
		jp.add(m_viewMotionLessSense);
		jp.add(m_viewMotionLessSay);
		optionPanel.add(jp);

		JPanel jp1 = new JPanel();
		jp1.setLayout(new GridLayout(1, 3, 2, 2));
		jp1.setBorder(new TitledBorder(new LineBorder(Color.white, 2, true), "S.O.S Tools"));
		jp1.setPreferredSize(new Dimension(100, 60));
		jp1.add(new JLabel("cluster"));
		jp1.add(m_viewClusterNum);
		jp1.add(m_viewCluster);

		return optionPanel;
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

	/**
	 * @param time
	 *            The timestep to show. If this value is out of range then this
	 *            method will silently return. Show a particular timestep in the
	 *            viewer.
	 */
	public void showTimestep(int time) {
			if (time < 0 || time > maxTime) {
				return;
			}
			commands.clear();
			updates.clear();
			while(time>timeHasBeenRead)
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			CommandsRecord commandsRecord = timesCommand.get(time);
			if (commandsRecord != null) {
				commands.addAll(commandsRecord.getCommands());
			}
			UpdatesRecord updatesRecord = timesUpdate.get(time);
			/*
			 * if (updatesRecord != null) {
			 * updates.addAll(updatesRecord.getChangeSet()); }
			 */
			model = timesModel.get(time);
			System.out.println("Viewing Time:"+time);
			for (ViewComponent next : viewers) {
				next.view(model, commandsRecord == null ? null : commandsRecord.getCommands(), updatesRecord == null ? null : updatesRecord.getChangeSet());
			}
	}

	private void registerViewers(Config config) {
		viewers = new ArrayList<ViewComponent>();
		for (String next : config.getArrayValue(VIEWERS_KEY, null)) {
			ViewComponent viewer = instantiate(next, ViewComponent.class);
			if (viewer != null) {
				viewer.initialise(config);
				viewers.add(viewer);
			}
		}
	}

	public void setSelectedObject(StandardEntity selectedObject) {
		if (this.selectedObject != null)
			this.selectedObject.setSelected(false);

		this.selectedObject = selectedObject;
		if (selectedObject != null) {
			selectedObject.setSelected(true);
		}
		inspector.inspect(selectedObject);
		for (ViewComponent viewer : viewers) {
			viewer.repaint();
		}
	}

	protected void handleTimestep() {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				while (m_playTimeButton.isSelected()) {
					int value = m_cycleProgressBar.getValue() + 1;
					if (value <= maxTime) {
						m_cycleProgressBar.setValue(value);
					} else
						m_playTimeButton.setSelected(false);
					try {
						Thread.sleep(intervalEachCycleSlider.getValue());
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}

				}
			}
		};
		// SwingUtilities.invokeLater(r);
		new Thread(r).start();

	}

	/**
	 * Launch a new LogViewer.
	 * 
	 * @param args
	 *            Command line arguments. Accepts only one argument: the name of
	 *            a log file.
	 */
	public static void main(String[] args) {
		System.out.println(Arrays.asList(args));
		showSplash();
		Config config = new Config();
		try {
			args = CommandLineOptions.processArgs(args, config);
			if (args.length != 1) {
				printUsage();
				return;
			}
			String name = args[0];
            Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
            Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
            Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);

			processJarFiles(config);
			LogReader reader = new FileLogReader(name, Registry.SYSTEM_REGISTRY);
			/* SOSLogViewer viewer = */new SOSLogViewerOld(reader, config, "SOS Log Namayangar: " + name);

		} catch (IOException e) {
			System.err.println("Error reading log");
			e.printStackTrace();
		} catch (ConfigException e) {
			System.err.println("Configuration error");
			e.printStackTrace();
		} catch (LogException e) {
			System.err.println("Error reading log");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void printUsage() {
		System.out.println("Usage: LogViewer <filename>");
	}

	private static void processJarFiles(Config config) throws IOException {
		LoadableTypeProcessor processor = new LoadableTypeProcessor(config);
		processor.addFactoryRegisterCallbacks(Registry.SYSTEM_REGISTRY);
		processor.process();
	}

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
}
