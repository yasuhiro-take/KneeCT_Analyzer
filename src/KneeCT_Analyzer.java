
import java.awt.EventQueue;
import java.awt.Frame;

import javax.swing.ImageIcon;

import imagejplugin.kneectanalyzer.CommonGUI;
import imagejplugin.kneectanalyzer.CreateModelsGUI;
import imagejplugin.kneectanalyzer.QuadrantAnalysisGUI; 
import ij.IJ;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;

public class KneeCT_Analyzer extends PlugInFrame {
	private static ImageIcon icons[];
	private static KneeCT_Analyzer instance;
	private int wintype;
	private static final int MODEL=1, QUAD=2;
	
	public KneeCT_Analyzer() {
		super(null);
		
		if (icons == null) {
			icons = new ImageIcon[6];
			icons[0] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-opened_folder.png"));
			icons[1] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-save.png"));
			icons[2] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-close_window.png"));
			icons[3] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-settings.png"));
			icons[4] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-down_filled.png"));
			icons[5] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-arrow_filled.png"));
			
			CommonGUI.setup(icons);
		}
		
	}

	@Override public void run(String arg) {
		int cmd = 0;
		switch (arg) {
		case "Model": cmd = MODEL; break;
		case "Quadrant": cmd = QUAD; break;
		default: return;
		}
		
		if (instance != null) {
			if (instance.wintype == cmd)
				instance.toFront();
			else
				IJ.error("Another launcher is running. Close it first.");
			return;
		}
		
		instance = this;
		wintype = cmd;
		
		switch (cmd) {
		case MODEL: new CreateModelsGUI(instance); break;
		case QUAD: new QuadrantAnalysisGUI(instance); break; 
		}
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WindowManager.addWindow(instance);
					instance.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override public void close() {
		instance = null;
		super.close();
	}

}
