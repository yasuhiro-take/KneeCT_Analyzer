package imagejplugin.kneectanalyzer;

import java.util.ArrayList;

import ij.measure.Calibration;
import ij.measure.ResultsTable;

public class RTBoundary {
	public static final String WINTITLE_BOUNDARY = "Anatomic Boundary";
	
	public static int getSplitX() {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		if (rt == null) return -1;
		
		int nrx = 0, cnt = 0;
		for (int i = 0; i < rt.size(); i++) {
			String label = rt.getLabel(i);
			if (label.equals("FemSplitX")) {
				return (int)rt.getValue("X", i);
			} else if (label.equals("notchroof")) {
				nrx += (int)rt.getValue("PixelX", i);
				cnt++;
			}
		}
		
		if (cnt > 0)
			return nrx / cnt;
		return -1;
	}
	
	public static int findProximal(ResultsTable rt, String type) {
		if (rt == null) return -1;
		
		int z = Integer.MAX_VALUE;
		int imin = -1;
		
		for (int i = 0; i < rt.size(); i++) {
			if (rt.getLabel(i).equals(type)) {
				int v = (int)rt.getValue("Z", i);
				if (v < z) {
					imin = i;
					z = v;
				}
			}
		}
		
		return imin;
	}
	
	public static int findProximal(String type) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		return findProximal(rt, type);
	}
	
	public static int findDistal(ResultsTable rt, String type) {
		if (rt == null) return -1;
		
		int z = 0;
		int imax = -1;
		
		for (int i = 0; i < rt.size(); i++) {
			if (rt.getLabel(i).equals(type)) {
				int v = (int)rt.getValue("Z", i);
				if (v > z) {
					imax = i;
					z = v;
				}
			}
		}
		
		return imax;
	}
	public static int findDistal(String type) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		return findDistal(rt, type);
	}
	
	private static int find(ResultsTable rt, int type, int z) {
		if (rt == null) return -1;
		String label = BoundaryData.TYPESTRING[type];
		
		for (int i = 0; i < rt.size(); i++) {
			if (rt.getLabel(i).equals(label)) {
				int v = (int)rt.getValue("Z", i);
				if (z == v)
					return i;
			}
		}
		
		return -1;
	}
	
	public static int find(int type, int z) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		return find(rt, type, z);
	}
	
	public static BoundaryData getProximal(String type) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		int imin = findProximal(type);
		
		if (imin != -1) {
			int z = (int)rt.getValue("Z", imin);
			int typevalue = BoundaryData.getType(type);
			
			return new BoundaryData(typevalue, z, getRect(rt, imin));
		}
		return null;
	}
	
	public static BoundaryData getDistal(String type) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		int imax = findDistal(type);
		
		if (imax != -1) {
			int z = (int)rt.getValue("Z", imax);
			int typevalue = BoundaryData.getType(type);
			
			return new BoundaryData(typevalue, z, getRect(rt, imax));
		}
		return null;
	}
	
	public static BoundaryData getProximal(int type) {
		return getProximal(BoundaryData.TYPESTRING[type]);
	}
	public static BoundaryData getDistal(int type) {
		return getDistal(BoundaryData.TYPESTRING[type]);
	}
	
	public static BoundaryData get(int type, int z) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		int i = find(rt, type, z);
		
		if (i != -1)
			return new BoundaryData(type, z, getRect(rt, i));
		return null;
	}
	
	public static BoundaryList getMulti(int z) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		if (rt == null) return null;
		
		BoundaryList bl = new BoundaryList();
		for (int i = 0; i < rt.size(); i++) {
			int s = (int)rt.getValue("Z", i);
			if (s == z) 
				bl.bdlist.add(getOne(rt, i));
				
		}
		
		return bl;
	}
	
	public static int getProximalZ(String type) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		int row = findProximal(rt, type);
		if (row != -1)
			return (int)rt.getValue("Z", row);
		return -1;
		
	}
	public static int getProximalZ(int type) {
		return getProximalZ(BoundaryData.TYPESTRING[type]);
	}
	
	public static int getDistalZ(String type) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		int row = findDistal(rt, type);
		if (row != -1)
			return (int)rt.getValue("Z", row);
		return -1;
		
	}
	public static int getDistalZ(int type) {
		return getDistalZ(BoundaryData.TYPESTRING[type]);
	}
	
	
	public static Rect getRect(ResultsTable rt, int row) {
		double x = rt.getValue("X", row);
		double y = rt.getValue("Y", row);
		double w = rt.getValue("Width", row);
		double h = rt.getValue("Height", row);
		
		return new Rect(x,y,w,h);
	}
	
	public static BoundaryData getOne(ResultsTable rt, int row) {
		int z = (int)rt.getValue("Z", row);
		String type = rt.getLabel(row);
		int typevalue = BoundaryData.getType(type);
		
		return new BoundaryData(typevalue, z, getRect(rt, row));
	}
	
	
	public static void getNotchRoofYZ(ArrayList<Double> yl, ArrayList<Double> zl) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		if (rt == null) return ;
		
		for (int i = 0; i < rt.size(); i++) {
			if (rt.getLabel(i).equals(BoundaryData.TYPESTRING[BoundaryData.NOTCH])) {
				double y = rt.getValue("Y", i);
				double z = rt.getValue("Z", i);
				yl.add(y); zl.add(z);
			}
		}
	}
	
	public static boolean isReal() {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		if (rt == null) return false;
		
		String[] heads = rt.getHeadings();
		if (heads == null) return false;
		
		for (int i = 0; i < heads.length; i++)
			if (heads[i].startsWith("Pixel"))
				return true;
		return false;
	}
	
	public static void real2px(Calibration cal) {
		ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
		if (rt == null) return;
		
		for (int i = rt.size() - 1; i >= 0; i--) {
			double x = rt.getValue("X", i);
			double y = rt.getValue("Y", i);
			double w = rt.getValue("Width", i);
			double h = rt.getValue("Height", i);
			
			Rect r = new Rect(x, y, w, h);
			r.real2px(cal);
			
			rt.setValue("X", i, r.x);
			rt.setValue("Y", i, r.y);
			rt.setValue("Width", i, r.w);
			rt.setValue("Height", i, r.h);
		}
	}
}