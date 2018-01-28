package imagejplugin.kneectanalyzer;

import java.util.ArrayList;

import ij.measure.ResultsTable;

public class RTBoundary {
	ResultsTable rt;
	
	public RTBoundary(String title) {
		rt = IJX.getResultsTable(title);
	}
	
	public int getSplitX() {
		if (rt == null) return -1;
		
		for (int i = 0; i < rt.size(); i++) {
			if (rt.getLabel(i).equals("FemSplitX")) {
				return (int)rt.getValue("PixelX", i);
			}
		}
		
		return -1;
	}
	
	public int findProximal(String type) {
		if (rt == null) return -1;
		
		int z = Integer.MAX_VALUE;
		int imin = -1;
		
		for (int i = 0; i < rt.size(); i++) {
			if (rt.getLabel(i).equals(type)) {
				int v = (int)rt.getValue("Slice", i);
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
				int v = (int)rt.getValue("Slice", i);
				if (v > z) {
					imax = i;
					z = v;
				}
			}
		}
		
		return imax;
	}
	
	public BoundaryData getProximal(String type) {
		int imin = findProximal(type);
		
		if (imin != -1) {
			int z = (int)rt.getValue("Slice", imin) - 1;
			int typevalue = BoundaryData.getType(type);
			
			return new BoundaryData(typevalue, z, getRect(imin));
		}
		return null;
	}
	
	public BoundaryData getDistal(String type) {
		int imax = findDistal(type);
		
		if (imax != -1) {
			int z = (int)rt.getValue("Slice", imax) - 1;
			int typevalue = BoundaryData.getType(type);
			
			return new BoundaryData(typevalue, z, getRect(imax));
		}
		return null;
	}
	
	public int getProximalSlice(String type) {
		int row = findProximal(type);
		if (row != -1)
			return (int)rt.getValue("Slice", row);
		return -1;
		
	}
	
	public int getDistalSlice(String type) {
		int row = findDistal(type);
		if (row != -1)
			return (int)rt.getValue("Slice", row);
		return -1;
		
	}
	
	public Rect getRect(int row) {
		double x = rt.getValue("X", row);
		double y = rt.getValue("Y", row);
		double w = rt.getValue("Width", row);
		double h = rt.getValue("Height", row);
		
		return new Rect(x,y,w,h);
	}
	
	public void getNotchRoofYZ(ArrayList<Double> yl, ArrayList<Double> zl) {
		if (rt == null) return ;
		
		for (int i = 0; i < rt.size(); i++) {
			if (rt.getLabel(i).equals(BoundaryData.TYPESTRING[BoundaryData.NOTCHROOF])) {
				double y = rt.getValue("PixelY", i);
				double z = rt.getValue("Slice", i) - 1;
				yl.add(y); zl.add(z);
			}
		}
	}
}