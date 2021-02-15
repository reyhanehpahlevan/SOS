package sosNamayangar;

import java.util.ArrayList;

import rescuecore2.LaunchComponents;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;

/**
 * 
 * @author Ali
 * 
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
        Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
        Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
        ArrayList<String> arglist=new ArrayList<String>();
        arglist.add("sosNamayangar.SOSViewer");
        arglist.add("-c");
        arglist.add( "config/kernel.cfg");
        for (String string : args) {
			arglist.add(string);
		}
			LaunchComponents.main(arglist.toArray(new String[0]));
	}

}
