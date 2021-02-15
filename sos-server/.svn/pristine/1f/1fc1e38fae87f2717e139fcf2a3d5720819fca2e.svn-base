package sosNamayangar.message_decoder;

/**
 * 
 * @author Ali
 * 
 */
public interface MessageConstants {
	static final String AMBULANCE_XML_FILE_NAME = "xml/ambulance.xml", FIRE_XML_FILE_NAME = "xml/fire.xml", POLICE_XML_FILE_NAME = "xml/police.xml", CENTER_XML_FILE_NAME = "xml/center.xml", LowCommunicaion_XML_FILE_NAME = "xml/centerlow.xml";

	final static int HEADER_SIZE = 5;
	final static boolean THROW_EXCEPTION = true;

	enum Destination {
		All, Fire, Police, Ambulance, FirePolice, FireAmbulance, PoliceAmbulance
	}

	enum MessageCenterType {
		FullCenter, TwoCenter, OneCenter, NoCenterA, NoCenterB, NoComunication
	}

	enum ChannelType {
		Voice, Radio
	}

	enum Noise {
		Input, Output
	}

	enum Type {
		WithMiddleMan,NoMiddleMan,NoComunication,LowComunication,CenteralMiddleMan
	}
	
}
