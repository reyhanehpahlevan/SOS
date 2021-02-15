package sosNamayangar;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import rescuecore2.log.LogException;
import rescuecore2.log.Logger;
import rescuecore2.log.PerceptionRecord;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * A log reader that reads from a file.
 */
public class NewSOSAgentFileLogReader extends NewSOSFileLogReader {

	private final EntityID agentId;

	public NewSOSAgentFileLogReader(File file, Registry registry,
			EntityID agentId) throws IOException, LogException {
		super(file, registry);
		this.agentId = agentId;
	}

	@Override
	public StandardWorldModel getWorldModel(int time) throws LogException {
		
		Logger.debug("Getting world model at time " + time);
		if (keyFrames.get(time) != null) {
			Logger.trace("Found WorldModel " + time + " in keyframe");
			return keyFrames.get(time);
		}
		SOSWorldModel result = new SOSWorldModel(agentId,config);
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
			PerceptionRecord p = getPerception(startTime, agentId);
			if (p != null) {
				ChangeSet updates = p.getChangeSet();
				Logger.trace("Merging " + updates.getChangedEntities().size()
						+ " updates for timestep " + i);
				
				result.merge(updates,i);
				result.mergeWithHearing(p.getHearing(),i);
			}
		}
		Logger.trace("Done");
		// Remove stale key frames
		removeStaleKeyFrames();
		// Store this as a key frame - it's quite likely that the next timestep
		// will be viewed soon.
		keyFrames.put(time, result);
		return (StandardWorldModel) result;
	}
}