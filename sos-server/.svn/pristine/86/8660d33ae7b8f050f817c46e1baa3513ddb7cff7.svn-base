package sosNamayangar.message_decoder;

import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import rescuecore2.standard.entities.StandardWorldModel;
import sosNamayangar.SOSWorldModel;
import sosNamayangar.message_decoder.blocks.DataArrayList;
import sosNamayangar.message_decoder.blocks.XmlBlockContent;


/**
 * @author Ali
 */
public class ReadXml implements MessageConstants {

	public static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	public static HashMap<String, XmlBlockContent> blocks;
	public static HashMap<Destination, ArrayList<XmlBlockContent>> subBlocks;
	public static HashMap<String, Integer> headerToIndex = new HashMap<String, Integer>();
	public static HashMap<Integer, String> indexToHeader = new HashMap<Integer, String>();
	private final SOSWorldModel model;

	public ReadXml(SOSWorldModel model) {
		this.model = model;
		if (blocks == null) {
			blocks = new HashMap<String, XmlBlockContent>();
			subBlocks = new HashMap<Destination, ArrayList<XmlBlockContent>>();
			//				if (sosAgent instanceof AmbulanceTeamAgent)
			//					 parsXML(AMBULANCE_XML_FILE_NAME);
			//				if (sosAgent instanceof FireBrigadeAgent)
			//					 parsXML(FIRE_XML_FILE_NAME);
			//				if (sosAgent instanceof PoliceForceAgent)
			//					 parsXML(POLICE_XML_FILE_NAME);
			//				if (sosAgent instanceof CenterAgent)
//			if(sosAgent.messageSystem.type==Type.LowComunication)
//				parsXML(LowCommunicaion_XML_FILE_NAME);
//			else				
				parsXML(CENTER_XML_FILE_NAME);

		}
	}

	public void parsXML(String fileName) {
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document xmlFile = db.parse(fileName);
			XmlBlockContent blockContent;

			NodeList destinationList = xmlFile.getElementsByTagName("Destination");
			int index = 1;
			for (int d = 0; d < destinationList.getLength(); d++) {
				NodeList destinationContent = destinationList.item(d).getChildNodes();
				String dest = ((Element) destinationList.item(d)).getAttribute("name");
				//Destination destination = Destination.valueOf(dest);
				Destination destination = null;
				subBlocks.put(destination, new ArrayList<XmlBlockContent>());
				NodeList blockList = ((Element) destinationContent).getElementsByTagName("Message");
				for (int i = 0; i < blockList.getLength(); i++) {
					Element messageContent = (Element) blockList.item(i).getChildNodes();

					String header = messageContent.getAttribute("header");

					headerToIndex.put(header, index);
					indexToHeader.put(index, header);
					index++;
					NodeList dataList = messageContent.getElementsByTagName("Data");
					blockContent = new XmlBlockContent(dataList.getLength(), header);
//					blockContent.setDestination(destination);
					String maxSize = messageContent.getElementsByTagName("MaxSize").item(0).getFirstChild().getNodeValue();
					blockContent.setMaxSize(Integer.parseInt(maxSize));
					String priority = messageContent.getElementsByTagName("Priority").item(0).getFirstChild().getNodeValue();
					blockContent.setPriority(Integer.parseInt(priority));

					for (int j = 0; j < dataList.getLength(); j++) {
						String data = ((Element) dataList.item(j)).getAttribute("name");
						int value = Integer.parseInt(dataList.item(j).getFirstChild().getNodeValue());
						if (data.endsWith("index")) {
							if (data.equalsIgnoreCase(MessageXmlConstant.DATA_BUILDING_INDEX)) {
								value = determineNeededBits(model.buildings().size());
							}
							if (data.equalsIgnoreCase(MessageXmlConstant.DATA_AREA_INDEX)) {
								value = determineNeededBits(model.areas().size());
							}
							// if (data.equalsIgnoreCase("RefugeIndex")) {
							// value = determineNeededBits(model.refuges().size());
							// mcLog.trace("Refuges size:" + model.refuges().size() + "-->Needed bits:" + value);
//							}
							if (data.equalsIgnoreCase(MessageXmlConstant.DATA_ROAD_INDEX)) {
								value = determineNeededBits(model.roads().size());
							}
							if (data.equalsIgnoreCase(MessageXmlConstant.DATA_POLICE_INDEX)) {
								value = determineNeededBits(model.policeForces().size());
							}
							if (data.equalsIgnoreCase(MessageXmlConstant.DATA_AMBULANCE_INDEX)) {
								value = determineNeededBits(model.ambulanceTeams().size());
							}
							if (data.equalsIgnoreCase(MessageXmlConstant.DATA_FIRE_INDEX)) {
								value = determineNeededBits(model.fireBrigades().size());
							}
							if (data.equalsIgnoreCase(MessageXmlConstant.DATA_AGENT_INDEX)) {
								value = determineNeededBits(model.agents().size());
							}
							
						}
						blockContent.addData(data, value);
						if (j == 0) {
							boolean hasKey = messageContent.getElementsByTagName("HasKey").item(0).getFirstChild().getNodeValue().equalsIgnoreCase("true");
							if (hasKey)
								blockContent.setDataKey(data);
						}
					}

					blocks.put(header, blockContent);
					subBlocks.get(destination).add(blockContent);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int determineNeededBits(int size) {
		return (int) Math.ceil((Math.log(size) / Math.log(2)));
	}

	//	 private static int getStringToIndex(String headerString) {
	//		  Integer temp = headerToIndex.get(headerString);
	//		  if (temp == null)
	//				if (THROW_EXCEPTION)
	//					 throw new Error("Header '" + headerString + "' dosen't initialise yet!");
	//				else {
	//					 System.err.println("Header '" + headerString + "' dosen't initialise yet!");
	//					 return 0;
	//				}
	//		  else
	//				return temp.intValue();
	//	 }

//	public static void main(String[] args) {
//		new ReadXml(new AmbulanceTeamAgent());
//		System.out.println(blocks);
//		System.out.println(headerToIndex);
//		System.out.println(indexToHeader);
//
//	}

	public static int getBlockSize(String header) {
		int size = HEADER_SIZE;
		if (blocks.get(header) == null)
			return 0;
		
		DataArrayList xmlData = ReadXml.blocks.get(header).data();
		for (int i = 0; i < xmlData.size(); i++) {
			size += xmlData.getValue(i);
		}

		return size;
	}

	public static String indexToHeader(int index) {
		return indexToHeader.get(index);
	}

	public static Integer headerToIndex(String header) {
		return headerToIndex.get(header);
	}

	public static ArrayList<XmlBlockContent> blocksIn(Destination destination) {
		return subBlocks.get(destination);
	}

}
