package sos.ambulance.States;

import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.base.entities.Refuge;
import sos.base.message.structure.MessageXmlConstant;
import sos.base.message.structure.blocks.MessageBlock;
import sos.base.util.SOSActionException;



/**
 * Created by IntelliJ IDEA.
 * User: ara
 * To change this template use File | Settings | File Templates.
 */
public class ImHurtState extends AmbulanceGeneralState {
	private boolean isSoon;

    public ImHurtState(AmbulanceTeamAgent ownerAgent, boolean soon) {
        super(ownerAgent);
        isSoon = soon;
    }

    @Override
	public void act() throws SOSActionException {
        if (!isSoon) {
			self.log().logln("*Stacking Im Hurt not soon..");
        } else {
        	self.log().logln("*Im Hurt certainly die bye bye..");

        	self.messageBlock=new MessageBlock(MessageXmlConstant.HEADER_DEAD_AGENT);
			self.messageBlock.addData(MessageXmlConstant.DATA_AGENT_INDEX,self.me().getAgentIndex());
			self.messages.add(self.messageBlock);
        }
        ignoreCurrentTarget();
        
        		
        self.move.moveStandard(self.model().refuges());
    }

	private void ignoreCurrentTarget() {
//		self.log().info("checking if current target can be ignore...");
//		if(self.target==null){
//			self.log().trace("No Target...");
//			return;
//		}
//		if(self.target.getBuriedness()<=5){
//			self.log().trace(self.target+" may can be rescue... because it's butiedness<=5");
//			return;
//		}
//		if(self.target.getAreaPosition() instanceof Road){
//			self.log().trace(self.target+" is in road");
//			return;
//		}
//		if(self.target.getAreaPosition() instanceof Road){
//			self.log().trace(self.target+" is in road");
//			return;
//		}	
//		Building targetBuilding = ((Building)self.target.getAreaPosition());
//		if(!targetBuilding.isBurning()){
//			self.log().trace(self.target +" is not in burning building");
//			return;
//		}
//		self.log().debug(self.target+" has been ignored...");
//		self.target.getRescueInfo().setIgnoredUntil(IgnoreReason.FiryCivilian, 1000);
	}

	@Override
	public boolean finished() {
		if (self.model().refuges().isEmpty())
            return true;
        if (isSoon)
            return false;
		return (!isSoon && self.location() instanceof Refuge);
    }

    @Override
	public void resetState() {
    }
}
