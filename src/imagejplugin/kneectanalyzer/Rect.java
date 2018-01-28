package imagejplugin.kneectanalyzer;

import ij.measure.Calibration;

public class Rect extends Object {
	double x, y, w, h;
	
	public Rect() {
		x = 0; y = 0; w = 0; h = 0;
	}
	public Rect(double v[]) {
		x = v[0]; y = v[1]; w = v[2]; h = v[3];
	}
	public Rect(Rect r) {
		x = r.x; y = r.y; w = r.w; h = r.h;
	}
	public Rect(double x2, double y2, double w2, double h2) {
		x = x2; y = y2; w = w2; h = h2;
	}
	public Rect(java.awt.Rectangle r) {
		x = r.x; y = r.y; w = r.width; h = r.height;
	}
	
	public boolean isInside(double ax, double ay) {
		if (x <= ax && ax < x + w && y <= ay && ay < y + h)
			return true;
		else
			return false;
	}
	public boolean isInside(double xy[]) {
		return isInside(xy[0], xy[1]);
	}
	public boolean isInside(XY xy) {
		return isInside(xy.x, xy.y);
	}
	
	boolean equals(Rect r) {
		if (r != null && r.x == x && r.y == y && r.w == w && r.h == h)
			return true;
		return false;
	}
	
	public XY getCenter() {
		return new XY(this.x + this.w / 2, this.y + this.h / 2);
	}
	
	public void px2real(Calibration cal) {
		x *= cal.pixelWidth; w *= cal.pixelWidth;
		y *= cal.pixelHeight; h *= cal.pixelHeight; 
	}
	public void real2px(Calibration cal) {
		x /= cal.pixelWidth; w /= cal.pixelWidth;
		y /= cal.pixelHeight; h /= cal.pixelHeight; 
	}
	public Rect clone() {
		return new Rect(this);
	}
	
	public Rect clonePixelized(Calibration cal) {
		Rect r = new Rect(this);
		r.real2px(cal);
		return r;
	}
	
	public String toString() {
		String ret = Double.toString(this.x) + " ";
		ret += Double.toString(this.y) + " ";
		ret += Double.toString(this.w) + " ";
		ret += Double.toString(this.h);
		return ret;
	}
	
	public XY[] toCoordsPx() {
		XY ret[] = new XY[4];
		
		ret[0].x = x; ret[0].y = y;
		ret[1].x = x + w - 1; ret[1].y = y;
		ret[2].x = x; ret[2].y = y + h - 1;
		ret[3].x = x + w - 1; ret[1].y = y + h - 1;
		
		return ret;
	}
}
