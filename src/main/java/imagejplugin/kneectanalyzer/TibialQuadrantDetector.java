package imagejplugin.kneectanalyzer;

import java.awt.AWTEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagejplugin.kneectanalyzer.Quadrant.SysCoord;

public class TibialQuadrantDetector implements PlugIn {
	boolean skipTP;
	double femCondyleSize = 225;
	
	public TibialQuadrantDetector() {
	}
	public TibialQuadrantDetector(boolean skipTibialPlateauAngle, double femCondyleSize) {
		skipTP = skipTibialPlateauAngle;
		this.femCondyleSize = femCondyleSize;
	}
	
	@Override public void run(String arg) {
		if (WindowManager.getImage("TibOnly") == null) {
			IJX.error("TibOnly not found.");
			return;
		}
		
		directRun();
	}
	
	public ImagePlus directRun() {
		ImagePlus impTib = WindowManager.getImage("TibOnly");
		ImagePlus impTibSag = IJX.createAx2Sag(impTib);
		double angle = createTibPlateau(impTibSag);
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
		SysCoord.output(Quadrant.TIB, qxy);
		
		IJX.forceClose(impTibSag, impTPZ, impTP);
		
		return Quadrant.create2DImageTib(impTib);
	}
	
	private double[] autoRotateTP(ImagePlus impTPZ) {
		Calibration cal = impTPZ.getCalibration();
		int H0 = impTPZ.getHeight();
		
		double halfTPsize = femCondyleSize * 2 / 3;
		AnalyzeParticle ap = new AnalyzeParticle(halfTPsize, cal, "BX BY Width Height Area");
		
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
	
	private XY[] getSystemCoordTP(ImagePlus impTPZ) {
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
	
	private double createTibPlateau(ImagePlus impTibSag) {
		IJ.run(impTibSag, "Fill Holes", "stack");
		ImagePlus impTibSagZ = IJX.zproject(impTibSag);
		impTibSagZ.show();
		
		double angle = 0;
		if (!skipTP) {
			SimpleRotateFilter srf = new SimpleRotateFilter(0);
			WindowManager.setCurrentWindow(impTibSagZ.getWindow());
			new PlugInFilterRunner(srf, null, null);
		
			if (!srf.wasOK)
				return 0;
			angle = srf.angle;
		}
		
		int w = impTibSag.getWidth(), h = impTibSag.getHeight();
		
		BoundaryData bd = RTBoundary.getProximal(BoundaryData.FIB);
		if (bd == null)
			bd = RTBoundary.getDistal(BoundaryData.TIB);
		
		int z;
		if (skipTP) {
			z = bd.z;
		} else {
			XY fibYZ = new XY(bd.y + bd.h, bd.z);
			XY center = new XY(w / 2, h / 2);
			XY fibYZr = IJX.Util.rotateXY(fibYZ, center, angle);
			z = (int)fibYZr.y;
		}
			
		if (angle != 0)
			IJX.rotate(impTibSag, angle);
			
		ImageStack ims = impTibSag.getImageStack().crop(0, 0, 0, w, z, impTibSag.getNSlices());
		impTibSag.setStack(ims);
		
		ImagePlus impTP = IJX.createSag2Ax(impTibSag);
		IJ.setThreshold(impTP, 64, 255, null); // TODO: threshold by user-defined ??
		IJ.run(impTP, "Make Binary", "method=Default background=Default");
		IJX.rename(impTP, "TibPlateau"); 
		IJ.wait(50);
		
		IJX.forceClose(impTibSagZ);
			
		return angle;
	}
}

class SimpleRotateFilter implements ExtendedPlugInFilter, DialogListener {
	public boolean wasOK;
	public double angle = 0;
	
	public SimpleRotateFilter(double initialAngle) {
		angle = initialAngle;
	}
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		return DOES_ALL | SNAPSHOT;
	}

	@Override
	public void run(ImageProcessor ip) {
		ip.rotate(angle);
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		angle = gd.getNextNumber();
		return true;
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		GenericDialog gd = new GenericDialog("Rotate sagitally TibOnly");
		gd.addMessage("Rotate the sagittal projection of TibOnly\nto obtain horizontal tibial plateau.");
		gd.addNumericField("Angle: ", this.angle, 3);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.getPreviewCheckbox().setState(true);
		gd.showDialog();
		
		wasOK = gd.wasOKed();
		
		return 0;
	}

	@Override public void setNPasses(int nPasses) {}
}
