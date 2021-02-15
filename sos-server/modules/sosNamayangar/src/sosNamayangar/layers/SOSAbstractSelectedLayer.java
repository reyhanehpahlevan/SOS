package sosNamayangar.layers;


public abstract class SOSAbstractSelectedLayer extends SOSAbstractToolsLayer<Object> {

	
	public void selected(Object object) {
		if (entities != null)
			entities.clear();
		if (object != null) {
			entities.add(object);
		}
	}
	public void addSelected(Object object) {
		if (object != null) {
			entities.add(object);
		}else
			entities.clear();
	}
}
