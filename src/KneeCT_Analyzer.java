
import javax.swing.ImageIcon;

import imagejplugin.kneectanalyzer.CreateModelsGUI;
import imagejplugin.kneectanalyzer.QuadrantAnalysisGUI; 
import ij.plugin.PlugIn;

public class KneeCT_Analyzer implements PlugIn {

	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		ImageIcon icons[] = new ImageIcon[6];
		
		icons[0] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-opened_folder.png"));
		icons[1] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-save.png"));
		icons[2] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-close_window.png"));
		icons[3] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-settings.png"));
		icons[4] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-down_filled.png"));
		icons[5] = new ImageIcon(KneeCT_Analyzer.class.getResource("img/icons8-arrow_filled.png"));
		
		if (arg.equals("Model"))
			CreateModelsGUI.main(icons, null);
		else if (arg.equals("Quadrant"))
			QuadrantAnalysisGUI.main(icons, null);
	}

}
