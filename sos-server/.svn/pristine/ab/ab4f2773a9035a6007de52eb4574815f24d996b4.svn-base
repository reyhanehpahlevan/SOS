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
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import rescuecore2.messages.control.KVTimestep;
import rescuecore2.standard.components.StandardViewer;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.view.AnimatedWorldModelViewer;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;
import rescuecore2.worldmodel.EntityID;

public class Namayangar extends StandardViewer implements ViewListener {
	 private static final int FONT_SIZE = 20;
	 static final int VIEWER_WIDTH = 1024;
	 static final int VIEWER_HEIGHT = 768;
	 // ////////S.O.S/////////
	 private boolean m_updateTimeTextBox = true;
	 private JLabel m_statusLabel = new JLabel();
	 private JCheckBox m_closeCheckBox = new JCheckBox("Close at the end", false);
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
	 // public Logger logger = new Logger();
	 private JProgressBar m_cycleProgressBar = new JProgressBar(0, 300);
	 JList m_totalList = new JList();
//	 private final JCheckBox showLoggerFromFirst = new JCheckBox("Show from first cycle", false);

	 private JTextArea commandsPane = new JTextArea(4, 0);
	 private JTextField commandLine = new JTextField();

	 // ////

	 private AnimatedWorldModelViewer viewer;
	 private JLabel timeLabel;
	 StandardEntity selectedObject;
	 private SOSEntityInspector inspector = new SOSEntityInspector();
	 private static Splash splash;

	 public static void showSplash() {
		  splash = new Splash(1000);
		  splash.showSplash();
	 }

	 @Override
	 protected void postConnect() {

		  super.postConnect();

		  // StartUpOptionPanel.sosStaring();
		  JFrame frame = new JFrame("S.O.S Namayangar");
		  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		  viewer = new AnimatedWorldModelViewer();
		  // viewer.get
		  viewer.initialise(config);
		  viewer.view(model);
		  // viewer.setPreferredSize(new Dimension(500, 500));
		  timeLabel = new JLabel("Time: Not started", JLabel.CENTER);
		  timeLabel.setBackground(Color.WHITE);
		  timeLabel.setOpaque(true);
		  timeLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, FONT_SIZE));
		  // inspector.setPreferredSize(new Dimension(10,100));
		  final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewer, rightPanel());

		  frame.add(split, BorderLayout.CENTER);
		  frame.add(statusPanel(), BorderLayout.NORTH);
		  frame.add(bottomPanel(), BorderLayout.SOUTH);
		  viewer.setPreferredSize(new Dimension(VIEWER_WIDTH, VIEWER_HEIGHT));

		  frame.setSize(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(), GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight() - 50);

		  viewer.addViewListener(this);
		  frame.pack();
		  frame.setVisible(true);
		  frame.setAlwaysOnTop(true);
		  frame.setAlwaysOnTop(false);
		  // frame.setFocusableWindowState(true);
		  frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		  split.setDividerLocation(0.8);
		  splash.exit();
		  splash = null;

	 }

	 JToggleButton m_stopTimeButton;
	 JToggleButton m_playTimeButton;

	 private JPanel statusPanel() {
		  JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		  panel.setBackground(Color.white);
		  m_statusLabel.setFont(m_statusLabel.getFont().deriveFont(Font.PLAIN, 20));
		  m_timeTextField.setFont(m_statusLabel.getFont().deriveFont(Font.PLAIN, 20));

		  m_closeCheckBox.setBackground(Color.white);
		  // setStatus();
		  JButton m_backTimeButton = new JButton("<<");
		  m_backTimeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					 int a = Integer.parseInt(m_timeTextField.getText()) - 1;
					 if (a >= 0) {
						  m_timeTextField.setText(a + "");
						  // WORLD.playback(Integer.parseInt(m_timeTextField.getText()));
						  // m_doRedo = false;
					 }

				}
		  });
		  m_stopTimeButton = new JToggleButton("||", false);
		  m_playTimeButton = new JToggleButton(">", true);

		  m_stopTimeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					 // m_doRedo = false;
					 // paused= true;
					 m_playTimeButton.setSelected(false);
					 m_stopTimeButton.setSelected(true);
				}
		  });

		  m_playTimeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					 try {
						  // WORLD.playback(Integer.parseInt(m_timeTextField.getText()));

						  // m_doRedo = true;
						  // if (paused)
						  // {
						  // m_pauseCheck.setSelected(false);
						  // paused = false;
						  // }
					 } catch (NumberFormatException nfe) {
					 }
					 m_stopTimeButton.setSelected(false);
					 m_playTimeButton.setSelected(true);

				}
		  });
		  JButton m_forwardTimeButton = new JButton(">>");
		  m_forwardTimeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					 m_timeTextField.setText(Integer.parseInt(m_timeTextField.getText()) + 1 + "");
					 // WORLD.playback(Integer.parseInt(m_timeTextField.getText()));
					 // m_doRedo = false;
					 // paused=true;
				}
		  });
		  m_timeTextField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					 try {
						  // WORLD.playback(Integer.parseInt(m_timeTextField.getText()));
						  // m_doRedo = true;
					 } catch (NumberFormatException nfe) {
					 }

					 m_timeTextField.setFocusable(false);
					 // repaint();
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
		  panel.add(m_closeCheckBox);

		  return panel;
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
		  panel.add(m_cycleProgressBar);
		  // addIntervalEachCycleSlider(panel);
		  // addDoAnimateCheckBox(panel);
		  addIdTextField(panel);
		  addShowCommandBox(panel);
		  JPanel rightPanel = new JPanel();
		  rightPanel.setLayout(new GridLayout());
		  addZoomPanel(rightPanel);
		  mainPanel.add(panel, BorderLayout.WEST);
		  mainPanel.add(rightPanel, BorderLayout.EAST);
		  return mainPanel;
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

		  panel.add(new JLabel(" Zoom:"));
		  panel.add(m_decreaseZoomButton);
		  panel.add(m_zoomTextField);
		  panel.add(m_increaseZoomButton);
		  panel.add(m_resetZoomButton);

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

	 private Component rightPanel() {
		  JPanel panel = new JPanel();

		  panel.setPreferredSize(new Dimension(200, 100));
		  panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		  JTabbedPane tabbedPane = new JTabbedPane();

		  // JPanel civ_infoPanel = new JPanel();
		  // JPanel bld_infoPanel = new JPanel();
		  // JPanel road_infoPanel = new JPanel();
		  tabbedPane.addTab("Information", addInfoPanel());
		  tabbedPane.addTab("Option", addOptionPanel());

		  panel.add(tabbedPane);
		  return panel;
		  // jsp.setPreferredSize(new Dimension(100, 100));
	 }

	 private JPanel addOptionPanel() {

		  JPanel optionPanel = new JPanel();
		  // optionPanel.setLayout();
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
					 // m_wasChangedBackground = true;
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
		  // m_viewClusterNum.addActionListener(new ActionListener() {
		  // public void actionPerformed(ActionEvent e) {
		  // for (Node node : WORLD.nodeList) {
		  // node.zoned = false;
		  // node.spec = false;
		  // }
		  // for (Road road : WORLD.roadList) {
		  // road.zone = 0;
		  // }
		  // new
		  // Clustering(WORLD).selectZone(Integer.parseInt(m_viewClusterNum.getText()),
		  // WORLD);
		  // isClustered = true;
		  // }
		  // });
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
		  jp1.setBorder(new TitledBorder(new LineBorder(Color.white, 2, true), "HDF Tools"));
		  jp1.setPreferredSize(new Dimension(100, 60));
		  jp1.add(new JLabel("cluster"));
		  jp1.add(m_viewClusterNum);
		  jp1.add(m_viewCluster);
		  // optionPanel.add(jp1);

		  return optionPanel;
	 }

	 private JPanel addInfoPanel() {
		  // propertiesTable.getColumnModel().getColumn(0).setHeaderValue("Property");
		  // propertiesTable.getColumnModel().getColumn(1).setHeaderValue("Value");
		  JScrollPane jsp = new JScrollPane(inspector);
		  JPanel totalPanel = new JPanel();
		  totalPanel = new JPanel();
		  // totalPanel.setPreferredSize(new Dimension(RIGHT_PABEL_SIZE,
		  // getHeight()));
		  totalPanel.setLayout(new BoxLayout(totalPanel, BoxLayout.Y_AXIS));
		  totalPanel.add(new JLabel("Properties"));
		  totalPanel.add(jsp);
		  //		
		  totalPanel.add(new JLabel("Total"));

		  totalPanel.add(new JScrollPane(m_totalList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		  totalPanel.add(new JLabel("Other"));

		  // JScrollPane jsp = new JScrollPane(neighborTable,
		  // JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		  // JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		  // attributePanel.add(neighborTable);
		  // totalPanel.add(jsp);

		  // totalPanel.add(new JScrollPane(neighborTable,
		  // JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		  // JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		  return totalPanel;
	 }

	 @Override
	 protected void handleTimestep(final KVTimestep t) {
		  super.handleTimestep(t);
		  SwingUtilities.invokeLater(new Runnable() {
				public void run() {

					 if (m_playTimeButton.isSelected()) {
						  timeLabel.setText("Time: " + t.getTime());
						  m_timeTextField.setText(t.getTime() + "");
						  viewer.view(model, t.getCommands());
						  viewer.repaint();
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

	 public void setSelectedObject(StandardEntity selectedObject) {
		  if(this.selectedObject!=null)
				this.selectedObject.setSelected(false);
		  
		  this.selectedObject = selectedObject;
		  if (selectedObject != null) {
				selectedObject.setSelected(true);
		  }
		  inspector.inspect(selectedObject);
		  viewer.repaint();
	 }

	 public void setM_updateTimeTextBox(boolean m_updateTimeTextBox) {
		  this.m_updateTimeTextBox = m_updateTimeTextBox;
	 }

	 public boolean isM_updateTimeTextBox() {
		  return m_updateTimeTextBox;
	 }

}
