package sosNamayangar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

/**
 * 
 * @author Ali
 * 
 */
public class Splash extends JWindow {
	 private static final long serialVersionUID = 1L;
	 private int duration;

	 public Splash(int d) {
		  duration = d;
	 }

	 public void showSplash() {
//		  showSplash("SOS Namayangar");
	 }
	 public void showSplash(String title) {
		  JPanel content = (JPanel) getContentPane();
		  content.setBackground(Color.white);
		  setAlwaysOnTop(true);

		  int width = 450;
		  int height = 115;
		  Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		  int x = (screen.width - width) / 2;
		  int y = (screen.height - height) / 2;
		  setBounds(x, y, width, height);

		  // Build the splash screen
//System.out.println(Splash.class.getClassLoader().getResource(""));
		  JLabel label;
		  if(Splash.class.getClassLoader().getResource("sosNamayangar/splash.gif")==null)
			  label = new JLabel();
		  else
			  label = new JLabel(new ImageIcon(Splash.class.getClassLoader().getResource("sosNamayangar/splash.gif")));
		  
		  JLabel copyrt = new JLabel(title+" is starting...", JLabel.CENTER);
		  copyrt.setFont(new Font("Sans-Serif", Font.BOLD, 12));
		  content.add(label, BorderLayout.CENTER);
		  content.add(copyrt, BorderLayout.SOUTH);
		  content.setBorder(BorderFactory.createLineBorder(Color.green.darker(), 10));

		  setVisible(true);

		  try {
				Thread.sleep(duration);
		  } catch (Exception e) {
		  }

	 }

	 public void showSplashAndExit() {
		  showSplash();
		  setVisible(false);
		  System.exit(0);
	 }

	 public void exit() {
		  setVisible(false);
	 }

}
