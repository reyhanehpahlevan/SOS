package sos.ambulance.States;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.base.AmbulanceConstants;
import sos.ambulance_v2.base.AmbulanceConstants.ATstates;
import sos.ambulance_v2.base.RescueInfo.IgnoreReason;
import sos.base.entities.Area;
import sos.base.entities.Edge;
import sos.base.entities.Human;
import sos.base.entities.Road;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.MessageXmlConstant;
import sos.base.message.structure.blocks.MessageBlock;
import sos.base.reachablity.Reachablity;
import sos.base.reachablity.Reachablity.ReachablityState;
import sos.base.util.SOSActionException;
import sos.base.util.SOSGeometryTools;
import sos.base.util.information_stacker.CycleInformations;
import sos.base.util.information_stacker.act.MoveAction;
import sos.base.util.information_stacker.act.StockMoveAction;
import sos.search_v2.tools.cluster.ClusterData;

/**
 * Created by IntelliJ IDEA.
 * User: ara
 */
public abstract class AmbulanceGeneralState {

	protected final Human target;
	protected final Pair<? extends Area, Point2D> place;
	protected final AmbulanceTeamAgent self;

	public boolean isDone = false;
	protected boolean acknowledgeMsgSent = false;
	protected int lastInfoSent = 2;
	protected boolean stateChanged = true;
	private int startTime;

	public abstract boolean finished();

	public abstract void resetState();

	public abstract void act() throws SOSActionException;

	public AmbulanceGeneralState(AmbulanceTeamAgent ownerAgent, Pair<? extends Area, Point2D> place, Human target) {
		this.self = ownerAgent;
		this.place = place;
		this.target = target;
		startTime = self.time();
	}

	public AmbulanceGeneralState(AmbulanceTeamAgent ownerAgent) {
		this.self = ownerAgent;
		this.place = null;
		this.target = null;
	}

	//	protected void ignoreTarget(int till){
	//	    	target.getRescueInfo().setIgnored(true);
	//			target.getRescueInfo().setIgnoredUntil(till);
	//			self.setUnderMissionTarget(null);
	//	}
	protected void ignoreTarget(IgnoreReason reason, int till) {
		target.getRescueInfo().setIgnoredUntil(reason, till);
	}

	//-------------------------------------------------------------------------------------
	protected void sendStatueMsgBySay(ATstates state, boolean needHelp) {
		if ((self.messageSystem.type != Type.NoComunication) || (self.messageSystem.type != Type.LowComunication))
			return;
		int mlposIndex;
		int id;
		int st = state.getMessageIndex();

		int help = needHelp ? 1 : 0;
		if (state == ATstates.SEARCH) { //search mode
			id = 0;
		} else {
			id = this.target.getID().getValue();
		}
		mlposIndex = self.me().getPositionArea().getAreaIndex();
		self.messageBlock = new MessageBlock(MessageXmlConstant.HEADER_AMBULANCE_STATUS);
		self.messageBlock.addData(MessageXmlConstant.DATA_AMBULANCE_INDEX, self.me().getAmbIndex());
		self.messageBlock.addData(MessageXmlConstant.DATA_AT_STATE, st);
		self.messageBlock.addData(MessageXmlConstant.DATA_ID, id);
		self.messageBlock.addData(MessageXmlConstant.DATA_AREA_INDEX, mlposIndex);
		self.messageBlock.addData(MessageXmlConstant.DATA_NEED_HELP, help);
		self.messageBlock.addData(MessageXmlConstant.DATA_TIME, self.time());
		self.messageBlock.setResendOnNoise(false);
		self.sayMessages.add(self.messageBlock);
	}

	//-------------------------------------------------------------------------------------
	protected void sendTaskAckMsg(int type) {
		self.messageBlock = new MessageBlock(MessageXmlConstant.HEADER_AMBULANCE_TASK_ACK);
		self.messageBlock.addData(MessageXmlConstant.DATA_AMBULANCE_INDEX, self.me().getAmbIndex());
		self.messageBlock.addData(MessageXmlConstant.DATA_ID, target.getID().getValue());
		self.messageBlock.addData(MessageXmlConstant.DATA_ACK_TYPE, type);
		self.messageBlock.setResendOnNoise(false);
		self.messages.add(self.messageBlock);
		self.log().info("sending Task Ack Message" + target + "==>" + ((type == 0) ? "accepted" : (type == 1) ? "finished" : (type == 2) ? "rejected" : "unknown " + type));
		self.logToAmbDecision("sending Task Ack Message" + target + "==>" + ((type == 0) ? "accepted" : (type == 1) ? "finished" : (type == 2) ? "rejected" : "unknown " + type));
	}

	//-------------------------------------------------------------------------------------
	protected void sendInfoMsg(int id, ATstates state, int finishTime) {
		int st = state.getMessageIndex();
		self.messageBlock = new MessageBlock(MessageXmlConstant.HEADER_AMBULANCE_INFO);
		self.messageBlock.addData(MessageXmlConstant.DATA_AMBULANCE_INDEX, self.me().getAmbIndex());
		self.messageBlock.addData(MessageXmlConstant.DATA_ID, id);
		self.messageBlock.addData(MessageXmlConstant.DATA_AT_STATE, st);
		self.messageBlock.addData(MessageXmlConstant.DATA_FINISH_TIME, self.time() + finishTime);
		self.messageBlock.setResendOnNoise(false);
		self.messages.add(self.messageBlock);
		self.sayMessages.add(self.messageBlock);
		
		lastInfoSent = self.time();
		stateChanged = false;
		self.log().info("sending info Message==>target=" + self.model().getEntity(id) + " state=" + state + " ft:" + finishTime);
		self.logToAmbDecision("sending info Message==>target=" + self.model().getEntity(id) + " state=" + state + " ft:" + finishTime);

	}

	//-------------------------------------------------------------------------------------
	protected boolean isReachablityBug() {
		Area myArea = self.me().getAreaPosition();
		if (!(self.me().getAreaPosition() instanceof Road))
			return false;
		if (self.me().getImReachableToEdges().isEmpty())
			return false;
		for (Edge edge : myArea.getPassableEdges()) {
			if (Reachablity.isReachable((Road) myArea, self.me().getPositionPoint(), edge) != ReachablityState.Close)
				return false;
		}
		self.log().error("Traffic Simulator bug-->reachablity should handle it...");
		return true;
	}

	public boolean isStockBug() {
		int longAgo = self.time() - startTime;
		int numberOfStock = 0;
		Point2D lastLocation = self.me().getPositionPoint();
		for (int i = 1; i <= longAgo; i++) {
			CycleInformations info = self.informationStacker.getInformations(i);

			if (info.getAct() instanceof StockMoveAction)
				if (lastLocation.distance(info.getPositionPair().second()) < AmbulanceConstants.STOCK_DISTANSE)
					numberOfStock++;
				else
					numberOfStock = 0;

			else if (!(info.getAct() instanceof MoveAction))
				numberOfStock = 0;
			lastLocation = info.getPositionPair().second();
			if(numberOfStock>2){
				self.log().warn("I'm in stock bug..:(");
				return true;
			}
		}
		return false;
	}


	public ArrayList<ClusterData> getNearestClusters(int number){
		ArrayList<ClusterData> nearestCluster=new ArrayList<ClusterData>();
		final ClusterData myCluster = self.model().searchWorldModel.getClusterData();

		ArrayList<ClusterData> cds = new ArrayList<ClusterData>(self.model().searchWorldModel.getAllClusters());
		Collections.sort(cds, new Comparator<ClusterData>() {

			@Override
			public int compare(ClusterData o1, ClusterData o2) {
				double o1s = SOSGeometryTools.distance(myCluster.getX(),myCluster.getY(),o1.getX(),o1.getY());
				double o2s = SOSGeometryTools.distance(myCluster.getX(),myCluster.getY(),o2.getX(),o2.getY());
				if (o1s>o2s )
					return 1;
				if ( o1s<o2s)
					return -1;
				return 0;
			}
		});
		int i = 0;

		for (ClusterData cd : cds) {
			if (i >= number)
				break;
			i++;
			nearestCluster.add(cd);
		}
		return nearestCluster;

	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + target + "]";
	}
}
