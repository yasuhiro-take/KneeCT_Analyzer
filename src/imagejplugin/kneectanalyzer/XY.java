package imagejplugin.kneectanalyzer;

import java.util.ArrayList;

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
	public void real2px(double w, double h) {
		x /= w; y /= h;
	}
	public void px2real(double w, double h) {
		x *= w; y *= h;
	}
	
	public String toString(int decimalPlace) {
		return IJ.d2s(this.x, decimalPlace) + " " + IJ.d2s(this.y, decimalPlace);
	}
	public String toString(int decimalPlace, String headX, String headY) {
		return headX + IJ.d2s(this.x, decimalPlace) + " " + headY + IJ.d2s(this.y, decimalPlace);
	}
	public String toString() {
		return toString(3);
	}
	
	public XY clone() {
		return new XY(this);
	}
	public XY clonePixelized(Calibration cal) {
		XY xy = new XY(this);
		xy.real2px(cal);
		return xy;
	}
	public XY cloneRealized(Calibration cal) {
		XY xy = new XY(this);
		xy.px2real(cal);
		return xy;
	}
	
	
	public static XY midstOf(XY xy1, XY xy2) {
		XY m = new XY((xy1.x + xy2.x) / 2, (xy1.y + xy2.y) / 2);
		return m;
	}
	
	public static XY sub(XY xy1, XY xy2) {
		return new XY(xy1.x - xy2.x, xy1.y - xy2.y);
	}
	
	public double getLength() {
		return Math.sqrt(x * x + y * y);
	}
	
	public static double getLength(XY xy1, XY xy2) {
		double xd = xy1.x - xy2.x, yd = xy1.y - xy2.y;
		return Math.sqrt(xd * xd + yd * yd);
	}
}

class XYZ {
	double x, y, z;
	String name; 
	
	public XYZ() {
		x = y = z = 0;
		name = null;
	}
	
	public XYZ(double v[]) {
		x = v[0]; y = v[1]; z = v[2];
		name = null;
	}
	
	public XYZ(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		name = null;
	}
	public XYZ(String name, double x, double y, double z) {
		this(x,y,z);
		this.name = name;
	}
	
	public XYZ(String name, XY xy, String xyz, double theother) {
		if (xyz.equals("XY")) {
			x = xy.x; y = xy.y; z = theother;
		} else if (xyz.equals("XZ")) {
			x = xy.x; z = xy.y; y = theother;
		} else if (xyz.equals("YZ")) {
			y = xy.x; z = xy.y; x = theother;
		}
			
		this.name = name;
	}
	
	public XYZ(String v[]) {
		x = Double.parseDouble(v[0]);
		y = Double.parseDouble(v[1]);
		z = Double.parseDouble(v[2]);
		name = null;
	}
	
	public XYZ(String name, String v[]) {
		this(v);
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String toString() {
		String ret = (name == null) ? "null " : name + " ";
		ret += x + " " + y + " " + z;
		return ret;
	}
	
	public XY getXY() {
		return new XY(this.x, this.y);
	}
	public XY getYZ() {
		return new XY(this.y, this.z);
	}
	public XY getXZ() {
		return new XY(this.x, this.z);
	}
	
	public XY getXYZ(String xyz) {
		if (xyz.equals("XY"))
			return getXY();
		if (xyz.equals("YZ"))
			return getYZ();
		if (xyz.equals("XZ"))
			return getXZ();
		return null;
	}
	
	public void px2real(Calibration cal) {
		x *= cal.pixelWidth; y *= cal.pixelHeight; z *= cal.pixelDepth;
	}
	public void real2px(Calibration cal) {
		x /= cal.pixelWidth; y /= cal.pixelHeight; z /= cal.pixelDepth;
	}
	
	public static XYZ sub(XYZ p1, XYZ p2) {
		return new XYZ(p1.x - p2.x, p1.y - p2.y, p1.z - p2.z);
	}
	
	public static XYZ add(XYZ p1, XYZ p2) {
		return new XYZ(p1.x + p2.x, p1.y + p2.y, p1.z + p2.z);
	}
	
	public void multiply(double p) {
		x *= p; y *= p; z *= p;
	}
}

class mPointList {
	private ArrayList<XYZ> list;
	private static final String LF = System.getProperty("line.separator");
	private int counter;
	
	public mPointList() {
		list = new ArrayList<XYZ>();
		counter = 1;
	}
	
	public XYZ get(int index) {
		return list.get(index);
	}
	
	public void add(XYZ xyz) {
		list.add(xyz);
		counter++;
	}
	public void add(String name, double x, double y, double z) {
		list.add(new XYZ(name, x, y, z));
		counter++;
	}
	public void add(double x, double y, double z) {
		String name = findUniqueName(counter);
		list.add(new XYZ(name, x, y, z));
		counter++;
	}
	public void add(XY xy, String xyz, double theother) {
		String name = findUniqueName(counter);
		list.add(new XYZ(name, xy, xyz, theother));
		counter++;
	}
	
	private String findUniqueName(int cnt) {
		String name = null;
		for (boolean contflag = true; contflag == true; cnt++) {
			name = "point" + cnt;
				
			contflag = false;
			for (int i = 0; i < list.size(); i++)
				if (list.get(i).name.equals(name))
					contflag = true;
		}
		
		return name;
	}
	
	
	public int size() { 
		return list.size();
	}
	
	public void clear() {
		list.clear();
	}
	
	public void remove(int i) {
		list.remove(i);
	}
	
	public XYZ[] toArray() {
		if (list.size() == 0) return null;
		
		XYZ xyz[] = new XYZ[list.size()];
		for (int i = 0; i < list.size(); i++)
			xyz[i] = list.get(i);
		
		return xyz;
	}
	
	public XY[] toArrayXY(String xyz) {
		if (list.size() == 0) return null;
		
		XY xy[] = new XY[list.size()];
		for (int i = 0; i < list.size(); i++)
			xy[i] = list.get(i).getXYZ(xyz);
		
		return xy;
	}
	
	public mPointList duplicate() {
		mPointList pl = new mPointList();
		for (int i = 0; i < this.list.size(); i++)
			pl.add(this.list.get(i));
		
		return pl;
	}
	
	public void parsePointsFileText(String text) {
		list.clear();
		
		String lines[] = text.split("\n");
		int start = lines[0].matches(".*frame.*") ? 1 : 0;
		
		for (int i = start; i < lines.length; i++) {
			String l[] = lines[i].split(":");
			String name = l[0].substring(1, l[0].length() - 1);
			String nums = l[1].substring(3, l[1].length() - 2);
			String xyz[] = nums.split(", ");
			this.add(new XYZ(name, xyz));
		}
	}
	
	public String getPointFileText() {
		String ret = null;
		for (int i = 0; i < list.size(); i++){
			if (i == 0) ret = "";
			
			XYZ p = list.get(i);
						
			String name = (p.name == null) ? "point" + Integer.toString(i + 1) : p.name;
			
			String l = "\"" + name + "\": [ ";
			l += p.x + ", " + p.y + ", " + p.z + " ]" + LF;
			
			ret += l;
		}
		
		return ret;
	}
	
	public mPointList clonePixelized(Calibration cal) {
		mPointList retPl = new mPointList();
		
		for (int i = 0; i < this.size(); i++) {
			double x = this.get(i).x / cal.pixelWidth;
			double y = this.get(i).y / cal.pixelHeight;
			double z = this.get(i).z / cal.pixelDepth;
			retPl.add(this.get(i).getName(), x, y, z);
		}
		
		return retPl;
	}
	
	public mPointList cloneRealized(Calibration cal) {
		mPointList retPl = new mPointList();
		
		for (int i = 0; i < this.size(); i++) {
			double x = this.get(i).x * cal.pixelWidth;
			double y = this.get(i).y * cal.pixelHeight;
			double z = this.get(i).z * cal.pixelDepth;
			retPl.add(this.get(i).getName(), x, y, z);
		}
		
		return retPl;
	}
	
	public void px2real(Calibration cal) {
		for (XYZ p: list)
			p.px2real(cal);
	}
	public void real2px(Calibration cal) {
		for (XYZ p: list)
			p.real2px(cal);
	}
	
}

