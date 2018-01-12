package imagejplugin.kneectanalyzer;

import java.io.File;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.WaitForUserDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.ZProjector;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

class IJX {
	public static ImagePlus zproject(ImagePlus stack, int start, int end) {
		Calibration calS = stack.getCalibration();
		
		ZProjector zp = new ZProjector(stack);
		zp.setStartSlice(start);
		zp.setStopSlice(end);
		zp.setMethod(ZProjector.AVG_METHOD);
		zp.doProjection();
		ImagePlus impZ = zp.getProjection();
		
		Calibration calZ = impZ.getCalibration();
		calZ.pixelWidth = calS.pixelWidth;
		calZ.pixelHeight = calS.pixelHeight;
		calZ.setUnit(calS.getUnit());
		impZ.setCalibration(calZ);
		
		return impZ;
	}
	
	public static ImagePlus zproject(ImagePlus stack) {
		return zproject(stack, 1, stack.getStackSize());
	}
	
	public static ImagePlus reslice(ImagePlus imp, double output, String start) {
		String arg = "output="+output + " start="+start + " avoid";
		
		WindowManager.setCurrentWindow(imp.getWindow());
		
		IJ.run("Reslice [/]...", arg);
		
		return WindowManager.getCurrentImage();
	}
	
	public static void rotate(ImagePlus imp, double angle) {
		String arg = "angle="+IJ.d2s(angle);
		arg += " interpolation=Bilinear stack";
		IJ.run(imp, "Rotate... ", arg);
	}
	
	public static void forceClose(ImagePlus... imps) {
		for (ImagePlus imp: imps) {
			if (imp != null) {
				imp.changes = false;
				imp.close();
			}
		}
	}
	
	public static void rename(ImagePlus imp, String name) {
		int IDs[] = WindowManager.getIDList();
		
		for (int id: IDs) 
			if (name.equals(WindowManager.getImage(id).getTitle()))
				WindowManager.getImage(id).setTitle(WindowManager.getUniqueName(name));
					
		imp.setTitle(name);
	}
	
	public static int WaitForUser(String msg) {
		WaitForUserDialog wd = new WaitForUserDialog(msg);
		wd.show();
		
		if (wd.escPressed())
			return -1;
		return 0;
	}
	
	public static int error(String msg, int r) {
		String fn = "";
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		if (ste.length > 3)
			fn = ste[3].getMethodName();
		
		IJ.error("At " + fn + "(): " + msg);
		return r;	
	}
	
	public int error(String msg) {
		return error(msg, -1);
	}
	
	public static void runCommand(String ijcommand, String arg) {
		IJ.run(ijcommand, arg);
	}
	public static void runCommand(String ijcommand) {
		IJ.run(ijcommand);
	}
	

	public static String getVoxelSizeAsString(ImagePlus imp) {
		Calibration cal = imp.getCalibration();
		String str = Double.toString(cal.pixelWidth) + " ";
		str += Double.toString(cal.pixelHeight) + " ";
		str += Double.toString(cal.pixelDepth) + " ";
		str += cal.getUnits();
		
		return str;
	}
	
	public static String getVoxelSizeAsString() {
		ImagePlus imp = WindowManager.getCurrentImage();
		return getVoxelSizeAsString(imp);
	}
	
	public static Calibration getCalibration(String wintitle) {
		ImagePlus imp = WindowManager.getImage(wintitle);
		if (imp != null)
			return imp.getCalibration();
		return null;
	}
	public static Calibration getBaseCalibration() {
		String lists[] = new String[] { "Base", "FemOnly", "TibOnly" };
		
		for (String w: lists) {
			Calibration cal = getCalibration(w);
			if (cal != null) return cal;
		}
		return null;
	}
	
	public static String[] getWindowTitles(String regex) {
		ArrayList<String> strlist = new ArrayList<String>();
		String wins[] = WindowManager.getImageTitles();
		
		for(String w: wins) {
			if (w.matches(regex))
				strlist.add(w);
		}
		
		if (strlist.isEmpty())
			return null;
		
		return (String[]) strlist.toArray(new String[0]);
	}
	
	public static ImagePlus createCombinedImage(String title, ImagePlus... imps) {
		int maxheight = 0;
		int px0 = 0;
		int px[] = new int[imps.length];
		int width[] = new int[imps.length];
		int height[] = new int[imps.length];
		int imageType = 0;
			
		for (int i = 0; i < imps.length; i++) {
			if (imps[i] != null) {
				int w = imps[i].getWidth();
				int h = imps[i].getHeight();
			
				px[i] = px0;
				width[i] = w;
				height[i] = h;
			
				px0 += w;
				maxheight = (maxheight > h) ? maxheight : h;
				
				imageType = Math.max(imps[i].getType(), imageType);
			}
		}
		
		ImageProcessor ip;
			
		switch (imageType) {
		case ImagePlus.GRAY8:
		case ImagePlus.COLOR_256:
		default:
			ip = new ByteProcessor(px0, maxheight); break;
		case ImagePlus.GRAY16:
			ip = new ShortProcessor(px0, maxheight); break;
		case ImagePlus.COLOR_RGB:
			ip = new ColorProcessor(px0, maxheight); break;
		}
		
		ImagePlus impC = new ImagePlus(title, ip);
		
		for (int i = 0; i < imps.length; i++) {
			if (imps[i] != null) {
				imps[i].killRoi();
				imps[i].getProcessor().snapshot();
				imps[i].copy();
			
				impC.setRoi(px[i], 0, width[i], height[i]);
				impC.paste();
				impC.killRoi();
			}
		}
		
		return impC;
	}
	
	public static int[] getRectLine(ImageProcessor ip, Rect px_r, int mode) {
		// mode == 0: upper horizontal line. 
		// mode == 1..3: ...clockwise
		int rx1 = (int)(px_r.x);
		int rx2 = rx1 + (int)(px_r.w - 1);
		int ry1 = (int)(px_r.y);
		int ry2 = ry1 + (int)(px_r.h - 1);
		int w = (int)(px_r.w), h = (int)(px_r.h);
		int ret[] = (mode == 0 || mode == 2) ? new int[w] : new int[h];
			
		switch (mode) {
		case 0: 
			ip.getRow(rx1, ry1, ret, w);
			return ret;
		case 1: 
			ip.getColumn(rx2, ry1, ret, h);
			return ret;
		case 2:
			ip.getRow(rx1, ry2, ret, w);
			return ret;
		case 3:
			ip.getColumn(rx1, ry1, ret, h);
			return ret;
		}
		
		return null;
	}
	
	public static int[] getRectLine(ImageProcessor ip, Rect real_r, Calibration cal, int mode) {
		Rect px_r = real_r.clone();
		px_r.real2px(cal);
		return getRectLine(ip, px_r, mode);
	}
	
	public static int[] getRect(ImageProcessor ip, Rect r) {
		int x = (int)r.x; int y = (int)r.y; int w = (int)r.w; int h = (int)r.h;
		int ret[] = new int[w * h], row[] = new int[w];
		
		for (int i = 0; i < h; i++) {
			ip.getRow(x,  y + i, row, w);
			for (int j = 0; j < w; j++)
				ret[i * w + j] = row[j];
		}
		
		return ret;
	}
	
	public static ImagePlus createLFCOnly(ImagePlus impFem, int nrx) {
		ImagePlus impLFC = impFem.duplicate();
		int H0 = impFem.getHeight();
		
		for (int z = 0; z < impLFC.getNSlices(); z++) {
			ByteProcessor ip = (ByteProcessor)impLFC.getImageStack().getProcessor(z + 1);
			ip.setColor(0);
			ip.fillRect(0, 0, nrx, H0);
		}
		
		return impLFC;
	}
	
	public static ImagePlus createLFCOnly(int nrx) {
		ImagePlus impFem = WindowManager.getImage("FemOnly");
		ImagePlus impLFC = createLFCOnly(impFem, nrx);
		IJX.rename(impLFC, "LFCOnly");
		impLFC.show();
		return impLFC;
	}
	
	public static ImagePlus createAx2Sag(ImagePlus imp) {
		imp.show();
		Calibration cal = imp.getCalibration();
		ImagePlus impSag = IJX.reslice(imp, cal.pixelDepth, "Left");
		return impSag;
	}
	
	public static ImagePlus createSag2Ax(ImagePlus imp) {
		imp.show();
		Calibration cal = imp.getCalibration();
		ImagePlus impAx = IJX.reslice(imp, cal.pixelDepth, "Top rotate");
		return impAx;
	}
	
	public static ImagePlus createLFCSagittalProjection(int nrx) {
		//int nrx = IJIF.bdatList.getMeanNotchRoofX();
		ImagePlus impLFC = WindowManager.getImage("LFCOnly");
		
		if (impLFC == null)
			impLFC = createLFCOnly(nrx); // entitled as 'LFCOnly'
		
		ImagePlus impSag = createAx2Sag(impLFC);
		
		ImagePlus impSagZ = IJX.zproject(impSag, nrx, impSag.getNSlices());
		IJX.forceClose(impSag);
		
		return impSagZ;
	}
	
	public static void savePNG(ImagePlus imp, String dir, String file) {
		if (imp.getOverlay() != null) {
			imp = imp.flatten();
			IJ.saveAs(imp, "PNG", IJX.Util.createPath(dir, file));
			IJX.forceClose(imp);
		} else {
			IJ.saveAs(imp, "PNG", IJX.Util.createPath(dir, file));
			IJX.rename(imp, file);
		}
	}
	
	public static void savePointList(mPointList pl, String dir, String filename) { 
		String path = IJX.Util.createPath(dir, filename);
		String text = pl.getPointFileText();
		IJ.saveString(text, path);
	}
	
	public static mPointList loadPointList(String dir, String file) {
		if (IJX.Util.doesFileExist(dir, file)) {
			mPointList pl = new mPointList();
			String filedata = IJ.openAsString(IJX.Util.createPath(dir, file));
			pl.parsePointsFileText(filedata);
			
			return pl;
		} else
			return null;
	}
	
	public static mPointList createPointListfromResults(String indexlabel, String... XYZ) {
		
		mPointList pl = new mPointList();
		ResultsTable rt = ResultsTable.getResultsTable();
		
		for (int i = 0; i < rt.size(); i++) {
			if (rt.getLabel(i).startsWith(indexlabel)) {
				double x = rt.getValue(XYZ[0], i);
				double y = rt.getValue(XYZ[1], i);
				double z = rt.getValue(XYZ[2], i);
				
				String name = "point" + Integer.toString(i + 1);
				pl.add(name, x,y,z);
			}
		}
		return pl;	
	}
	
	static class Util {
		static double cosA(double angle_in_degrees) {
			return Math.cos(angle_in_degrees * Math.PI / 180);
		}
		static double sinA(double angle_in_degrees) {
			return Math.sin(angle_in_degrees * Math.PI / 180);
		}
		
		static XY rotateXY(XY xy, XY center, double angle) {
			double xc = center.x;
			double yc = center.y;
			
			double cos = cosA(angle);
			double sin = sinA(angle);
			
			XY xyNew = new XY();
			 
			xyNew.x = (xy.x - xc) * cos - (xy.y - yc) * sin + xc;
			xyNew.y = (xy.y - yc) * cos + (xy.x - xc) * sin + yc;
			
			return xyNew;
		}
		
		static double[] list2array(ArrayList<Double> list) {
			if (list.size() == 0)
				return null;
			
			double ret[] = new double[list.size()];
			for (int i = 0; i < list.size(); i++)
				ret[i] = list.get(i);
			return ret;
		}
		
		static int getMinIndex(double array[]) {
			double v = Double.POSITIVE_INFINITY;
			int ret = 0;
			for (int i = 0; i < array.length; i++) {
				if (array[i] < v) {
					v = array[i]; ret = i;
				}
			}
			return ret;
		}
		
		static int getMaxIndex(double array[]) {
			double v = Double.NEGATIVE_INFINITY;
			int ret = 0;
			for (int i = 0; i < array.length; i++) {
				if (array[i] > v) {
					v = array[i]; ret = i;
				}
			}
			return ret;
		}
		
		static int getIndexOf(double array[], double v) {
			for (int i = 0; i < array.length; i++)
				if (array[i] == v)
					return i;
			return -1;
		}
		
		static int getIndexOf(int array[], int v) {
			for (int i = 0; i < array.length; i++)
				if (array[i] == v)
					return i;
			return -1;
		}
		
		static int getLastIndexOf(double array[], double v) {
			for (int i = array.length - 1; i >= 0; i--)
				if (array[i] == v)
					return i;
			return -1;
		}
		
		static int getLastIndexOf(int array[], int v) {
			for (int i = array.length - 1; i >= 0; i--)
				if (array[i] == v)
					return i;
			return -1;
		}
		
		static int getMax(int array[]) {
			int v = Integer.MIN_VALUE;
			for (int i = 0; i < array.length; i++)
				v = (array[i] > v) ? array[i] : v;
				
			return v;
		}
		
		static double getMax(double array[]) {
			double v = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < array.length; i++)
				v = (array[i] > v) ? array[i] : v;
				
			return v;
		}
		
		static byte getMaxAbs(byte array[]) {
			int v = 0;
			for (int i = 0; i < array.length; i++)
				v = (Math.abs(array[i]) > v) ? Math.abs(array[i]) : v;
				
			return (byte)v;
		}
		
		static double getMean(int array[]) {
			int sum = 0;
			for (int i = 0; i < array.length; i++)
				sum += array[i];
			return sum / array.length;
		}
		
		static double getMean(byte array[]) {
			int sum = 0;
			for (int i = 0; i < array.length; i++)
				sum += (array[i] & 0xff);
			return sum / array.length;
		}
		
		static String createPath(String dir, String filename) {
			String ret = dir;
			if (!ret.endsWith(File.separator))
				ret += File.separator;
			ret += filename;
			return ret;
		}
		
		static boolean doesFileExist(String dir, String filename) {
			File f = new File(Util.createPath(dir, filename));
			return f.exists();
		}
		
		static int string2int(String str, String delimiter, int index) {
			String l[] = str.split(delimiter);
			return Integer.parseInt(l[index]);
		}
		
		static String metadata(String str, double... values) {
			String ret = str + " ";
			for (int i = 0; i < values.length; i++)
				ret += values[i] + " ";
			
			return ret;
		}
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
	
	public XY getXY() {
		return new XY(this.x, this.y);
	}

	public XY getYZ() {
		return new XY(this.y, this.z);
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
}