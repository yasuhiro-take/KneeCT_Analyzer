package imagejplugin.kneectanalyzer;

import java.awt.EventQueue;

import javax.swing.JFrame;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory; 

import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;  
import java.util.List;

import javax.swing.JTextPane;

import imagejplugin.kneectanalyzer.IJIF;


public class QuadrantAnalysisGUI {
	private static boolean opened;
	
	
	private JFrame frame;
	private JButton btn_1, btn_2, btn_3, btn_4, btn_open, btn_save, btn_close;
	private int status;
	private JTextPane messageBox;
	private ImageIcon arrowR, arrowD;
	private JLabel label_open, label_save, label_close;
	private JLabel label_1L, label_2L, label_3L, label_4L;
	private JToggleButton tbtn1, tbtn2;
	
	private String[] btntitles = { "Detect Quadrant System", "Manual-set Quadrant System", "Detect Tunnels", "Refresh Results" };
	private String[] btntitles3D = { "3D View (Bone Only)", "Manual-set Quadrant System", "3D View (Bone + Tunnel)", "Refresh Results" };
	private static ImageIcon icons[];
	private static String quadStatus[] = { "none", "fem", "tib", "fem&tib" };
	private static int mode2D3D = 1;
	
	/**
	 * Launch the application.
	 */
	public static void main(ImageIcon icons[], String[] args) {
		QuadrantAnalysisGUI.icons = icons;
		
		main(args);
	}
	
	public static void main(String[] args) {
		if (opened) {
			System.out.println("Quadrant System UI already exists.");
			return;
		}
		
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					QuadrantAnalysisGUI window = new QuadrantAnalysisGUI();
					window.frame.setVisible(true);
					opened = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public QuadrantAnalysisGUI() {
		status = 0;
		IJIF.initIJIF();
		
		initialize();
		suggestion();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 350, 450);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(56dlu;default):grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),})); 
		
		label_open = new JLabel("");	
		frame.getContentPane().add(label_open, "2, 2, center, default");
		arrowD = icons[4];
		
		label_save = new JLabel("");
		frame.getContentPane().add(label_save, "4, 2, center, default");
		
		label_close = new JLabel("");
		frame.getContentPane().add(label_close, "6, 2, center, default");
		
		btn_open = new JButton("");
		btn_open.setIcon(icons[0]);
		frame.getContentPane().add(btn_open, "2, 4");
		btn_open.addActionListener(new btnActionListener());
		
		btn_save = new JButton("");
		btn_save.setIcon(icons[1]);
		frame.getContentPane().add(btn_save, "4, 4");
		btn_save.addActionListener(new btnActionListener());
		
		btn_close = new JButton("");		
		btn_close.setIcon(icons[2]);
		frame.getContentPane().add(btn_close, "6, 4");
		btn_close.addActionListener(new btnActionListener());
		
		JButton button_q = new JButton("");
		button_q.setIcon(icons[3]);
		frame.getContentPane().add(button_q, "10, 4");
		
		JSeparator separator = new JSeparator();
		frame.getContentPane().add(separator, "2, 6, 9, 1");
		
		tbtn1 = new JToggleButton("2D");tbtn1.setSelected(true);
		frame.getContentPane().add(tbtn1, "2, 8, 6, 1");
		tbtn1.addActionListener(new togglebtnActionListener());
		tbtn2 = new JToggleButton("3D");tbtn2.setSelected(false);
		frame.getContentPane().add(tbtn2, "8, 8, 3, 1");
		tbtn2.addActionListener(new togglebtnActionListener());
		mode2D3D = 1;
		if (!IJIF.has3D())
			tbtn2.setEnabled(false);
		
		
		label_1L = new JLabel("");
		frame.getContentPane().add(label_1L, "2, 10");
		arrowR = icons[5];
		
		btn_1 = new JButton(btntitles[0]);
		frame.getContentPane().add(btn_1, "4, 10, 5, 1");
		btn_1.addActionListener(new btnActionListener());
		
		label_2L = new JLabel("");
		frame.getContentPane().add(label_2L, "2, 12");
			
		btn_2 = new JButton(btntitles[1]);
		frame.getContentPane().add(btn_2, "4, 12, 5, 1");
		btn_2.addActionListener(new btnActionListener());
		btn_2.setEnabled(false);
		
		label_3L = new JLabel("");
		frame.getContentPane().add(label_3L, "2, 14");
		
		btn_3 = new JButton(btntitles[2]);
		frame.getContentPane().add(btn_3, "4, 14, 5, 1");
		btn_3.addActionListener(new btnActionListener());
		
		label_4L = new JLabel("");
		frame.getContentPane().add(label_4L, "2, 16");
		
		btn_4 = new JButton(btntitles[3]);
		frame.getContentPane().add(btn_4, "4, 16, 5, 1");
		btn_4.addActionListener(new btnActionListener());
		
		JSeparator separator_1 = new JSeparator();
		frame.getContentPane().add(separator_1, "2, 18, 9, 1");
		
		messageBox = new JTextPane();
		messageBox.setEditable(false);
		frame.getContentPane().add(messageBox, "2, 20, 9, 1, fill, fill");
		
		frame.addWindowListener(new frameWindowListener());
	}
	
	private void resetLabelIcons()
	{
		label_open.setIcon(null);
		label_save.setIcon(null);
		label_close.setIcon(null);
		label_1L.setIcon(null);
		label_2L.setIcon(null);
		label_3L.setIcon(null);
		label_4L.setIcon(null);
	}
	
	public void suggestion()
	{
		this.suggestion(0);
	}
	
	public void suggestion(int ret)
	{
		resetLabelIcons();
		
		if (mode2D3D == 2) {
			suggestion3D(ret);
			return;
		}
		
		switch (status) {
		case 0: {
			String msg;
			if (IJIF.checkModels("FemOnly", "TibOnly")) {
				if (IJIF.hasBoundaryData()) {
					int qsystem = Quadrant.systemDetermined();
					msg = "Model data OK.\n";
					msg += "Available Quadrant system: " + quadStatus[qsystem] + ".\n";
					
					if (qsystem == 3) {
						msg += "You may want to proceed to *";
						msg += btntitles[2] + "*.";
						label_3L.setIcon(arrowR);
					} else {
						msg += "Determine quadrant coordinate system automatically (*" +btntitles[0] + "*), ";
						msg += "or manually (*" + btntitles[1] + "*).";
					
						label_1L.setIcon(arrowR);
						label_2L.setIcon(arrowR);
					}
				} else {
					msg = "Models available, but lacking automatically analyzed data. ";
					msg += "Determine quadrant coordinates manually (*" + btntitles[1] + "*).";
					label_2L.setIcon(arrowR);
				}
			} else {
				msg = "Open KCA-folder.";
				label_open.setIcon(arrowD);
			}
			messageBox.setText(msg);
			break; 
		}
		case 1: {
			String msg = null;
			int qsystem = Quadrant.systemDetermined();
			if (qsystem == 3) {
				msg = "Quadrant coordinates were successfully determined. ";
				msg += "You may want to proceed to *" + btntitles[2] + "*.";
				msg += " You can save quadrant system coord. data (*floppy icon*).";
				label_3L.setIcon(arrowR);
				label_save.setIcon(arrowD);
			} else {
				msg = "Available Quadrant system: " + quadStatus[qsystem] + ".\n";
				msg += "Proceed to *"+btntitles[1]+"* to manually determine Quadrant coordinates (by 2D/3D), or *";
				msg += btntitles[2]+"* for available system.";
				label_2L.setIcon(arrowR);
				label_3L.setIcon(arrowR);
			}
			messageBox.setText(msg);
			break;
		}
		case 2: break; 
		case 3: {
			String msg = "Detected tunnel data are listed in Results window, and 2D images (";
			msg += Quadrant.WINTITLE_FEM2D + " & " + Quadrant.WINTITLE_TIB2D + ").\n";
			msg += "Review those images, and remove non-tunnel data from Results (by right-click & choose clear).\n";
			msg += "You can add a small ROI on an auto-detected region through a selection tool and ";
			msg += "IJ's *Measure* command.\n";
			msg += "After clear & adding, proceed to *"+btntitles[3]+"*.";
			messageBox.setText(msg);
			label_4L.setIcon(arrowR);
			break;
			
		}
		case 4: {
			String msg = "Results & internal data are refreshed.\n";
			msg += "You can repeat *"+btntitles[2]+"* and *"+btntitles[3]+"*.\n";
			msg += "Copy the QuadX & Y data in Results to a spreadsheet software, or just save them through IJ.\n";
			msg += "You can save the 3D coords of tunnel locations (X, Y & Z in Results) in KCA folder ";
			msg += "(*floppy icon*).";
			messageBox.setText(msg);
			label_3L.setIcon(arrowR);
			label_4L.setIcon(arrowR);
			label_save.setIcon(arrowD);
			break;
		}
			
		case 5: {
			String msg = "Data stored in KCA folder. Close the image windows and initialize internal data (*X icon*).";
			messageBox.setText(msg);
			label_close.setIcon(arrowD);
			break;
		}
	
		case -1: // macro returned error;
			messageBox.setText("Unsuccessful finishing of a sublet command. This launcher no longer can help you. Sorry.");
			break;
		}
		
		frame.toFront();
	}
	
	void suggestion3D(int ret) {
		resetLabelIcons();
		
		switch (status) {
		case 0:
		case 1:
		case 2: {
			String msg;
			if (IJIF.checkModels("FemOnly", "TibOnly")) {
				int qsystem = Quadrant.systemDetermined();
				msg = "Model data OK.\n";
				msg += "Determined Quadrant System: " + quadStatus[qsystem] + ".\n";
				
				if (qsystem != 3) {
					msg += "You may want to create 3D model (*"+btntitles3D[0]+"*), then proceed to *";
					msg += btntitles3D[1]+"* for missing quadrant system.\n";
					msg += "You can also auto-detect quadrant system in *2D*.";
					label_1L.setIcon(arrowR);
					label_2L.setIcon(arrowR);
				} else {
					msg += "You may want to detect tunnels in 2D (*"+btntitles[2]+"*), ";
					msg += "followed by specifying tunnel center in *" + btntitles3D[2];
					msg += "* and calculation of Quadrant coordinates (*" + btntitles3D[3] + "*).";
					label_3L.setIcon(arrowR);
					label_4L.setIcon(arrowR);
				}	
			} else {
				msg = "Open KCA-folder.";
				label_open.setIcon(arrowD);
			}
			messageBox.setText(msg);
			break;
		}
		case 3: 
		case 4: {
			String msg;
			msg = "After *" + btntitles3D[2] + "*, point-out tunnel center by IJ's *point tool*, followed by *"+btntitles3D[3];
			msg += "* to calculate quadrant coordinates of the point.";
			messageBox.setText(msg);
			label_3L.setIcon(arrowR);
			label_4L.setIcon(arrowR);
			break;
		} 
		}
	}
	
	
	class btnActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Object o = e.getSource();
			char btn = ' ';
			
			if (o == btn_open) btn = 'o';
			else if (o == btn_save) btn = 's';
			else if (o == btn_close) btn = 'c';
			else if (o == btn_1) btn = '1';
			else if (o == btn_2) btn = '2';
			else if (o == btn_3) btn = '3';
			else if (o == btn_4) btn = '4';
			
			btnSwingWorker sw = new btnSwingWorker(btn);
			sw.execute();
		}
	}
	
	class togglebtnActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Object o = e.getSource();
			
			if (o == tbtn1 && mode2D3D == 2) {
				mode2D3D = 1;
				btn_1.setText(btntitles[0]);
				btn_2.setText(btntitles[1]);
				btn_3.setText(btntitles[2]);
				btn_4.setText(btntitles[3]);
				tbtn1.setSelected(true);
				tbtn2.setSelected(false);
				status = (status >= 3) ? 3 : 0;
			} else if (o == tbtn2 && mode2D3D == 1) {
				mode2D3D = 2;
				btn_1.setText(btntitles3D[0]);
				btn_2.setText(btntitles3D[1]);
				btn_3.setText(btntitles3D[2]);
				btn_4.setText(btntitles3D[3]);
				tbtn1.setSelected(false);
				tbtn2.setSelected(true);
				status = (status >= 3) ? 3 : 0;
			}
			
			suggestion(0);
		}
	}
	
	class frameWindowListener implements WindowListener {
		@Override
		public void windowClosed(WindowEvent arg0) {
			// TODO Auto-generated method stub
			opened = false;
		}
		@Override public void windowActivated(WindowEvent arg0) {}
		@Override public void windowClosing(WindowEvent arg0) {}
		@Override public void windowDeactivated(WindowEvent arg0) {}
		@Override public void windowDeiconified(WindowEvent arg0) {}
		@Override public void windowIconified(WindowEvent arg0) {}
		@Override public void windowOpened(WindowEvent arg0) {}
	}
	
	class btnSwingWorker extends IJIF_SwingWorker {
		private char btn;
		private String message;
		
		public btnSwingWorker(char btnType) 
		{
			btn = btnType;
			message = "";
			
			frame.getGlassPane().addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					e.consume();
				}
			});
			frame.getGlassPane().setVisible(true);
			frame.setAlwaysOnTop(true);
		}
		
		@Override
		public Integer doInBackground() {
			int r = -1;
			IJIF.setCallback(this);
			
			switch(btn) {
			case 'o':
				r = IJIF.openKCADirectory("TibOnly", "FemOnly");
				break;
			case 's':
				r = IJIF.Quad.saveData(mode2D3D);
				break;
			case 'c':
				r = IJIF.closeWorkingFiles("Base", "TibOnly", "FemOnly", "LFCOnly", "FemoralQuadrant", 
											"TibialQuadrant", "TunOnlyFem", "TunOnlyTib");
				break;
			case '1':
				r = (mode2D3D == 1) ? IJIF.Quad.detectSystem2D() : IJIF3D.Quad.view3D(false);
				break;
			case '2':
				r = -1;
				break;
			case '3':
				r = (mode2D3D == 1) ? IJIF.Quad.detectTunnel2D() : IJIF3D.Quad.view3D(true);		
				break;
			case '4':
				r = (mode2D3D == 1) ? IJIF.Quad.refreshResults2D() : IJIF3D.Quad.refreshResults3D();
				break;
			} 
			
			return (Integer)(r);
		}
		
		public void callback(String str) {
			if (str == null)
				message = "";
			else {
				message += str + "\n";
				publish(message);
			}
		}
		
		@Override
		protected void process(List<String> l) {
			messageBox.setText(l.get(0));
			frame.toFront();
		}
		
		@Override
		protected void done() {
			Integer r;
			
			try {
				r = get();
			} catch (Exception ex) {
				r = -1;
				IJX.error(ex.toString(), 0);
				ex.printStackTrace();
			}
			
			IJIF.setCallback(null);
			
			switch(btn) {
				case 'o':
					suggestion();
					
					break;
				case 's':
					if (status == 4)
						status = 5;
					
					suggestion();
					
					break;
				case 'c':
					status = 0;
					
					resetLabelIcons();
					suggestion();
					break;
				case '1':
				case '2':
				case '3':
				case '4':
					if (r == 0)
						status = Character.getNumericValue(btn);
					else
						status = -1;

					suggestion();
					break;
				
			}
			
			frame.getGlassPane().setVisible(false);
            MouseListener[] listeners = frame.getGlassPane().getMouseListeners();
            
            for (MouseListener listener: listeners) {
                frame.getGlassPane().removeMouseListener(listener);
            }
            frame.setAlwaysOnTop(false);
		}
	}
	
	
	
	

}

/*
label_1L = new JLabel("");
frame.getContentPane().add(label_1L, "2, 8, center, default");
arrowR = icons[5];

btn_1 = new JButton(btntitles[0]);
frame.getContentPane().add(btn_1, "4, 8, 5, 1");
btn_1.addActionListener(new btnActionListener());

label_2L = new JLabel("");
frame.getContentPane().add(label_2L, "2, 10");
	
btn_2 = new JButton(btntitles[1]);
frame.getContentPane().add(btn_2, "4, 10, 5, 1");
btn_2.addActionListener(new btnActionListener());

label_3L = new JLabel("");
frame.getContentPane().add(label_3L, "2, 12");

btn_3 = new JButton(btntitles[2]);
frame.getContentPane().add(btn_3, "4, 12, 5, 1");
btn_3.addActionListener(new btnActionListener());

label_4L = new JLabel("");
frame.getContentPane().add(label_4L, "2, 14");

btn_4 = new JButton(btntitles[3]);
frame.getContentPane().add(btn_4, "4, 14, 5, 1");
btn_4.addActionListener(new btnActionListener());

JSeparator separator_1 = new JSeparator();
frame.getContentPane().add(separator_1, "2, 16, 9, 1");

messageBox = new JTextPane();
messageBox.setEditable(false);
frame.getContentPane().add(messageBox, "2, 18, 9, 1, fill, fill");
*/