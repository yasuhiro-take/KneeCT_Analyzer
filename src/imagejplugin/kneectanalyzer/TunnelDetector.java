package imagejplugin.kneectanalyzer;

import java.awt.AWTEvent;
import java.awt.Button;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.MultiLineLabel;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.text.TextPanel;
import ij.text.TextWindow;
import ij.util.Tools;

public class TunnelDetector implements PlugIn, Measurements, ActionListener {
	private double minSize, maxSize, minCirc, maxCirc;
	private Analyzer analyzer;
	private QRoiList tunnelRois;
	private TextPanel tp;
	private ImagePlus imp2D;
	private Button btnDel, btnAdd;
	
	private static final int FEM = Quadrant.FEM, TIB = Quadrant.TIB;  

	@Override public void run(String arg) { 
		int quadsys = Quadrant.SysCoord.getDetermined();
		if (quadsys == 0) {
			IJ.error("Before tunnel detection, determine Quadrant System first.");
			return;
		}
		
		int ft = dialog(quadsys);
		if (ft == FEM) detectFemoralTunnel();
		else if (ft == TIB) detectTibialTunnel();
		else return;
		
		imp2D = Quadrant.get2DImage(ft, true);
		drawTunnelApertures(imp2D, tunnelRois);
		
		ResultsTable rt = new ResultsTable();
		analyzer = new Analyzer(imp2D, AREA | CENTROID | LABELS, rt);
		tunnelRois.toResults(analyzer, imp2D);
		
		int r = startTunnelEditor(rt);
		if (r == 0) {
			outputToResults(ft);
			createTunnelModel(ft);
		} else {
			clearTunnelApertures(imp2D);
		}
		
		imp2D.hide(); imp2D.show();
	}
	
	public int detectFemoralTunnel() {
		int nrx = RTBoundary.getSplitX();
		
		ImagePlus impFem = WindowManager.getImage("FemOnly").duplicate();
		impFem.show();
		IJ.run(impFem, "Fill Holes", "stack");
		
		ImagePlus impLFC = IJX.createLFCOnly(impFem, nrx); impLFC.show(); // not entitled as 'LFCOnly' at this time.
		ImagePlus impSag = IJX.createAx2Sag(impLFC);
		IJX.forceClose(impFem); 
		
		tunnelRois = getTunnels(impSag, nrx, impSag.getNSlices() - 1); 
		IJX.forceClose(impSag);
		
		if (WindowManager.getImage("LFCOnly") == null)
			impLFC.setTitle("LFCOnly");
		else
			IJX.forceClose(impLFC);
		
		return 0;
	}
	
	public int detectTibialTunnel() {
		ImagePlus impTib = WindowManager.getImage("TibOnly");
		ImagePlus impSag = IJX.createAx2Sag(impTib);
		
		IJ.run(impSag, "Fill Holes", "stack");
		ImagePlus imp = IJX.createSag2Ax(impSag);
		IJX.forceClose(impSag);
		
		int z1 = RTBoundary.getProximalZ(BoundaryData.TIB);
		int z2 = RTBoundary.getDistalZ(BoundaryData.TIB);
		
		tunnelRois = getTunnels(imp, z1, z2); 
		IJX.forceClose(imp);
		
		return 0;
	}
	
	// copied from ParticleAnalyzer.java
	private int dialog(int quadsys) {
		String items[] = new String[] { "Femoral Tunnel", "Tibial Tunnel" };
		Calibration cal = IJX.getBaseCalibration();
		String unit = cal.getUnit();
		String units = unit+"^2";
		
		GenericDialog gd = new GenericDialog("Tunnel Detection Dialog");
		
		if (quadsys == 3) 
			gd.addChoice("F/T: ", items, items[0]);
		else 
			gd.addMessage("F/T: " + items[quadsys - 1]);
		
		gd.addStringField("Size ("+units+"):", "5-300", 12);
		gd.addStringField("Circularity:", "0.5-1.0", 12);
		gd.showDialog();
		
		if (gd.wasCanceled()) return 0;
		
		int ft;
		if (quadsys == 3)
			ft = (gd.getNextChoice() == items[0]) ? FEM : TIB; 
		else
			ft = quadsys;
			
		String size = gd.getNextString(); 
		String circ = gd.getNextString(); 
		
		String[] minAndMax = Tools.split(size, " -");
		double min = minAndMax.length>=1?gd.parseDouble(minAndMax[0]):0.0;
		double max = minAndMax.length==2?gd.parseDouble(minAndMax[1]):Double.NaN;
		minSize = Double.isNaN(min) ? 0 : min;
		maxSize = Double.isNaN(max) ? Double.MAX_VALUE : max;
		
		minAndMax = Tools.split(circ, " -");
		min = minAndMax.length>=1?gd.parseDouble(minAndMax[0]):0.0;
		max = minAndMax.length==2?gd.parseDouble(minAndMax[1]):Double.NaN;
		minCirc = Double.isNaN(min) ? 0.0 : min;
		maxCirc = Double.isNaN(max) ? 1.0 : max;
		
		return ft;
	}
	
	private QRoiList getTunnels(ImagePlus imp, int start, int end) {
		// imp should have undergone *fill-hole* except tunnels.
		Calibration cal = imp.getCalibration();
		AnalyzeParticle ap = new AnalyzeParticle(ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.ADD_TO_MANAGER,
												0, minSize, maxSize, minCirc, maxCirc, cal, null);
		QRoiList ql = new QRoiList();
		
		for (int z = start; z < end; z++) {
			ImageProcessor ip = imp.getImageStack().getProcessor(z + 1);
			imp.setSliceWithoutUpdate(z + 1);
			byte b = IJX.Util.getMaxAbs((byte[])ip.getPixels());
			
			if (b == 0 && ql.size() > 0)
				z = end;
			else {
				int nResults = ap.newAnalyze(imp, ip, 0, 0);
				
				for (int i = 0; i < nResults; i++) {
					boolean isAperture = true;
					int apertureID = 0;
					Roi roi = ap.rois.getRoi(i);
					Rect r = new Rect(roi.getBounds());
					XY roiCenter = r.getCenter();
							
					for (QRoi qroi: ql.qroiList) {
						java.awt.Rectangle rect = qroi.roi.getBounds();
						if (rect.contains(roiCenter.x, roiCenter.y)) {
							apertureID = qroi.apertureID;
							isAperture = false;
						}
					}
					
					if (isAperture) {
						// confirm the roi isAperture,by scanning the centroid XY at previous slice.
						ImageProcessor ipPrev = imp.getImageStack().getProcessor(z);
						ipPrev.setRoi(roi);
						ImageStatistics stats = ipPrev.getStats();
						
						if (stats.median != 0)
							isAperture = false;
					}
					ql.add(roi, z, isAperture, apertureID);
				}
			}
		}
		
		ap.rois.reset(); ap.rois.close();
		
		return ql;
	}
	
	private void clearTunnelApertures(ImagePlus imp) {
		Overlay overlay = imp.getOverlay();
		
		for (int i = overlay.size() - 1; i >= 0; i--) {
			Roi roi = overlay.get(i);
			String name = roi.getName();
			if (name != null && name.matches("[0-9]+"))
				overlay.remove(i);
		}
		
		imp.setOverlay(overlay);
	}
	
	private void drawTunnelApertures(ImagePlus imp, QRoiList ql) {
		Overlay overlay = imp.getOverlay();
		
		clearTunnelApertures(imp);
		
		ql.toOverlay(overlay);
		
		overlay.setFillColor(Quadrant.tunnelColor);
		imp.setOverlay(overlay);
		imp.updateAndDraw();
	}
	
	private int startTunnelEditor(ResultsTable rt) {
		rt.show("TunnelEditor");
		java.awt.Window win = WindowManager.getWindow("TunnelEditor");
		if (win == null || !(win instanceof TextWindow))
			return -1;
		
		tp = ((TextWindow)win).getTextPanel();
		win.setVisible(false);
		
		imp2D.getWindow().toFront();
		WindowManager.setCurrentWindow(imp2D.getWindow());
		
		NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Tunnel Editor Dialog");
		gd.addPanel(createPanel(tp, imp2D.getCanvas()));
	    gd.showDialog();
	    
	    if (gd.wasOKed()) return 0;
	    return -1;
	}
	
	private void outputToResults(int ft) {
		ResultsTable rt = tp.getResultsTable();
		Calibration calBase = IJX.getBaseCalibration();
		Calibration cal2D = imp2D.getCalibration();
		double scale = (ft == FEM) ? calBase.pixelWidth : calBase.pixelDepth;
		
		ResultsTable mainrt = Analyzer.getResultsTable();
    	clearResultsTable(mainrt, imp2D.getTitle()+":");
    	
    	for (int i = 0; i < rt.size(); i++) {
    		String label = rt.getLabel(i);
			XY centroid = new XY(rt.getValue("X", i), rt.getValue("Y", i));
			XY quad = Quadrant.calcCoord(ft, centroid);
			
			double z = 0;
			if (label.matches(".*:[0-9]+")) {
				int apID = IJX.Util.string2int(label, ":", 1);
				QRoi qroi =  tunnelRois.findAperture(apID);
				z = (qroi != null) ? qroi.z * scale : 0;
			} else {
				QRoi qroi = (tunnelRois != null) ? tunnelRois.findAperture(centroid, cal2D) : null;
				z = (qroi != null) ? qroi.z * scale : 0;
			}
			
			if (quad != null) {
				mainrt.incrementCounter();
				mainrt.addLabel(label);
			
				mainrt.addValue("Area",  rt.getValue("Area", i));
				mainrt.addValue("X", centroid.x);
				mainrt.addValue("Y", centroid.y);
				mainrt.addValue("Z", z);
				mainrt.addValue("QuadX", quad.x);
				mainrt.addValue("QuadY", quad.y);
			}
		}

    	mainrt.show("Results");
	}
	
	private void createTunnelModel(int ft) {
		if (ft == FEM) createTunOnlyFem();
		else createTunOnlyTib();
	}

	private Panel createPanel(TextPanel tp, ImageCanvas ic) {
		Panel panel = new Panel();
		GridBagLayout pgrid = new GridBagLayout();
		GridBagConstraints pc  = new GridBagConstraints();
		panel.setLayout(pgrid);
		
		String msg = "Tunnel apertures were drawn graphically in the right panel,\n";
		msg += "and listed below. Select non-tunnel data in the list, and\n";
		msg += "press *Delete* button.\n";
		msg += "You can add an aperture data by ROI and *Add* button.";
		MultiLineLabel label = new MultiLineLabel(msg);
		pc.gridx = 0; pc.gridy = 0;
		pc.gridwidth = 3; pc.gridheight = 1;
		pgrid.setConstraints(label, pc);
		panel.add(label);
		
		pc.gridx = 0; pc.gridy = 1;
		pc.gridwidth = 3; pc.gridheight = 1;
		pgrid.setConstraints(tp, pc);
		panel.add(tp);
		
		btnDel = new Button("Delete");
		pc.gridx = 0; pc.gridy = 2;
		pc.gridwidth = 1; pc.gridheight = 1;
		pc.anchor = GridBagConstraints.NORTH;
		pgrid.setConstraints(btnDel, pc);
		panel.add(btnDel);
		
		btnAdd = new Button("Add");
		pc.gridx = 1; pc.gridy = 2;
		pc.gridwidth = 1; pc.gridheight = 1;
		pc.anchor = GridBagConstraints.NORTH;
		pgrid.setConstraints(btnAdd, pc);
		panel.add(btnAdd);
		
		btnDel.addActionListener(this);
		btnAdd.addActionListener(this);
		
		pc.gridx = 3; pc.gridy = 0;
		pc.gridwidth = 1; pc.gridheight = 3;
		pgrid.setConstraints(ic, pc);
		panel.add(ic);
    	
		return panel;
	}

	@Override public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnDel)
			removeRois();
		if (e.getSource() == btnAdd)
			addRoi();
	}
	
	private void clearResultsTable(ResultsTable rt, String label) {
		for (int i = rt.size() - 1; i >= 0; i--) {
			if (rt.getLabel(i).startsWith(label))
				rt.deleteRow(i);
		}
	}
	
	private int addRoi() {
		Roi roi = imp2D.getRoi();
		if (roi == null)
			return IJX.error("Specify ROI in the Image.");
		
		String label = imp2D.getTitle();
		ImageStatistics istats = imp2D.getStatistics(AREA | CENTROID);
		
		ResultsTable rt = tp.getResultsTable();
		rt.incrementCounter();
		rt.addLabel(label);
		rt.addValue("Area", istats.area);
		rt.addValue("X", istats.xCentroid);
		rt.addValue("Y", istats.yCentroid);
		
		String line = rt.getRowAsString(rt.size() - 1);
		
		tp.appendLine(line);
		//tp.updateDisplay();
		return 0;
	}
	
	private void removeRois() {
		int is = tp.getSelectionStart();
		int ie = tp.getSelectionEnd(); 
		
		if (is == -1) return;
		System.out.println("delete "+is+" "+ie);
		
		ResultsTable rt = tp.getResultsTable();
		
		for (int i = ie; i >= is; i--) {
			String label = rt.getLabel(i);
			String l[] = label.split(":");
			if (l.length == 2) {
				int li = Integer.parseInt(l[1]);
				
				Overlay ol = imp2D.getOverlay();
				int oli = ol.getIndex(l[1]);
				if (oli != -1)
					ol.remove(oli);
				
				tunnelRois.removeByID(li);
				
				//rt.deleteRow(i);
			}
				
		}
		
		imp2D.updateAndDraw();
		tp.clearSelection();
	}
	
	private ImagePlus createTunOnlyFem() {
		ImagePlus impLFC = WindowManager.getImage("LFCOnly");
		ImagePlus impSag = IJX.createAx2Sag(impLFC);
		//IJX.convertTunOnly(impSag, Quadrant.tunnelRoisFem);
		convertTunOnly(impSag, tunnelRois);
		ImagePlus impTun = IJX.createSag2Ax(impSag);
		IJX.rename(impTun, "TunOnlyFem");
		
		IJX.forceClose(impSag);
		
		return impTun;
	}
	
	private ImagePlus createTunOnlyTib() {
		ImagePlus impTib = WindowManager.getImage("TibOnly");
		ImagePlus impTun = impTib.duplicate();
		impTun.show();
		convertTunOnly(impTun, tunnelRois);
		IJX.rename(impTun, "TunOnlyTib");
		
		return impTun;
	}
	
	private void convertTunOnly(ImagePlus imp, QRoiList ql) {
		IJ.setForegroundColor(128, 128, 128);
		for (QRoi qroi: ql.qroiList) {
			imp.setSliceWithoutUpdate(qroi.z + 1);
			imp.getProcessor().fill(qroi.roi);
		}
		
		imp.updateAndDraw();
		
		IJ.setThreshold(imp, 127, 128);
		IJ.run(imp, "Make Binary", "method=Default background=Default");
	}
	
	
}
