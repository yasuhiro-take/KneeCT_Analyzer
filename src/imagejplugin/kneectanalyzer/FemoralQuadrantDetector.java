package imagejplugin.kneectanalyzer;

import ij.ImagePlus;
import ij.gui.Line;
import ij.measure.CurveFitter;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;



public class FemoralQuadrantDetector implements PlugIn {
	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		
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
