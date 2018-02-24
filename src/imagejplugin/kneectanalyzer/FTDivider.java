package imagejplugin.kneectanalyzer;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;

public class FTDivider implements PlugIn {
	private static final String WINTITLE_BOUNDARY = AnatomyDetector.WINTITLE_BOUNDARY;
	
	@Override public void run(String arg) {
	}
	
	public int directRun(ImagePlus imp) {
		RTBoundary rtb = new RTBoundary(WINTITLE_BOUNDARY);
		
		ImagePlus impTib = createTibOnly(imp, rtb); impTib.show();
		ImagePlus impFem = createFemOnly(imp, impTib); impFem.show();  
		
		return 0;
	}
	
	
	private static ImagePlus createTibOnly(ImagePlus imp, RTBoundary rtb) {
		ImagePlus impTib = imp.duplicate();
		IJX.rename(impTib, "TibOnly");
		Calibration cal = impTib.getCalibration();
		int W0 = imp.getWidth(), H0 = imp.getHeight();
		
		int zM = rtb.getDistal(BoundaryData.MFC).z;
		int zL = rtb.getDistal(BoundaryData.LFC).z;
		int zT = rtb.getProximal(BoundaryData.TIB).z;
		
		for (int z = 0; z < zT; z++) {
			ByteProcessor ip = (ByteProcessor)impTib.getImageStack().getProcessor(z + 1);
			ip.setColor(0);
			ip.fillRect(0, 0, W0, H0);
		}
		
		for (int z = zT; z <= Math.max(zM, zL); z++) {
			impTib.setSliceWithoutUpdate(z + 1);
			ByteProcessor ip = (ByteProcessor)impTib.getProcessor();
			ip.setColor(0);
			
			BoundaryData bd = rtb.get(BoundaryData.MFC, z);
			if (bd != null) {
				Rect r = ((Rect)bd).clonePixelized(cal);
				ip.fillRect((int)r.x,  (int)r.y,  (int)r.w,  (int)r.h);
			}
			
			bd = rtb.get(BoundaryData.LFC, z);
			if (bd != null) {
				Rect r = ((Rect)bd).clonePixelized(cal);
				ip.fillRect((int)r.x,  (int)r.y,  (int)r.w,  (int)r.h);
			}
			
			bd = rtb.get(BoundaryData.TIB, z);
			if (bd != null) {
				Rect r = ((Rect)bd).clonePixelized(cal);
				
				imp.setSliceWithoutUpdate(z + 1);
				imp.setRoi((int)r.x,  (int)r.y,  (int)r.w,  (int)r.h);
				imp.copy();
				imp.killRoi();
				impTib.setRoi((int)r.x,  (int)r.y,  (int)r.w,  (int)r.h);
				impTib.paste();
				impTib.killRoi();
			}
		}
		
		return impTib;
	}
	
	private static ImagePlus createFemOnly(ImagePlus imp, ImagePlus impTib) {
		ImageCalculator ic = new ImageCalculator();
		
		ImagePlus impFem = ic.run("XOR create stack", imp, impTib);
		IJX.rename(impFem, "FemOnly");
		return impFem;
	}
	
}