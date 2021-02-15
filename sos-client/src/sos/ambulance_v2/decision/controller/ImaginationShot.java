package sos.ambulance_v2.decision.controller;

import java.util.ArrayList;

import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.base.AmbulanceConstants.CivilianState;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Human;

//***********************************************************************************************************
	//***********************************************************************************************************
	public class ImaginationShot {
		//final variables
		public CivilianState goalCondition;
		final public Human target;
		int conditionAssigningExpireTime;
		int conditionExpireTime;
		final public int index;
		//changing variables
		public CivilianState resultCondition;
		public int rescueMargin;
		public ArrayList<AmbulanceTeam> atAssigned;

		public ImaginationShot(Human Target, CivilianState goalCondition, int index) {
			this.target = Target;
			this.goalCondition = goalCondition;
			this.index = index;
			switch (this.goalCondition) {
			case AVERAGE:
			case HEALTHY:
			case CRITICAL:
				conditionAssigningExpireTime = AmbulanceUtils.taskAssigningExpireTime(target, target.getRescueInfo().getDeathTime());
				conditionExpireTime = target.getRescueInfo().getDeathTime();
				break;
			}
			atAssigned = new ArrayList<AmbulanceTeam>();
		}

		public void reset() {
			atAssigned.clear();
			rescueMargin = 0;
			resultCondition = null;
		}

		@Override
		public String toString() {
			return "[ " + target + " ,rm: " + rescueMargin + " , caet:" + conditionAssigningExpireTime + " , rc:" + resultCondition + " , at:" + atAssigned + " , indx:" + index + "buried:"+target.getBuriedness()+" d:"+target.getDamage()+" hp:"+target.getHP()+" dt:"+target.getRescueInfo().getDeathTime() +" ], ";
		}
	}
