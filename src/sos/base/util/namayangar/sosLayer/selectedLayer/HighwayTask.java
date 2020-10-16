package sos.base.util.namayangar.sosLayer.selectedLayer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.OpenHighwayesState;
import sos.police_v2.state.OpenHighwayesState.Yal;
import sos.police_v2.state.preCompute.PoliceForceTask;
import sos.police_v2.state.preCompute.Task;

public class HighwayTask extends SOSAbstractSelectedComponent<PoliceForce> {

	public HighwayTask() {
		super(PoliceForce.class);
		setVisible(true);
	}
	@Override
	protected void paint(PoliceForce selectedObj, Graphics2D g, ScreenTransform transform) {
		 OpenHighwayesState state = ((PoliceForceAgent) selectedObj.getAgent()).getState(OpenHighwayesState.class);
		 PoliceForceTask ptask = ((PoliceWorldModel) model()).getPoliceTasks(selectedObj);
		ArrayList<Task<? extends StandardEntity>> mytask = state.agent_tasks.get(ptask);
		//		ArrayList<Task> mytask = tasks.get(selectedObj);
		//		if(mytask==null)
		//			return;
		g.setColor(Color.blue);
		g.setStroke(new BasicStroke(2));
		//
		int i = 0;
		for (Task<?> task : mytask) {
			NamayangarUtils.drawShape(task.getAreaPosition().getShape(), g, transform);
			NamayangarUtils.drawString("priority:" + i++, g, transform, task.getX(), task.getY());
		}
		ArrayList<Yal> myyals =state.agent_yals.get(ptask);
		g.setColor(Color.red);
		for (Yal yal : myyals) {
			NamayangarUtils.drawShape(yal.getHead().getShape(), g, transform);
			NamayangarUtils.drawShape(yal.getTail().getShape(), g, transform);
			NamayangarUtils.drawLine(yal.getHead(), yal.getTail(), g, transform);
		}
//		for (Yal yal : state.yals) {
//			NamayangarUtils.drawLine(yal.getHead(), yal.getTail(), g, transform);
//		}
		g.setStroke(new BasicStroke(1));
		//		
	}

	@Override
	public boolean isValid() {
		return model() instanceof PoliceWorldModel &&((PoliceForceAgent)model().sosAgent()).getState(OpenHighwayesState.class)!=null;
	}

}
