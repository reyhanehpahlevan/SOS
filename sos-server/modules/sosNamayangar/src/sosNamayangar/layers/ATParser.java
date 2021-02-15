package sosNamayangar.layers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class ATParser {
	public static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	
	public void  readXML(String fileName) {
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document xmlFile = db.parse(fileName);
			NodeList cycleList = xmlFile.getElementsByTagName("Cycle");
			Node item = cycleList.item(10);
			NodeList childs = item.getChildNodes();
			Node update = childs.item(0);
//			update..
//			for (iterable_type iterable_element : cycleList.) {
//				
//			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

}