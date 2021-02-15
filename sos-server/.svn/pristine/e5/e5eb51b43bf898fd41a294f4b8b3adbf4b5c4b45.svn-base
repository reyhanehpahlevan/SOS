package sosNamayangar;

import gis2.GisScenario;
import gis2.ScenarioException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import rescuecore2.config.Config;
import rescuecore2.log.FileLogReader;
import rescuecore2.log.LogException;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.AmbulanceCentre;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.GasStation;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.StandardMessageFactory;

public class LogMapMaker {

	private Config config;
	private FileLogReader logReader;

	public LogMapMaker() {
		try {
			config=new Config();
			JOptionPane.showMessageDialog(null, "enter log file");
			JFileChooser jfc = new JFileChooser(".");
			if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				String logname = jfc.getSelectedFile().getAbsolutePath();
				logReader = new FileLogReader(logname, Registry.SYSTEM_REGISTRY);
				config.merge(logReader.getConfig());
				writeScenario();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void writeScenario() {
		try {
			StandardWorldModel model = StandardWorldModel.createStandardWorldModel(logReader.getWorldModel(0));
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
					if (entity instanceof GasStation)
						scenario.addGasStation(entity.getID().getValue());
					if (entity instanceof Hydrant)
						scenario.addHydrant(entity.getID().getValue());			
				}
				StandardWorldModel model1 = StandardWorldModel.createStandardWorldModel( logReader.getWorldModel(1));
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
	
	public static void main(String[] args)
	{
	       Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
	        Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
	        Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
	   
		new LogMapMaker();
	}
}
