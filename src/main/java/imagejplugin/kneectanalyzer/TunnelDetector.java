package imagejplugin.kneectanalyzer;

import java.awt.AWTEvent;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

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
import ij.gui.TextRoi;
import ij.gui.Toolbar;
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
	private double minSize = 5, maxSize = 300, minCirc = 0.5, maxCirc = 1.0;
	private Analyzer analyzer;
	private QRoiList tunnelRois;
	private TextPanel tp;
	private ImagePlus imp2D;
	private Button btnDel, btnAdd;
	private Checkbox cbox;
	
	private static final int FEM = Quadrant.FEM, TIB = Quadrant.TIB;
	
	public TunnelDetector() {
	}
	
	
	@Override public void run(String arg) {
		directrun();
	}

	public String directrun() { 
		int quadsys = Quadrant.SysCoord.getDetermined();
		if (quadsys == 0) {
			IJ.error("Before tunnel detection, determine Quadrant System first.");
			return null;
		}
		
		int ft = dialog(quadsys), r;
		if (ft == FEM) r = detectFemoralTunnel();
		else if (ft == TIB) r = detectTibialTunnel();
		else return null;
		
		if (r != 0) return null;
		
		imp2D = Quadrant.get2DImage(ft, true);
		drawTunnelApertures(imp2D, tunnelRois);
		
		ResultsTable rt = new ResultsTable();
		analyzer = new Analyzer(imp2D, AREA | CENTROID | LABELS, rt);
		//analyzer = new Analyzer(imp2D, AREA | CENTER_OF_MASS | LABELS, rt);
		tunnelRois.toResults(analyzer, imp2D);
		
		r = startTunnelEditor(rt);
		imp2D.killRoi();
		String outputImages = null;
		if (r == 0) {
			outputImages = imp2D.getTitle();
			XY xy[] = outputToResults(ft);
			if (cbox.getState()) {
				createTunnelModel(ft);
				outputImages += " & " + "TunOnly" + ((ft == FEM) ? "Fem" : "Tib");
			}
			finalizeTunnelApertures(imp2D, xy, ft);
		} else {
			clearTunnelApertures(imp2D);
		}
		
		imp2D.hide(); imp2D.show();
		
		return outputImages;
	}
	
	public int detectFemoralTunnel() {
		ImagePlus impF;
		if ((impF = WindowManager.getImage("FemOnly")) == null)
			return IJX.error("FemOnly not found.", -1);
		
		int nrx = RTBoundary.getSplitX();
		
		ImagePlus impFem = impF.duplicate();
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
		if (impTib == null)
			return IJX.error("TibOnly not found.", -1);
		
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
		
		String defaultSize = Double.toString(minSize) + "-" + Double.toString(maxSize);
		String defaultCirc = Double.toString(minCirc) + "-" + Double.toString(maxCirc);
		
		gd.addStringField("Size ("+units+"):", defaultSize, 12); //TODO
		gd.addStringField("Circularity:", defaultCirc, 12); //TODO
		gd.showDialog();
		
		if (gd.wasCanceled()) return 0;
		
		int ft;
		if (quadsys == 3)
			ft = (gd.getNextChoice() == items[0]) ? FEM : TIB; 
		else
			ft = quadsys;
			
		String size = gd.getNextString(); 
		String circ = gd.getNextString(); 
		
		double ranges[] = IJX.Util.stringRange2double(size);
		minSize = ranges[0];
		maxSize = ranges[1];
		
		ranges = IJX.Util.stringRange2double(circ, 0, 1);
		minCirc = ranges[0];
		maxCirc = ranges[1];
		
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
		overlay.setLabelColor(Quadrant.labelColor);
		imp.setOverlay(overlay);
		imp.updateAndDraw();
	}
	
	private void finalizeTunnelApertures(ImagePlus imp, XY xy[], int ft) {
		Overlay overlay = imp.getOverlay();
		Font font = overlay.getLabelFont();
		
		//overlay.setFillColor(null);
		overlay.drawLabels(false);
		overlay.drawNames(false);
		
		Roi rois[] = Quadrant.getQuadrantTextRoi(overlay, ft);
		
		for (int i = 0; i < 3; i++) {
			if (rois[i] != null) {
				overlay.remove(rois[i]);
				
				Rectangle r = rois[i].getBounds();
				TextRoi text = new TextRoi(r.x, r.y, rois[i].getName(), font);
				text.setStrokeColor(Color.BLACK);
				overlay.add(text);
			}
		}
		
		if (xy != null) {
			for (int i = 0; i < xy.length; i++) {
				Rectangle r = new Rectangle((int)xy[i].x - 1, (int)xy[i].y - 1, 3, 3);
				Roi roi = new Roi(r);
				roi.setStrokeColor(Color.BLACK); roi.setFillColor(Color.BLACK);
				overlay.add(roi);
			}
		}
		
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
		
		IJ.setTool("ellip");
		IJ.wait(50);
		
		NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Tunnel Editor Dialog");
		gd.addPanel(createPanel(tp, imp2D.getCanvas()));
	    gd.showDialog();
	    
	    if (gd.wasOKed()) return 0;
	    return -1;
	}
	
	private XY[] outputToResults(int ft) {
		ResultsTable rt = tp.getResultsTable();
		Calibration calBase = IJX.getBaseCalibration();
		Calibration cal2D = imp2D.getCalibration();
		double scale = (ft == FEM) ? calBase.pixelWidth : calBase.pixelDepth;
		
		ResultsTable mainrt = Analyzer.getResultsTable();
    	clearResultsTable(mainrt, imp2D.getTitle()+":");
    	
    	XY retxy[];
    	if (rt.size() > 0) {
    		retxy = new XY[rt.size()];
        	for (int i = 0; i < rt.size(); i++) {
        		String label = rt.getLabel(i);
    			XY centroid = retxy[i] = new XY(rt.getValue("X", i), rt.getValue("Y", i));
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
    			
    			retxy[i].real2px(cal2D);
    		}
    	} else {
    		retxy = null;
    	}
    	
    	mainrt.show("Results");
    	
    	return retxy;
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
		
		pc.gridx = 0; pc.gridy = 3;
		pc.gridwidth = 3;
		pc.anchor = GridBagConstraints.WEST;
		cbox = new Checkbox("Create Tunnel Model");
		pgrid.setConstraints(cbox, pc);
		cbox.setState(true);
		panel.add(cbox);
		
		pc.gridx = 3; pc.gridy = 0;
		pc.gridwidth = 1; pc.gridheight = 4;
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

class QRoiList {
	ArrayList<QRoi> qroiList;
	int apertureCounter;
	
	public QRoiList() {
		qroiList = new ArrayList<QRoi>();
		apertureCounter = 1;
	}
	public QRoiList(ArrayList<QRoi> list) {
		qroiList = list;
		apertureCounter = 1;
	}
	public void add(QRoi qroi) {
		qroiList.add(qroi);
	}
	public void add(Roi r, int z, boolean isAperture, int apertureID) {
		if (isAperture)
			apertureID = apertureCounter++;
		qroiList.add(new QRoi(r, z, isAperture, apertureID));
	}
	
	
	public int size() {
		return qroiList.size();
	}
	
	public QRoi findAperture(int id) {
		for (QRoi qroi: qroiList)
			if (qroi.isAperture && qroi.apertureID == id)
				return qroi;
		return null;
	}
	
	public QRoi findAperture(XY xy, Calibration cal) {
		XY xyPx = xy.clonePixelized(cal);
		
		for (QRoi qroi: qroiList) {
			if (qroi.isAperture) {
				Rect r = new Rect(qroi.roi.getBounds());
				
				if (r.isInside(xyPx))
					return qroi;
			}
		}
		return null;		
	}
	
	public int findClearedAperture(String labelRegex, Calibration cal, ResultsTable rt) {
		for (QRoi qroi: qroiList) {
			if (qroi.isAperture) {
				Rect r = new Rect(qroi.roi.getBounds());
				r.px2real(cal);
				boolean isInRT = false;
				for (int i = 0; i < rt.size(); i++) {
					if (rt.getLabel(i).matches(labelRegex)) {
						XY xy = new XY(rt.getValue("X", i), rt.getValue("Y", i));
											
						if (r.isInside(xy))
							isInRT = true;
					}
				}
				
				if (isInRT == false)
					return qroi.apertureID;
			}
		}
			
		return -1;	
	}
	
	public int findClearedAperture(String indexlabel, ResultsTable rt) {
		for (QRoi qroi: qroiList) {
			if (qroi.isAperture) {
				String label = indexlabel + ":"+qroi.apertureID;
				
				boolean cleaered = true;
				for (int i = 0; i < rt.size(); i++) 
					if (rt.getLabel(i).equals(label))
						cleaered = false;
				
				if (cleaered)
					return qroi.apertureID;
			}
		}
		return -1;
	}
	
	public void removeByID(int apertureID) {
		for (int i = qroiList.size() - 1; i >= 0; i--) { 
			QRoi qroi = qroiList.get(i);
			if (qroi.apertureID == apertureID) {
				qroiList.remove(i);
			}
		}
	}
	
	public void sync(String labelRegex, Calibration cal, ResultsTable rt) {
		int id;
		while ((id = this.findClearedAperture(labelRegex, cal, rt)) != -1)
			removeByID(id);
	}
	
	public void sync(String indexlabel, ResultsTable rt) {
		int id;
		while ((id = this.findClearedAperture(indexlabel, rt)) != -1)
			removeByID(id);
	}
	
	public void toResults(Analyzer analyzer, ImagePlus imp) {
		for (QRoi r: qroiList) {
			if (r.isAperture) {
				imp.setRoi(r.roi);
				analyzer.measure();
				
			}
		}
		
		imp.killRoi();
	}
	
	/*
	public void toResults(ImagePlus imp) {
		IJ.run("Set Measurements...", "area centroid display redirect=None decimal=3");
		if (!IJ.isResultsWindow())
			IJ.getTextPanel();
		
		Analyzer analyzer = new Analyzer(imp);
		toResults(analyzer, imp);
		
		ResultsTable rt = Analyzer.getResultsTable();
		rt.updateResults();
		rt.show("Results");
	}
	*/
	
	
	public Overlay toOverlay(Overlay overlay) {
		overlay.drawLabels(true);
		overlay.drawNames(true);
		
		for (QRoi r: qroiList) {
			if (r.isAperture) {
				r.roi.setLocation(r.x, r.y);
				overlay.add(r.roi);
			}
		}
		
		return overlay;
	}
	
	public void removeFromOverlay(Overlay overlay) {
		for (QRoi r: qroiList) {
			if (r.isAperture)
				overlay.remove(r.roi);
		}
	}
}
