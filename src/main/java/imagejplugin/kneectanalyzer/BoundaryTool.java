package imagejplugin.kneectanalyzer;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;

class BoundaryList {
	ArrayList<BoundaryData> bdlist;
	
	public BoundaryList() {
		bdlist = new ArrayList<BoundaryData>();
	}
	
	public BoundaryList(String lines[]) {
		this.bdlist = StringToList(lines);
	}
	
	public void fromList(String lines[]) {
		this.bdlist = StringToList(lines);
	}
	
	static ArrayList<BoundaryData> StringToList(String lines[]) {
		ArrayList<BoundaryData> bdatList = new ArrayList<BoundaryData>();
		
		for(String line: lines) {
			if (line != null) {
				String l[] = line.split(" ");
				if (l.length == 6) {
					double d[] = new double[4];
					for (int i = 2; i <= 5; i++)
						d[i - 2] = Double.parseDouble(l[i]);
					
					BoundaryData bd = new BoundaryData(Integer.parseInt(l[0]), Integer.parseInt(l[1]), d);
					
					bdatList.add(bd);
				}
			}
		}
		
		return bdatList;
	}
	
	public int getMeanNotchRoofX() {
		return getMeanNotchRoofX(this);
	}
	
	public static int getMeanNotchRoofX(BoundaryList bl) {
		int ret = 0, cnt = 0;
		for (BoundaryData bd: bl.bdlist) {
			if (bd.type == BoundaryData.NOTCHROOF) {
				ret += (int)bd.x;
				cnt++;
			}
		}
		
		return ret / cnt;
	}
	
	public String toString() {
		String ret = "";
		
		for (BoundaryData bd: bdlist) {
			ret += bd.toString() + System.getProperty("line.separator");
		}
		
		return ret;
	}
	
	public void clear() {
		bdlist.clear();
	}
	
	public void clear(int type) {
		for (int i = bdlist.size() - 1; i >= 0; i--) {
			BoundaryData bd = bdlist.get(i);
			if (bd.type == type)
				bdlist.remove(i);
		}
	}
	
	private BoundaryData find(int type, boolean proximal) {
		BoundaryData bdRet = null;
		
		for (BoundaryData bd: bdlist) {
			if (type == bd.type) {
				if (bdRet == null) 
					bdRet = bd;
				else if (proximal && bd.z < bdRet.z)
					bdRet = bd;
				else if (!proximal && bd.z > bdRet.z)
					bdRet = bd;
			}
		}
		
		return bdRet;
	}
	public BoundaryData findProximal(int type) {
		return find(type, true);
	}
	public BoundaryData findDistal(int type) {
		return find(type, false);
	}
	
	public BoundaryData find(int type, int z) {
		for (BoundaryData bd: bdlist) {
			if (type == bd.type && z == bd.z)
				return bd;
		}
		
		return null;
	}
	
	public int findFirstZ() {
		int z = Integer.MAX_VALUE;
		for (BoundaryData bd: bdlist) {
			z = Math.min(z, bd.z);
		}
		return z;
	}
	
	public int findLastZ() {
		int z = 0;
		for (BoundaryData bd: bdlist) {
			z = Math.max(z, bd.z);
		}
		return z;
	}
	
	public void real2px(Calibration cal) {
		for (BoundaryData bd: bdlist)
			if (bd.type != BoundaryData.NOTCHROOF)
				bd.real2px(cal);
	}
	
	public void px2real(Calibration cal) {
		for (BoundaryData bd: bdlist)
			if (bd.type != BoundaryData.NOTCHROOF)
				bd.px2real(cal);
	}
	
	public void toResults(String title, int nrx, boolean isPixelized) {
		ResultsTable rt = IJX.getResultsTable(title);
		if (rt != null)
			rt.reset();
		else
			rt = new ResultsTable();
		
		rt.incrementCounter();
		rt.addLabel("FemSplitX");
		rt.addValue(isPixelized ? "X" : "PixelX", nrx);
		
		BoundaryData bd;
		int z1 = findFirstZ(); int z2 = findLastZ();
		for (int z = z1; z <= z2; z++) {
			 for (int type: BoundaryData.TYPES) {
				 if ((bd = find(type, z)) != null) {
					 rt.incrementCounter();
					 rt.addLabel(BoundaryData.TYPESTRING[type]);
				
					 if (type == BoundaryData.NOTCHROOF && !isPixelized) {
						 rt.addValue("PixelX", bd.x);
						 rt.addValue("PixelY", bd.y);
					 } else {
						 rt.addValue("X", bd.x);
						 rt.addValue("Y", bd.y);
						 rt.addValue("Width", bd.w);
						 rt.addValue("Height", bd.h);
					 } 
				
					 rt.addValue(isPixelized ? "Z" : "Slice-1", z);
				 }
			 }
		}
		
		rt.show(title);
	}
}

public class BoundaryTool extends BoundaryList implements Measurements  {
	private ImagePlus imp;
	private AnalyzeParticle ap;
	private double condyleSize;
	
	final static int BD_MFC = BoundaryData.MFC;
	final static int BD_LFC = BoundaryData.LFC;
	final static int BD_FEM = BoundaryData.FEM;
	final static int BD_TIB = BoundaryData.TIB;
	final static int BD_NOTCHEND = BoundaryData.NOTCHEND;
	final static int BD_NOTCH = BoundaryData.NOTCH;
	final static int BD_NOTCHROOF = BoundaryData.NOTCHROOF;
	final static int BD_TIBPLATEAU = BoundaryData.TIBPLATEAU;
	final static int BD_FIB = BoundaryData.FIB;
	final static int[] BD_TYPES = { BD_MFC, BD_FEM, BD_LFC, BD_TIB, BD_NOTCHEND, BD_NOTCH, BD_NOTCHROOF, BD_TIBPLATEAU, BD_FIB };
	
	class RXYA extends Object {
		Rect r;
		double cx, cy, area;
		public RXYA(double v[]) {
			r = new Rect(v);
			cx = v[4]; cy = v[5]; area = v[6];
		}
	}
	
	public BoundaryTool(ImagePlus imp, double condyleSize) {
		super();
		
		ImagePlus imp2 = imp.duplicate();
		IJ.run(imp2, "Fill Holes", "stack");
		IJ.run(imp2, "Open", "stack");
		imp2.show();
		
		this.imp = imp2;
		ap = new AnalyzeParticle(condyleSize, imp.getCalibration(), "BX BY Width Height X Y Area");
		this.condyleSize = condyleSize;
	}
	
	public void close() {
		IJX.forceClose(this.imp);
	}
	
	private static Rect reduceBoundaryBox(ByteProcessor ip, Calibration cal, BoundaryData bd) {
		int x1 = (int)(bd.x / cal.pixelWidth); int w = (int)(bd.w / cal.pixelWidth);
		int y1 = (int)(bd.y / cal.pixelHeight); int h = (int)(bd.h / cal.pixelHeight);
		
		Rect r = new Rect();
		
		for (int x = x1; x < x1 + w; x++) 
			for (int y = y1; y < y1 + h; y++) 
				if (ip.getPixel(x, y) != 0) {
					r.x = x; x = x1 + w; y = y1 + h;
				}
			
		for (int x = x1 + w - 1; x >= x1; x--)
			for (int y = y1; y < y1 + h; y++) 
				if (ip.getPixel(x, y) != 0) {
					r.w = x - r.x + 1; x = x1; y = y1 + h;
				}
		
		for (int y = y1; y < y1 + h; y++)
			for (int x = x1; x < x1 + w; x++)
				if (ip.getPixel(x, y) != 0) {
					r.y = y; y = y1 + h; x = x1 + w;
				}
		
		for (int y = y1 + h - 1; y >= y1; y--)
			for (int x = x1; x < x1 + w; x++)
				if (ip.getPixel(x, y) != 0) {
					r.h = y - r.y + 1; y = y1; x = x1 + w;
				}
		
		if (r.x == 0)
			r = null;
		else 
			r.px2real(cal);
		
		return r;
	}
	
	private static Rect reduceBoundaryBox2(ByteProcessor ip, Calibration cal, Rect rOrig) {
		Rect r = rOrig.clonePixelized(cal);
		int w = (int)r.w, h = (int)r.h, x = (int)r.x, y = (int)r.y;
		
		int[] pxRow = new int[w], pxColumn = new int[h];
		double[] rows = new double[h], columns = new double[w];
		
		for (int i = 0; i < h; i++) {
			ip.getRow(x, y + i, pxRow, w);
			rows[i] = IJX.Util.getMean(pxRow);
		}
		for (int i = 0; i < w; i++) {
			ip.getColumn(x + i, y, pxColumn, h);
			columns[i] = IJX.Util.getMean(pxColumn);
		}
		
		if (IJX.Util.getMax(rows) == 0)
			return null;
		
		int ic = IJX.Util.getMaxIndex(columns), ir = IJX.Util.getMaxIndex(rows);
		
		int nx1 = x, nx2 = x + w - 1, ny1 = y, ny2 = y + h - 1;
		for (int i = ir; i >= 0; i--)
			if (rows[i] == 0) {
				ny1 = y + i; i = 0;
			}
		
		for (int i = ir; i < h; i++)
			if (rows[i] == 0) {
				ny2 = y + i; i = h;
			}
		
		for (int i = ic; i >= 0; i--)
			if (columns[i] == 0) {
				nx1 = x + i; i = 0;
			}
		
		for (int i = ic; i < w; i++)
			if (columns[i] == 0) {
				nx2 = x + i; i = w;
			}
		
		if (nx1 == x && nx2 == x + w - 1 && ny1 == y && ny2 == y + h - 1)
			return null;
		
		r.x = nx1; r.y = ny1; r.w = nx2 - nx1 + 1; r.h = ny2 - ny1 + 1;
		r.px2real(cal);
		
		return r;
	}
	
	public int scanFEC() {
		int nSlices = imp.getNSlices();
		double maxRectArea = 0;
		int theSlice = 0;
		Rect maxRect = null;
		
		for (int s = 0; s < nSlices; s++) {
			int nResults = ap.newAnalyze(imp, s + 1);
					
			if (nResults > 0) {
				double area[] = ap.getAllRowValues(6);
				int row = IJX.Util.getMaxIndex(area);
				Rect r = new Rect(ap.getMultiColumnValues(0, 3, row));
				double rectarea = r.h * r.w;
				if (rectarea > maxRectArea) {
					maxRectArea = rectarea;
					theSlice = s;
					maxRect = r;
				}
				
				if (nResults == 2 && maxRect != null) {
					XY cxy1 = new XY (ap.getMultiColumnValues(4, 5, 0));
					XY cxy2 = new XY (ap.getMultiColumnValues(4, 5, 1));
					
					if (maxRect.isInside(cxy1) && maxRect.isInside(cxy2) &&
						Math.abs(cxy1.x - cxy2.x) > Math.abs(cxy1.y - cxy2.y) * 2) {
						System.out.println("probable MFC/LFC slice:" + s);
						
						bdlist.add(new BoundaryData(BD_FEM, theSlice, maxRect));
						
						return 0;
					}
				}
			}
		}
		
		System.out.println("findFEC: theSlice=" + theSlice + " maxRect="+maxRect);
		
		return -1;
	}
	
	public int scanFemur(double condyleratioMin, double condyleratioMax) {
		final Calibration cal = imp.getCalibration();
		
		ArrayList<RXYA> pdlist = new ArrayList<RXYA>();
		
		BoundaryData bdatNotchEnd = null, bdatM = null, bdatL = null;
		BoundaryData bdatF = findProximal(BD_FEM);
			
		for (int z = bdatF.z + 1; z < imp.getNSlices(); z++) {
			ImageProcessor ip = imp.getImageStack().getProcessor(z + 1);
			
			int nResults = ap.newAnalyze(imp, ip);
			
			Rect rectM = null, rectL = null, rectT = null;
			pdlist.clear(); 
			if (nResults > 0) {
				for (int i = 0; i < nResults; i++) 
					pdlist.add(new RXYA(ap.getAllColumnValues(i)));
			
				for(int i = pdlist.size() - 1; i >= 0; i--)
					if (!bdatF.isInside(pdlist.get(i).cx, pdlist.get(i).cy))
						pdlist.remove(i);
				
				if (pdlist.size() > 0) { // particles inside the bdatF(=boundarybox of femur at epicondylar level)
					if (bdatNotchEnd == null && pdlist.size() == 2) {
						// when the slice# for distal-end of notch is not determined,
						RXYA pd1 = pdlist.get(0), pd2 = pdlist.get(1); 
						double arearatio = Math.min(pd1.area, pd2.area) / Math.max(pd1.area, pd2.area);
						if (Math.abs(pd1.cx - pd2.cx) > Math.abs(pd1.cy - pd2.cy) &&
							condyleratioMin < arearatio && arearatio < condyleratioMax) {
							// when two particles locate medio-laterally, not antero-posteriorly
							// the sizes of two particles are similar 
							bdatNotchEnd = new BoundaryData(BD_NOTCHEND, z - 1,
								Math.min(pd1.cx, pd2.cx), Math.min(pd1.r.y, pd2.r.y), 
								Math.abs(pd1.cx - pd2.cx), Math.max(pd1.r.h, pd2.r.h));
							bdlist.add(bdatNotchEnd);
							
							if (pd1.cx < pd2.cx) {
								rectM = pd1.r; rectL = pd2.r;
							} else {
								rectM = pd2.r; rectL = pd1.r;
							}
						}
					} else if (bdatM != null || bdatL != null) {
						// when medial or lateral condyle is separately identified in the previous slice	
						for (RXYA pd: pdlist) {
							if (bdatM != null && bdatM.isInside(pd.cx, pd.cy) && bdatM.getArea() >= pd.area)
								rectM = pd.r;
							if (bdatL != null && bdatL.isInside(pd.cx, pd.cy) && bdatL.getArea() >= pd.area)
								rectL = pd.r;
						}

						if (rectM == null || rectL == null) {
							// when condyle is depicted smaller (than the thresholded size),
							// the tibial spine may be identified as particle
							for (RXYA pd: pdlist) 
								if (bdatNotchEnd.isInside(pd.cx, pd.cy) && pd.r != rectM && pd.r != rectL) 
									rectT = pd.r;
							
							// reduce the bounding box determined in the previous slice
							if (rectM == null && bdatM != null) 
								rectM = reduceBoundaryBox2((ByteProcessor)ip, cal, bdatM);
							if (rectL == null && bdatL != null)
								rectL = reduceBoundaryBox2((ByteProcessor)ip, cal, bdatL);
						} // if rectM/rectL == null
					} // elseif bdatM/bdatL != null
				} // if rows.size > 0
			} // if nResults > 0
			
			if (pdlist.size() == 0 && bdatM != null)
				rectM = reduceBoundaryBox2((ByteProcessor)ip, cal, bdatM);
			if (pdlist.size() == 0 && bdatL != null)
				rectL = reduceBoundaryBox2((ByteProcessor)ip, cal, bdatL);
			
			bdatM = (rectM != null) ? new BoundaryData(BD_MFC, z, rectM) : null;
			bdatL = (rectL != null) ? new BoundaryData(BD_LFC, z, rectL) : null;
			BoundaryData bdatT = (rectT != null) ? new BoundaryData(BD_TIB, z, rectT) : null;
			
			if (bdatM != null) bdlist.add(bdatM);
			if (bdatL != null) bdlist.add(bdatL);
			if (bdatT != null) bdlist.add(bdatT);		
			
			if (bdatNotchEnd != null && bdatM == null && bdatL == null)
				z = imp.getNSlices();
			
			ap.rt.reset();
		} // end for(z) loop
		
		if (bdatNotchEnd == null || findDistal(BD_MFC) == null || findDistal(BD_LFC) == null || findDistal(BD_TIB) == null)
			return -1;
		return 0;
	}
	
	public void scanProxTibia() {
		Calibration cal = imp.getCalibration();
		
		BoundaryData bdatT = findProximal(BD_TIB);
		int z0 = bdatT.z;
		for (int z = z0; z > 0; z--) {
			ImageProcessor ip = imp.getImageStack().getProcessor(z + 1);
			ip.setColor(0);
			int nResults = ap.newAnalyze(imp, ip);
			
			Rect r = null;
			for (int i = 0; i < nResults; i++) {
				double cxy[] = ap.getMultiColumnValues(4, 5, i);
				if (bdatT.isInside(cxy)) {
					r = new Rect(ap.getMultiColumnValues(0, 3, i));
					i = nResults; 
				}
			}
			
			if (r == null) {
				BoundaryData bdatM = find(BD_MFC, z), bdatL = find(BD_LFC, z);
				if (bdatM != null)	bdatM.fill(imp); 
				if (bdatL != null) bdatL.fill(imp);
				r = reduceBoundaryBox2((ByteProcessor)ip, cal, bdatT);
			}
			
			if (r != null) {
				bdatT = new BoundaryData(BD_TIB, z, r);
				bdlist.add(bdatT);
			} else
				z = 0;
		}
	}
	
	public void scanDistalTibia(double fibx1, double fibx2, double fiby1, double fiby2) {
		BoundaryData bdatM = findProximal(BoundaryData.MFC);
		BoundaryData bdatL = findProximal(BoundaryData.LFC);
		double FCw = (bdatL.x + bdatL.w - bdatM.x);
		double FCh = Math.max(bdatM.y + bdatM.h, bdatL.y + bdatL.h) - Math.min(bdatM.y,  bdatL.y);
		double areaLFMC = FCw * FCh;
		//double areaLFMC = bdatM.w * bdatM.h + bdatL.w * bdatL.h;
		
		BoundaryData bdatT = findDistal(BoundaryData.TIB);
		int z0 = bdatT.z + 1;
		
		AnalyzeParticle apT = new AnalyzeParticle(condyleSize / 10, imp.getCalibration(), "BX BY Width Height X Y Area");
				
		for (int z = z0; z < imp.getNSlices(); z++) {
			int nResults = apT.newAnalyze(imp, z + 1);
			Rect rectT = null;
			if (nResults == 1) {
				rectT = new Rect(apT.getMultiColumnValues(0, 3, 0));
			} else if (nResults > 1) {
					double areas[] = apT.getAllRowValues(6);
					int i_maxarea = IJX.Util.getMaxIndex(areas);
					rectT = new Rect(apT.getMultiColumnValues(0, 3, i_maxarea));
					
					if (nResults == 2) { // TODO: scan more than two particles.
						int iFib = 1 - i_maxarea;
						double areaTib = rectT.w * rectT.h;
						Rect rectF = new Rect(apT.getMultiColumnValues(0, 3, iFib));
						
						if (rectT.x + rectT.w * fibx1 < rectF.x && rectF.x < rectT.x + rectT.w  * fibx2 &&
							rectT.y + rectT.h * fiby1 < rectF.y && rectF.y < rectT.y + rectT.h * fiby2 &&
							0.7 < areaTib / areaLFMC && areaTib / areaLFMC < 1.3) {
							rectT = null;
							bdlist.add(new BoundaryData(BoundaryData.FIB, z, rectF));
						}
					}
			}
			
			if (rectT != null)
				bdlist.add(new BoundaryData(BoundaryData.TIB, z, rectT));
			else
				z = imp.getNSlices();
		}
	}
	
	public void scanProxNotch(double notchSize, double proxNotchAngle) {
		final Calibration cal = imp.getCalibration();
		ImagePlus imp2 = imp.duplicate();
		int H0 = imp2.getHeight();
		imp2.show();
		
		int notchop = ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
		AnalyzeParticle apNotch = new AnalyzeParticle(notchop, AnalyzeParticle.defaultMeasurements, 
				notchSize, cal, "BX BY Width Height X Y");
		BoundaryData bdatF = findProximal(BD_FEM);
		BoundaryData bdatM = findProximal(BD_MFC);
		BoundaryData bdatL = findProximal(BD_LFC);
		BoundaryData bdatNE = findProximal(BD_NOTCHEND);
		BoundaryData bdatNotch = bdatNE;
		
		int splitX = (int)((bdatL.x + bdatM.x + bdatM.w) / 2 / cal.pixelWidth);
		int z0 = bdatNE.z; boolean cont = true;
		for (int z = z0; z > 0 && cont; z--) {
			int pxdata[] = new int[H0];
			
			imp2.setSliceWithoutUpdate(z + 1);
			ByteProcessor ip = (ByteProcessor)imp2.getImageStack().getProcessor(z + 1);
			ip.getColumn(splitX, 0, pxdata, H0);
			
			ip.setColor(0); ip.setLineWidth(1); 
			ip.drawLine(splitX, 0, splitX, H0);
			int nResults = ap.newAnalyze(imp2, ip, 255, 255);
			
			ip.putColumn(splitX, 0, pxdata, H0);
					
			Rect rectM = null, rectL = null, rectN = null;
			for (int i = 0; i < nResults; i++) {
				if (bdatM.isInside(ap.getMultiColumnValues(4, 5, i)))
					rectM = new Rect(ap.getMultiColumnValues(0, 3, i));
				else if (bdatL.isInside(ap.getMultiColumnValues(4, 5, i)))
					rectL = new Rect(ap.getMultiColumnValues(0, 3, i));
			}
				
			if (rectM != null && rectL != null) {
				if (rectL.h / bdatL.h >= 1.15)
					rectL = bdatL;
				
				Rect rectMpx = rectM.clonePixelized(cal), rectLpx = rectL.clonePixelized(cal);
				
				int[] lineM = IJX.getRectLine(ip, rectMpx, 2), lineL = IJX.getRectLine(ip, rectLpx, 2);
				int[] x = new int[3], y = new int[3];
				x[0] = IJX.Util.getLastIndexOf(lineM, 255) + (int)rectMpx.x;
				x[2] = IJX.Util.getIndexOf(lineL, 255) + (int)rectLpx.x;
				y[0] = (int)(rectMpx.y + rectMpx.h - 1);
				y[2] = (int)(rectLpx.y + rectLpx.h - 1);
				int outerx0 = IJX.Util.getIndexOf(lineM, 255) + (int)rectMpx.x;
				int outerx2 = IJX.Util.getLastIndexOf(lineL, 255) + (int)rectLpx.x;
				int lfcx = (int)(rectLpx.x + rectLpx.w / 2);
				int lfcy = (int)(rectLpx.y + rectLpx.h / 2);
				
				ip.setColor(255); ip.setLineWidth(3); 
				ip.drawLine(x[0], y[0], x[2], y[2]); 
				ip.drawLine(outerx0, y[0], outerx2, y[2]);
				if (Math.abs(y[0] - y[2]) / rectMpx.h > 0.05)
					ip.drawLine(x[2], y[2], lfcx, lfcy);
				
				nResults = apNotch.newAnalyze(imp2, ip, 0, 0);
				if (nResults == 1 && bdatNotch.isInside(apNotch.getMultiColumnValues(4, 5, 0))) {
					rectN = new Rect(apNotch.getMultiColumnValues(0, 3, 0));
					Rect rectNpx = rectN.clonePixelized(cal);
						
					int lineN[] = IJX.getRectLine(ip, rectNpx, 0);
					x[1] = (IJX.Util.getIndexOf(lineN, 0) + IJX.Util.getLastIndexOf(lineN, 0)) / 2 + (int)rectNpx.x;
					y[1] = (int)rectNpx.y;
						
					PolygonRoi pol = new PolygonRoi(x, y, 3, Roi.ANGLE);
					double a = pol.getAngle();
						
					if (a > proxNotchAngle) { // || y[1] < maxNRy - (5 / cal.pixelHeight)) {
						cont = false; //rectN = null;
					} else {
						bdatNotch = new BoundaryData(BD_NOTCH, z, rectN);
						bdlist.add(bdatNotch);
							
						BoundaryData bdatNR = new BoundaryData(BD_NOTCHROOF, z, x[1], y[1], 0, 0);
						bdlist.add(bdatNR);
						//maxNRy = (y[1] > maxNRy) ? y[1] : maxNRy;
					}
				} else {
					// when the femur is too low-dense, notch is not detected in a single particle; such slice is ignored.
				}
			}
			
			// if (rectN == null) z = 0;
			if (rectN != null) {
				bdatM = (rectM != null) ? new BoundaryData(BD_MFC, z, rectM) : bdatM;
				bdatL = (rectL != null) ? new BoundaryData(BD_LFC, z, rectL) : bdatL;
			}
		}
		
		imp2.close();
	}
	
	
	public void draw(ImagePlus imp) {
		imp.getProcessor().setColor(128);
		
		for (BoundaryData bd: bdlist)
			bd.draw(imp);
		
	}
	public void draw() {
		draw(this.imp);
	}
	
	public void draw1(ImagePlus imp, int type) {
		imp.getProcessor().setColor(128);
		
		for (BoundaryData bd: bdlist)
			if (bd.type == type) 
				bd.draw(imp);
	}
	public void draw1(int type) {
		draw1(this.imp, type);
	}
	
	public void drawMulti(ImagePlus imp, boolean excludeSelected, int... types) {
		int flag = 0;
		for (int type: types)
			flag |= 1 << type;
		
		for (int type: BD_TYPES) {
			if ((flag & (1 << type)) != 0) {
				if (!excludeSelected)
					draw1(imp, type);
			} else if (excludeSelected)
				draw1(imp, type);
		}
	}
	
	public void drawMulti(ImagePlus imp, int... types) {
		this.drawMulti(imp, false, types);
	}
	public void drawMultiExcept(ImagePlus imp, int... types) {
		this.drawMulti(imp, true, types);
	}

	
}