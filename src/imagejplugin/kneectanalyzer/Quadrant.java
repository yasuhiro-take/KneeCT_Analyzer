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
	
	public void toResults(ImagePlus imp) {
		IJ.run("Set Measurements...", "area centroid display redirect=None decimal=3");
		if (!IJ.isResultsWindow())
			IJ.getTextPanel();
		
		Analyzer analyzer = new Analyzer(imp);
		
		for (QRoi r: qroiList) {
			if (r.isAperture) {
				imp.setRoi(r.roi);
				//IJ.run(imp, "Measure", "");
				analyzer.measure();
			}
		}
		
		imp.killRoi();
		ResultsTable rt = Analyzer.getResultsTable();
		rt.updateResults();
		rt.show("Results");
	}
	
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

class QCurveFitter extends CurveFitter {
	int repetition;
	ImagePlus impWork;
	
	QCurveFitter(double x[], double y[]) {
		super(x, y);
	}
}

class Quadrant  {
	static final int FEM=1, TIB=2;
	//static mPointList systemCoordFem, systemCoordTib;
	//static QRoiList tunnelRoisFem, tunnelRoisTib;
	static QRoiList tunnelRois[] = new QRoiList[3];
	static final String WINTITLE_FEM2D = "FemoralQuadrant";
	static final String WINTITLE_TIB2D = "TibialQuadrant";
	static final String WINTITLES[] = new String[] { null, WINTITLE_FEM2D, WINTITLE_TIB2D };
	static final String WINTITLES3D[] = new String[] { null, WINTITLE_FEM2D+"3D", WINTITLE_TIB2D+"3D" };
	static final String WINTITLE_RTQUAD = "Quadrant System RefCoords";
	static final String WINTITLE_BOUNDARY = "Anatomic Boundary"; // should be equal to IJIF.java
	static final String XYZSTR[] = new String[] { null, "YZ", "XY" }; 
	private static final String FILENAME_FEMQ = "femQuad.points";
	private static final String FILENAME_TIBQ = "tibQuad.points";
	private static final String FILENAME_FEMTUN = "femTun.points";
	private static final String FILENAME_TIBTUN = "tibTun.points";
	private static final String FILENAME_RESULTS = "tunnelCoords.txt";
	private static final String FILENAME_QSYSTEM = "QuadSysCoords.txt";
	private static final String COORDSTR_FEM[] = new String[] { "DS:100", "DS:0 HL:0", "HL:100"};
	private static final String COORDSTR_TIB[] = new String[] { "ML:0 AP:0", "ML:100", "AP:100"};
	private static final String COORDSTRS[][] = new String[][] { null, COORDSTR_FEM, COORDSTR_TIB };
	private static Font coordFont = new Font(Font.SERIF, 0, 16);
	private static Color gridColor = new Color(0,255,0);
	private static Color textColor = new Color(255,0,0);
	private static Color notchRoofColor = new Color(0,0,255);
	private static Color tunnelColor = new Color(0,255,255);
	
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
	
	public static void init() {
		//systemCoordFem = null; systemCoordTib = null;
		//tunnelRoisFem = null; tunnelRoisTib = null;
		tunnelRois[1] = tunnelRois[2] = null;
	}
	/*
	 * deprecated. use SysCoord.getDetermined()
	public static int systemDetermined() {
		int r = 0;
		if (systemCoordFem != null) r |= FEM;
		if (systemCoordTib != null) r |= TIB;
			
		return r;
	}
	*/
	
	public static int tunnelDetermined() {
		int r = 0;
		if (tunnelRois[FEM] != null) r |= FEM;
		if (tunnelRois[TIB] != null) r |= TIB;
		return r;
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
	
	private static void draw(ImageProcessor cip, Color c, XY qxy[]) {
		cip.setLineWidth(1); cip.setColor(c);
		
		for (double i = 0; i <= 1; i+= 0.25) {
			int x1 = (int)(qxy[0].x + (qxy[1].x - qxy[0].x) * i);
			int y1 = (int)(qxy[0].y + (qxy[1].y - qxy[0].y) * i);
			int x2 = (int)(x1 + qxy[2].x - qxy[0].x);
			int y2 = (int)(y1 + qxy[2].y - qxy[0].y);
			
			//cip.drawLine(x1, y1, x2, y2);
			Line l = new Line(x1, y1, x2, y2); l.drawPixels(cip);
		}
		
		for (double i = 0; i <= 1; i+= 0.25) {
			int x1 = (int)(qxy[0].x + (qxy[2].x - qxy[0].x) * i);
			int y1 = (int)(qxy[0].y + (qxy[2].y - qxy[0].y) * i);
			int x2 = (int)(x1 + qxy[1].x - qxy[0].x);
			int y2 = (int)(y1 + qxy[1].y - qxy[0].y);
			
			//cip.drawLine(x1, y1, x2, y2);
			Line l = new Line(x1, y1, x2, y2); l.drawPixels(cip);
		}
	}
	
	private static void drawFem(ImageProcessor ip, Color c, mPointList pl_px) {
		XY qxy[] = new XY[3];
		for (int i = 0; i < 3; i++) {
			//qxy[i] = new XY();
			//qxy[i].setFromPointYZ(pl_px.get(i));
			qxy[i] = pl_px.get(i).getYZ();
		}
		
		draw(ip, c, qxy);
	}
	
	private static void drawFem(Overlay overlay, Color c, mPointList pl_px) {
		XY qxy[] = new XY[3];
		for (int i = 0; i < 3; i++)
			qxy[i] = pl_px.get(i).getYZ();
		
		draw(overlay, qxy);
		overlay.setStrokeColor(c);
	}
	
	private static void drawTib(ImageProcessor ip, Color c, mPointList pl_px) {
		XY qxy[] = new XY[3];
		for (int i = 0; i < 3; i++) {
			//qxy[i] = new XY();
			//qxy[i].setFromPointXY(pl_px.get(i));
			qxy[i] = pl_px.get(i).getXY();
		}
		
		draw(ip, c, qxy);
	}
	private static void drawTib(Overlay ol, Color c, mPointList pl_px) {
		XY qxy[] = new XY[3];
		for (int i = 0; i < 3; i++)
			qxy[i] = pl_px.get(i).getXY();
		
		draw(ol, qxy);
		ol.setStrokeColor(c);
	}
	
	private static Overlay createQuadrantOverlay(XY qxy_px[], String coordStr[]) {
		Overlay ol = new Overlay();
		draw(ol, qxy_px);
		ol.setStrokeColor(gridColor);
		
		int xoff[] = new int[] { -24, 0, -16 }; int yoff[] = new int [] { -14, -14, +4 };
		for (int i = 0; i < 3; i++) {
			TextRoi text = new TextRoi((int)qxy_px[i].x + xoff[i], (int)qxy_px[i].y + yoff[i], coordStr[i], coordFont);
			text.setStrokeColor(textColor);
			ol.add(text);
		}
		
		return ol;
	}
	
	private static void drawAsOverlay(ImagePlus imp, XY qxy_px[], String coordStr[]) {
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
	
	private static QRoiList getTunnels(ImagePlus imp, int start, int end) {
		// imp should have undergone *fill-hole* except tunnels.
		Calibration cal = imp.getCalibration();
		AnalyzeParticle ap = new AnalyzeParticle(ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.ADD_TO_MANAGER,
												0, 5, 300, cal, null);
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
	
	private static void drawTunnelApertures(ImagePlus imp, QRoiList ql) {
		Overlay overlay = imp.getOverlay();
		if (overlay == null) overlay = new Overlay();
		
		ql.toOverlay(overlay);
		
		overlay.setFillColor(tunnelColor);
		imp.setOverlay(overlay);
		imp.updateAndDraw();
	}
	
	private static QCurveFitter analyzeBlumensaat(ImagePlus impSagZ) {
		int W0 = impSagZ.getWidth(); 
		Calibration cal = impSagZ.getCalibration();
		double pxSize = Math.sqrt(Math.pow(cal.pixelWidth, 2) + Math.pow(cal.pixelHeight, 2));
		
		ArrayList<Double> xList = new ArrayList<Double>(), yList = new ArrayList<Double>();
		(new RTBoundary(WINTITLE_BOUNDARY)).getNotchRoofYZ(xList, yList);
		
		boolean continueFlag; int cnt = 1; QCurveFitter cf; 
		do {
			continueFlag = false;
			double[] x = IJX.Util.list2array(xList), y = IJX.Util.list2array(yList); 
			cf = new QCurveFitter(x, y); 
			cf.doFit(CurveFitter.STRAIGHT_LINE);
					
			ImagePlus impS = impSagZ.duplicate();
			IJ.run(impS, "RGB Color", "");
			ColorProcessor cip = (ColorProcessor)impS.getProcessor();
			cip.setLineWidth(1); cip.setColor(notchRoofColor);
			
			for (int i = 0; i < x.length; i++)
				cip.fillRect((int)x[i] - 1, (int)y[i] - 1, 3, 3);
			
			cip.setColor(textColor);
			int x1 = 0, y1 = (int)cf.f(0), x2 = W0 - 1, y2 = (int)cf.f(W0 - 1);
			cip.drawLine(x1, y1, x2, y2);
			
			cip.drawString(cf.getResultString(), 0, 16);
			impS.setTitle("KCAwork-Blumensaat_" + Integer.toString(cnt++));
			impS.show();
			
			double residuals[] = cf.getResiduals(); double sd = cf.getSD();
			for (int i = x.length - 1; i >= 0; i--) {
				double r = Math.abs(residuals[i]);
				boolean out = false;
				
				if (sd * pxSize > 5) {
					if (r * pxSize >= 10) out = true;
				} else if (sd * pxSize > 2.5 && (r > sd * 2 || r * pxSize >= 5))
					out = true;
				
				if (out) {
					xList.remove(i); yList.remove(i);
					continueFlag = true;
				}
			}
				
			cf.repetition = cnt;
			cf.impWork = impS;
		} while (continueFlag);
		
		return cf;
	}
	
	private static XY[] getRefCoordsFem(ImagePlus impSagM, CurveFitter cf) {
		int W0 = impSagM.getWidth(), H0 = impSagM.getHeight();
		
		ByteProcessor ip = (ByteProcessor)impSagM.getProcessor();

		int xQ[] = new int[3];
		for (int x = 0; x <= W0 - 1; x++)
			if (ip.getPixel(x, (int)cf.f(x)) == 255) {
				xQ[0] = x; x = W0;
			}
		
		for (int x = W0 - 1; x >= 0; x--) 
			if (ip.getPixel(x, (int)cf.f(x)) == 255) {
				xQ[1] = x; x = 0;
			}
		
		int b = 0; boolean flag; do {
			b++; flag = false;
			
			for (int x = xQ[0]; x <= W0 - 1; x++) {
				int y = (int)cf.f(x) + b;
				if (ip.getPixel(x, y) == 255) {
					flag = true; x = W0;
				}
			}
		} while (flag && b < H0);
		
		for (int x = xQ[0]; x <= W0 - 1; x++) {
			int px = ip.getPixel(x, (int)cf.f(x) + b - 1);
			if (px == 255) {
				xQ[2] = x; x = W0;
			}
		}
		
		XY qxy[] = new XY[3];
		for (int i = 0; i < 3; i++) {
			int y = (int)cf.f(xQ[i]) + ((i == 2) ? b - 1 : 0);
			qxy[i] = new XY(xQ[i], y);
			//pl.add(pl_x, xQ[i], y);
		}
		
		return qxy;
	}
	
	private static XY[] getSystemCoordFem(XY[] refxy) {
		
		
		Line l1 = new Line(refxy[0].x, refxy[0].y, refxy[1].x, refxy[1].y);
		Line l2 = new Line(refxy[0].x, refxy[0].y, refxy[2].x, refxy[2].y);
		double a1 = l1.getAngle(), a2 = l2.getAngle();
		
		double lenHL = l2.getLength() * IJX.Util.sinA(a1 - a2);
		double xHL100 = refxy[0].x + lenHL * IJX.Util.cosA(90 - a1);
		double yHL100 = refxy[0].y + lenHL * IJX.Util.sinA(90 - a1);
		
		XY[] qxy = new XY[3];
		qxy[0] = refxy[0];
		qxy[1] = refxy[1];
		qxy[2] = new XY(xHL100, yHL100);
			
		return qxy;
	}
	
	private static void correctHL100(ImagePlus impSagM, XY qxy[]) {
		double x1 = qxy[0].x, y1 = qxy[0].y, x2 = qxy[1].x, y2 = qxy[1].y, x3 = qxy[2].x, y3 = qxy[2].y;
		
		
		for (int hl = 100; hl > 75; hl--) {
			double lx1 = x1 + (x3 - x1) * hl / 100, ly1 = y1 + (y3 - y1) * hl / 100;
			double lx2 = x2 + (x3 - x1) * hl / 100, ly2 = y2 + (y3 - y1) * hl / 100;
			
			double pxdata[] = impSagM.getProcessor().getLine(lx1, ly1, lx2, ly2);
			int i = IJX.Util.getIndexOf(pxdata, 255);
			if (i < pxdata.length * 3 / 4) {
				qxy[2].x = lx1; qxy[2].y = ly1;
				return;
			}
		}
	}
	
	private static double[] autoRotateTP(ImagePlus impTPZ) {
		Calibration cal = impTPZ.getCalibration();
		int H0 = impTPZ.getHeight();
		
		AnalyzeParticle ap = new AnalyzeParticle(150, cal, "BX BY Width Height Area");
		
		int pxdata[] = new int[H0];
		double angle, angleSum = 0; 
		boolean failed = false;
		do {
			ByteProcessor ip = (ByteProcessor)impTPZ.getProcessor();
			//ap = new AnalyzeParticle(150, cal, "BX BY Width Height Area");
			int nResults = ap.newAnalyze(impTPZ, ip, 16, 255);
			
			angle = 0;
			if (nResults > 0) {
				int imax = IJX.Util.getMaxIndex(ap.getAllRowValues(4));
				Rect rMax = new Rect(ap.getAllColumnValues(imax));
				int midx = (int)((rMax.x + rMax.w / 2) / cal.pixelWidth);
			
				ip.getColumn(midx, 0, pxdata, H0);
		
				ip.setColor(0); ip.setLineWidth(1); 
				ip.drawLine(midx, 0, midx, H0);
			
				nResults = ap.newAnalyze(impTPZ, ip, 16, 255);
			
				ip.putColumn(midx, 0, pxdata, H0);
				
				Rect r[] = new Rect[2];
				if (nResults >= 2) { 
					for (int i = 0, j = 0; i < nResults && j < 2; i++) {
						Rect rTmp = new Rect(ap.getAllColumnValues(i));
						if (rMax.isInside(rTmp.getCenter()))
							r[j++] = rTmp.clonePixelized(cal);
					}
			
					int il = (r[0].x < r[1].x) ? 0 : 1;
					Rect rectL = r[il], rectR = r[1 - il];
			
					int xl = IJX.Util.getIndexOf(IJX.getRectLine(ip, rectL, 2), 255) + (int)rectL.x;
					int yl = (int)(rectL.y + rectL.h - 1);
					int xr = IJX.Util.getLastIndexOf(IJX.getRectLine(ip, rectR, 2), 255) + (int)rectR.x;
					int yr = (int)(rectR.y + rectR.h - 1);
			
					Line l = new Line(xl, yl, xr, yr);
					angle = l.getAngle();
					ip.rotate(angle);
			
					angleSum += angle;
				} else {
					failed = true;
				}
			} else {
				failed = true;
			}
		} while (angle > 1 && !failed);
		
		if (failed)
			return null;
		
		ap.newAnalyze(impTPZ, impTPZ.getProcessor(), 16, 255);
		int imax = IJX.Util.getMaxIndex(ap.getAllRowValues(4));
		
		double ret[] = ap.getAllColumnValues(imax);
		ret[4] = angleSum;
		
		return ret;
	}
	
	private static XY[] getSystemCoordTP(ImagePlus impTPZ) {
		double dat[] = autoRotateTP(impTPZ); 
		if (dat == null) return null;
		
		double angle = dat[4];
		Rect r = new Rect(dat);
		
		XY xy = new XY(), qxy[] = new XY[3]; 
		XY center = new XY(impTPZ.getWidth() / 2, impTPZ.getHeight() / 2);
		center.px2real(impTPZ.getCalibration());
		
		// reset the axial rotation
		xy.x = r.x; xy.y = r.y;
		qxy[0] = IJX.Util.rotateXY(xy, center, -angle);
		
		xy.x = r.x + r.w;
		qxy[1] = IJX.Util.rotateXY(xy, center, -angle);
		
		xy.x = r.x; xy.y = r.y + r.h;
		qxy[2] = IJX.Util.rotateXY(xy, center, -angle);
		
		return qxy;
	}
	
	private static int getSplitX() {
		RTBoundary rtb = new RTBoundary(WINTITLE_BOUNDARY);
		
		return rtb.getSplitX();
	}
	
	
	public static ImagePlus detectFemoralSystem() {
		int nrx = getSplitX();
		
		ImagePlus impSagZ = IJX.createLFCSagittalProjection(nrx);
		
		QCurveFitter cf = analyzeBlumensaat(impSagZ);
		
		AnalyzeParticle ap = new AnalyzeParticle(ParticleAnalyzer.INCLUDE_HOLES | ParticleAnalyzer.SHOW_MASKS, 
				0, 500, impSagZ.getCalibration(), null);
		ap.analyze(impSagZ, impSagZ.getProcessor(), 16, 255);
		ImagePlus impSagM = ap.analyzer.getOutputImage();
		
		XY refxy[] = getRefCoordsFem(impSagM, cf);
		XY qxyPx[] = getSystemCoordFem(refxy);
		correctHL100(impSagM, qxyPx);
		SysCoord.output(FEM, qxyPx, impSagZ.getCalibration());
		
		drawAsOverlay(cf.impWork, qxyPx, COORDSTR_FEM);
		IJX.rename(cf.impWork, Quadrant.WINTITLE_FEM2D);
		
		IJX.forceClose(impSagZ, impSagM);
		
		return cf.impWork;
	}
	
	public static ImagePlus detectTibialSystem() {
		ImagePlus impTib = WindowManager.getImage("TibOnly");
		ImagePlus impTibSag = IJX.createAx2Sag(impTib);
		double angle = IJIF.createTibPlateau(impTibSag);
		ImagePlus impTP = WindowManager.getImage("TibPlateau");
		if (impTP == null) return null;
		int TPz1 = IJX.getFirstSlice(impTP);
		ImagePlus impTPZ = IJX.zproject(impTP, TPz1 + 1, impTP.getNSlices());
		
		XY qxyTP[] = getSystemCoordTP(impTPZ); // auto-rotated impTPZ
		if (qxyTP == null) return null;
		
		int W0 = impTibSag.getWidth(), H0 = impTibSag.getHeight();
		XY center = new XY(W0 / 2, H0 / 2); center.px2real(impTibSag.getCalibration());
		 XY qxy[] = new XY[3];
		for (int i = 0; i < 3; i++) {
			XY qyzTP = new XY(qxyTP[i].y, TPz1 * impTP.getCalibration().pixelDepth);
			XY qyzTib = IJX.Util.rotateXY(qyzTP, center, -angle);
			qxy[i] = new XY(qxyTP[i].x, qyzTib.x);
		}
		SysCoord.output(TIB, qxy);
		
		IJX.forceClose(impTibSag, impTPZ, impTP);
		
		return create2DImageTib(impTib);
	}
	
	
	private static ImagePlus create2DImageFem(int nrx) {
		//ImagePlus impFem = WindowManager.getImage("FemOnly");
		ImagePlus impSagZ = IJX.createLFCSagittalProjection(nrx);
		IJ.run(impSagZ, "RGB Color", "");
		
		XY qxy[] = SysCoord.getPx(FEM, impSagZ.getCalibration());
		if (qxy != null) {
			//mPointList pl = systemCoordFem.clonePixelized(impFem.getCalibration());
			//XY qxy[] = pl.toArrayXY("YZ");
			drawAsOverlay(impSagZ, qxy, COORDSTR_FEM);
		}
		
		IJX.rename(impSagZ, WINTITLE_FEM2D);
		impSagZ.show();
		
		return impSagZ;
	}
		
	private static ImagePlus create2DImageTib(ImagePlus impTib) {
		RTBoundary rtb = new RTBoundary(WINTITLE_BOUNDARY);
		int z1 = rtb.getProximalSlice(BoundaryData.TYPESTRING[BoundaryData.TIB]) - 1;
		int z2 = rtb.getDistalSlice(BoundaryData.TYPESTRING[BoundaryData.TIB]) - 1;
		ImagePlus impTibZ = IJX.zproject(impTib, z1, z2);
		IJ.run(impTibZ, "RGB Color", "");
		
		XY qxy[] = SysCoord.getPx(TIB);
		if (qxy != null) {
			//mPointList pl = systemCoordTib.clonePixelized(impTib.getCalibration());
			//XY qxy[] = pl.toArrayXY("XY");
			drawAsOverlay(impTibZ, qxy,COORDSTR_TIB);
		}
		
		IJX.rename(impTibZ, WINTITLE_TIB2D);
		impTibZ.show();
		
		return impTibZ;
	}
	
	public static ImagePlus get2DImage(int ft, boolean create, int nrx) {
		ImagePlus imp = null;
		if (ft == FEM)
			if ((imp = WindowManager.getImage(WINTITLE_FEM2D)) == null && create)
				imp = create2DImageFem(nrx);
		
		if (ft == TIB)
			if ((imp = WindowManager.getImage(WINTITLE_TIB2D)) == null && create)
				imp = create2DImageTib(WindowManager.getImage("TibOnly"));
		
		//imp is shown already.
		return imp;
	}
	
	public static int detectFemoralTunnel() {
		int nrx = getSplitX();
		
		ImagePlus impFem = WindowManager.getImage("FemOnly").duplicate();
		impFem.show();
		IJ.run(impFem, "Fill Holes", "stack");
		
		ImagePlus impLFC = IJX.createLFCOnly(impFem, nrx); impLFC.show(); // not entitled as 'LFCOnly' at this time.
		ImagePlus impSag = IJX.createAx2Sag(impLFC);
		IJX.forceClose(impFem); 
		
		tunnelRois[FEM] = getTunnels(impSag, nrx, impSag.getNSlices() - 1); 
		IJX.forceClose(impSag);
		
		if (WindowManager.getImage("LFCOnly") == null)
			impLFC.setTitle("LFCOnly");
		else
			IJX.forceClose(impLFC);
		
		ImagePlus imp2D = get2DImage(FEM, true, nrx);
		
		drawTunnelApertures(imp2D, tunnelRois[FEM]);
		tunnelRois[FEM].toResults(imp2D);
				
		return 0;
	}
	
	public static int detectTibialTunnel() {
		
		
		ImagePlus impTib = WindowManager.getImage("TibOnly");
		ImagePlus impSag = IJX.createAx2Sag(impTib);
		
		IJ.run(impSag, "Fill Holes", "stack");
		ImagePlus imp = IJX.createSag2Ax(impSag);
		IJX.forceClose(impSag);
		
		RTBoundary rtb = new RTBoundary(WINTITLE_BOUNDARY);
		int z1 = rtb.getProximalSlice(BoundaryData.TYPESTRING[BoundaryData.TIB]) - 1;
		int z2 = rtb.getDistalSlice(BoundaryData.TYPESTRING[BoundaryData.TIB]) - 1;
		
		tunnelRois[TIB] = getTunnels(imp, z1, z2); 
		IJX.forceClose(imp);
		
		ImagePlus imp2D = get2DImage(TIB, true, 0);
		
		drawTunnelApertures(imp2D, tunnelRois[TIB]);
		tunnelRois[TIB].toResults(imp2D);
		
		return 0;
	}
	
	public static void measurements2Coord(ResultsTable rt) {
		Calibration calBase = IJX.getBaseCalibration();
		Calibration calFem2D = IJX.getCalibration(WINTITLE_FEM2D);
		
		int nResults = rt.size();
		for (int i = 0; i < nResults; i++) {
			String label = rt.getLabel(i);
			XY centroid = new XY(rt.getValue("X", i), rt.getValue("Y", i));
			
			XY quad = null; double z = 0;
			
			//if (label.indexOf(WINTITLE_FEM2D) == 0 && systemCoordFem != null) {
			if (label.startsWith(WINTITLE_FEM2D) && (quad = calcCoord(FEM, centroid)) != null) {
				if (label.matches(WINTITLE_FEM2D+":[0-9]+")) {
					int apID = IJX.Util.string2int(label, ":", 1);
					QRoi qroi = (tunnelRois[FEM] != null) ? tunnelRois[FEM].findAperture(apID) : null;

					z = (qroi != null) ? qroi.z * calBase.pixelWidth : 0;
				} else {
					QRoi qroi = (tunnelRois[FEM] != null) ? tunnelRois[FEM].findAperture(centroid, calFem2D) : null;
					z = (qroi != null) ? qroi.z * calBase.pixelWidth : 0;
				}
				
			} else if (label.startsWith(WINTITLE_TIB2D) && (quad = calcCoord(TIB, centroid)) != null) {
				if (label.matches(WINTITLE_TIB2D+":[0-9]+")) {
					int apID = IJX.Util.string2int(label, ":", 1);
					QRoi qroi = (tunnelRois[TIB] != null) ? tunnelRois[TIB].findAperture(apID) : null;

					z = (qroi != null) ? qroi.z * calBase.pixelDepth : 0;
				} else {
					QRoi qroi = (tunnelRois[TIB] != null) ? tunnelRois[TIB].findAperture(centroid, calBase) : null;
					z = (qroi != null) ? qroi.z * calBase.pixelDepth : 0;
				}
					
			}
			
			if (quad != null) {
				rt.setValue("QuadX", i, quad.x);
				rt.setValue("QuadY", i, quad.y);
				if (z != 0)
					rt.setValue("Z", i, z);
			}
		}
	}
	
	public static void measurements2Coord() {
		ResultsTable rt = Analyzer.getResultsTable();
		measurements2Coord(rt);
		rt.updateResults();
		rt.show("Results");
	}
	
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
		
		if (impFem != null)
			drawTunnelApertures(impFem, tunnelRois[FEM]);
		if (impTib != null)
			drawTunnelApertures(impTib, tunnelRois[TIB]);
	}
	
	public static int setSystem() {
		int nrx = getSplitX();
		int ft = IJX.radiobuttonFTDialog("Choice", "Choose below to manually determine.", "Quadrant System");
		if (ft == 0) return -1;
		ImagePlus imp = (ft == 1) ? get2DImage(FEM, true, nrx) : get2DImage(TIB, true, 0);
		
		XY qxy[] = SysCoord.getPx(ft);
		RotRectTool rrt = (qxy != null) ? new RotRectTool(qxy) : new RotRectTool(imp); 
			//mPointList pl = systemCoordFem.clonePixelized(IJX.getBaseCalibration());
			//rrt = new RotRectTool(pl.toArrayXY("YZ"));
		
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
		/*
		mPointList pl = new mPointList();
		for (int i = 0; i < 3; i++)
			pl.add(qxy[i], XYZSTR[ft], 0);
		if (ft == FEM)
			systemCoordFem = pl.cloneRealized(IJX.getBaseCalibration());
		else
			systemCoordTib = pl.cloneRealized(IJX.getBaseCalibration());
		*/
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
		
		if (Quadrant.tunnelRois[FEM] != null) {
			mPointList pl = IJX.createPointListfromResults(WINTITLE_FEM2D, "Z", "X", "Y");
			IJX.savePointList(pl, basepath, FILENAME_FEMTUN);
		}
		if (Quadrant.tunnelRois[TIB] != null) {
			mPointList pl = IJX.createPointListfromResults(WINTITLE_TIB2D, "X", "Y", "Z");
			IJX.savePointList(pl, basepath, FILENAME_TIBTUN);
		}
		
		ImagePlus imp = get2DImage(FEM, false, 0);
		if (imp != null && imp.getProcessor() != null) 
			IJX.savePNG(imp, basepath, WINTITLE_FEM2D);
		
		imp = get2DImage(TIB, false, 0);
		if (imp != null && imp.getProcessor() != null)
			IJX.savePNG(imp, basepath, WINTITLE_TIB2D);
		
		ResultsTable rt = Analyzer.getResultsTable();
		if (rt.size() > 0)
			rt.save(IJX.Util.createPath(basepath, FILENAME_RESULTS));
		
	}
}

/*
 private static QCoord[] outputTunnels2D(ImagePlus imp, ArrayList<QRoi> rois, boolean isFem) {
		Color col = new Color(0,255,255), colBlack = new Color(0,0,0);
		
		ArrayList<QCoord> ret = new ArrayList<QCoord>();
		
		int y = 16;
		for (QRoi r: rois) {
			if (r.isAperture) {
				XY cxy = new XY(r.roi.getContourCentroid());
				cxy.px2real(imp.getCalibration());
				XY quadCoord = isFem ? getCoordFem(cxy) : getCoordTib(cxy);
				ret.add(new QCoord(cxy, quadCoord));
		
				imp.setColor(col);
				imp.getProcessor().fill(r.roi);
				
				imp.setColor(colBlack);
				String outStr = isFem ? quadCoord.toString(3, "DS:", "HL:") : quadCoord.toString(3, "ML:", "AP:");
				int w = imp.getProcessor().getStringWidth(outStr);
				imp.getProcessor().drawString(outStr, imp.getWidth() - w - 10, y);
				y += 16;
			}
		}
		
		imp.updateAndDraw();
		
		return (QCoord[])ret.toArray(new QCoord[0]);
	}
	
	public static XY getCoordFem(XY xy) {
		// x for DS%, y for HL%
		XY qxy[] = new XY[3];
		
		for (int i = 0; i < 3; i++) {
			//qxy[i] = new XY();
			//qxy[i].setFromPointYZ(systemCoordFem.get(i));
			qxy[i] = systemCoordFem.get(i).getYZ();
		}
		
		XY r = getCoord(xy, qxy);
		r.x = 1 - r.x;
		return r;
	}
	
	public static XY getCoordTib(XY xy) {
		// x for ML%, y for AP%
		XY qxy[] = new XY[3];
		
		for (int i = 0; i < 3; i++) {
			//qxy[i] = new XY();
			//qxy[i].setFromPointXY(systemCoordTib.get(i));
			qxy[i] = systemCoordTib.get(i).getXY();
		}
		return getCoord(xy, qxy);
	}
	
	*/
