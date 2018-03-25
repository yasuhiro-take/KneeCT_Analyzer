package imagejplugin.kneectanalyzer;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.RotatedRectRoi;
import ij.gui.TextRoi;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;


class QRoi {
	Roi roi;
	int x, y, z;
	int apertureID;
	boolean isAperture;
	
	public QRoi() {
		roi = new Roi(null);
		x = y = z = 0;
		isAperture = false;
	}
	public QRoi(Roi r, int z, boolean b, int apertureID) {
		this.roi = r;
		this.z = z;
		isAperture = b;
		x = y = 0;
		
		if (isAperture) {
			this.apertureID = apertureID; //apertureCounter++;
			this.roi.setName(Integer.toString(this.apertureID));
			this.x = r.getBounds().x;
			this.y = r.getBounds().y;
		} else {
			this.apertureID = apertureID;
			this.roi.setName("sub-ap:"+Integer.toString(this.apertureID));
		}
	}
	
	public String toString() {
		String ret = isAperture ? "ApertureROI" : "non-ApertureROI";
		ret += " w/apID:"+apertureID+" in slice:"+z;
		ret += " ("+roi.toString() + ")";
		return ret;
	}
}


class Quadrant  {
	static final int FEM=1, TIB=2;
	//static mPointList systemCoordFem, systemCoordTib;
	//static QRoiList tunnelRoisFem, tunnelRoisTib;
	//static QRoiList tunnelRois[] = new QRoiList[3];
	static final String WINTITLE_FEM2D = "FemoralQuadrant";
	static final String WINTITLE_TIB2D = "TibialQuadrant";
	static final String WINTITLES[] = new String[] { null, WINTITLE_FEM2D, WINTITLE_TIB2D };
	static final String WINTITLES3D[] = new String[] { null, WINTITLE_FEM2D+"3D", WINTITLE_TIB2D+"3D" };
	static final String WINTITLE_RTQUAD = "Quadrant System RefCoords";
	static final String WINTITLE_BOUNDARY = RTBoundary.WINTITLE_BOUNDARY;
	static final String XYZSTR[] = new String[] { null, "YZ", "XY" }; 
	private static final String FILENAME_FEMQ = "femQuad.points";
	private static final String FILENAME_TIBQ = "tibQuad.points";
	private static final String FILENAME_RESULTS = "tunnelCoords.txt";
	private static final String FILENAME_QSYSTEM = "QuadSysCoords.txt";
	static final String COORDSTR_FEM[] = new String[] { "DS:100", "DS:0 HL:0", "HL:100"};
	static final String COORDSTR_TIB[] = new String[] { "ML:0 AP:0", "ML:100", "AP:100"};
	static final String COORDSTRS[][] = new String[][] { null, COORDSTR_FEM, COORDSTR_TIB };
	private static Font coordFont = new Font(Font.SERIF, 0, 16);
	static final Color gridColor = Color.GREEN; // new Color(0,255,0);
	static final Color textColor = Color.RED; // new Color(255,0,0);
	static final Color tunnelColor = Color.ORANGE; //new Color(0,255,255);
	static final Color labelColor = Color.BLACK;
	
	static class SysCoord {
		public static int getDetermined() {
			ResultsTable rt = IJX.getResultsTable(WINTITLE_RTQUAD);
			if (rt == null) return 0;
			
			int ft = 0;
			if (get(FEM) != null) ft |= FEM;
			if (get(TIB) != null) ft |= TIB;
			
			return ft;
		}
		
		public static void output(int ft, XY qxy[]) {
			ResultsTable rt = IJX.getResultsTable(WINTITLE_RTQUAD);
			if (rt == null)
				rt = new ResultsTable();
			else {
				for (int i = rt.size() - 1; i >= 0; i--) {
					if (rt.getLabel(i).startsWith(WINTITLES[ft]))
						rt.deleteRow(i);
				}
			}
			
			for (int i = 0; i < qxy.length; i++) {
				rt.incrementCounter();
				rt.addLabel(Quadrant.WINTITLES[ft] + ":" +Integer.toString(i + 1));
				rt.addValue("X", qxy[i].x);
				rt.addValue("Y", qxy[i].y);	
			}
			
			rt.show(WINTITLE_RTQUAD);
		}
		
		public static void output(int ft, XY qxyPx[], Calibration cal) {
			XY qxy[] = new XY[qxyPx.length];
			for (int i = 0; i < qxyPx.length; i++)
				qxy[i] = qxyPx[i].cloneRealized(cal);
			
			output(ft, qxy);
		}
		
		public static void outputPx(int ft, XY qxyPx[]) {
			Calibration cal = IJX.getBaseCalibration();
			if (ft == TIB) {
				output(ft, qxyPx, cal);
			} else if (ft == FEM) {
				XY qxy[] = new XY[3];
				for (int i = 0; i < 3; i++) {
					qxy[i] = qxyPx[i].clone();
					qxy[i].px2real(cal.pixelHeight, cal.pixelDepth);
				}
				output(ft, qxy);
			}
		}
		
		public static XY[] get(int ft) {
			XY qxy[] = new XY[3];
			
			ResultsTable rt = IJX.getResultsTable(WINTITLE_RTQUAD);
			if (rt == null) return null;
			
			for (int i = 0; i < rt.size(); i++) {
				String label = rt.getLabel(i);
				if (label.startsWith(WINTITLES[ft])) {
					int j = Integer.parseInt(label.substring(label.length() - 1));
					if (1 <= j && j <= 3) {
						double x = rt.getValue("X", i);
						double y = rt.getValue("Y", i);
						qxy[j - 1] = new XY(x, y);
					}
				}
			}
			
			if (qxy[0] == null || qxy[1] == null || qxy[2] == null) return null;
			return qxy;
		}
		
		public static XY[] getPx(int ft, Calibration cal) {
			XY qxy[] = get(ft);
			if (qxy != null) {
				for (XY xy: qxy)
					xy.real2px(cal);
			}
			return qxy;
		}
		
		public static XY[] getPx(int ft) {
			Calibration cal = IJX.getBaseCalibration();
			if (ft == FEM) {
				XY qxy[] = get(ft);
				if (qxy != null)
					for (XY xy: qxy)
						xy.real2px(cal.pixelHeight, cal.pixelDepth);
				return qxy;
			} else if (ft == TIB) {
				 return getPx(ft, cal);
			}
			return null;
		}
		
		public static boolean save(String basepath) {
			ResultsTable rt = IJX.getResultsTable(WINTITLE_RTQUAD);
			
			if (rt != null && rt.size() > 0) {
				rt.save(IJX.Util.createPath(basepath, FILENAME_QSYSTEM));
				return true;
			}
			return false;
		}
		
		public static boolean load(String basepath) {
			IJX.closeResultsTable(WINTITLE_RTQUAD);
			if (IJX.Util.doesFileExist(basepath, FILENAME_QSYSTEM)) {
				ResultsTable rt = ResultsTable.open2(IJX.Util.createPath(basepath, FILENAME_QSYSTEM));
				if (rt != null) {
					rt.show(WINTITLE_RTQUAD);
					return true;
				}
			}
			return false;
		}
	}
	
	public static int tunnelDetermined() {
		int r = 0;
		//if (tunnelRois[FEM] != null) r |= FEM;
		//if (tunnelRois[TIB] != null) r |= TIB;
		if (WindowManager.getImage("TunOnlyFem") != null) r |= FEM;
		if (WindowManager.getImage("TunOnlyTib") != null) r |= TIB;
		return r;
	}
	
	public static int getTunnelResults() {
		int r = 0;
		ResultsTable rt = Analyzer.getResultsTable();
		if (rt != null) {
			for (int i = 0; i < rt.size(); i++) {
				if (rt.getLabel(i).startsWith(WINTITLE_FEM2D)) r |= FEM;
				if (rt.getLabel(i).startsWith(WINTITLE_TIB2D)) r |= TIB;
			}
		}
		return r;
	}
	
	public static Roi[] getQuadrantTextRoi(Overlay overlay, int ft) {
		Roi rois[] = new Roi[3];
		
		for (int i = 0, j = 0; i < overlay.size(); i++) {
			String name = overlay.get(i).getName();
			if (name != null && IJX.Util.getIndexOf(COORDSTRS[ft], name) != -1)
				rois[j++] = overlay.get(i);
		}
		
		return rois;
	}
	
	private static void draw(Overlay overlay, XY qxy[]) {
			
		for (double i = 0; i <= 1; i+= 0.25) { // vertical line (HL line)
			int x1 = (int)(qxy[0].x + (qxy[1].x - qxy[0].x) * i);
			int y1 = (int)(qxy[0].y + (qxy[1].y - qxy[0].y) * i);
			int x2 = (int)(x1 + qxy[2].x - qxy[0].x);
			int y2 = (int)(y1 + qxy[2].y - qxy[0].y);
			
			//cip.drawLine(x1, y1, x2, y2);
			Line l = new Line(x1, y1, x2, y2); 
			overlay.add(l);
		}
		
		for (double i = 0; i <= 1; i+= 0.25) { // horizontal line (DS line)
			int x1 = (int)(qxy[0].x + (qxy[2].x - qxy[0].x) * i);
			int y1 = (int)(qxy[0].y + (qxy[2].y - qxy[0].y) * i);
			int x2 = (int)(x1 + qxy[1].x - qxy[0].x);
			int y2 = (int)(y1 + qxy[1].y - qxy[0].y);
			
			//cip.drawLine(x1, y1, x2, y2);
			Line l = new Line(x1, y1, x2, y2);
			overlay.add(l);
		}
	}
	
	private static Overlay createQuadrantOverlay(XY qxy_px[], String coordStr[]) {
		Overlay ol = new Overlay();
		draw(ol, qxy_px);
		ol.setStrokeColor(gridColor);
		
		Font font = ol.getLabelFont();
		int xoff[] = new int[] { -24, 0, -16 }; int yoff[] = new int [] { -14, -14, +4 };
		for (int i = 0; i < 3; i++) {
			//TextRoi text = new TextRoi((int)qxy_px[i].x + xoff[i], (int)qxy_px[i].y + yoff[i], coordStr[i], coordFont);
			TextRoi text = new TextRoi((int)qxy_px[i].x + xoff[i], (int)qxy_px[i].y + yoff[i], coordStr[i], font);
			Rectangle r = text.getBounds();
			Roi rectroi = new Roi(r);
			//rectroi.setName(coordStr[i]);
			//text.setStrokeColor(textColor);
			ol.add(rectroi, coordStr[i]);
		}
		
		ol.drawLabels(true);
		ol.drawNames(true);
		ol.setFillColor(tunnelColor);
		ol.setLabelColor(labelColor);
		
		return ol;
	}
	
	public static void drawAsOverlay(ImagePlus imp, XY qxy_px[], String coordStr[]) {
		Overlay ol = createQuadrantOverlay(qxy_px, coordStr);
		imp.killRoi();
		imp.setOverlay(ol);
		imp.updateAndDraw();
	}
	
	private static XY calcCoord(XY xy, XY qxy[]) {
		XY ret = new XY();
		
		Line lineH = new Line(qxy[0].x, qxy[0].y, qxy[1].x, qxy[1].y) ;
		Line lineV = new Line(qxy[0].x, qxy[0].y, qxy[2].x, qxy[2].y) ;
		Line l = new Line(qxy[0].x, qxy[0].y, xy.x, xy.y);
		
		ret.x = l.getLength() * IJX.Util.cosA(l.getAngle() - lineH.getAngle()) / lineH.getLength();
		ret.y = l.getLength() * IJX.Util.sinA(-(l.getAngle() - lineH.getAngle())) / lineV.getLength();
		
		return ret;
	}
	
	public static XY calcCoord(int ft, XY xy) {
		// x for DS%, y for HL%
		XY qxy[] = SysCoord.get(ft);
		
		if (qxy != null) {
			XY r = calcCoord(xy, qxy);
			if (ft == FEM)
				r.x = 1 - r.x;
			return r;
		}
		return null;
	}
	
	
	private static ImagePlus create2DImageFem() {
		int nrx = RTBoundary.getSplitX();
		ImagePlus impSagZ = IJX.createLFCSagittalProjection(nrx);
		IJ.run(impSagZ, "RGB Color", "");
		
		XY qxy[] = SysCoord.getPx(FEM, impSagZ.getCalibration());
		if (qxy != null)
			drawAsOverlay(impSagZ, qxy, COORDSTR_FEM);
		
		IJX.rename(impSagZ, WINTITLE_FEM2D);
		impSagZ.show();
		
		return impSagZ;
	}
		
	public static ImagePlus create2DImageTib(ImagePlus impTib) {
		int z1 = RTBoundary.getProximalZ(BoundaryData.TIB);
		int z2 = RTBoundary.getDistalZ(BoundaryData.TIB);
		ImagePlus impTibZ = IJX.zproject(impTib, z1, z2);
		IJ.run(impTibZ, "RGB Color", "");
		
		XY qxy[] = SysCoord.getPx(TIB);
		if (qxy != null)
			drawAsOverlay(impTibZ, qxy,COORDSTR_TIB);
		
		IJX.rename(impTibZ, WINTITLE_TIB2D);
		impTibZ.show();
		
		return impTibZ;
	}
	
	public static ImagePlus get2DImage(int ft, boolean create) {
		ImagePlus imp = null;
		if (ft == FEM)
			if ((imp = WindowManager.getImage(WINTITLE_FEM2D)) == null && create)
				imp = create2DImageFem();
		
		if (ft == TIB)
			if ((imp = WindowManager.getImage(WINTITLE_TIB2D)) == null && create)
				imp = create2DImageTib(WindowManager.getImage("TibOnly"));
		
		//imp is shown already.
		return imp;
	}
	
	public static void updateResults(ResultsTable rt) {
		if (rt == null) return;
		
		for (int i = 0; i < rt.size(); i++) {
			int ft = 0;
			if (rt.getLabel(i).startsWith("Femoral")) ft = FEM;
			if (rt.getLabel(i).startsWith("Tibial")) ft = TIB;
			
			XY centroid = new XY(rt.getValue("X", i), rt.getValue("Y", i));
			XY quad = Quadrant.calcCoord(ft, centroid);
			
			rt.setValue("QuadX", i, quad.x);
			rt.setValue("QuadY", i, quad.y);
		}
	}
	
	public static void updateResults() {
		ResultsTable rt = Analyzer.getResultsTable();
		if (rt != null && rt.size() > 0) {
			updateResults(rt);
			rt.show("Results");
		}
	}
	
	/*
	public static void syncWithResults() {
		ImagePlus impFem = WindowManager.getImage(WINTITLE_FEM2D);
		ImagePlus impTib = WindowManager.getImage(WINTITLE_TIB2D);
		if (impFem != null && impFem.getOverlay() != null && tunnelRois[FEM] != null)
			tunnelRois[FEM].removeFromOverlay(impFem.getOverlay());
		if (impTib != null && impTib.getOverlay() != null && tunnelRois[TIB] != null)
			tunnelRois[TIB].removeFromOverlay(impTib.getOverlay());
		
		if (tunnelRois[FEM] != null) {
			tunnelRois[FEM].sync(WINTITLE_FEM2D, ResultsTable.getResultsTable());
			IJX.forceClose("TunOnlyFem");
		}
		if (tunnelRois[TIB] != null) {
			tunnelRois[TIB].sync(WINTITLE_TIB2D, ResultsTable.getResultsTable());
			IJX.forceClose("TunOnlyTib");
		}
		
		//if (impFem != null) drawTunnelApertures(impFem, tunnelRois[FEM]);
		// if (impTib != null) drawTunnelApertures(impTib, tunnelRois[TIB]);
			
	}
	*/

	
	public static int setSystem() { 
		int ft = IJX.radiobuttonFTDialog("Choice", "Choose below to manually determine.", "Quadrant System");
		if (ft == 0) return -1;
		
		ImagePlus imp = get2DImage(ft, true);
		
		XY qxy[] = SysCoord.getPx(ft);
		RotRectTool rrt = (qxy != null) ? new RotRectTool(qxy) : new RotRectTool(imp); 
		RotatedRectRoi rrect = rrt.makeRoi();	
		imp.setRoi(rrect);
		IJ.setTool("rotrect");
		
		int r = IJX.WaitForUser("Modify Rotated Rect Roi to fit the Quadrant system.\n"+
								"Then click OK.");
		if (r == -1) return -1;
		
		RotatedRectRoi rrectN = (RotatedRectRoi)imp.getRoi();
		RotRectTool rrt2 = new RotRectTool(rrectN);
		qxy = rrt2.toSysCoords(ft);
		if (qxy == null)
			return IJX.error("Unexpected Error:"+ft+" "+rrt2.isUnrotatedRect()+rrt2.isDiagonalHorizontal() + rrt2.isDiagonalVertical(), -1);
				
		drawAsOverlay(imp, qxy, COORDSTRS[ft]);
		
		SysCoord.outputPx(ft, qxy);
		
		return 0;
	}
	
	public static void load(String dir) {
		if (!SysCoord.load(dir)) {
			mPointList pl[] = new mPointList[3];
			pl[1] = IJX.loadPointList(dir, FILENAME_FEMQ);
			pl[2] =  IJX.loadPointList(dir, FILENAME_TIBQ);
			
			for (int i = 1; i < 3; i++) {
				if (pl[i] != null) {
					XY qxy[] = pl[i].toArrayXY(XYZSTR[i]);
					SysCoord.output(i, qxy);
				}
			}
		}
		
	}
	
	public static void save(String basepath) {
		SysCoord.save(basepath);
		/*
			if (systemCoordFem != null)
				IJX.savePointList(systemCoordFem, basepath, FILENAME_FEMQ);
			if (Quadrant.systemCoordTib != null)
				IJX.savePointList(Quadrant.systemCoordTib, basepath, FILENAME_TIBQ);
		}
		*/
		/*
		if (Quadrant.tunnelRois[FEM] != null) {
			mPointList pl = IJX.createPointListfromResults(WINTITLE_FEM2D, "Z", "X", "Y");
			IJX.savePointList(pl, basepath, FILENAME_FEMTUN);
		}
		if (Quadrant.tunnelRois[TIB] != null) {
			mPointList pl = IJX.createPointListfromResults(WINTITLE_TIB2D, "X", "Y", "Z");
			IJX.savePointList(pl, basepath, FILENAME_TIBTUN);
		}
		*/
		
		ImagePlus imp = get2DImage(FEM, false);
		if (imp != null && imp.getProcessor() != null) 
			IJX.savePNG(imp, basepath, WINTITLE_FEM2D);
		
		imp = get2DImage(TIB, false);
		if (imp != null && imp.getProcessor() != null)
			IJX.savePNG(imp, basepath, WINTITLE_TIB2D);
		
		ResultsTable rt = Analyzer.getResultsTable();
		if (rt.size() > 0)
			rt.save(IJX.Util.createPath(basepath, FILENAME_RESULTS));
		
	}
}

