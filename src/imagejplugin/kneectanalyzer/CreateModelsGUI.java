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
import javax.swing.JSeparator;

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

public class CreateModelsGUI {
	private static boolean opened;
	
	private String wintitle3D;
	private JFrame frame;
	private JButton btn_1, btn_2, btn_3, btn_4, btn_open, btn_save, btn_close;
	private int status;
	private JTextPane messageBox;
	private ImageIcon arrowR, arrowD;
	private JLabel label_open, label_save, label_close;
	private JLabel label_1L, label_2L, label_3L, label_4L;
	
	
	private String[] btntitles = { "Binarize", "3D Viewer", "Alignment correction", "Auto edit" };
	private static ImageIcon icons[];
	
	/**
	 * Launch the application.
	 */
	public static void main(ImageIcon icons[], String[] args) {
		CreateModelsGUI.icons = icons;
		
		main(args);
	}
	
	public static void main(String[] args) {
		if (opened) {
			System.out.println("Create Models UI already exists.");
			return;
		}
		
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CreateModelsGUI window = new CreateModelsGUI();
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
	public CreateModelsGUI() {
		status = 0;
		wintitle3D = null;
		IJIF.initIJIF();
		
		initialize();
		suggestion();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 350, 400);
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
		
		label_1L = new JLabel("");
		frame.getContentPane().add(label_1L, "2, 8, center, default");
		arrowR = icons[5];
		
		btn_1 = new JButton(btntitles[0]);
		/* btn_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});*/
		frame.getContentPane().add(btn_1, "4, 8, 5, 1");
		btn_1.addActionListener(new btnActionListener());
		
		label_2L = new JLabel("");
		frame.getContentPane().add(label_2L, "2, 10");
		
		btn_2 = new JButton("<html><center>"+btntitles[1] + "<br>for epicondyle selection</center></html>");
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
	
		if (!IJIF.has3D())
			btn_2.setEnabled(false);
		
		frame.addWindowListener(new frameWindowListener());
	}
	
	class frameWindowListener implements WindowListener {

		@Override
		public void windowActivated(WindowEvent arg0) {}

		@Override
		public void windowClosed(WindowEvent arg0) {
			// TODO Auto-generated method stub
			opened = false;
		}

		@Override
		public void windowClosing(WindowEvent arg0) {}

		@Override
		public void windowDeactivated(WindowEvent arg0) {}

		@Override
		public void windowDeiconified(WindowEvent arg0) {}

		@Override
		public void windowIconified(WindowEvent arg0) {}

		@Override
		public void windowOpened(WindowEvent arg0) {}
		
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
		}
		
		@Override
		public Integer doInBackground() {
			int r = -1;
			IJIF.setCallback(this);
			
			switch(btn) {
			case 'o':
				r = IJIF.openKCADirectory();
				r = (r >= 0) ? 0 : -1;
				
				break;
			case 's':
				if (status == 4)
					r = IJIF.Modeler.save("Base", "TibOnly", "FemOnly");
				else if (status == 3)
					r = IJIF.Modeler.save("Base");
				else
					r = IJIF.Modeler.saveOpened();
					
				r = 0;
				break;
			case 'c':
				r = IJIF.closeWorkingFiles(wintitle3D, "Base", "TibOnly", "FemOnly");
				break;
			case '1':
				r = IJIF.Modeler.binarize();
				break;
			case '2':
				wintitle3D = IJIF3D.Modeler.determineFEC();
				if (wintitle3D != null)
					r = 0;
				else
					r = -1;
				
				break;
			case '3':
				r = IJIF.Modeler.align(wintitle3D);
						
				break;
			case '4':
				r = IJIF.Modeler.autoEdit();
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
					wintitle3D = null;
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
		}
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
			
		switch (status) {
		case 0:
			
			if (IJIF.isStack()) {
				if (IJIF.isBin()) {
					String msg = "Current image is Binary stack, ";
					
					if (IJIF.isFEC()) {
						msg += "with picondylar coordinates specified.\n";
						msg += "If the image is not pre-aligned, proceed to *" + btntitles[2] + "*.\n";
						msg += "If the image has already undergone *" + btntitles[2] + "*, " +
								"You may want to proceed to *" + btntitles[3] + "*.\n";
						
						messageBox.setText(msg);
						label_3L.setIcon(arrowR);
						label_4L.setIcon(arrowR);
					} else {
						msg += "but epicondylar coordinates are not defined.\n" +
								"Proceed to *" + btntitles[1] +"* and determine epicondyles, or "+
								"skip this process and proceed to *" + btntitles[2] + 
								"* if 3D Viewer is not available on your system.";
						
						messageBox.setText(msg);
						label_2L.setIcon(arrowR);
						label_3L.setIcon(arrowR);
					}
				} else {
					messageBox.setText("Current image is a non-binary stack. Please *Binarize* it.");
					label_1L.setIcon(arrowR);
				} 
			} else {
				messageBox.setText("Current image is not a stack, or no stack image is found.\n" + 
									"You should open DICOM directory (*folder icon*).");
				label_open.setIcon(arrowD);
			}
			break;
		case 1: {
			// after binarize 
				String msg = "Now you may want to proceed to *"+btntitles[1]+
						"* to determine the epicondyles. ";
				msg += "If 3D Viewer is not available on your system, you can skip it and proceed to *" +
						btntitles[2] + "*.\n";
								
				messageBox.setText(msg);
				label_2L.setIcon(arrowR);
				label_3L.setIcon(arrowR);
			break;
		}
		case 2: {
			// after 3D viewer
			// if points == 2
			String msg = "Two points were determined. "+
						"You can modify their placements by draging, or remove one "+
						"in the point list window and add another.\n"+
						"Then you may want to proceed to *"+btntitles[2] + "*.\n";
			messageBox.setText(msg);
			label_3L.setIcon(arrowR);
			break;		
		}
		case 3:
			// after align.
			messageBox.setText("Base model were created. You should save the Base model (*floppydisk icon*).\n" +
					"Then you may want to proceed to *"+btntitles[3]+"*.");
			label_save.setIcon(arrowD);
			label_4L.setIcon(arrowR);
			break;
		case 4: // after auto-edit.
			messageBox.setText("All models were created. You can save all the models and metadata (*floppydisk*).\n");
			label_save.setIcon(arrowD);
			break;
		case 5:
			messageBox.setText("If you want to create another Models, close the working files (*X icon*), then open another stacked-image (*folder icon*).\n" +
					"Otherwise, terminate this launcher (just close the window).");
			label_close.setIcon(arrowD);
			break;
			
		case -1: // macro returned error;
			messageBox.setText("Unsuccessful finishing of a sublet command. This launcher no longer can help you. Sorry.");
			break;
		}
		
		frame.toFront();
	}

}

