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
import rescuecore2.log.Logger;
import rescuecore2.log.PerceptionRecord;
import rescuecore2.log.RecordType;
import rescuecore2.log.UpdatesRecord;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;

/**
   A log reader that reads from a file.
 */
public class SOSFileLogReader extends AbstractLogReader {
    private static final int KEY_FRAME_BUFFER_MAX_SIZE = 10;

    protected RandomAccessFile file;
    private int maxTime;
    private NavigableMap<Integer, StandardWorldModel> keyFrames;
    private Map<Integer, Map<EntityID, Long>> perceptionIndices;
    private Map<Integer, Long> updatesIndices;
    private Map<Integer, Long> commandsIndices;
    private Config config;

    /**
       Construct a new FileLogReader.
       @param name The name of the file to read.
       @param registry The registry to use for reading log entries.
       @throws IOException If the file cannot be read.
       @throws LogException If there is a problem reading the log.
    */
    public SOSFileLogReader(String name, Registry registry) throws IOException, LogException {
        this(new File(name), registry);
    }

    /**
       Construct a new FileLogReader.
       @param file The file object to read.
       @param registry The registry to use for reading log entries.
       @throws IOException If the file cannot be read.
       @throws LogException If there is a problem reading the log.
    */
    public SOSFileLogReader(File file, Registry registry) throws IOException, LogException {
        super(registry);
        //Logger.info("Reading file log: " + file.getAbsolutePath());
        this.file = new RandomAccessFile(file, "r");
        index();
    }
    

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public int getMaxTimestep() throws LogException {
        return maxTime;
    }

    public StandardWorldModel getWorldModelWithOutUpdate(int time) throws LogException {
/*    	StandardWorldModel result = new StandardWorldModel();
    	StandardWorldModel oldWorld = getWorldModel(time-1);
    	for (StandardEntity standardEntity : oldWorld) {
    		result.addEntity(standardEntity.copy());
		}
    	result.index();
        return result;
*/       return getWorldModel(time);
    }
    public StandardWorldModel getWorldModelImp(int time) throws LogException {
    	if(keyFrames.get(time)!=null)
    		return keyFrames.get(time);
    	StandardWorldModel result = new StandardWorldModel();
    	StandardWorldModel oldWorld = getWorldModelImp(time-1);
    	for (StandardEntity standardEntity : oldWorld) {
    		result.addEntity(standardEntity.copy());
		}
    	ChangeSet updates = getUpdates(time).getChangeSet();
    	result.merge(updates);
    	Logger.trace("get world model of time "+time);
//    	removeStaleKeyFrames();
//    	result.index();
//        keyFrames.put(time, result);
        return result;
    }
    @Override
    public StandardWorldModel getWorldModel(int time) throws LogException {
    	if(keyFrames.get(time)!=null)
    		return keyFrames.get(time);
    	StandardWorldModel result = new StandardWorldModel();
    	StandardWorldModel oldWorld = getWorldModelImp(time-1);
    	for (StandardEntity standardEntity : oldWorld) {
    		result.addEntity(standardEntity.copy());
		}
    	ChangeSet updates = getUpdates(time).getChangeSet();
    	result.merge(updates);
    	removeStaleKeyFrames();
    	result.index();
        keyFrames.put(time, result);
        return result;
/*        //Logger.debug("Getting world model at time " + time);
        WorldModel<? extends Entity> result = new DefaultWorldModel<Entity>(Entity.class);
        // Look for a key frame
        Map.Entry<Integer, StandardWorldModel> entry = keyFrames.floorEntry(time);
        int startTime = entry.getKey();
        //Logger.trace("Found key frame " + startTime);
        // Copy the initial conditions
        //Logger.trace("Cloning initial conditions");
        for (Entity next : entry.getValue()) {
            result.addEntity(next.copy());
        }
        // Go through updates and apply them all
        for (int i = startTime + 1; i <= time; ++i) {
            ChangeSet updates = getUpdates(time).getChangeSet();
            //Logger.trace("Merging " + updates.getChangedEntities().size() + " updates for timestep " + i);
            result.merge(updates);
        }
        //Logger.trace("Done");
        // Remove stale key frames
        removeStaleKeyFrames();
        // Store this as a key frame - it's quite likely that the next timestep will be viewed soon.
        StandardWorldModel swd=StandardWorldModel.createStandardWorldModel(result);
        keyFrames.put(time, swd);
        return swd;*/
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
    public PerceptionRecord getPerception(int time, EntityID entity) throws LogException {
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
        }
        catch (IOException e) {
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
        }
        catch (IOException e) {
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
        }
        catch (IOException e) {
            throw new LogException(e);
        }
    }

    protected void index() throws LogException {
        try {
        	Logger.trace("indexing file...");
            Registry.setCurrentRegistry(registry);
            keyFrames = new TreeMap<Integer, StandardWorldModel>();
            perceptionIndices = new HashMap<Integer, Map<EntityID, Long>>();
            updatesIndices = new HashMap<Integer, Long>();
            commandsIndices = new HashMap<Integer, Long>();
            file.seek(0);
            int id;
            RecordType type;
            boolean startFound = false;
            do {
                id = readInt32(file);
                type = RecordType.fromID(id);
                if (!startFound) {
                    if (!RecordType.START_OF_LOG.equals(type)) {
                        throw new LogException("Log does not start with correct magic number");
                    }
                    startFound = true;
                }
                indexRecord(type);
            } while (!RecordType.END_OF_LOG.equals(type));
        }
        catch (EOFException e) {
            //Logger.debug("EOF found");
        }
        catch (IOException e) {
            throw new LogException(e);
        }
        Logger.trace("indexing file finished");
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
        int size = readInt32(file);
        reallySkip(file, size);
    }

    private void indexEnd() throws IOException {
        int size = readInt32(file);
        reallySkip(file, size);
    }

    private void indexInitialConditions() throws IOException, LogException {
        int size = readInt32(file);
        if (size < 0) {
            throw new LogException("Invalid initial conditions size: " + size);
        }
        byte[] bytes = readBytes(size, file);
        InitialConditionsRecord record = new InitialConditionsRecord(new ByteArrayInputStream(bytes));
        
        keyFrames.put(0, StandardWorldModel.createStandardWorldModel(record.getWorldModel()));
    }

    private void indexPerception() throws IOException, LogException {
        long position = file.getFilePointer();
        int size = readInt32(file);
        byte[] bytes = readBytes(size, file);
        PerceptionRecord record = new PerceptionRecord(new ByteArrayInputStream(bytes));
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
        long position = file.getFilePointer();
        int size = readInt32(file);
        byte[] bytes = readBytes(size, file);
        CommandsRecord record = new CommandsRecord(new ByteArrayInputStream(bytes));
        int time = record.getTime();
        commandsIndices.put(time, position);
        maxTime = Math.max(time, maxTime);
    }

    private void indexUpdates() throws IOException, LogException {
        long position = file.getFilePointer();
        int size = readInt32(file);
        byte[] bytes = readBytes(size, file);
        UpdatesRecord record = new UpdatesRecord(new ByteArrayInputStream(bytes));
        int time = record.getTime();
        updatesIndices.put(time, position);
        maxTime = Math.max(time, maxTime);
    }

    private void indexConfig() throws IOException, LogException {
        int size = readInt32(file);
        byte[] bytes = readBytes(size, file);
        ConfigRecord record = new ConfigRecord(new ByteArrayInputStream(bytes));
        config = record.getConfig();
    }

    private void removeStaleKeyFrames() {
        //Logger.trace("Removing stale key frames");
        int size = keyFrames.size();
        if (size < KEY_FRAME_BUFFER_MAX_SIZE) {
            //Logger.trace("Key frame buffer is not full: " + size + (size == 1 ? " entry" : " entries"));
            return;
        }
        // Try to balance the number of key frames.
        int window = maxTime / KEY_FRAME_BUFFER_MAX_SIZE;
        for (int i = 0; i < maxTime; i += window) {
            NavigableMap<Integer, StandardWorldModel> next = keyFrames.subMap(i, false, i + window, true);
            //Logger.trace("Window " + i + " -> " + (i + window) + " has " + next.size() + " entries");
            if (next.size() > 1) {
                // Remove all but the last entry in this window
                Map.Entry<Integer, StandardWorldModel> last = next.lastEntry();
                next.clear();
                next.put(last.getKey(), last.getValue());
                //Logger.trace("Retained entry " + last);
            }
        }
        //Logger.trace("New key frame set: " + keyFrames);
    }
}
