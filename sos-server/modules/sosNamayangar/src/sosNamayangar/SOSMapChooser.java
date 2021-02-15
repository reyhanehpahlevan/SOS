package sosNamayangar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class SOSMapChooser {
	static String startDirctory = ".";
//	static String startDirctory = "boot";
	static File mapsDirectory = new File(startDirctory + "/maps/");
	private JTextArea resultPane;

	public SOSMapChooser() {

		 Vector<String> list = getMaps();


		final JList<String> fileList = new JList<String>(list);
		JFrame f = new JFrame("SOS Map Chooser");
		JButton refresh=new JButton("Refresh");
		refresh.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fileList.setListData(getMaps());
			}
		});
		f.add(refresh,BorderLayout.NORTH);
		JPanel jp = new JPanel();
		final JCheckBox autorunCheck = new JCheckBox("autorun",false);
		final JCheckBox nomenuCheck = new JCheckBox("nomenu",true);
		final JCheckBox clusterRunCheck = new JCheckBox("cluster",false);
		final JCheckBox compileCheck = new JCheckBox("compileCode",false);
//		jp.add(autorunCheck);
		jp.add(nomenuCheck);
		jp.add(compileCheck);
		jp.add(clusterRunCheck);
		JButton showMapButton = new JButton("Show Map");
		final JTextField jtf = new JTextField();

		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		ActionListener action = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				resultPane.setText("");

				try {
					File startDirctoryFile = new File(startDirctory);
					if (System.getProperty("os.name").contains("Linux") ||System.getProperty("os.name").contains("Mac")) {
						killallKernel();
						System.out.println(fileList.getSelectedValue());
						String command;
						if(clusterRunCheck.isSelected()){
							startDirctoryFile=new File(startDirctory+"/script");
							if(compileCheck.isSelected())
								command= "./run-compile-multi.sh";
							else
								command= "./run-multi.sh";
						}else
							command= "./SOSMapChooserStartKernel.sh";
						ArrayList<String> args=new ArrayList<String>();
						args.add(fileList.getSelectedValue()+"");
						args.add("SOS");
						if(nomenuCheck.isSelected())
							args.add("--nomenu");
						if(nomenuCheck.isSelected())
							args.add("--autorun");
						runCommandInNewThread(command, args.toArray(new String[0]), startDirctoryFile);
					} else {
						String command = "cmd /c start /d \""
								+ startDirctoryFile.getAbsolutePath()
								+ "\" sosstart.bat --fullmappath "
								+ fileList.getSelectedValue();
						if (nomenuCheck.isSelected())
							command += " --nomenu";
						command += " " + jtf.getText();
						System.out.println(command);
						Runtime.getRuntime().exec(command, null, startDirctoryFile);
						// Process p=Runtime.getRuntime().exec(command);
						// BufferedReader br=new BufferedReader(new
						// InputStreamReader(p.getInputStream()));
						// String line=null;
						// while((line=br.readLine())!=null){
						// System.out.println(line);
						// }
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

		};

		showMapButton.addActionListener(action);
		jtf.addActionListener(action);
		f.add(new JScrollPane(fileList));
		jp.add(showMapButton);
		JPanel jpanel = new JPanel(new GridLayout(2, 1));
		jpanel.add(jtf);
		jpanel.add(jp);
		resultPane = new JTextArea(5,80);
		resultPane.setBackground(Color.black);
		resultPane.setForeground(Color.white);
		resultPane.setEnabled(false);
		JScrollPane jsp = new JScrollPane(resultPane);
		JPanel tmpp = new JPanel();
		tmpp.setLayout(new GridLayout(2, 1));
		tmpp.add(jpanel);
		tmpp.add(jsp);
		f.add(tmpp, BorderLayout.SOUTH);
		f.setSize(400, 600);
		f.setVisible(true);
		// action.actionPerformed(null);
	}

	private Vector<String> getMaps() {
		Vector<String> list = new Vector<String>();
		findMaps(mapsDirectory, list);
		Collections.sort(list);
		return list;
	}

	private void findMaps(File currentDirectory, Vector<String> list) {
		if (!currentDirectory.isDirectory())
			return;
		for (File file : currentDirectory.listFiles()) {
			if (file.isDirectory() && file.getName().equals("map")) {
				list.add(file
						.getParentFile()
						.getAbsolutePath()
						.substring(mapsDirectory.getAbsolutePath().length() + 1));
			} else
				findMaps(file, list);
		}
	}

	public static void main(String[] args) throws IOException {
		setToolkit();
		new SOSMapChooser();
//		System.out.println(runCommandAndGetResult("ls",new String[]{"s"},null));
		// System.out.println(mapsDirectory.getAbsolutePath());
		// killallKernel();
		// File f = new File("/home/ali/Desktop/sos-server/boot");
		// // String
		// //
		// command="xterm -e 'cd "+f.getAbsolutePath()+"; ./startkernel.sh Kobe sos'";
		// // String command="xterm -e 'bash startkernel.sh Kobe sos'";
		// String command = "bash SOSMapChooserStartKernel.sh Kobe sos";
		// // String command="ls";
		// System.out.println(command);
		// // Runtime.getRuntime().exec(command, null, f);
		// Process p = Runtime.getRuntime().exec(command, null, f);
		// BufferedReader br = new BufferedReader(new InputStreamReader(
		// p.getInputStream()));
		// String line = null;
		// while ((line = br.readLine()) != null) {
		// System.out.println(line);
		// }
	}

	public void runCommandInNewThread(final String command,
			final String[] args, final File currentDirectory) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				runCommand(command, args, currentDirectory);
			}
		}).start();

	}

	public void runCommand(String command, String[] args, File currentDirectory) {
		try {
			ArrayList<String> com = new ArrayList<String>();
			com.add(command);
			if (args != null)
				com.addAll(Arrays.asList(args));
			ProcessBuilder pb = new ProcessBuilder(com);
			pb.directory(currentDirectory);
			pb.redirectErrorStream(true);
			System.out.println(command);
			Process p = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				resultPane.setText(resultPane.getText() + "\n" + line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static String runCommandAndGetResult(String command,
			File currentDirectory) {
		return runCommandAndGetResult(command, null, currentDirectory);
	}

	public static String runCommandAndGetResult(String command, String[] args,
			File currentDirectory) {
		String result = "";
		try {
			ArrayList<String> com = new ArrayList<String>();
			com.add(command);
			if (args != null)
				com.addAll(Arrays.asList(args));
			ProcessBuilder pb = new ProcessBuilder(com);
			pb.directory(currentDirectory);
			pb.redirectErrorStream(true);
			System.out.println(command);
			Process p = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				result += "\n" + line;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;

	}

	private static void killallKernel() {
		killProcessByCommand("xterm -T kernel");
	}

	private static void killProcessByCommand(String string) {
		final File f = new File(startDirctory);
		String[] s = new String[] { string };
		String find = "./showPID.sh";
		String result = runCommandAndGetResult(find, s, f);
		System.out.println(result);
		String[] all = result.split("\n");
		for (String pid : all) {
			runCommandAndGetResult("kill", new String[] { pid.trim() }, f);
		}
	}
	private static void setToolkit() {
		try {
			Toolkit xToolkit = Toolkit.getDefaultToolkit();
			java.lang.reflect.Field awtAppClassNameField;
			awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
			awtAppClassNameField.setAccessible(true);
			awtAppClassNameField.set(xToolkit, "MapChooser");
		} catch (Exception e1) {
		}
	}
}
