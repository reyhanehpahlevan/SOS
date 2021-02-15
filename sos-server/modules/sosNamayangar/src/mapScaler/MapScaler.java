package mapScaler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class MapScaler {

	/**
	 * @param args
	 */
	static float SCALE = .5f;

	public static void main(String[] args) {

		try {
			writeNewMap();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void writeNewMap() throws Exception {
		String oldMapName = "/home/sos/Desktop/sos-server/boot/maps/test/2012/Mexico1/map/originalmap.gml";
		String newMapName = "/home/sos/Desktop/sos-server/boot/maps/test/2012/Mexico1/map/map5.gml";
		File f = new File(newMapName);
		File f1 = new File(oldMapName);
		BufferedWriter w = new BufferedWriter(new FileWriter(f,false));
		BufferedReader br = new BufferedReader(new FileReader(f1));
		List<Point> points = readPoints(oldMapName);
		scalPoints(points);
		String s;
		while (!(s = br.readLine()).trim().equals("<rcr:nodelist>"))
			w.write(s + '\n');
		w.write(s + '\n');
		writePoints(w, points);
		while (!(s = br.readLine()).trim().equals("</rcr:nodelist>")) {
		}
		w.write(s + '\n');
		while ((s = br.readLine()) != null)
			w.write(s + '\n');
		w.flush();
		w.close();
		br.close();
	}

	private static void scalPoints(List<Point> points) {
		for (Point p : points) {
			p.setX(p.getX() * SCALE);
			p.setY(p.getY() * SCALE);
		}

	}

	private static void writePoints(BufferedWriter w, List<Point> points)
			throws IOException {
		for (Point p : points) {
			w.write("    <gml:Node gml:id=\"" + p.getId() + "\">\n");
			w.write("      <gml:pointProperty>\n");
			w.write("        <gml:Point>\n");
			w.write("          <gml:coordinates>" + p.getX() + "," + p.getY()
					+ "</gml:coordinates>\n");
			w.write("        </gml:Point>\n");
			w.write("      </gml:pointProperty>\n");
			w.write("    </gml:Node>\n");
		}
	}

	private static List<Point> readPoints(String mapName) {
		StaXParser read = new StaXParser();
		return read.readConfig(mapName);
	}

}
