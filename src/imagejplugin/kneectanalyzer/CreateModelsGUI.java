package imagejplugin.kneectanalyzer;

import java.awt.Frame;

import imagejplugin.kneectanalyzer.IJIF;

public class CreateModelsGUI extends CommonGUI {
	private int status;
	private String wintitle;
	
	/*
	
	
	private JButton btn_open, btn_save, btn_close, btn_q;
	private Button btn_1, btn_2, btn_3, btn_4;
	
	private JTextPane messageBox;
	private ImageIcon arrowR, arrowD;
	private JLabel label_open, label_save, label_close;
	private JLabel label_1L, label_2L, label_3L, label_4L;
	*/
	
	private static final String[] btntitles = { "Correct Alignment", "Binarize", "Detect Anatomy", "Divide F/T" };
	
	/**
	 * Launch the application.
	 */
	
	/*
	public static void main(String[] args) {
		if (opened) {
			System.out.println("Create Models UI already exists.");
			return;
		}
		
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CreateModelsGUI window = new CreateModelsGUI();
					WindowManager.addWindow(window.frame);
					window.frame.setVisible(true);
					
					opened = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	*/

	public CreateModelsGUI(Frame instance) {
		super(instance, btntitles);
		status = 0;
		wintitle = null;
		IJIF.initIJIF();
		
		suggestion();
	}
	
	@Override int executeCommand(char btn) {
		int r = -1;;
		
		switch(btn) {
		case 'o':
			r = IJIF.openKCADirectory();
			if (r == 0)
				wintitle = IJIF.getOpenedTitle();
			else 
				r = (r > 0) ? 0 : -1;
			
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
			r = IJIF.closeWorkingFiles(wintitle, "Aligned "+wintitle, "Base", "TibOnly", "FemOnly");
			break;
		case '1':
			r = IJIF.Modeler.align();
			break;
		case '2':
			r = IJIF.Modeler.binarize(); 
			break;
		case '3':
			r = IJIF.Modeler.detectAnatomy();
			break;
		case '4':
			r = IJIF.Modeler.autoEdit();
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
			if (status == 4)
				status = 5;
			
			suggestion();
			
			break;
		case 'c':
			status = 0;
			wintitle = null;
			resetLabelIcons();
			suggestion();
			break;
		case '1':
		case '2':
		case '4':
			if (r == 0)
				status = Character.getNumericValue(btn);
			else
				status = -1;

			suggestion();
			break;
		case '3':
			status = 3;
			suggestion();
			break;
		
		}
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
					
					if (IJIF.hasBoundaryData()) {
						msg += "with anatomic boundary data.\n";
						msg += "You may want to proceed to *" + btntitles[3] + "* ";
						msg += "to create  FemOnly & TibOnly models.";
						
						messageBox.setText(msg);
						label_4L.setIcon(arrowR);
					} else {
						msg += "but no anatomic boundary data.\n" +
								"Proceed to *" + btntitles[2] +"*.";
						
						messageBox.setText(msg);
						label_3L.setIcon(arrowR);
					}
				} else {
					String msg = "Current image is a non-binary stack.\n";
					msg += "If its alignment is not corrected, proceed to *";
					msg += btntitles[0] + "*; otherwise, proceed to *";
					msg += btntitles[1] + "*.";
					
					label_1L.setIcon(arrowR);
					label_2L.setIcon(arrowR);
					messageBox.setText(msg);
				} 
			} else {
				messageBox.setText("Current image is not a stack, or no stack image is found.\n" + 
									"You should open DICOM directory (*folder icon*).");
				label_open.setIcon(arrowD);
			}
			break;
		case 1: {
			// after alignment correction
			
			String msg = "Alignment correection was successfully finished.";
			msg += "Now you may want to proceed to *"+btntitles[1]+ "*."; 

			messageBox.setText(msg);
			label_2L.setIcon(arrowR);
			break;
		}
		case 2: {
			// after binarize;
			if (IJIF.isBin()) {
				String msg = "Image is successfully binarized. Now you may want to proceed to *";
				msg += btntitles[2] + "*.";
				messageBox.setText(msg);
				label_3L.setIcon(arrowR);
			}
			break;		
		}
		case 3:
			// after anatomy detection
			if (IJIF.hasBoundaryData()) {
				String msg = "The 'Base' model w/ Anatomic Boundary data ";
				msg += "were created. You should save the Base model (*floppydisk icon*).\n";
				msg += "Then proceed to *" + btntitles[3] + "*. ";
				messageBox.setText(msg);
				label_save.setIcon(arrowD);
				label_4L.setIcon(arrowR);
			} else {
				String msg = "Anatomic Boundary Detection was failed; you should return to *";
				msg += btntitles[1] +"* with a different threshold, and try re-do *" + btntitles[2] + "*.";
				messageBox.setText(msg);
				label_2L.setIcon(arrowR);
			}
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
		if (wintitle != null)
			frame.setTitle("Create Models" + " - " + wintitle);
		else
			frame.setTitle("Create Models");
	}
}

