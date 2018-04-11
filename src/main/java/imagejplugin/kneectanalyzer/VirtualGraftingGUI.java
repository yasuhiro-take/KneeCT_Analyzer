package imagejplugin.kneectanalyzer;

import java.awt.Frame;
import java.awt.event.ActionEvent;

public class VirtualGraftingGUI extends CommonGUI {
	private int status;
	private String basePathLast;
	
	private static final String[] btntitles = { "Detect Quadrant System", "Manual Quadrant System", "Import Quadrant Coord", "Create Graft Model", 
												"3D Viewer", "Show Tunnel Centroid", "Edit Appearance", "Snapshot", "2D", "3D" };
	static final String[] quadStatus = QuadrantAnalysisGUI.quadStatus;
	
	public VirtualGraftingGUI(Frame instance) {
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
		
		if (status <= 2 && IJIF.checkModels("FemOnly", "TibOnly") && qsystem == 3) {
			status = 2;
			if (IJIF.hasTunnelCoords())
				status = 3;
			
		}
		
		switch (status) {
		case 0: {
			String msg = null;
			if (!IJIF.checkModels("FemOnly", "TibOnly") || !IJIF.hasBoundaryData()) {
				msg = "Open KCA-folder.\n\n\n";
				label_open.setIcon(arrowD);
			} else if (qsystem < 3) {
				msg = "Model data OK.\n";
				msg += "Available Quadrant system: " + quadStatus[qsystem] + ".\n";
				
				msg += "Determine quadrant coordinate system automatically (*" +btntitles[0] + "*), ";
				msg += "or manually (*" + btntitles[1] + "*).";
					
				label_1L.setIcon(arrowR);
				label_2L.setIcon(arrowR);
			}
			
			messageBox.setText(msg);
			break; 
		}
		case 1:
		case 2:
		 {
			String msg = null;
			if (qsystem == 3) {
				if (IJIF.hasTunnelCoords()) {
					msg = "You can create a virtual graft model using the known tunnel locations ";
					msg += "and/or specified quadrant coords.\n";
					
				} else {
					msg = "You may want to import tunnel location data (in quadrant coords) from another KCA ";
					msg += "folder.\n";
					msg += "You can also create a virtual graft model, specifying quadrant coords.\n";
					label_3L.setIcon(arrowR);
				}
				label_4L.setIcon(arrowR);
			} else {
				msg = "Available Quadrant system: " + quadStatus[qsystem] + ".\n";
				msg += "Proceed to *"+btntitles[0]+"* or *"+btntitles[1]+"* to determine quadrant system.\n";
				
				label_1L.setIcon(arrowR);
				label_2L.setIcon(arrowR);
			}
			
			
			messageBox.setText(msg);
			break;
		} 
		case 3: {
			String msg;
			if (IJIF.hasTunnelCoords()) {
				msg = "Tunnel location data in quadrant system are available. Proceed to *" + btntitles[3];
				msg += "* to create virtual graft model.\n";
				
 			} else {
 				msg = "Tunnel location data is not available. You may want to *" +btntitles[2];
 				msg += "* from another KCA folder, or to *" + btntitles[3] + "* and directly specify the tunnel location.\n";
 				label_3L.setIcon(arrowR);
 				
 			}
			label_4L.setIcon(arrowR);
			messageBox.setText(msg);
			
			break;
			
		}
		case 4: {
			String msg = "Virtual graft model is created. You can repeat *" + btntitles[3] +"* to create one for ";
			msg += "another bundle.\n";
			msg += "You can save data in KCA folder ";
			msg += "(*floppy icon*).\n";
			
			label_4L.setIcon(arrowR);
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
		
		String title = "Virtual Grafting";
		if (basePathLast != null) title += " - " + basePathLast;
		frame.setTitle(title);
	}
	
	void suggestion3D(int ret) {
		resetLabelIcons();
		
		String msg;
		if (IJIF.checkModels("FemOnly", "TibOnly") && IJIF.hasBoundaryData()) {
			if (IJIF.hasGrafts()) {
				msg = "Bone and graft models are available.\n";
				if (IJIF3D.getVisibleFT() == 0) {
					msg += "Create 3D model first (*" + btntitles[4] + "*).";
					label_1L.setIcon(arrowR);
				} else {
					if (status == 4) {
						msg = "You can save created snapshots (*floppy icon*).\n";
						label_save.setIcon(arrowD);
					} else {
						if (IJIF.hasTunnelCoords()) {
							msg += "You can add a point presenting centroid of tunnel aperture (*"+btntitles[5]+"*).\n";
							label_2L.setIcon(arrowR);
						}
						
						msg += "You can modify the color and transparency (*" + btntitles[6];
						msg += "*). You can create 2D *" + btntitles[7] +"*.\n";
						label_3L.setIcon(arrowR);
						label_4L.setIcon(arrowR);
					}
				}
			} else {
				msg = "Only bone models are available. ";
				if (IJIF3D.getVisibleFT() == 0) {
					msg += "Create 3D model first (*" + btntitles[4] + "*).";
					label_1L.setIcon(arrowR);
				} else {
					if (IJIF.hasTunnelCoords())
						msg += "You can add a point presenting centroid of tunnel aperture (*"+btntitles[5]+"*).\n";
					
					msg += "You may want to create virtual graft model (*"+btntitles[3]+"* in 2D menu).\n"; 
				}
			}
		} else {
			msg = "Open KCA-folder.\n\n\n";
			label_open.setIcon(arrowD);
		}
		
		messageBox.setText(msg);	
		
		frame.toFront();
		
		String title = "Virtual Grafting";
		if (basePathLast != null) title += " - " + basePathLast;
		frame.setTitle(title);
	}
	

	@Override int executeCommand(char btn) {
		int r = -1;
		switch(btn) {
		case 'o':
			r = IJIF.Grafter.open("TibOnly", "FemOnly", "TunOnlyFem", "TunOnlyTib");
			if (r > 0)
				basePathLast = IJX.Util.getLastDirectory(IJIF.getBaseDirectory());
			
			break;
		case 's':
			r = IJIF.Grafter.save();
			break;
		case 'c':
			r = IJIF.closeWorkingFiles("Base", "TibOnly", "FemOnly", "LFCOnly", "FemoralQuadrant", 
										"TibialQuadrant", "TunOnlyFem", "TunOnlyTib", "Graft-.*");
			basePathLast = null;
			
			break;
		case 'q':
			IJIF.Property.settingDialog();
			r = 0;
			break;
		case '1':
			r = (toggleSwitch == 1) ? IJIF.Quad.detectSystem2D() : IJIF3D.Grafter.view3d();
			break;
		case '2':
			r = (toggleSwitch == 1) ? IJIF.Quad.determineSystem2D() : IJIF3D.Grafter.showPoints();
			break;
		case '3':
			r = (toggleSwitch == 1) ? IJIF.Grafter.importData() : 0; IJIF3D.Grafter.modifyAppearance();
			break;
		case '4':
			r = (toggleSwitch == 1) ? IJIF.Grafter.createGraft() : IJIF3D.Quad.snapshot();
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
				if (status >= 4 && toggleSwitch == 1)
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

