package sos.ambulance.States;

import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.base.message.structure.MessageXmlConstant;
import sos.base.message.structure.blocks.MessageBlock;
import sos.base.util.SOSActionException;

public class OtherClusterSearchState extends AbstractSearchState{

	public OtherClusterSearchState(AmbulanceTeamAgent ownerAgent) {
		super(ownerAgent);
	}

	@Override
	public boolean finished() {
		return isDone;
	}

	@Override
	public void resetState() {

	}

	@Override
	public void act() throws SOSActionException {
		if (self.time() % 5 == 0) {
			self.messageBlock = new MessageBlock(MessageXmlConstant.HEADER_AMBULANCE_INFO);
			self.messageBlock.addData(MessageXmlConstant.DATA_AMBULANCE_INDEX, self.me().getAmbIndex());
			self.messageBlock.addData(MessageXmlConstant.DATA_AT_STATE, 3);
			self.messageBlock.addData(MessageXmlConstant.DATA_ID, 0);
			self.messageBlock.addData(MessageXmlConstant.DATA_FINISH_TIME, self.time());
			self.messageBlock.setResendOnNoise(false);
			self.messages.add(self.messageBlock);
			self.sayMessages.add(self.messageBlock);
		}

		self.randomWalk(true);
		/**************************** Mission Completed Task ***********************/
		if (self.model().refuges().isEmpty()&&!self.model().centers().isEmpty())
			self.move.moveStandard(self.model().centers());
		else if(!self.model().refuges().isEmpty())
			self.move.moveStandard(self.model().refuges());
	}

}
