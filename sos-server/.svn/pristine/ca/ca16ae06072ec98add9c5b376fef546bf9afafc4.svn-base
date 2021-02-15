package rescuecore2.standard.entities;

import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.AbstractEntity;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;

/**
 * Abstract base class for all standard entities.
 */
public abstract class StandardEntity extends AbstractEntity {
	/**
	 * @author Ali
	 */
	private boolean selected = false;

	/**
	 * Construct a StandardEntity with entirely undefined property values.
	 * 
	 * @param id
	 *            The ID of this entity.
	 */
	protected StandardEntity(EntityID id) {
		super(id);
	}

	/**
	 * StandardEntity copy constructor.
	 * 
	 * @param other
	 *            The StandardEntity to copy.
	 */
	protected StandardEntity(StandardEntity other) {
		super(other);
	}

	/**
	 * Get the location of this entity.
	 * 
	 * @param world
	 *            The world model to look up for entity references.
	 * @return The coordinates of this entity, or null if the location cannot be
	 *         determined.
	 */
	public Pair<Integer, Integer> getLocation(
			WorldModel<? extends StandardEntity> world) {
		return null;
	}

	/**
	 * Get the URN of this entity type as an instanceof StandardEntityURN.
	 * 
	 * @return A StandardEntityURN.
	 */
	public abstract StandardEntityURN getStandardURN();

	@Override
	public final String getURN() {
		return getStandardURN().toString();
	}

	/**
	 * @author Ali
	 * @param selected
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * @author Ali
	 * @return isSelected
	 */
	public boolean isSelected() {
		return selected;
	}

	private int lastSenseTime = 1;
	private int lastMsgTime = 1;

	public void setLastSenseTime(int time) {
		this.lastSenseTime = time;
	}

	public int getLastSenseTime() {
		return this.lastSenseTime;
	}

	public void setLastMsgTime(int time) {
		this.lastMsgTime = time;
	}

	public int getLastMsgTime() {
		return this.lastMsgTime;
	}

	public boolean isLastKnowledgeFromMsg() {
		return lastMsgTime > lastSenseTime;
	}

	public int updatedtime() {
		return Math.max(lastMsgTime, lastSenseTime);
	}
	
	@Override
	public Entity copy() {
		StandardEntity cp= (StandardEntity) super.copy();
		cp.setLastMsgTime(getLastMsgTime());
		cp.setLastSenseTime(getLastSenseTime());
		return cp;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+getID()+"]";
				
	}
}
