package imagejplugin.kneectanalyzer;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.ZProjector;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.AWTEvent;
import java.awt.Scrollbar;
import java.util.Vector;

public class Thresholder implements ExtendedPlugInFilter, DialogListener {
	private int maxPx;
	private int maxSlice, slice;
	private String title, msg;
	public int min, max;
	private ImagePlus imp;
	public boolean wasOK;
	
	
	public Thresholder(String title, String msg) {
		this.title = title;
		this.msg = msg;
		min = 150; max = 1500;
		wasOK = false;
	}
	
	@Override public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		maxSlice = imp.getNSlices();
		slice = imp.getSlice();
		ImagePlus impZ = IJX.zproject(imp, ZProjector.MAX_METHOD, 1, maxSlice);
		ImageStatistics istats = impZ.getStatistics();
		maxPx = (int)istats.max;
		
		return DOES_16 | DOES_8G | STACK_REQUIRED | NO_CHANGES;
	}

	@Override public void run(ImageProcessor ip) {
		IJ.setThreshold(imp, min, max);
		IJ.wait(50);
		imp.setSlice(slice);
		//
		//imp.updateAndDraw();
	}

	@Override public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		GenericDialog gd = new GenericDialog(title);
		gd.addMessage(msg);
		
		gd.addSlider("Min Threshold: ", 0, maxPx, min);
		gd.addSlider("Max Threshold: ", 0, maxPx, max);
		gd.addSlider("Slice choice: ", 1, maxSlice, slice);
		
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.getPreviewCheckbox().setState(true);
		gd.showDialog();
		
		wasOK = gd.wasOKed();
		
		return 0;
	}

	@Override public void setNPasses(int nPasses) {}

	@Override public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		/*
		min = (int)gd.getNextNumber();
		max = (int)gd.getNextNumber();
		slice = (int)gd.getNextNumber();
		*/
		Vector<Scrollbar> slider = gd.getSliders();
		min = slider.get(0).getValue();
		max = slider.get(1).getValue();
		slice = slider.get(2).getValue();
	
		return true;
	}

}