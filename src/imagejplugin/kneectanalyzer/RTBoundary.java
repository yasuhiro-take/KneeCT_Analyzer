package imagejplugin.kneectanalyzer;

import java.util.ArrayList;

import ij.measure.Calibration;
import ij.measure.ResultsTable;

public class RTBoundary {
	ResultsTable rt;
	
	public RTBoundary(String title) {
		rt = IJX.getResultsTable(title);
	}
	
	public int getSplitX() {
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
	
	public int findProximal(String type) {
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
	
	public int findDistal(String type) {
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
	
	public int find(int type, int z) {
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
	
	public BoundaryData getProximal(String type) {
		int imin = findProximal(type);
		
		if (imin != -1) {
			int z = (int)rt.getValue("Z", imin);
			int typevalue = BoundaryData.getType(type);
			
			return new BoundaryData(typevalue, z, getRect(imin));
		}
		return null;
	}
	
	public BoundaryData getDistal(String type) {
		int imax = findDistal(type);
		
		if (imax != -1) {
			int z = (int)rt.getValue("Z", imax);
			int typevalue = BoundaryData.getType(type);
			
			return new BoundaryData(typevalue, z, getRect(imax));
		}
		return null;
	}
	
	public BoundaryData getProximal(int type) {
		return getProximal(BoundaryData.TYPESTRING[type]);
	}
	public BoundaryData getDistal(int type) {
		return getDistal(BoundaryData.TYPESTRING[type]);
	}
	
	public BoundaryData get(int type, int z) {
		int i = find(type, z);
		
		if (i != -1)
			return new BoundaryData(type, z, getRect(i));
		return null;
	}
	
	public BoundaryList getMulti(int z) {
		if (rt == null) return null;
		
		BoundaryList bl = new BoundaryList();
		for (int i = 0; i < rt.size(); i++) {
			int s = (int)rt.getValue("Z", i);
			if (s == z) 
				bl.bdlist.add(getOne(i));
				
		}
		
		return bl;
	}
	
	public int getProximalZ(String type) {
		int row = findProximal(type);
		if (row != -1)
			return (int)rt.getValue("Z", row);
		return -1;
		
	}
	
	public int getDistalZ(String type) {
		int row = findDistal(type);
		if (row != -1)
			return (int)rt.getValue("Z", row);
		return -1;
		
	}
	
	public Rect getRect(int row) {
		double x = rt.getValue("X", row);
		double y = rt.getValue("Y", row);
		double w = rt.getValue("Width", row);
		double h = rt.getValue("Height", row);
		
		return new Rect(x,y,w,h);
	}
	
	public BoundaryData getOne(int row) {
		int z = (int)rt.getValue("Z", row);
		String type = rt.getLabel(row);
		int typevalue = BoundaryData.getType(type);
		
		return new BoundaryData(typevalue, z, getRect(row));
	}
	
	
	public void getNotchRoofYZ(ArrayList<Double> yl, ArrayList<Double> zl) {
		if (rt == null) return ;
		
		for (int i = 0; i < rt.size(); i++) {
			if (rt.getLabel(i).equals(BoundaryData.TYPESTRING[BoundaryData.NOTCH])) {
				double y = rt.getValue("Y", i);
				double z = rt.getValue("Z", i);
				yl.add(y); zl.add(z);
			}
		}
	}
	
	public boolean isReal() {
		if (rt == null) return false;
		
		String[] heads = rt.getHeadings();
		if (heads == null) return false;
		
		for (int i = 0; i < heads.length; i++)
			if (heads[i].startsWith("Pixel"))
				return true;
		return false;
	}
	
	public void real2px(Calibration cal) {
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