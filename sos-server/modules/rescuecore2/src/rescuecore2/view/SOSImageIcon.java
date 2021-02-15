package rescuecore2.view;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

import javax.swing.ImageIcon;

public class SOSImageIcon extends ImageIcon{

	public SOSImageIcon(URL url) {
		System.out.println(url);
		if(url!=null){
			Image image = Toolkit.getDefaultToolkit().getImage(url);
	        if (image == null) {
	            return;
	        }
	        loadImage(image);
		}
			
	}
	
}
