package sosNamayangar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
/**
 * 
 * @author Ali
 *
 */
public class StartUpOptionPanel {
    private static final String DEFAULT_HOST = "127.0.0.1";
    public static void sosStaring(String... args) {
    	
    	final JFrame sosStratFrame=new JFrame("SOS Starting dialog");
    	JPanel optionPanel =new JPanel(new GridLayout(4, 1));
    	JPanel mainPanel= new JPanel(new BorderLayout());
    	final JToggleButton all =new JToggleButton("All");
    	final JToggleButton fire =new JToggleButton("Fire");
    	final JToggleButton police =new JToggleButton("Police");
    	final JToggleButton ambulance =new JToggleButton("Ambulance");
    	final JCheckBox useDefaultConnectionSettings=new JCheckBox("use Default Connection Settings",true);
    	
    	final JTextField host =new JTextField(DEFAULT_HOST);
    	final JTextField port =new JTextField("7000");
    	final JPanel connectionPanel=new JPanel();
    	connectionPanel.add(new Label("Host:Port"));
    	connectionPanel.add(host);
    	connectionPanel.add(new Label(":"));
    	connectionPanel.add(port);
    	connectionPanel.setVisible(false);
    	useDefaultConnectionSettings.addActionListener(new ActionListener() {
    	    public void actionPerformed(ActionEvent arg0) {
    		connectionPanel.setVisible(!connectionPanel.isVisible());
    	    }
    	}) ;
    	JButton start = new JButton("Start");
    	start.addActionListener(new ActionListener() {
    	    public void actionPerformed(ActionEvent e) {
    		ArrayList<String> argsList=new ArrayList<String>();
    		if(all.isSelected())
    		    argsList.add("-all");
    		if(fire.isSelected())
    		    argsList.add("-fire");
    		if(ambulance.isSelected())
    		    argsList.add("-ambulance");
    		if(police.isSelected())
    		    argsList.add("-police");
    		if(!useDefaultConnectionSettings.isSelected()){
    		    argsList.add("-h "+host.getText());
    		    argsList.add("-p "+port.getText());
    		}
    		
    		sosStratFrame.setVisible(false);
    	    }
    	});
    	optionPanel.add(all);
    	optionPanel.add(fire);
    	optionPanel.add(police); 
    	optionPanel.add(ambulance);
    	optionPanel.add(useDefaultConnectionSettings);
    	optionPanel.add(connectionPanel);
    	
    	mainPanel.add(optionPanel,BorderLayout.CENTER);
    	mainPanel.add(new Label("SOS options"),BorderLayout.BEFORE_FIRST_LINE);
    	mainPanel.add(start,BorderLayout.SOUTH);
    	
    	sosStratFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	sosStratFrame.add(mainPanel);
    	
    	sosStratFrame.setAlwaysOnTop(true);
    	sosStratFrame.pack();
//    	sosStratFrame.setSize(200,200);
    	Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    	sosStratFrame.setLocation((screen.width / 2) - (sosStratFrame.getWidth() / 2), (screen.height / 2) - (sosStratFrame.getHeight() / 2)-170);
    	sosStratFrame.setVisible(true);
    	
        }

}
