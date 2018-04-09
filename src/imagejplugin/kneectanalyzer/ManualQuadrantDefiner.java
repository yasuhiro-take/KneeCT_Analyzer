package imagejplugin.kneectanalyzer;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.gui.Line;
import ij.gui.RotatedRectRoi;

public class ManualQuadrantDefiner implements PlugIn {
	@Override public void run(String arg) {
		directrun();
	}
	
	public int directrun() { 
		int ft = IJX.radiobuttonFTDialog("Choice", "Choose below to manually determine.", "Quadrant System");
		if (ft == 0) return -1;
		
		ImagePlus imp = Quadrant.get2DImage(ft, true);
		
		XY qxy[] = Quadrant.SysCoord.getPx(ft);
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
				
		Quadrant.drawAsOverlay(imp, qxy, Quadrant.COORDSTRS[ft]);
		
		Quadrant.SysCoord.outputPx(ft, qxy);
		
		return 0;
	}
	
	
}

class RotRectTool {
	private XY[] rectxy = new XY[4];
	private XY[] rotxy = new XY[2];
	private XY centroid;
	double length;
	
	public RotRectTool(XY[] rectxy) {
		for (int i = 0; i < 4; i++) {
			if (i < rectxy.length)
				this.rectxy[i] = rectxy[i].clone();
			else
				this.rectxy[i] = new XY(0,0);
		}
		
		toRotated();
	}
	
	public RotRectTool(ImagePlus imp) {
		int w = imp.getWidth(), h = imp.getHeight();
		rotxy[0] = new XY(w / 4, h / 2);
		rotxy[1] = new XY(w * 3 / 4, h / 2);
		length = h / 4;
	}
	
	public RotRectTool(RotatedRectRoi roi) {
		double params[] = roi.getParams();
		centroid = new XY(roi.getContourCentroid());
		
		rotxy[0] = new XY(params[0], params[1]);
		rotxy[1] = new XY(params[2], params[3]);
		length = params[4];
		
		fromRotated();
	}
	
	public void toRotated() {
		Line l = new Line(rectxy[0].x, rectxy[0].y, rectxy[2].x, rectxy[2].y);
		length = l.getLength();
		double angle = l.getAngle();
		double xoff = (length / 2) * IJX.Util.cosA(angle), yoff = (length / 2) * IJX.Util.sinA(-angle);
		
		for (int i = 0; i < 2; i++) 
			this.rotxy[i] = new XY(rectxy[i].x + xoff, rectxy[i].y + yoff); 
	}
	
	public void fromRotated() {
		Line l = new Line(rotxy[0].x, rotxy[0].y, rotxy[1].x, rotxy[1].y);
		double angle = l.getAngle() - 90;
				
		double xoff = (length / 2) * IJX.Util.cosA(angle), yoff = (length / 2) * IJX.Util.sinA(-angle);
		
		for (int i = 0; i < 2; i++)
			rectxy[i] = new XY(rotxy[i].x - xoff, rotxy[i].y - yoff);
		
		for (int i = 0; i < 2; i++)
		rectxy[i + 2] = new XY(rotxy[i].x + xoff, rotxy[i].y + yoff);
	}
	
	public RotatedRectRoi makeRoi() {
		if (rotxy != null)
			return new RotatedRectRoi(rotxy[0].x, rotxy[0].y, rotxy[1].x, rotxy[1].y, length);
		return null;
		}
	
	public XY[] getRect() {
		return rectxy;
	}
	
	public void setCentroid(XY c) {
		centroid = c.clone(); 
	}
	
	public XY getN() {
		XY ret = null;
		for (int i = 0; i < 4; i++) {
			if (rectxy[i].y < centroid.y) {
				if (ret == null) 
					ret = rectxy[i];
				else if (rectxy[i].y < ret.y)
					ret = rectxy[i];
			}
		}
		
		return ret;
	}
	
	public XY getS() {
		XY ret = null;
		for (int i = 0; i < 4; i++) {
			if (rectxy[i].y > centroid.y) {
				if (ret == null) 
					ret = rectxy[i];
				else if (rectxy[i].y > ret.y)
					ret = rectxy[i];
			}
		}
		
		return ret;
	}
	
	public XY getW() {
		XY ret = null;
		for (int i = 0; i < 4; i++) {
			if (rectxy[i].x < centroid.x) {
				if (ret == null) 
					ret = rectxy[i];
				else if (rectxy[i].x < ret.x)
					ret = rectxy[i];
			}
		}
		
		return ret;
	}
	
	public XY getE() {
		XY ret = null;
		for (int i = 0; i < 4; i++) {
			if (rectxy[i].x > centroid.x) {
				if (ret == null) 
					ret = rectxy[i];
				else if (rectxy[i].x > ret.x)
					ret = rectxy[i];
			}
		}
		
		return ret;
	}
	
	public XY getNW() {
		for (int i = 0; i < 4; i++)
			if (rectxy[i].x < centroid.x && rectxy[i].y < centroid.y)
				return rectxy[i];
		return null; 
	}
	
	public XY getNE() {
		for (int i = 0; i < 4; i++)
			if (rectxy[i].x > centroid.x && rectxy[i].y < centroid.y)
				return rectxy[i];
		return null; 
	}
	
	public XY getSW() {
		for (int i = 0; i < 4; i++)
			if (rectxy[i].x < centroid.x && rectxy[i].y > centroid.y)
				return rectxy[i];
		return null; 
	}
	
	public XY getSE() {
		for (int i = 0; i < 4; i++)
			if (rectxy[i].x > centroid.x && rectxy[i].y > centroid.y)
				return rectxy[i];
		return null; 
	}
	
	public boolean isUnrotatedRect() {
		return (rotxy[0].x == rotxy[1].x || rotxy[0].y == rotxy[1].y);
	}
	
	public boolean isDiagonalHorizontal() {
		return (rectxy[0].y == rectxy[3].y || rectxy[1].y == rectxy[2].y);
	}
	
	public boolean isDiagonalVertical() {
		return (rectxy[0].x == rectxy[3].x || rectxy[1].x == rectxy[2].x);
	}
	
	public XY[] toSysCoords(int ft) {
		XY qxy[] = new XY[3];
		if ((ft == 1 && !isUnrotatedRect()) || 
				(ft == 2 && (isDiagonalHorizontal() || isDiagonalVertical()))) {
			qxy[0] = getW(); qxy[1] = getN(); qxy[2] = getS();
		} else if ((ft == 2 && !isDiagonalHorizontal() && !isDiagonalVertical()) ||
					(ft == 1 && isUnrotatedRect())) {
			qxy[0] = getNW(); qxy[1] = getNE(); qxy[2] = getSW();
		} else return null;
		return qxy;
	}
}

