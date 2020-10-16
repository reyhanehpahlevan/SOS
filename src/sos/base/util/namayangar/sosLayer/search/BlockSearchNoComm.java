package sos.base.util.namayangar.sosLayer.search;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.entities.FireBrigade;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.sosFireZone.util.ConvexHull_arr_New;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.fire_v2.FireBrigadeAgent;
import sos.fire_v2.base.tools.FireStarZone;
import sos.fire_v2.target.SOSFireZoneSelector;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.tools.cluster.ClusterData;

public class BlockSearchNoComm extends SOSAbstractToolsLayer<FireStarZone> {

	public BlockSearchNoComm() {
		super(FireStarZone.class);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 5;
	}

	private AgentSearch<?> search() {
		return model().sosAgent().newSearch;
	}

	@Override
	protected void makeEntities() {
		SOSFireZoneSelector x = ((FireBrigadeAgent) model().sosAgent()).FDK.getInfoModel().getFireZoneSelector();
		setEntities(x.starCluster.getStarZones());
	}

	@Override
	protected Shape render(FireStarZone cd, Graphics2D g, ScreenTransform transform) {
		SOSFireZoneSelector x = ((FireBrigadeAgent) model().sosAgent()).FDK.getInfoModel().getFireZoneSelector();
		
		for (FireBrigade fb : x.zone_FireBrigade.get(cd.getIndex())) {
			
			ClusterData sub = search().getSearchWorld().getClusterData(fb);
			Color col = new Color(((sub.getIndex() + 1) * 100) % 255, ((sub.getIndex() + 1) * 140) % 255, ((sub.getIndex() + 1) * 30) % 255);
			g.setColor(col);

			for (Building b : sub.getBuildings()) {
				NamayangarUtils.fillShape(b.getShape(), g, transform);
			}
		}
		ConvexHull_arr_New convex = new ConvexHull_arr_New(cd.getZoneBuildings());
		NamayangarUtils.drawShape(convex.getShape(), g, transform);
		g.setColor(Color.black);
		NamayangarUtils.drawString("INDEX=" + cd.getIndex(), g, transform, (int) cd.getCx(), (int) cd.getCy());
		return convex.getShape();
	}

	@Override
	public JComponent getGUIComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid() {
		return model().sosAgent().messageSystem.type == Type.NoComunication;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(FireStarZone entity) {
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Search;
	}

}
