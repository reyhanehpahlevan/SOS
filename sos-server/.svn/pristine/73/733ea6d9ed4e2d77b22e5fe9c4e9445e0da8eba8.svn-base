package sosNamayangar;

import static rescuecore2.misc.EncodingTools.readBytes;
import static rescuecore2.misc.EncodingTools.readInt32;
import static rescuecore2.misc.EncodingTools.reallySkip;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import rescuecore2.config.Config;
import rescuecore2.log.AbstractLogReader;
import rescuecore2.log.CommandsRecord;
import rescuecore2.log.ConfigRecord;
import rescuecore2.log.InitialConditionsRecord;
import rescuecore2.log.LogException;
import rescuecore2.log.Logger;
import rescuecore2.log.PerceptionRecord;
import rescuecore2.log.RecordType;
import rescuecore2.log.UpdatesRecord;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.StandardMessageFactory;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * A log reader that reads from a file.
 */
public class NewSOSFileLogReader extends AbstractLogReader {
	private static final int KEY_FRAME_BUFFER_MAX_SIZE = 10;

	protected RandomAccessFile file;
	protected RandomAccessFile indexFile;
	private int maxTime;
	protected NavigableMap<Integer, StandardWorldModel> keyFrames;
	private Map<Integer, Map<EntityID, Long>> perceptionIndices;
	private Map<Integer, Long> updatesIndices;
	private Map<Integer, Long> commandsIndices;
	protected Config config;
	private boolean finishIndexing = false;

	/**
	 * Construct a new FileLogReader.
	 * 
	 * @param name
	 *            The name of the file to read.
	 * @param registry
	 *            The registry to use for reading log entries.
	 * @throws IOException
	 *             If the file cannot be read.
	 * @throws LogException
	 *             If there is a problem reading the log.
	 */
	public NewSOSFileLogReader(String name, Registry registry)
			throws IOException, LogException {
		this(new File(name), registry);
	}

	/**
	 * Construct a new FileLogReader.
	 * 
	 * @param file
	 *            The file object to read.
	 * @param registry
	 *            The registry to use for reading log entries.
	 * @throws IOException
	 *             If the file cannot be read.
	 * @throws LogException
	 *             If there is a problem reading the log.
	 */
	public NewSOSFileLogReader(File file, Registry registry)
			throws IOException, LogException {
		super(registry);
		Logger.info("Reading file log: " + file.getAbsolutePath());
		while (!file.exists()) {
			System.err.println(file.getAbsolutePath()
					+ " not found... waiting for creating this");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.file = new RandomAccessFile(file, "r");
		indexFile = new RandomAccessFile(file, "r");
		index(1000);
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					index();
				} catch (LogException e) {
					e.printStackTrace();
				}
				finishIndexing = true;
				
			}
		}).start();

	}

	private void index(int i) throws LogException {
		try {
			Registry.setCurrentRegistry(registry);
			keyFrames = new TreeMap<Integer, StandardWorldModel>();
			perceptionIndices = new HashMap<Integer, Map<EntityID, Long>>();
			updatesIndices = new HashMap<Integer, Long>();
			commandsIndices = new HashMap<Integer, Long>();
			indexFile.seek(0);
			int id;
			RecordType type;
			boolean startFound = false;
			do {
				id = readInt32(indexFile);
				type = RecordType.fromID(id);
				if (!startFound) {
					if (!RecordType.START_OF_LOG.equals(type)) {
						throw new LogException(
								"Log does not start with correct magic number");
					}
					startFound = true;
				}
				indexRecord(type);
				if(i==0)
					break;
				i--;
			} while (!RecordType.END_OF_LOG.equals(type));
		} catch (EOFException e) {
			Logger.debug("EOF found");
		} catch (IOException e) {
			throw new LogException(e);
		}		
	}

	@Override
	public Config getConfig() {
		return config;
	}

	@Override
	public int getMaxTimestep() throws LogException {
		if (!finishIndexing)
			Logger.trace("Indexing is not finished... current indexing time:"
					+ maxTime);
		// else
		// Logger.trace("Indexing is finished... indexed time:"+maxTime);
		return maxTime;
	}

	@Override
	public StandardWorldModel getWorldModel(int time) throws LogException {
		Logger.debug("Getting world model at time " + time);
		if (keyFrames.get(time) != null) {
			Logger.trace("Found WorldModel " + time + " in keyframe");
			return keyFrames.get(time);
		}
		StandardWorldModel result = new StandardWorldModel();
		// Look for a key frame
		Entry<Integer, StandardWorldModel> entry = keyFrames.floorEntry(time);
		if (entry == null) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (getMaxTimestep() < time)
				return null;
			return getWorldModel(time);
		}
		int startTime = entry.getKey();
		Logger.trace("Found key frame " + startTime);
		// Copy the initial conditions
		Logger.trace("Cloning key frame conditions");
		for (Entity next : entry.getValue()) {
			result.addEntity(next.copy());
		}
		// Go through updates and apply them all
		for (int i = startTime + 1; i <= time; ++i) {
			ChangeSet updates = getUpdates(i).getChangeSet();
			Logger.trace("Merging " + updates.getChangedEntities().size()
					+ " updates for timestep " + i);
			result.merge(updates);
		}
		Logger.trace("Done");
		// Remove stale key frames
		removeStaleKeyFrames();
		// Store this as a key frame - it's quite likely that the next timestep
		// will be viewed soon.
		keyFrames.put(time, result);
		return (StandardWorldModel) result;
	}

	@Override
	public Set<EntityID> getEntitiesWithUpdates(int time) throws LogException {
		Map<EntityID, Long> timestepMap = perceptionIndices.get(time);
		if (timestepMap == null) {
			return new HashSet<EntityID>();
		}
		return timestepMap.keySet();
	}

	@Override
	public PerceptionRecord getPerception(int time, EntityID entity)
			throws LogException {
		Map<EntityID, Long> timestepMap = perceptionIndices.get(time);
		if (timestepMap == null) {
			return null;
		}
		Long l = timestepMap.get(entity);
		if (l == null) {
			return null;
		}
		try {
			file.seek(l);
			int size = readInt32(file);
			byte[] bytes = readBytes(size, file);
			return new PerceptionRecord(new ByteArrayInputStream(bytes));
		} catch (IOException e) {
			throw new LogException(e);
		}
	}

	@Override
	public CommandsRecord getCommands(int time) throws LogException {
		Long index = commandsIndices.get(time);
		if (index == null) {
			return null;
		}
		try {
			file.seek(index);
			int size = readInt32(file);
			byte[] bytes = readBytes(size, file);
			return new CommandsRecord(new ByteArrayInputStream(bytes));
		} catch (IOException e) {
			throw new LogException(e);
		}
	}

	@Override
	public UpdatesRecord getUpdates(int time) throws LogException {
		Long index = updatesIndices.get(time);
		if (index == null) {
			return null;
		}
		try {
			file.seek(index);
			int size = readInt32(file);
			byte[] bytes = readBytes(size, file);
			return new UpdatesRecord(new ByteArrayInputStream(bytes));
		} catch (IOException e) {
			throw new LogException(e);
		}
	}

	protected void index() throws LogException {
		try {
			Registry.setCurrentRegistry(registry);
			if(keyFrames==null){
			keyFrames = new TreeMap<Integer, StandardWorldModel>();
			perceptionIndices = new HashMap<Integer, Map<EntityID, Long>>();
			updatesIndices = new HashMap<Integer, Long>();
			commandsIndices = new HashMap<Integer, Long>();
			}
			indexFile.seek(0);
			int id;
			RecordType type;
			boolean startFound = false;
			do {
				id = readInt32(indexFile);
				type = RecordType.fromID(id);
				if (!startFound) {
					if (!RecordType.START_OF_LOG.equals(type)) {
						throw new LogException(
								"Log does not start with correct magic number");
					}
					startFound = true;
				}
				indexRecord(type);
			} while (!RecordType.END_OF_LOG.equals(type));
		} catch (EOFException e) {
			Logger.debug("EOF found");
		} catch (IOException e) {
			throw new LogException(e);
		}
	}

	private void indexRecord(RecordType type) throws IOException, LogException {
		switch (type) {
		case START_OF_LOG:
			indexStart();
			break;
		case INITIAL_CONDITIONS:
			indexInitialConditions();
			break;
		case PERCEPTION:
			indexPerception();
			break;
		case COMMANDS:
			indexCommands();
			break;
		case UPDATES:
			indexUpdates();
			break;
		case CONFIG:
			indexConfig();
			break;
		case END_OF_LOG:
			indexEnd();
			break;
		default:
			throw new LogException("Unexpected record type: " + type);
		}
	}

	private void indexStart() throws IOException {
		int size = readInt32(indexFile);
		reallySkip(indexFile, size);
	}

	private void indexEnd() throws IOException {
		int size = readInt32(indexFile);
		reallySkip(indexFile, size);
	}

	private void indexInitialConditions() throws IOException, LogException {
		int size = readInt32(indexFile);
		if (size < 0) {
			throw new LogException("Invalid initial conditions size: " + size);
		}
		byte[] bytes = readBytes(size, indexFile);
		InitialConditionsRecord record = new InitialConditionsRecord(
				new ByteArrayInputStream(bytes));
		keyFrames.put(0, StandardWorldModel.createStandardWorldModel(record
				.getWorldModel()));
	}

	private void indexPerception() throws IOException, LogException {
		long position = indexFile.getFilePointer();
		int size = readInt32(indexFile);
		byte[] bytes = readBytes(size, indexFile);
		PerceptionRecord record = new PerceptionRecord(
				new ByteArrayInputStream(bytes));
		int time = record.getTime();
		EntityID agentID = record.getEntityID();
		Map<EntityID, Long> timestepMap = perceptionIndices.get(time);
		if (timestepMap == null) {
			timestepMap = new HashMap<EntityID, Long>();
			perceptionIndices.put(time, timestepMap);
		}
		timestepMap.put(agentID, position);
	}

	private void indexCommands() throws IOException, LogException {
		long position = indexFile.getFilePointer();
		int size = readInt32(indexFile);
		byte[] bytes = readBytes(size, indexFile);
		CommandsRecord record = new CommandsRecord(new ByteArrayInputStream(
				bytes));
		int time = record.getTime();
		commandsIndices.put(time, position);
		maxTime = Math.max(time, maxTime);
	}

	private void indexUpdates() throws IOException, LogException {
		long position = indexFile.getFilePointer();
		int size = readInt32(indexFile);
		byte[] bytes = readBytes(size, indexFile);
		UpdatesRecord record = new UpdatesRecord(
				new ByteArrayInputStream(bytes));
		int time = record.getTime();
		updatesIndices.put(time, position);
		maxTime = Math.max(time, maxTime);
	}

	private void indexConfig() throws IOException, LogException {
		int size = readInt32(indexFile);
		byte[] bytes = readBytes(size, indexFile);
		ConfigRecord record = new ConfigRecord(new ByteArrayInputStream(bytes));
		config = record.getConfig();
	}

	protected void removeStaleKeyFrames() {
		Logger.trace("Removing stale key frames");
		int size = keyFrames.size();
		if (size < KEY_FRAME_BUFFER_MAX_SIZE) {
			Logger.trace("Key frame buffer is not full: " + size
					+ (size == 1 ? " entry" : " entries"));
			return;
		}
		// Try to balance the number of key frames.
		int window = maxTime / KEY_FRAME_BUFFER_MAX_SIZE;
		for (int i = 0; i < maxTime; i += window) {
			NavigableMap<Integer, StandardWorldModel> next = keyFrames.subMap(
					i, false, i + window, true);
			Logger.trace("Window " + i + " -> " + (i + window) + " has "
					+ next.size() + " entries");
			if (next.size() > 1) {
				// Remove all but the last entry in this window
				Entry<Integer, StandardWorldModel> last = next.lastEntry();
				next.clear();
				next.put(last.getKey(), last.getValue());
				Logger.trace("Retained entry " + last);
			}
		}
		Logger.trace("New key frame set: " + keyFrames);
	}

	/*
	public static void main(String[] args) {
		try {
			Registry.SYSTEM_REGISTRY
					.registerEntityFactory(StandardEntityFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY
					.registerMessageFactory(StandardMessageFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY
					.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
			System.out.println(args[0]);
			NewSOSFileLogReader logReader = new NewSOSFileLogReader(args[0],
					Registry.SYSTEM_REGISTRY);
			BufferedWriter log = new BufferedWriter(new FileWriter("log.ini"));
			BufferedWriter log2 = new BufferedWriter(new FileWriter("log2.ini"));
			log.write("id\ttime\tHP\tDamage\n");
			StandardWorldModel lastmodel = null;
			Thread.sleep(1000);
			System.out.println(logReader.getMaxTimestep());
			for (int i = 2; i < logReader.getMaxTimestep(); i++) {
				System.out.println("time=" + i);
				StandardWorldModel model = logReader.getWorldModel(i);
				for (StandardEntity standardEntity : model
						.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
					Civilian civ = (Civilian) standardEntity;
					if (civ.getDamage() == 0)
						continue;
					log.write(civ.getID() + "\t" + i + "\t" + civ.getHP()
							+ "\t" + civ.getDamage() + "\n");
					if (civ.getHP() == 0
							&& lastmodel != null
							&& ((Civilian) lastmodel.getEntity(civ.getID()))
									.getHP() != 0)
						log2.write(civ.getID() + "\t" + i + "\n");
				}
				lastmodel = model;

			}
			log.flush();
			log2.flush();
		} catch (IOException | LogException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	*/
}
