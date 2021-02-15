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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.imageio.ImageIO;
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
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.log.FileLogReader;
import rescuecore2.log.LogException;
import rescuecore2.log.UpdatesRecord;
import rescuecore2.messages.control.KVTimestep;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.misc.gui.ConfigTree;
import rescuecore2.registry.Registry;
import rescuecore2.score.CompositeScoreFunction;
import rescuecore2.score.ScoreFunction;
import rescuecore2.standard.components.StandardViewer;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.entities.StandardWorldModel;
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
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;
import sosNamayangar.layers.SOSAbstractSelectedLayer;
import sosNamayangar.message_decoder.channelDistribution.Channel;
import sosNamayangar.message_decoder.channelDistribution.MessageConfig;
import sosNamayangar.sos_score.SearchScoreFunction;

/**
 * @author Ali
 */
public class LogPictureMaker {
	private ScoreFunction scoreFunction;
	ArrayList<ScoreFunction> sosScores = new ArrayList<ScoreFunction>();
	private SOSWorldModelViewer viewer;
	private NumberFormat format;
	private FileLogReader logReader;
	private Config config;
	private static int SIZE=1024;
	public LogPictureMaker(Config config,String outputFolder) {
		this.config = config;
		try {
			format = NumberFormat.getInstance();
			format.setMaximumFractionDigits(3);
			String logname = config.getValue("kernel.logname");
			logReader = new FileLogReader(logname, Registry.SYSTEM_REGISTRY);
			config.merge(logReader.getConfig());
			viewer = new SOSWorldModelViewer();
			viewer.setSize(SIZE,SIZE);	
			//
			WorldModel<? extends Entity> initalModel = logReader.getWorldModel(0);
			viewer.initialise(config);
			viewer.view(initalModel);
			makeScoreFunction(initalModel);

			makePictures(outputFolder);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Connecting to SOS Namayangar have Error", "Error", 0);
		}

	}

	private void makePictures(String outputFolderPath) throws LogException, IOException {
		File outputFolder=new File(outputFolderPath);
		File outputPictures=new File(outputFolder,"pics");
		outputPictures.mkdirs();
		StringBuilder htmlout=new StringBuilder("<html><head></head><body>\n");
		htmlout.append("<table border='1'>\n<tr><td>\n"+getStaticInformation(logReader.getWorldModel(0)).toString().replace(",", "<br />")+"</td></tr>");
		for (int time = 0; time < logReader.getMaxTimestep(); time++) {
			System.out.println("TimeStep: "+time);
			Timestep timeStep = getTimeStep(time);
			WorldModel<? extends Entity> model = logReader.getWorldModel(time);
			updateScores(model,timeStep);
			if(time==3||time%30==0||time==logReader.getMaxTimestep()-1){
				
				File outputPicture = new File(outputPictures, time+".png");
				makePicture(outputPicture,model, timeStep);
				
				htmlout.append("<tr><td>"+time+"<br /><br />score="+format.format(scoreFunction.score(model, timeStep))+"</td><td><a href='pics/"+time+".png'><img src='pics/"+time+".png' width='400'/></a></td><td>"+getInformation(model, timeStep).toString().replace(",", "<br />")+"</td></tr>\n");
				
			}
		}
		htmlout.append("</table></body></html>");
		FileWriter fw=new FileWriter(new File(outputFolder, "index.html"));
		fw.write(htmlout.toString());
		fw.flush();
		fw.close();
		System.out.println("Finished");
	}
	private Timestep getTimeStep(int time) throws LogException {
		Timestep timestep=new Timestep(time);
//		System.out.println("updates:"+logReader.getUpdates(time));
		if(logReader.getUpdates(time)!=null){
			timestep.setChangeSet(logReader.getUpdates(time).getChangeSet());
		}
//		System.out.println("commands:"+logReader.getCommands(time));
		if(logReader.getCommands(time)!=null)
		timestep.setCommands(logReader.getCommands(time).getCommands());
		return timestep;
	}

	private void makePicture(File outputPicture, WorldModel<? extends Entity> model, Timestep timestep) throws IOException {
		
		
		viewer.view(model);
		
//		JFrame jf=new JFrame(timestep.getTime()+"");
//		jf.setSize(100, 100);
//		jf.add(viewer);
//		jf.setVisible(true);
		BufferedImage image=new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		viewer.paint(image.createGraphics());
		ImageIO.write(image, "png",outputPicture);
	}

	private void updateScores(WorldModel<? extends Entity> model, Timestep timestep) {
		scoreFunction.score(model, timestep);
		for (ScoreFunction score : sosScores) {
			score.score(model, timestep);
		}
	}

	@Override
	public String toString() {
		return "S.O.S Log Picture Maker";
	}

	private Vector<String> getStaticInformation(WorldModel<?extends Entity> worldmodel) {
		StandardWorldModel model=StandardWorldModel.createStandardWorldModel(worldmodel);
		Vector<String> totalList = new Vector<String>();
		totalList.add("Building:" + model.getEntitiesOfType(StandardEntityURN.BUILDING).size());
		totalList.add("Road:" + model.getEntitiesOfType(StandardEntityURN.ROAD).size());
		totalList.add("Fire Brigade:" + model.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE).size());
		totalList.add("Police Force:" + model.getEntitiesOfType(StandardEntityURN.POLICE_FORCE).size());
		totalList.add("Ambulance Team:" + model.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM).size());
		totalList.add("Centers:" + model.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.FIRE_STATION, StandardEntityURN.POLICE_OFFICE).size());
		totalList.add("Refuges:" + model.getEntitiesOfType(StandardEntityURN.REFUGE).size());
		return totalList;
	}
	
	private Vector<String> getInformation(WorldModel<?extends Entity> model, Timestep timestep) {
		Vector<String> totalList = new Vector<String>();
		int dead = 0;
		int all = 0;
		for (Entity standardEntity : model) {
			if(standardEntity instanceof Civilian){
				dead += ((Civilian) standardEntity).getHP() == 0 ? 1 : 0;
				all++;
			}
		}
		totalList.add("Alive Civilian:" + (all-dead));
		totalList.add("Dead Civilian:" + dead);

		if (scoreFunction instanceof CompositeScoreFunction) {
			for (ScoreFunction score : ((CompositeScoreFunction) scoreFunction).getChildFunctions()) {
				totalList.add(score.getName() + ": " + format.format(score.score(model, timestep)));
			}
		}
		for (ScoreFunction score : sosScores) {
			totalList.add(score.getName() + ": " + format.format(score.score(model, timestep)));
		}
		return totalList;
	}

	private void makeScoreFunction(WorldModel<? extends Entity> model) {
		String className = config.getValue(Constants.SCORE_FUNCTION_KEY);
		scoreFunction = instantiate(className, ScoreFunction.class);
		scoreFunction .initialise(model, config);
		sosScores.add(new SearchScoreFunction());
		sosScores.add(new DiscoveryScoreFunction());
		sosScores.add(new DistanceTravelledScoreFunction());
		sosScores.add(new HealthScoreFunction());
		for (ScoreFunction s : sosScores) {
			s.initialise(model, config);
		}
	}
	public static void main(String[] args) {
		Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
		Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
		Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
		if(args.length!=2){
			System.err.println("usage: logfile outputfile");
			return;
		}
		try {
			Config config = new Config(new File("/home/sos/Desktop/sos-server/boot/config/kernel.cfg"));
			if (args.length == 2)
				config.setValue("kernel.logname", args[0]);
			
			args = CommandLineOptions.processArgs(args, config);
			System.out.println(config.getValue("kernel.logname"));
			new LogPictureMaker(config,args[1]);
		} catch (ConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
