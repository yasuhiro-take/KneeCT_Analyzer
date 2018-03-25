package imagejplugin.kneectanalyzer;

import java.awt.EventQueue;
import java.awt.Frame;

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

import ij.WindowManager;
import imagejplugin.kneectanalyzer.IJIF;


public class QuadrantAnalysisGUI extends CommonGUI {
	private static final String[] btntitles = { "Detect Quadrant System", "Manual Quadrant System", "Detect Tunnels", "Refresh Results",
		"3D Viewer", "Manual Quadrant System", "Measure ROI", "Snapshot", "2D", "3D" };
	
	static final String quadStatus[] = { "none", "fem", "tib", "fem&tib" };
	
	private int status;
	private String basePathLast;
	
	/**
	 * Launch the application.
	 */
	
	public QuadrantAnalysisGUI(Frame instance) {
		super(instance, btntitles);
		status = 0;
		basePathLast = null;
		IJIF.initIJIF();
		
		suggestion();
	}

	public void suggestion()
	{
		this.suggestion(0);
	}
	
	public void suggestion(int ret)
	{
		resetLabelIcons();
		
		if (toggleSwitch == 2) {
			suggestion3D(ret);
			return;
		}
		
		int qsystem = Quadrant.SysCoord.getDetermined();
		int tuns = Quadrant.tunnelDetermined();
		
		if (status == 0 && IJIF.checkModels("FemOnly", "TibOnly") && qsystem != 0) {
			status = 2;
			if (qsystem == tuns)
				status = 3;
		}
		
		switch (status) {
		case 0: {
			String msg;
			if (IJIF.checkModels("FemOnly", "TibOnly")) {
				if (IJIF.hasBoundaryData()) {
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
				msg = "Open KCA-folder.\n\n\n";
				label_open.setIcon(arrowD);
			}
			messageBox.setText(msg);
			break; 
		}
		case 1:
		case 2:
		 {
			String msg = null;
			if (qsystem == 3) {
				msg = "Quadrant coordinates were successfully determined. ";
				msg += "You may want to proceed to *" + btntitles[2] + "*.";
				msg += " You can save quadrant system coord (*floppy icon*).\n";
				label_3L.setIcon(arrowR);
				label_save.setIcon(arrowD);
			} else {
				msg = "Available Quadrant system: " + quadStatus[qsystem] + ".\n";
				msg += "Proceed to *"+btntitles[1]+"* to manually determine Quadrant coordinates, or *";
				msg += btntitles[2]+"* for available system.\n";
				label_2L.setIcon(arrowR);
				label_3L.setIcon(arrowR);
			}
			if (Quadrant.getTunnelResults() != 0) {
				msg += "You can refresh Quadrant Coord in Results (*" + btntitles[3] + "*).";
				label_4L.setIcon(arrowR);
			}
			
			messageBox.setText(msg);
			break;
		} 
		case 3: {
			String msg = "Detected tunnel data are listed in Results window, with 2D and 3D images.\n";
			msg += "You can save these data in KCA folder ";
			msg += "(*floppy icon*).\n";
			
			if (tuns < 3) {
				int missft = 3 - tuns;
				msg += "You can repeat *" + btntitles[2] +"* to detect " + quadStatus[missft] + " tunnels.";
				label_3L.setIcon(arrowR);
			}
				
			
			messageBox.setText(msg);
			label_save.setIcon(arrowD);
			break;
			
		}
		case 4: {
			String msg = "QuadX & QuadY in Results are just recalculated. ";
			msg += "You can save data in KCA folder ";
			msg += "(*floppy icon*).\n";
			
			messageBox.setText(msg);
			label_save.setIcon(arrowD);
			break;
		}
			
		case 5: {
			String msg = "Data stored in KCA folder. Close the image & results windows (*X icon*).";
			messageBox.setText(msg);
			label_close.setIcon(arrowD);
			break;
		}
	
		case -1: // macro returned error;
			messageBox.setText("Unsuccessful finishing of a sublet command. This launcher no longer can help you. Sorry.");
			break;
		}
		
		frame.toFront();
		
		String title = "Quadrant Analysis";
		if (basePathLast != null) title += " - " + basePathLast;
		frame.setTitle(title);
	}
	
	void suggestion3D(int ret) {
		resetLabelIcons();
		
		if (IJIF.checkModels("FemOnly", "TibOnly")) {
			int qsystem = Quadrant.SysCoord.getDetermined();
			String msg = "Model data OK.\n";
			msg += "Determined Quadrant System: " + quadStatus[qsystem] + ".\n";
		
			if (IJIF3D.getVisibleFT() == 0) {
				msg += "Create 3D model first (*" + btntitles[4] + "*).";
				label_1L.setIcon(arrowR);
			} else {
				msg += "You can determine/edit quadrant system manually (*" +btntitles[5];
				msg += "*).\n";
				msg += "You can measure a tunnel center using ROI tool, followed by *" + btntitles[6];
				msg += "* to calculate its quadrant coord.";
				
				label_2L.setIcon(arrowR);
				label_3L.setIcon(arrowR);	
			}
			
			messageBox.setText(msg);	
		}
		
		/*
		switch (status) {
		case 0:
		case 1:
		case 2: {
			String msg;
			if (IJIF.checkModels("FemOnly", "TibOnly")) {
				int qsystem = Quadrant.SysCoord.getDetermined();
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
		*/
		
		frame.toFront();
		
		String title = "Quadrant Analysis";
		if (basePathLast != null) title += " - " + basePathLast;
		frame.setTitle(title);
	}
	

	@Override int executeCommand(char btn) {
		int r = -1;
		switch(btn) {
		case 'o':
			r = IJIF.openKCADirectory("TibOnly", "FemOnly");
			if (r > 0)
				basePathLast = IJX.Util.getLastDirectory(IJIF.getBaseDirectory());
			
			break;
		case 's':
			r = IJIF.Quad.saveData(toggleSwitch);
			break;
		case 'c':
			r = IJIF.closeWorkingFiles("Base", "TibOnly", "FemOnly", "LFCOnly", "FemoralQuadrant", 
										"TibialQuadrant", "TunOnlyFem", "TunOnlyTib");
			basePathLast = null;
			
			break;
		case '1':
			r = (toggleSwitch == 1) ? IJIF.Quad.detectSystem2D() : IJIF3D.Quad.view3dOne(true);
			break;
		case '2':
			r = (toggleSwitch == 1) ? IJIF.Quad.determineSystem2D() : IJIF3D.Quad.determineSystem3D();
			break;
		case '3':
			r = (toggleSwitch == 1) ? IJIF.Quad.detectTunnel2D() :IJIF3D.Quad.refreshResults3D(); 		
			break;
		case '4':
			r = (toggleSwitch == 1) ? IJIF.Quad.refreshResults() : IJIF3D.Quad.snapshot();
			break;
		} 
		
		return r;
	}
	
	@Override void afterCommand(char btn, int r) {
		switch(btn) {
			case 'o':
				suggestion();
				
				break;
			case 's':
				if (status >= 3 && toggleSwitch == 1)
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
	}
	
	@Override void afterActionPerformed(ActionEvent e) {
		Object o = e.getSource();
		
		if (o == tbtn1 || o == tbtn2)
			suggestion(status = 0);
	}
}
