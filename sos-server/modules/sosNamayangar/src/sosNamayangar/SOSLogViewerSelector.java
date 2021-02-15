package sosNamayangar;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import rescuecore2.view.Icons;
import sosNamayangar.NewSOSViewer;

/**
 * 
 * @author Yoosef
 * 
 */
public class SOSLogViewerSelector extends JFrame implements DropTargetListener {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	public static String[]		args;
	private DropTarget			dropTarget;
	static JTextArea			area				= new JTextArea();

	public SOSLogViewerSelector() {
		super("SOS LogViewer");
		setLayout(null);
		setSize(400, 400);
		setLocation(0, 0);
		// JTextArea area=new JTextArea();
		area.setSize(380, 300);
		area.setLocation(0, 0);
		// add(area);

		JScrollPane pan = new JScrollPane(area);
		pan.setSize(380, 300);
		add(pan);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		init();
	}

	public void init() {
		if (dropTarget == null) {
			dropTarget = new DropTarget(this, this);
		}
	}

	public void dragEnter(DropTargetDragEvent arg0) {
	}

	public void dragOver(DropTargetDragEvent arg0) {
	}

	public void dropActionChanged(DropTargetDragEvent arg0) {
	}

	public void dragExit(DropTargetEvent arg0) {
	}

	public void drop(DropTargetDropEvent evt) {
		int action = evt.getDropAction();
		evt.acceptDrop(action);
		try {
			Transferable data = evt.getTransferable();
			if (data.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				List<?> list = (List<?>) data.getTransferData(
						DataFlavor.javaFileListFlavor);
				processFiles(list);
			}
		} catch (UnsupportedFlavorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			evt.dropComplete(true);
			repaint();
		}
	}

	private void processFiles(List<?> files) throws IOException {
		try {
			NewSOSViewer.main(new String[] { "-c", "config","--kernel.logname="+files.get(0).toString() }/*, area*/);
//			NewSOSFileLogReader.main(new String[] { files.get(0).toString() }/*, area*/);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public static void main(String[] args) {
//		System.out.println(Icons.class.getClassLoader().getResource("rescuecore2/view/tick.png"));
		new SOSLogViewerSelector();
		
		//LogViewer.main(new String[] { "-c", "config", "sss" }, area);
	}
}