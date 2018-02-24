package imagejplugin.kneectanalyzer;

import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;
import java.awt.Scrollbar;
import java.util.Vector;

public class SliceChooseFilter implements ExtendedPlugInFilter, DialogListener {
	private int index;
	private int[] sliceChoices;
	private int maxSlice;
	private ImagePlus imp;
	public boolean wasOK;
	public String title, msg;
	
	public SliceChooseFilter(int[] sliceChoices, String title, String msg) {
		this.sliceChoices = sliceChoices;
		this.title = title;
		this.msg = msg;
	}
	public SliceChooseFilter(int maxSlice, String title, String msg) {
		this.maxSlice = maxSlice;
		sliceChoices = null;
		this.title = title;
		this.msg = msg;
	}
	
	public int getChoice() {
		return index;
	}
	
	public int getSlice() {
		if (sliceChoices != null)
			return sliceChoices[index] + 1;
		else
			return index + 1;
	}
	 
	@Override
	public int setup(String arg, ImagePlus imp) {
		this.index = 0;
		this.imp = imp;
		this.wasOK = false;
		
		return DOES_ALL | STACK_REQUIRED | NO_CHANGES;
	}

	@Override
	public void run(ImageProcessor ip) {
		if (sliceChoices != null)
			imp.setSlice(sliceChoices[index] + 1);
		else
			imp.setSlice(index + 1);
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		//index = gd.getNextChoiceIndex();
		Vector<Scrollbar> slider = gd.getSliders();
		index = slider.get(0).getValue() - 1;
		
		return true;
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		GenericDialog gd = new GenericDialog(title);
		gd.addMessage(msg);
		
		int max = (sliceChoices != null) ? sliceChoices.length : maxSlice; 
		gd.addSlider("Slice choice: ", 1, max, 1);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.getPreviewCheckbox().setState(true);
		gd.showDialog();
		
		if (gd.wasOKed())
			wasOK = true;
		
		return 0;
	}

	@Override public void setNPasses(int nPasses) {}
}
