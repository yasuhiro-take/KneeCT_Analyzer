package imagejplugin.kneectanalyzer;

import java.awt.Color;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.plugin.PlugIn;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import imagejplugin.kneectanalyzer.Quadrant.SysCoord;



public class FemoralQuadrantDetector implements PlugIn {
	private static Color notchRoofColor = new Color(0,0,255);
	private double femSize = 500;
	private double blumenR1 = 10, blumenR2 = 5, blumenSD = 2;
	private boolean DS0ByCondyle = true;
	
	public FemoralQuadrantDetector() {
	}
	
	public void setSagittalFemoralProjectionSize(double size) {
		femSize = size;
	}
	public void setBlumensaatDetectionParams(double r1, double r2, double sd) {
		blumenR1 = r1;
		blumenR2 = r2;
		blumenSD = sd;
	}
	public void setDS0ByCondyle(boolean byCondyle) {
		DS0ByCondyle = byCondyle;
	}
	
	@Override public void run(String arg) {
		if (WindowManager.getImage("FemOnly") == null) {
			IJX.error("FemOnly not found.");
			return;
		}
		
		directRun();
	}
	
	public ImagePlus directRun() {
		int nrx = RTBoundary.getSplitX();
		
		ImagePlus impSagZ = IJX.createLFCSagittalProjection(nrx);
		
		QCurveFitter cf = analyzeBlumensaat(impSagZ);
		
		AnalyzeParticle ap = new AnalyzeParticle(ParticleAnalyzer.INCLUDE_HOLES | ParticleAnalyzer.SHOW_MASKS, 
				0, femSize, impSagZ.getCalibration(), null); 
		ap.analyze(impSagZ, impSagZ.getProcessor(), 16, 255);
		ImagePlus impSagM = ap.analyzer.getOutputImage();
		
		XY refxy[] = getRefCoordsFem(impSagM, cf);
		XY qxyPx[] = getSystemCoordFem(refxy);
		correctHL100(impSagM, qxyPx);
		if (DS0ByCondyle)
			correctDS0(impSagM, qxyPx);
		SysCoord.output(Quadrant.FEM, qxyPx, impSagZ.getCalibration());
		
		Quadrant.drawAsOverlay(cf.impWork, qxyPx, Quadrant.COORDSTR_FEM);
		IJX.rename(cf.impWork, Quadrant.WINTITLE_FEM2D);
		
		IJX.forceClose(impSagZ, impSagM);
		
		return cf.impWork;
	}
	
	private QCurveFitter analyzeBlumensaat(ImagePlus impSagZ) {
		int W0 = impSagZ.getWidth(); 
		Calibration cal = impSagZ.getCalibration();
		//double pxSize = Math.sqrt(Math.pow(cal.pixelWidth, 2) + Math.pow(cal.pixelHeight, 2));
		double pxSize = cal.pixelHeight;
		
		ArrayList<Double> xList = new ArrayList<Double>(), yList = new ArrayList<Double>();
		RTBoundary.getNotchRoofYZ(xList, yList);
		
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
			
			cip.setColor(Quadrant.textColor);
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
					if (r * pxSize >= blumenR1) out = true; 
				} else if (sd * pxSize > 2.5 && (r > sd * blumenSD || r * pxSize >= blumenR2)) 
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
	
	private XY[] getRefCoordsFem(ImagePlus impSagM, CurveFitter cf) {
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
	
	private XY[] getSystemCoordFem(XY[] refxy) {
		
		
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
	
	private void correctHL100(ImagePlus impSagM, XY qxy[]) {
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
	
	private void correctDS0(ImagePlus impSagM, XY qxy[]) {
		double x1 = qxy[0].x, y1 = qxy[0].y, x2 = qxy[1].x, y2 = qxy[1].y, x3 = qxy[2].x, y3 = qxy[2].y;
		
		
		for (int k = 100;;k++) {
			double lx1 = x1 + (x2 - x1) * k / 100, ly1 = y1 + (y2 - y1) * k / 100;
			double lx2 = x3 + (x2 - x1) * k / 100, ly2 = y3 + (y2 - y1) * k / 100;
			
			double pxdata[] = impSagM.getProcessor().getLine(lx1, ly1, lx2, ly2);
			if (IJX.Util.getIndexOf(pxdata, 255) == -1) {
				qxy[1].x = x1 + (x2 - x1) * (k - 1) / 100;
				qxy[1].y = y1 + (y2 - y1) * (k - 1) / 100;
				return;
			}
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


/*
private XY[] getSystemCoordFem(mPointList refpl) {
mPointList qpl = new mPointList();

Line l1 = new Line(refpl.get(0).y, refpl.get(0).z, refpl.get(1).y, refpl.get(1).z);
Line l2 = new Line(refpl.get(0).y, refpl.get(0).z, refpl.get(2).y, refpl.get(2).z);
double a1 = l1.getAngle(), a2 = l2.getAngle();

double lenHL = l2.getLength() * IJX.Util.sinA(a1 - a2);
double yHL100 = refpl.get(0).y + lenHL * IJX.Util.cosA(90 - a1);
double zHL100 = refpl.get(0).z + lenHL * IJX.Util.sinA(90 - a1);

qpl.add(refpl.get(0));
qpl.add(refpl.get(1));
qpl.add(refpl.get(0).x, yHL100, zHL100);
	
return qpl;
}

private static void correctHL100(ImagePlus impSagM, mPointList pl) {
double x1 = pl.get(0).y, y1 = pl.get(0).z, x2 = pl.get(1).y, y2 = pl.get(1).z, x3 = pl.get(2).y, y3 = pl.get(2).z;


for (int hl = 100; hl > 75; hl--) {
	double lx1 = x1 + (x3 - x1) * hl / 100, ly1 = y1 + (y3 - y1) * hl / 100;
	double lx2 = x2 + (x3 - x1) * hl / 100, ly2 = y2 + (y3 - y1) * hl / 100;
	
	double pxdata[] = impSagM.getProcessor().getLine(lx1, ly1, lx2, ly2);
	int i = IJX.Util.getIndexOf(pxdata, 255);
	if (i < pxdata.length * 3 / 4) {
		pl.remove(2);
		pl.add(pl.get(0).x, lx1, ly1);
		return;
	}
}
}*/
