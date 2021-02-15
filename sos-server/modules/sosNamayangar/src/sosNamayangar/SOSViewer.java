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
import java.awt.Label;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import kernel.ui.KernelControlPanel;
import kernel.ui.KernelControlPanel.KernelControlPanelListener;
import rescuecore2.Constants;
import rescuecore2.Timestep;
import rescuecore2.messages.control.KVTimestep;
import rescuecore2.misc.gui.ConfigTree;
import rescuecore2.score.CompositeScoreFunction;
import rescuecore2.score.ScoreFunction;
import rescuecore2.standard.components.StandardViewer;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.score.DiscoveryScoreFunction;
import rescuecore2.standard.score.DistanceTravelledScoreFunction;
import rescuecore2.standard.score.HealthScoreFunction;
import rescuecore2.view.EntityInspector;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.SOSImageIcon;
import rescuecore2.view.SOSViewListener;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewLayer;
import rescuecore2.worldmodel.EntityID;
import sosNamayangar.layers.CustomLayer;
import sosNamayangar.layers.SOSAbstractSelectedLayer;
import sosNamayangar.message_decoder.channelDistribution.Channel;
import sosNamayangar.message_decoder.channelDistribution.MessageConfig;
import sosNamayangar.sos_score.SearchScoreFunction;

/**
 * @author Ali
 */
public class SOSViewer extends StandardViewer implements SOSViewListener {
	private static ImageIcon ICON=new SOSImageIcon(SOSViewer.class.getClassLoader().getResource("sosNamayangar/viewer.png"));
	private static final int DEFAULT_FONT_SIZE = 20;
	private static final int PRECISION = 3;
	private static final String FONT_SIZE_KEY = "viewer.font-size";
	private static final String TEAM_NAME_KEY = "viewer.team-name";
	private JLabel scoreLabel;
	private JLabel teamLabel;
	private NumberFormat format;
	private ScoreFunction scoreFunction;
	ArrayList<ScoreFunction> sosScores = new ArrayList<ScoreFunction>();
	static final int VIEWER_WIDTH = 1024;
	static final int VIEWER_HEIGHT = 768;
	// ////////S.O.S/////////
	JFrame frame;


	private boolean m_updateTimeTextBox = true;
	private JLabel m_statusLabel = new JLabel();
	private JCheckBox m_closeCheckBox = new JCheckBox("Close at the end", false);
	private JTextField m_timeTextField = new JTextField("0", 3);

	private JPanel m_commandBox = new JPanel();

	private JProgressBar m_cycleProgressBar = new JProgressBar(0, 300);
	JList<String> m_totalList = new JList<String>();

	private JTextArea commandsPane = new JTextArea(4, 0);
	private JTextField commandLine = new JTextField();
	// //
	private JLabel jlX = new JLabel("X");
	private JLabel jlY = new JLabel("Y");
	private JTextField jtX = new JTextField(10);
	private JTextField jtY = new JTextField(10);

	// ////

	private SOSAnimatedWorldModelViewer viewer;
	private JLabel timeLabel;
	StandardEntity selectedObject;
	private EntityInspector inspector = new EntityInspector();
	private int fontSize;
	private String teamName;
	private JSplitPane split;
	private Component rightPanel;
	private static Splash splash;

	public static void showSplash() {
		splash = new Splash(1000);
		splash.showSplash();
	}

	@Override
	protected void postConnect() {
		try {
//			setToolkit();
			showSplash();
			super.postConnect();
			fontSize = config.getIntValue(FONT_SIZE_KEY, DEFAULT_FONT_SIZE);
			teamName = config.getValue(TEAM_NAME_KEY, "") + "       ";
			scoreFunction = makeScoreFunction();
			format = NumberFormat.getInstance();
			format.setMaximumFractionDigits(PRECISION);
			scoreLabel = new JLabel("Score: Unknown"+"     ", JLabel.RIGHT);
			frame = new JFrame("S.O.S Namayangar");
			frame.setIconImage(ICON.getImage());
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			viewer = new SOSAnimatedWorldModelViewer();
//
			viewer.initialise(config);
			viewer.view(model);
			frame.add(bottomPanel(), BorderLayout.SOUTH);//
			split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewer, rightPanel=rightPanel());
//
			frame.add(split, BorderLayout.CENTER);
			frame.add(statusPanel(), BorderLayout.NORTH);

			viewer.setPreferredSize(new Dimension(VIEWER_WIDTH, VIEWER_HEIGHT));
//
			frame.setSize(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(), GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDisplayMode().getHeight() - 50);
//
			viewer.addViewListener(this);
			frame.pack();
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			frame.setVisible(true);
			enableFullscreen(frame);
			frame.setAlwaysOnTop(true);
			frame.setAlwaysOnTop(false);
			frame.revalidate();
			split.setDividerLocation((int) (GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth() * .8d));

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
	private JToggleButton multipleSelection;

	private JPanel statusPanel() {
//		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//		panel.setBackground(Color.white);
		m_statusLabel.setFont(m_statusLabel.getFont().deriveFont(Font.PLAIN, 20));
		m_timeTextField.setFont(m_statusLabel.getFont().deriveFont(Font.PLAIN, 20));
		final JLabel statusLabel=new JLabel("       ");
		m_closeCheckBox.setBackground(Color.white);
		// setStatus();

		final JButton m_stepTimeButton = new JButton("Step");
		m_stepTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				int a = Integer.parseInt(m_timeTextField.getText()) - 1;
//				if (a >= 0) {
//					m_timeTextField.setText(a + "");
//				}
				statusLabel.setText(KernelControlPanel.stepButtonPressed());
			}
		});
		m_stopTimeButton = new JToggleButton("||", false);
		m_playTimeButton = new JToggleButton(">",true);

		m_stopTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_playTimeButton.setSelected(false);
				m_stopTimeButton.setSelected(true);
			}
		});

		m_playTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
				} catch (NumberFormatException nfe) {
				}
				m_stopTimeButton.setSelected(false);
				m_playTimeButton.setSelected(true);
			}
		});
		final JButton m_startTimeButton = new JButton("Run");
		m_startTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				statusLabel.setText(KernelControlPanel.startButtonPressed());
			}
		});
		final JButton m_stopTimeButton = new JButton("Stop");
		m_stopTimeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				statusLabel.setText(KernelControlPanel.stopButtonPressed());
			}
		});
		m_timeTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
				} catch (NumberFormatException nfe) {
				}

				m_timeTextField.setFocusable(false);
			}
		});
		m_timeTextField.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent arg0) {
				setM_updateTimeTextBox(true);
				m_timeTextField.setFocusable(true);
			}

			public void focusGained(FocusEvent arg0) {
				setM_updateTimeTextBox(false);

			}
		});


		m_stepTimeButton.setToolTipText("Step");
		m_stopTimeButton.setToolTipText("Stop");
		m_timeTextField.setToolTipText("Time");
//		m_playTimeButton.setToolTipText("Play");
		m_startTimeButton.setToolTipText("Run/Stop");
		JPanel control=new JPanel();
		control.setBackground(Color.white);
		control.add(statusLabel);
		if(KernelControlPanel.isValidForKernelStatusChanging())
			control.add(m_stepTimeButton);
//		panel.add(m_stopTimeButton);
		control.add(m_timeTextField);
		control.add(new Label("/"+config.getIntValue("kernel.timesteps")));
//		panel.add(m_playTimeButton);
		if(KernelControlPanel.isValidForKernelStatusChanging()){
		control.add(m_stopTimeButton);
		m_stopTimeButton.setEnabled(false);
		control.add(m_startTimeButton);
		KernelControlPanel.addListener(new KernelControlPanelListener() {

			@Override
			public void eventProcess(Status s) {
				switch (s) {
				case Finished:
					m_stopTimeButton.setEnabled(false);
					m_stepTimeButton.setEnabled(false);
					m_startTimeButton.setEnabled(false);
					break;
				case Start:
					m_stopTimeButton.setEnabled(true);
					m_stepTimeButton.setEnabled(false);
					m_startTimeButton.setEnabled(false);
					break;
				case StepEnd:
					m_stopTimeButton.setEnabled(false);
					m_stepTimeButton.setEnabled(true);
					m_startTimeButton.setEnabled(true);
					m_stepTimeButton.setText("step");
					break;
				case StepStart:
					m_stopTimeButton.setEnabled(false);
					m_stepTimeButton.setEnabled(false);
					m_startTimeButton.setEnabled(false);
					m_stepTimeButton.setText("working");
					break;
				case Stop:
					m_stopTimeButton.setEnabled(false);
					m_stepTimeButton.setEnabled(true);
					m_startTimeButton.setEnabled(true);
					break;

				}
			}
		});
		}
		// panel.add(m_statusLabel);
		// panel.add(m_closeCheckBox);
		timeLabel = new JLabel("Time: Not started", JLabel.CENTER);
		teamLabel = new JLabel("       "+teamName, JLabel.CENTER);
		scoreLabel = new JLabel("Score: "+scoreFunction.score(model, new Timestep(0))+"     ", JLabel.CENTER);
		timeLabel.setBackground(Color.WHITE);
		timeLabel.setOpaque(true);
		timeLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, fontSize));
		teamLabel.setBackground(Color.WHITE);
		teamLabel.setOpaque(true);
		teamLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, fontSize));
		scoreLabel.setBackground(Color.WHITE);
		scoreLabel.setOpaque(true);
		scoreLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, fontSize));
		// CHECKSTYLE:OFF:MagicNumber

		JPanel labels = new JPanel(new BorderLayout());
		// CHECKSTYLE:ON:MagicNumber
		labels.add(teamLabel,BorderLayout.WEST);
		labels.add(control,BorderLayout.CENTER);
		labels.add(scoreLabel,BorderLayout.EAST);

		return labels;
	}

	private Component bottomPanel() {
		JPanel panel = new JPanel(new BorderLayout());
//		panel.add(commandBox(), BorderLayout.CENTER);
		panel.add(controlPanel(), BorderLayout.SOUTH);
		return panel;
	}

//	private JPanel commandBox() {
//		m_commandBox.setLayout(new BorderLayout());
//		m_commandBox.setBorder(new TitledBorder(new LineBorder(Color.white, 2, true), "Command Box"));
//		commandsPane.setAutoscrolls(true);
//		commandsPane.setEditable(false);
//		commandLine.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				if (!commandLine.getText().trim().equals("")) {
//					commandsPane.setText(commandsPane.getText() + "\r\n" + commandLine.getText());
//					commandLine.setText("");
//				}
//			}
//		});
//
//		m_commandBox.add(new JScrollPane(commandsPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
//		m_commandBox.add(commandLine, BorderLayout.SOUTH);
//		return m_commandBox;
//	}

	private JPanel controlPanel() {
		jtX.setEditable(false);
		jtY.setEditable(false);
		jtX.setVisible(true);
		jtY.setVisible(true);
		jtX.setFont(new Font("Arial", Font.BOLD, 12));
		jtY.setFont(new Font("Arial", Font.BOLD, 12));
		JPanel jp = new JPanel(new GridLayout(2, 1));
		// jp.setSize(new Dimension(30, 20));
		jp.add(jtX);
		jp.add(jtY);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setSize(new Dimension(VIEWER_WIDTH, 20));
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(jp);
		// panel.add(m_cycleProgressBar);
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
		return mainPanel;
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
	private void addIntervalEachCycleSlider(JPanel panel) {
		intervalEachCycleSlider = new JSlider(0, Math.max(config.getIntValue("kernel.agents.think-time"), 2000),config.getIntValue("kernel.agents.think-time"));
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
		viewer.addLayer(new CustomLayer(m_commandBox));
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
				setSelectedObject(model.getEntity(new EntityID(Integer.parseInt(idTextField.getText()))));
			}
		});
		panel.add(idTextField);

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

	private Component rightPanel() {
		JPanel panel = new JPanel();

		panel.setPreferredSize(new Dimension(200, 100));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		final JTabbedPane tabbedPane = new JTabbedPane();

		tabbedPane.addTab("Information", addInfoPanel());
		tabbedPane.addTab("Option", addOptionPanel());
		tabbedPane.addTab("SOSOption", addSOSOptionPanel());
		tabbedPane.addTab("MapConfig", addMapConfigPanel());
		tabbedPane.addTab("Communication", addCommunicationPanel());
		panel.add(tabbedPane);
		panel.setVisible(false);
		return panel;

	}

	private Component addCommunicationPanel() {
		Vector<String> string=new Vector<>();
		MessageConfig mconfig=new MessageConfig(config);
		for (Channel channel : mconfig.getAllChannels().values()) {
			string.add(channel.toString());
		}
		return new JList<String>(string);
	}

	private Component addMapConfigPanel() {
		ConfigTree configTree = new ConfigTree(config);
		configTree.setEditable(false);
		return configTree;
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
		JPanel jp = new JPanel();
		jp.setLayout(new GridLayout(viewer.getLayers().size(), 1, 2, 2));
		jp.setBorder(new TitledBorder(new LineBorder(Color.white, 2, true), "View Setting"));
		jp.setPreferredSize(new Dimension(150, 300));
		for (final ViewLayer layer : viewer.getLayers()) {
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

		JPanel jp1 = new JPanel();
		jp1.setLayout(new GridLayout(1, 3, 2, 2));
		jp1.setBorder(new TitledBorder(new LineBorder(Color.white, 2, true), "HDF Tools"));
		jp1.setPreferredSize(new Dimension(100, 60));
		jp1.add(new JLabel("cluster"));

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
		updateInformation(new Timestep(0));
		totalPanel.add(new JScrollPane(m_totalList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		totalPanel.add(new JLabel("Other"));

		return totalPanel;
	}

	@Override
	protected void handleTimestep(final KVTimestep t) {
		super.handleTimestep(t);
		inspector.repaint();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				if (m_playTimeButton.isSelected()) {
					timeLabel.setText("Time: " + t.getTime());
					m_timeTextField.setText(t.getTime() + "");
					Timestep timestep = new Timestep(t.getTime());
					timestep.setChangeSet(t.getChangeSet());
					timestep.setCommands(t.getCommands());
					updateInformation(timestep);
					viewer.view(model, t.getCommands(), timestep,this);
					viewer.repaint();
					scoreLabel.setText("Score: " + format.format(scoreFunction.score(model, timestep))+"     ");
				}
			}
		});
	}

	@Override
	public String toString() {
		return "S.O.S Namayangar";
	}

	@Override
	public void objectsClicked(ViewComponent view, List<RenderedObject> objects) {
	}

	@Override
	public void objectsClicked(ViewComponent view, List<RenderedObject> objects, MouseEvent e) {
		System.out.println("X:" + viewer.getTransform().screenToX(e.getX()) + "Y:" + viewer.getTransform().screenToY(e.getY()));
		if (objects.size() > 0) {
			if ((e.getButton() == MouseEvent.BUTTON3))
				showMenuSelectObject(view, objects, e);
			else if (objects.get(0).getObject() instanceof StandardEntity) {
				setSelectedObject((StandardEntity) objects.get(0).getObject());
			}
		} else {
			setSelectedObject(null);
		}

	}

	private void showMenuSelectObject(ViewComponent view, List<RenderedObject> objects, MouseEvent e) {

		JPopupMenu popupMenu = new JPopupMenu();
		for (RenderedObject renderObject : objects) {
			JMenuItem menuItem = new JMenuItem(renderObject.toString());
			menuItem.setToolTipText(renderObject.toString());
			menuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Object s = e.getSource();
					if (s instanceof JMenuItem) {
						JMenuItem source = (JMenuItem) s;
						StringTokenizer st = new StringTokenizer(source.getText(), " ");
						/* String menuID = */st.nextToken();
						for (StandardEntity stEnt : model.getAllEntities()) {
							if (stEnt.toString().equals(source.getToolTipText()))
								setSelectedObject(stEnt);
						}

					}
				}
			});
			popupMenu.add(menuItem);
		}
		popupMenu.show(viewer, e.getX(), e.getY());// viewer, objs.get(0)., y)
		// if (popupMenu.isPopupTrigger(e))
		viewer.repaint();
	}

	static int idcounter = 100000;

	@Override
	public void objectsRollover(ViewComponent view, List<RenderedObject> objects, MouseEvent e) {
		jtX.setText("x: " + format.format(viewer.getTransform().screenToX(e.getX())));
		jtY.setText("y: " + format.format(viewer.getTransform().screenToY(e.getY())));
	}

	@Override
	public void objectsRollover(ViewComponent view, List<RenderedObject> objects) {

	}

	public void setSelectedObject(StandardEntity selectedObject) {
		if (this.selectedObject != null)
			this.selectedObject.setSelected(false);

		this.selectedObject = selectedObject;
		if (selectedObject != null) {
			selectedObject.setSelected(true);
		}
		inspector.inspect(selectedObject);
		for (ViewLayer layers : viewer.getLayers()) {
			if (layers instanceof SOSAbstractSelectedLayer) {
				if (multipleSelection.isSelected())
					((SOSAbstractSelectedLayer) layers).addSelected(selectedObject);
				else
					((SOSAbstractSelectedLayer) layers).selected(selectedObject);
			}
		}
		viewer.repaint();
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

		Collection<StandardEntity> civ = model.getEntitiesOfType(StandardEntityURN.CIVILIAN);
		int dead = 0;
		for (StandardEntity standardEntity : civ) {
			dead += ((Civilian) standardEntity).getHP() == 0 ? 1 : 0;
		}
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
		m_totalList.setListData(totalList);
	}

	public void setM_updateTimeTextBox(boolean m_updateTimeTextBox) {
		this.m_updateTimeTextBox = m_updateTimeTextBox;
	}

	public boolean isM_updateTimeTextBox() {
		return m_updateTimeTextBox;
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

	private static void setToolkit() {
		try {
			Toolkit xToolkit = Toolkit.getDefaultToolkit();
			java.lang.reflect.Field awtAppClassNameField;
			awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
			awtAppClassNameField.setAccessible(true);
			awtAppClassNameField.set(xToolkit, "Viewer");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public int getIntervalEachCycle() {

		return intervalEachCycleSlider.getValue();
	}
}
