package sosNamayangar;

import static rescuecore2.misc.EncodingTools.readBytes;
import static rescuecore2.misc.EncodingTools.readInt32;
import static rescuecore2.misc.EncodingTools.reallySkip;

import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.RandomAccessFile;
import java.io.ByteArrayInputStream;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.TreeMap;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.DefaultWorldModel;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.config.Config;
import rescuecore2.log.AbstractLogReader;
import rescuecore2.log.CommandsRecord;
import rescuecore2.log.ConfigRecord;
import rescuecore2.log.InitialConditionsRecord;
import rescuecore2.log.LogException;
import rescuecore2.log.PerceptionRecord;
import rescuecore2.log.RecordType;
import rescuecore2.log.UpdatesRecord;
import rescuecore2.messages.control.KVTimestep;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;

/**
   A log reader that reads from a file.
 */
public class SOSRunTimeFileLogReader extends NewSOSFileLogReader {

	public SOSRunTimeFileLogReader(String file, Registry registry)
	throws IOException, LogException {
		super(file, registry);
}
	long lastIndexFileSize=0;
	@Override
	protected void index() throws LogException {
		try{
		lastIndexFileSize=file.length();
		}catch (Exception e) {
			e.printStackTrace();
		}
		super.index();
	}
	public SOSRunTimeFileLogReader(File file, Registry registry)
			throws IOException, LogException {
		super(file, registry);
	}
	
	static int timelimit=0;
	@Override
	public StandardWorldModel getWorldModel(int time) throws LogException {
		smartIndex();
		try{
			StandardWorldModel wm = super.getWorldModel(time);
			timelimit=0;
			return wm;
		}catch (NullPointerException e) {
			timelimit++;
			if(timelimit>10){
				timelimit=0;
				return null;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			return getWorldModel(time);
			
		}
	}
	@Override
	public CommandsRecord getCommands(int time) throws LogException {
		smartIndex();
		return super.getCommands(time);
	}
	
	@Override
	public UpdatesRecord getUpdates(int time) throws LogException {
		smartIndex();
		return super.getUpdates(time);
	}
	@Override
	public int getMaxTimestep() throws LogException {
		smartIndex();
		return super.getMaxTimestep();
	}
	
	public void smartIndex() throws LogException {
		try {
			if(lastIndexFileSize!=file.length())
				index();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
