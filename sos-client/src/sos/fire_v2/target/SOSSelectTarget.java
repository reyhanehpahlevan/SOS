package sos.fire_v2.target;

import java.util.List;

import sos.base.SOSAgent;
import sos.base.entities.FireBrigade;
import sos.base.entities.StandardEntity;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.base.util.sosLogger.SOSLoggerSystem.OutputType;
import sos.search_v2.tools.cluster.MapClusterType;

public abstract class SOSSelectTarget<E> {

	protected SOSAgent agent;
	protected SelectStrategy strategy = SelectStrategy.NONE;
	protected SOSLoggerSystem log;
	protected MapClusterType<FireBrigade> cluster;
	private int lastSelectTime = -1;
	private E lastSelected = null;
	private Object lastLink = null;

	public SOSSelectTarget(SOSAgent agent, MapClusterType<FireBrigade> cluster) {
		this.agent = agent;
		this.cluster = cluster;
		log = new SOSLoggerSystem((StandardEntity) agent.me(), "NewTargerSelector/" + this.getClass().getSimpleName() + "/", true, OutputType.File, true);
		agent.sosLogger.addToAllLogType(log);
		preCompute();
	}

	public enum SelectStrategy {
		NONE, CLUSTER_GOOD, CLUSTER_NORMAL
	}

	public SelectStrategy getStrategy() {
		return strategy;
	}

	private boolean doSelect(Object link)
	{
		if (lastSelectTime == agent.model().time())
		{
			if (link != null)
			{
				if (link == lastLink)
				{
					return false;
				}
				return true;
			}
			return false;
		}
		return true;
	}

	public E decide(Object link) {
		if (doSelect(link)) {
			List<E> validTarget = getValidTask(link);

			reset(validTarget);

			setPriority(validTarget);

			lastSelected = getBestTarget(validTarget);

			lastLink = link;

			lastSelectTime = agent.model().time();
		}
		return lastSelected;
	}

	public abstract E getBestTarget(List<E> validTarget);

	public abstract void reset(List<E> validTarget);

	public abstract void preCompute();

	public abstract void setPriority(List<E> validTarget);

	public abstract List<E> getValidTask(Object link);

}
