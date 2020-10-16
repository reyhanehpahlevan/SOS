package sos.base.util.namayangar.sosLayer.selectedLayer;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.OpenHighwayesState;
import sos.police_v2.state.preCompute.Task;

public class Highways extends SOSAbstractSelectedComponent<PoliceForce>{

	public Highways() {
		super(PoliceForce.class);
	}
	@Override
	protected void paint(PoliceForce selectedObj, Graphics2D g, ScreenTransform transform) {
		PoliceForceAgent agent=(PoliceForceAgent) model().sosAgent();
		ArrayList<Task<? extends StandardEntity>> tasks = agent.getState(OpenHighwayesState.class).agent_tasks.get(((PoliceWorldModel)model()).getPoliceTasks(selectedObj));
		if(!tasks.isEmpty()){
			Path2D.Double path=new Path2D.Double();
			path.moveTo(transform.xToScreen(tasks.get(0).getX()),transform.yToScreen(tasks.get(0).getY()));
			for (Task<? extends StandardEntity> task : tasks) {
				path.lineTo(transform.xToScreen(task.getX()), transform.yToScreen(task.getY()));
			}
			g.draw(path);
		}
	}

	@Override
	public boolean isValid() {
		return model() instanceof PoliceWorldModel &&((PoliceForceAgent)model().sosAgent()).getState(OpenHighwayesState.class)!=null;
	}
	

}
