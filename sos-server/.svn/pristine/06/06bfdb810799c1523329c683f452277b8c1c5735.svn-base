package mapScaler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;


public class StaXParser {
	static final String NODE = "Node";
	static final String ID = "id";
	static final String PP = "pointProperty";
	static final String POINT = "Point";
	static final String COORDINATES = "coordinates";
	public List<Point> items = new ArrayList<Point>();
	@SuppressWarnings({ "unchecked" })
	public List<Point> readConfig(String configFile) {
		
		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			InputStream in = new FileInputStream(configFile);
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			Point item = null;
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();
				if (event.isStartElement()) {
					StartElement startElement = event.asStartElement();
					if (startElement.getName().getLocalPart() == (NODE)) {
						item = new Point();
						Iterator<Attribute> attributes = startElement
								.getAttributes();
						while (attributes.hasNext()) {
							Attribute attribute = attributes.next();
							if (attribute.getName().getLocalPart().equals(ID)) {
								item.setId(Integer.parseInt(attribute
										.getValue()));
							}
						}
					}
					if (event.asStartElement().getName().getLocalPart()
							.equals(COORDINATES)) {
						event = eventReader.nextEvent();
						String s = event.asCharacters().getData();
						StringTokenizer st = new StringTokenizer(s, ",");
						item.setX(Double.parseDouble(st.nextToken()));
						item.setY(Double.parseDouble(st.nextToken()));
						continue;
					}
				}
				// If we reach the end of an item element we add it to the list
				if (event.isEndElement()) {
					EndElement endElement = event.asEndElement();
					if (endElement.getName().getLocalPart() == (NODE)) {
						items.add(item);
					}
				}

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		return items;
	}

}