package imagejplugin.kneectanalyzer;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.measure.Calibration;

public class BoundaryData extends Rect {
	int type, z;
	final static int MFC = 1;
	final static int LFC = 3;
	final static int FEM = 2;
	final static int TIB = 4;
	final static int NOTCHEND = 5;
	final static int NOTCH = 6;
	final static int NOTCHROOF = 7;
	final static int TIBPLATEAU = 8;
	final static int FIB = 9;
	final static int[] TYPES = { MFC, FEM, LFC, TIB, NOTCHEND, NOTCH, NOTCHROOF, TIBPLATEAU, FIB };
	final static String TYPESTRING[] = { "unknown", "mfc", "fem", "lfc", "tib", "notchend", "notch", "notchroof", "tibplateau", "fib" };
	
	public BoundaryData() {
		super();
		type = 0; z = 0;
	}
	public BoundaryData(int type, int z, double v[]) {
		super(v);
		this.type = type; this.z = z;
	}
	public BoundaryData(int type, int z, Rect r) {
		super(r);
		this.type = type; this.z = z; 
	}
	public BoundaryData(int type, int z, java.awt.Rectangle r) {
		super(r);
		this.type = type; this.z = z; 
	}
	public BoundaryData(int type, int z, double x, double y, double w, double h) {
		super(x, y, w, h);
		this.type = type; this.z = z;
	}
	
	public String toString() {
		String ret = Integer.toString(this.type) + " " + Integer.toString(this.z) + " ";
		ret += super.toString();
		
		return ret;
	}
	
	public String getTypeString() {
		return TYPESTRING[this.type];
	}
	
	public static int getType(String type) {
		for (int i = 1; i < TYPESTRING.length; i++)
			if (TYPESTRING[i].equals(type))
				return i;
		return 0;
	}
	
	public void draw(ImagePlus imp) {
		imp.setSlice(this.z + 1);
		Rect r = ((Rect)this).clone();
		r.real2px(imp.getCalibration());
		
		imp.getProcessor().drawRect((int)r.x, (int)r.y, (int)r.w, (int)r.h);	
		imp.getProcessor().drawString(this.getTypeString(), (int)r.x, (int)r.y);
	}
	
	
	public void fill(ImagePlus imp) {
		imp.setSlice(this.z + 1);
		Rect r = ((Rect)this).clone();
		r.real2px(imp.getCalibration());
		
		imp.getProcessor().fillRect((int)r.x, (int)r.y, (int)r.w, (int)r.h);
	}
}
