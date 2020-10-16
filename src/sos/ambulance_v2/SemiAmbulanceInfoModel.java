package sos.ambulance_v2;

import sos.base.SOSAgent;
import sos.base.entities.StandardEntity;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.base.util.sosLogger.SOSLoggerSystem.OutputType;

/**
 * @author Reyhaneh
 */

public class SemiAmbulanceInfoModel extends AbstractAmbulanceInfoModel {
	private int lastupdateTime;
	private SOSLoggerSystem dclog;
	private SOSLoggerSystem humanUpdateLog;

	public SemiAmbulanceInfoModel(SOSAgent<? extends StandardEntity> sosAgent) {
		super(sosAgent);
		dclog = new SOSLoggerSystem(getEntity(), "Agent/AmbulanceDecision", true, OutputType.File);
		dclog.setFullLoggingLevel();
		getAgent().sosLogger.addToAllLogType(dclog);
		humanUpdateLog = new SOSLoggerSystem(getEntity(), "Agent/ATHumanUpdate", true, OutputType.File, true);
		getAgent().	sosLogger.addToAllLogType(humanUpdateLog);
	}

	@Override
	public void setlastTimeUpdated(int time) {
		lastupdateTime = time;
	}
	
	@Override
	public int getLastTimeUpdated() {
		return lastupdateTime;
	}

	@Override
	public SOSLoggerSystem getADLogger() {
		return dclog;
	}

	@Override
	public SOSLoggerSystem getHumanUpdateLogger() {
		return humanUpdateLog;
	}


}
