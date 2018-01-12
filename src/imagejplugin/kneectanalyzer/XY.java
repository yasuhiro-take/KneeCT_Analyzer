package imagejplugin.kneectanalyzer;

import ij.IJ;
import ij.measure.Calibration;

public class XY extends Object {
	double x, y;
	public XY(double x, double y) {
		this.x = x; this.y = y;
	}
	public XY() {
		x = y = 0;
	}
	public XY(XY xy) {
		this.x = xy.x;
		this.y = xy.y;
	}
	public XY(double[] array) {
		x = array[0]; y = array[1];
	}
	
	
	public void px2real(Calibration cal) {
		x *= cal.pixelWidth; y *= cal.pixelHeight;
	}
	public void real2px(Calibration cal) {
		x /= cal.pixelWidth; y /= cal.pixelHeight;
	}
	
	public String toString(int decimalPlace) {
		return IJ.d2s(this.x, decimalPlace) + " " + IJ.d2s(this.y, decimalPlace);
	}
	public String toString(int decimalPlace, String headX, String headY) {
		return headX + IJ.d2s(this.x, decimalPlace) + " " + headY + IJ.d2s(this.y, decimalPlace);
	}
	
	public XY clone() {
		return new XY(this);
	}
	public XY clonePixelized(Calibration cal) {
		XY xy = new XY(this);
		xy.real2px(cal);
		return xy;
	}
}
