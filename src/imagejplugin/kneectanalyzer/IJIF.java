package imagejplugin.kneectanalyzer;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.awt.AWTEvent;
import java.awt.Scrollbar;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.SwingWorker;


public class IJIF implements Measurements {
	public static boolean ij3d = false;
	private static IJIF_SwingWorker callbackSwingWorker;
	
	private static mPointList fec;
	
	static BoundaryList bdatList;
	static int notchRoofX;
	static double minThreshold, maxThreshold;
	
	//private static double TPangle;
	
	private static boolean initializedFlag;
	
	private static final String FILENAME_FEC = "fec.points";
	
	private static final String MD_THRESHOLD = "Threshold";
	
	public static void initIJIF() {
		if (IJ.getInstance() == null)
			new ij.ImageJ();
		
		if (initializedFlag)
			return;
		
		bdatList = null;
		fec = null;
		notchRoofX = 0;
		//TPangle = 0;
		initializedFlag = true;
		minThreshold = maxThreshold = 0;
		
		Quadrant.init();
		
		try {
			ij3d = (Class.forName("ij3d.Image3DUniverse") != null);			
		} catch (ClassNotFoundException e) {
			ij3d = false;
		} catch (NoClassDefFoundError e) {
			ij3d = false;
		} finally {
			System.out.println("3D Viewer available? "+ij3d);
		}
		
	}
	
	/*    -------------------------------------------------------------  
	 *    ----------------------     METHODS     ---------------------- 
	 */
	
	public static boolean has3D() {
		return ij3d;
	}
	
	public static boolean hasBoundaryData() {
		return (IJIF.bdatList != null);
	}
	
	public static boolean checkModels(String... models) {
		boolean ret = true;
		
		for (String m: models) {
			if (WindowManager.getImage(m) == null)
				ret = false;
		}
		
		return ret;
	}
	
	public static boolean isStack() {
		// check and return how many stack image open
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			if (imp.getNSlices() > 1)
				return true;
		}
		return false;
	}
	
	public static boolean isBin() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			//System.out.println(imp.getType());
			if (imp.getType() == ImagePlus.GRAY8)
				return true;
		}
		return false;
	}
	
	public static boolean isCalibrated() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			Calibration cal = imp.getCalibration();
			if (cal != null && cal.getUnits().equals("mm"))
				return true;
		}
		
		return false;
	}
	
	
	public static void setCallback(IJIF_SwingWorker sw) {
		callbackSwingWorker = sw; 
	}
	
	public static void notice(String str) {
		if (callbackSwingWorker != null)
			callbackSwingWorker.callback(str);
		else
			IJ.error(str);
	}
	
	public static boolean isFEC() {
		if (IJIF.fec != null && IJIF.fec.size() == 2)
			return true;
		return false;
	}
	
	public static void setFEC(double xyz[]) {
		if (fec == null)
			fec = new mPointList();
			
		fec.clear();
		fec.add("MFEC", xyz[0], xyz[1], xyz[2]);
		fec.add("LFEC", xyz[3], xyz[4], xyz[5]);
	}
	
	public static void setFEC(mPointList mpl) {
		IJIF.fec = mpl;
	}
	
	public static void saveAVI(ImagePlus imp, String path) {
		String arg = "compression=PNG frame=" + String.valueOf(imp.getNSlices());
		arg += " save=[" + path + "]";
		
		IJ.selectWindow(imp.getID());
		IJ.run("AVI... ", arg);
	}
	
	private static String openDirectoryDialog(String title, String defaultpath) {
		if (defaultpath != null)
			DirectoryChooser.setDefaultDirectory(defaultpath);
		DirectoryChooser dc = new DirectoryChooser(title);
		String dir = dc.getDirectory();
			
		return dir;
	}
	
	private static String openDirectoryDialog(String title) {
		return openDirectoryDialog(title, null);
	}
	
	public static String getBaseDirectory() {
		ImagePlus imp = WindowManager.getImage("Base");
		FileInfo finfo = (imp != null) ? imp.getOriginalFileInfo() : null;
		String basepath = (finfo != null) ? finfo.directory : null;
		basepath = (basepath == null) ? openDirectoryDialog("Specify directory to save") : basepath;
		return basepath;
	}
	
	private static Calibration parseMetadata(String filedata, Calibration cal) {
		String voxel = null;
		//String lines[] = filedata.split(System.getProperty("line.separator"));
		String lines[] = filedata.split("\n");
		
		for (int i = 0; i < lines.length; i++) {
			if (i == 0) {
				voxel = lines[0];
				lines[0] = null;
			} else {
				if (lines[i].matches("^[a-zA-Z].*")) {
					String l[] = lines[i].split(" ");
					if (l[0].equals(MD_THRESHOLD)) {
						minThreshold = Double.parseDouble(l[1]); maxThreshold = Double.parseDouble(l[2]);
					}
					lines[i] = null;
				} else {
					IJIF.bdatList = new BoundaryList(lines);
					IJIF.notchRoofX = bdatList.getMeanNotchRoofX();
					i = lines.length;
				}
			}
		}
		
		if (voxel != null) {
			String voxeldata[] = voxel.split(" ");
			
			String unit = voxeldata[3].replaceAll("[\r\n]", "");
						
			cal.setUnit(unit);
			cal.pixelWidth = Double.parseDouble(voxeldata[0]);
			cal.pixelHeight = Double.parseDouble(voxeldata[1]);
			cal.pixelDepth = Double.parseDouble(voxeldata[2]);
		}
		
		return cal;
	}
	
	public static int getFirstSlice(ImagePlus imp) {
		for (int s = 0; s < imp.getNSlices(); s++) {
			byte[] array = (byte[])imp.getImageStack().getProcessor(s + 1).getPixels();
			if (IJX.Util.getMaxAbs(array) != 0)
				return s;
		}
		return -1;
	}
	
	private static Rect[] findMaxParticlePerSlice(ImagePlus imp) {
		AnalyzeParticle ap = new AnalyzeParticle(300, imp.getCalibration(), "Area BX BY Width Height");
		
		int nSlices = imp.getNSlices();
		Rect rect[] = new Rect[nSlices];
		
		for (int s = 0; s < nSlices; s++) {
			int nResults = ap.newAnalyze(imp, s + 1);
					
			if (nResults > 0) {
				double area[] = ap.getAllRowValues(0);
				int row_maxarea = IJX.Util.getMaxIndex(area);
				rect[s] = new Rect(ap.getMultiColumnValues(1, 4, row_maxarea));
			}
		}
		
		return rect;
	}
	
	private static int[] getMaxParticleSlices(double b_area[]) {
		int slices[] = new int[20];
		double[] b_area_sorted = b_area.clone();
		double[] b_area_unique = new double[b_area.length];
		
		Arrays.sort(b_area_sorted);
		
		double lastvalue = 0;
		for (int i = b_area.length - 1, j = 0; i >= 0; i--)
			if (b_area_sorted[i] != lastvalue)
				b_area_unique[j++] = lastvalue = b_area_sorted[i];
		
		for (int s = 0, u = 0; s < 20 && u < b_area.length; u++) {
			for (int i = 0; i < b_area.length; i++) {
				if (b_area[i] == b_area_unique[u]) {
					slices[s++] = i;
					for (int j = Math.max(i - 5, 0); j <= Math.min(i + 5, b_area.length - 1); j++)
						b_area[j] = 0;
					i = b_area.length;
				}
			}
		}
		
		return slices;
	}
	
	private static int[] getMaximaParticleSlices(double b_area[]) {
		int[] slices = null;
		
		try {
			if (Class.forName("ij.plugin.filter.MaximumFinder") != null)
				if (MaximumFinder.class.getMethod("findMaxima") != null)
				slices = MaximumFinder.findMaxima(b_area, 1.0, 1);
		} catch (ClassNotFoundException e) {
			slices = getMaxParticleSlices(b_area);
		} catch (NoSuchMethodException e) {
			slices = getMaxParticleSlices(b_area);
		}
		
		return slices;
	}
	
	private static int determineFemEpiCondylesBy2D(ImagePlus imp) {
		Rect rect[] = findMaxParticlePerSlice(imp);
		double b_area[] = new double[imp.getNSlices()];
		
		for (int i = 0; i < b_area.length; i++)
			b_area[i] = (rect[i] != null) ? rect[i].w * rect[i].h : 0;
		
		
		int[] slices = getMaximaParticleSlices(b_area);
		
		SliceChooseFilter scf = new SliceChooseFilter(slices);
		imp.getWindow().toFront();
		WindowManager.setCurrentWindow(imp.getWindow());
		new PlugInFilterRunner(scf, null, null);
		
		if (!scf.wasOK)
			return -1;
		
		int s = slices[scf.getChoice()];
		
		Calibration cal = imp.getCalibration();
		double xyz[] = new double[6];
		xyz[0] = rect[s].x; 
		xyz[1] = rect[s].y + rect[s].h / 2; 
		xyz[2] = s * cal.pixelDepth;
		xyz[3] = xyz[0] + rect[s].w; 
		xyz[4] =xyz[1]; 
		xyz[5] = xyz[2];
		
		setFEC(xyz);
		
		return 0;
	}
	
	private static ImagePlus createTibOnly(ImagePlus imp, BoundaryList bl) {
		ImagePlus impTib = imp.duplicate();
		IJX.rename(impTib, "TibOnly");
		Calibration cal = impTib.getCalibration();
		int W0 = imp.getWidth(), H0 = imp.getHeight();
		
		int zM = bl.findDistal(BoundaryData.MFC).z;
		int zL = bl.findDistal(BoundaryData.LFC).z;
		int zT = bl.findProximal(BoundaryData.TIB).z;
		
		for (int z = 0; z < zT; z++) {
			ByteProcessor ip = (ByteProcessor)impTib.getImageStack().getProcessor(z + 1);
			ip.setColor(0);
			ip.fillRect(0, 0, W0, H0);
		}
		
		for (int z = zT; z <= Math.max(zM, zL); z++) {
			impTib.setSliceWithoutUpdate(z + 1);
			ByteProcessor ip = (ByteProcessor)impTib.getProcessor();
			ip.setColor(0);
			
			BoundaryData bd = bl.find(BoundaryData.MFC, z);
			if (bd != null) {
				Rect r = ((Rect)bd).clonePixelized(cal);
				ip.fillRect((int)r.x,  (int)r.y,  (int)r.w,  (int)r.h);
			}
			
			bd = bl.find(BoundaryData.LFC, z);
			if (bd != null) {
				Rect r = ((Rect)bd).clonePixelized(cal);
				ip.fillRect((int)r.x,  (int)r.y,  (int)r.w,  (int)r.h);
			}
			
			bd = bl.find(BoundaryData.TIB, z);
			if (bd != null) {
				Rect r = ((Rect)bd).clonePixelized(cal);
				
				imp.setSliceWithoutUpdate(z + 1);
				imp.setRoi((int)r.x,  (int)r.y,  (int)r.w,  (int)r.h);
				imp.copy();
				imp.killRoi();
				impTib.setRoi((int)r.x,  (int)r.y,  (int)r.w,  (int)r.h);
				impTib.paste();
				impTib.killRoi();
			}
		}
		
		return impTib;
	}
	
	public static double createTibPlateau(ImagePlus impTibSag) {
		IJ.run(impTibSag, "Fill Holes", "stack");
		Calibration cal = impTibSag.getCalibration();
		ImagePlus impTibSagZ = IJX.zproject(impTibSag);
		impTibSagZ.show();
		
		SimpleRotateFilter srf = new SimpleRotateFilter(0);
		WindowManager.setCurrentWindow(impTibSagZ.getWindow());
		new PlugInFilterRunner(srf, null, null);
		
		if (!srf.wasOK)
			return 0;
	
		int w = impTibSag.getWidth(), h = impTibSag.getHeight();
			
		BoundaryData bd = bdatList.findProximal(BoundaryData.FIB);
		if (bd == null)
			bd = bdatList.findDistal(BoundaryData.TIB);
			
		XY fibYZ = new XY((bd.y + bd.h) / cal.pixelWidth, bd.z);
		XY center = new XY(w / 2, h / 2);
		XY fibYZr = IJX.Util.rotateXY(fibYZ, center, srf.angle);
		int z = (int)fibYZr.y;
			
		if (srf.angle != 0)
			IJX.rotate(impTibSag, srf.angle);
			
		ImageStack ims = impTibSag.getImageStack().crop(0, 0, 0, w, z, impTibSag.getNSlices());
		impTibSag.setStack(ims);
		
		//ImagePlus impTP = OrthogonalTransformer.reslice(impTibSag, cal.pixelDepth, "Top rotate");
		ImagePlus impTP = IJX.createSag2Ax(impTibSag);
		IJ.setThreshold(impTP, 64, 255, null); // TODO: threshold by user-defined ??
		IJ.run(impTP, "Make Binary", "method=Default background=Default");
		IJX.rename(impTP, "TibPlateau");
		
		IJX.forceClose(impTibSagZ);
			
		return srf.angle;
	}
	
	private static ImagePlus createFemOnly(ImagePlus imp, ImagePlus impTib) {
		ImageCalculator ic = new ImageCalculator();
		
		ImagePlus impFem = ic.run("XOR create stack", imp, impTib);
		IJX.rename(impFem, "FemOnly");
		return impFem;
	}
	
	private static boolean[] generalFTDialog(String title, String msg, String suffix, boolean f, boolean t, boolean[] defaultvalue) {
		if (f == false && t == false)
			return null;
		
		GenericDialog gd = new GenericDialog(title);
		gd.addMessage(msg);
		if (f) 	gd.addCheckbox("Femoral "+suffix, defaultvalue[0]);
		if (t)  gd.addCheckbox("Tibial "+suffix, defaultvalue[1]);
		gd.showDialog();
		
		if (gd.wasCanceled()) 
			return null;
		
		boolean ret[] = new boolean[2];
		
		ret[0] = (f) ? gd.getNextBoolean() : false; 
		ret[1] = (t) ? gd.getNextBoolean() : false;
		
		return ret;
	}
	private static boolean[] generalFTDialog(String title, String msg, String suffix, boolean ft[], boolean[] defaultvalue) {
		return generalFTDialog(title, msg, suffix, ft[0], ft[1], defaultvalue);
	}
	
	public static int radiobuttonFTDialog(String title, String msg, String suffix) {
		GenericDialog gd = new GenericDialog(title);
		gd.addMessage(msg);
		String items[] = new String[2];
		items[0] = "Femoral "+ (suffix != null ? suffix : "");
		items[1] = "Tibial "+ (suffix != null ? suffix : "");
		gd.addRadioButtonGroup(null, items, 2, 1, items[0]);
		gd.showDialog();
		
		if (gd.wasCanceled()) 
			return 0;
		
		String r = gd.getNextRadioButton();
		if (r.equals(items[0])) return 1;
		if (r.equals(items[1])) return 2;
		
		return 0;
	}
	
	
	
	/*
	 * Interface for dialog buttons 
	 * 
	 */
	
	
	public static int openKCADirectory(String... models) {
		String dir = openDirectoryDialog("Select directory");
				
		if (dir == null)
			return -1;
		
		if (IJX.Util.doesFileExist(dir, "Base.avi") && 
			(IJX.Util.doesFileExist(dir, "Voxel.txt") || IJX.Util.doesFileExist(dir, "metadata.txt"))) {
			int ret = 0;
			
			notice("Opening Base...");
			IJ.open(IJX.Util.createPath(dir, "Base.avi"));
			ImagePlus imp = WindowManager.getCurrentImage();
			IJX.rename(imp, "Base");
			ret++;
			
			notice("Loading metadata...");
			String filedata;
			if (IJX.Util.doesFileExist(dir, "metadata.txt")) 
				filedata = IJ.openAsString(IJX.Util.createPath(dir, "metadata.txt"));
			 else 
				filedata = IJ.openAsString(IJX.Util.createPath(dir, "Voxel.txt"));
				
			Calibration cal = parseMetadata(filedata, imp.getCalibration());
			imp.setCalibration(cal);
			
			IJIF.fec = null;
			IJIF.fec = IJX.loadPointList(dir, IJIF.FILENAME_FEC);
			
			Quadrant.init();
			Quadrant.load(dir);
				
			for (String m: models) {
				if (IJX.Util.doesFileExist(dir, m + ".avi")) {
					notice("Opening " + m + "...");
					IJ.open(IJX.Util.createPath(dir, m + ".avi"));
					imp = WindowManager.getCurrentImage();
					IJX.rename(imp, m);
					imp.setCalibration(cal);
					ret++;
				}
			}
			
			return ret;
		} else {
			IJ.run("Image Sequence...", "open="+dir);				
			return 0;
		}
	}
	
	public static int closeWorkingFiles(String... wintitles) {
		IJIF.fec = null;
		IJIF.bdatList = null;
		IJIF.notchRoofX = 0;
		
		for (String win: wintitles) 
			IJX.forceClose(win);
		
		String workwins[] = IJX.getWindowTitles("KCAwork-.*");
		if (workwins != null)
			for (String win: workwins)
				IJX.forceClose(win);
		
		workwins = IJX.getWindowTitles("3D-.*");
		if (workwins != null)
			for (String win: workwins)
				IJX.forceClose(win);
		
		if (IJ.isResultsWindow())
			ResultsTable.getResultsWindow().close(false);
		
		java.awt.Window win = WindowManager.getWindow("Results3D");
		if (win != null && win instanceof TextWindow)
			((TextWindow)win).close(false);
		
		if (ij3d)
			IJIF3D.close();
		
		Quadrant.init();
		
		return 0;
	}
	
	public static String getCurrentWindowTitle() {
		ImagePlus imp = WindowManager.getCurrentImage();
		return (imp != null) ? imp.getTitle() : null;
	}
	
	static class Modeler {
		public static int save(String... modellist) {
			String basepath = getBaseDirectory();
			if (basepath == null)
				return -1;
			
			for (String m: modellist) {
				if (m != null) {
					ImagePlus imp = WindowManager.getImage(m);
				
					if (m.equals("Base")) {
						if (imp.changes) {
							notice("Saving Base...");
							saveAVI(imp, IJX.Util.createPath(basepath, "Base.avi"));
							imp.changes = false;
							FileInfo finfo = imp.getOriginalFileInfo();
							if (finfo == null) finfo = new FileInfo();
							finfo.directory = basepath;
							imp.setFileInfo(finfo);
						}
					
						notice("Saving metadata...");
						
						String LF = System.getProperty("line.separator");
						String outStr = IJX.getVoxelSizeAsString(imp) + LF;
						outStr += IJX.Util.metadata(MD_THRESHOLD, minThreshold, maxThreshold) + LF;
						
						if (IJIF.bdatList != null)
							outStr += bdatList.toString(); 
									
						IJ.saveString(outStr, IJX.Util.createPath(basepath, "metadata.txt"));
					
						if (IJIF.fec != null && IJIF.fec.size() >= 2)
							IJX.savePointList(IJIF.fec, basepath, IJIF.FILENAME_FEC);
					} else if (imp != null) {
						notice("Saving " + m + "...");
						saveAVI(imp, IJX.Util.createPath(basepath, m + ".avi"));
						imp.changes = false;
					}
				}
			}
			
			return 0;
		}
		
		public static int saveOpened() {
			int IDs[] = WindowManager.getIDList();
			String wins[] = WindowManager.getImageTitles();
			
			for (int id: IDs) {
				String winname = WindowManager.getImage(id).getTitle();
				
				int cnt = 0;
				for (int i = 0; i < wins.length; i++)
					if (wins[i].equals(winname))
						cnt++;
				
				if (cnt > 1)
					WindowManager.getImage(id).setTitle(WindowManager.makeUniqueName(winname));	
			}
			
			wins = WindowManager.getImageTitles();
			boolean changed[] = new boolean[wins.length];
			for(int i = 0; i < wins.length; i++) 
				changed[i] = WindowManager.getImage(wins[i]).changes;
			
			GenericDialog gd = new GenericDialog("Select windows");
			gd.addMessage("Select windows that you want to save as a model.\nThose with any change are checked.");
			for(int i = 0; i < wins.length; i++) 
				gd.addCheckbox(wins[i], changed[i]);
			gd.showDialog();
			
			for (int i = 0; i < wins.length; i++)
				if (!gd.getNextBoolean())
					wins[i] = null;
				else
					WindowManager.getImage(wins[i]).changes = true;
			
			
			return save(wins);
		}
		
		public static String binarize() {
			ImagePlus imp = WindowManager.getCurrentImage();
			imp.killRoi();
			
			IJ.run(imp, "Threshold...", "");
			IJ.setThreshold(imp, 150, 1500);
			
			int r = IJX.WaitForUser("Image is thresholded for bone density.\n"+
				"Please review and adjust the threshold if necessary;\nthen press OK of this dialog.");
			if (r == -1) return null;
			
			Calibration cal = imp.getCalibration();
			minThreshold = cal.getCValue(imp.getProcessor().getMinThreshold());
			maxThreshold = cal.getCValue(imp.getProcessor().getMaxThreshold());
			
			IJ.run(imp, "Make Binary", "method=Default background=Default");
			
			int option = AnalyzeParticle.defaultOption | ParticleAnalyzer.SHOW_MASKS;
			AnalyzeParticle ap = new AnalyzeParticle(option, AnalyzeParticle.defaultMeasurements,
					0, 4, imp.getCalibration(), null);
			ap.analyzer.setHideOutputImage(true);
			
			for (int z = 0; z < imp.getNSlices(); z++) {
				imp.setSliceWithoutUpdate(z+1);
				int nResults = ap.newAnalyze(imp, imp.getProcessor());
				ImagePlus impMsk = ap.analyzer.getOutputImage();
				if (nResults > 0) {
					ImageCalculator ic = new ImageCalculator();
					ic.run("XOR", imp, impMsk);
					//imp3.show();
				}
				IJX.forceClose(impMsk);
				
				
			}
			//IJ.run(imp, "Fill Holes", "stack");
			
			return imp.getTitle();
		}
		
		public static int align(String wintitle) {
			if (ij3d) {
				IJIF3D.Modeler.updateFEC(wintitle);
			}
			
			OrthogonalTransformer ot = new OrthogonalTransformer();
			int r = ot.run(wintitle, IJIF.fec);
			
			if (r == 0) {
				ImagePlus imp = ot.getTransformedImage();
				IJ.setThreshold(imp, 64, 255, null); // TODO: threshold by user-defined ??
				IJ.run(imp, "Make Binary", "method=Default background=Default");
				//IJ.run(imp, "Fill Holes", "stack");
				IJX.rename(imp, "Base");
				
				mPointList pl = ot.getTransformedPointList(); 
				if (pl != null && pl.size() >= 2) {
					fec = pl;
					
					if (ij3d)
						IJIF3D.Modeler.showModelAndPoints(imp, pl);
				} else {
					r = determineFemEpiCondylesBy2D(imp);
				}
			}
			
			return r;
		}
		
		public static int autoEdit() {
			notice("Analyzing...");
			
			ImagePlus imp = WindowManager.getImage("Base");
			if (fec == null) {
				int r = determineFemEpiCondylesBy2D(imp);
				if (r == -1) return -1;
			}
					
			BoundaryTool bt = new BoundaryTool(imp);
			bt.scanFemur(fec.toArray());
			bt.scanProxTibia();
			bt.scanDistalTibia();
			bt.scanProxNotch();
			
			int nrx = bt.getMeanNotchRoofX();
			int z2 = bt.findDistal(BoundaryData.NOTCH).z;
			
			ImagePlus imp2 = imp.duplicate(); imp2.show();
			
			for (int z = 0; z <= z2; z++) {
				ByteProcessor ip = (ByteProcessor)imp2.getImageStack().getProcessor(z + 1);
				
				ip.setColor(128);
				ip.drawLine(nrx, 0, nrx, imp2.getHeight() - 1);
			}
			
			bt.drawMulti(imp2, BoundaryData.MFC, BoundaryData.LFC, BoundaryData.TIB, BoundaryData.NOTCH);
			
			notice(null);
			notice("Review the created image; then click OK or press ESC key.");
			
			String msg = "To create isolated bone models (FemOnly and TibOnly),\n";
			msg += "MFC, LFC,Tibial spine and plateau, and splitting line between MFC and LFC are\n";
			msg += "machinary identified at the notch-level slices.\n";
			msg += "Please review the image; if you agree, press OK. If not, press ESC key.";
			
			int r = IJX.WaitForUser(msg);
			
			bt.close();
			
			if (r == -1) return -1;
			
			IJX.forceClose(imp2);
			
			notice(null);
			notice("Creating isolated bone models...\nThis may take some time...");
			
			ImagePlus impTib = createTibOnly(imp, bt); impTib.show();
			ImagePlus impFem = createFemOnly(imp, impTib); impFem.show();  
			
			IJIF.bdatList = bt;
			IJIF.notchRoofX = nrx;
			
			return 0;
		}
	}
	
	/*
	 * 
	 *            for Quadrant Analysis
	 * 
	 */
	
	static class Quad {
		public static int saveData(int mode2D3D) {
			String basepath = getBaseDirectory();
			if (basepath == null)
				return -1;
			
			Quadrant.save(basepath);
			
			if (ij3d)
				IJIF3D.Quad.save(basepath);
			
			return 0;
		}
	
		public static int detectSystem2D() {
			boolean pre[] = new boolean[] { (Quadrant.systemCoordFem == null), (Quadrant.systemCoordTib == null) };
			boolean boo[] = generalFTDialog("Choice", "Check below for automatic identification of quadrant coord system.", 
											"Quadrant System", true, true, pre);
			if (boo == null)
				return -1;
			
			boolean fem = boo[0], tib = boo[1];
			
			ImagePlus impF = null, impT = null;
			if (fem) impF = Quadrant.detectFemoralSystem(notchRoofX);
			if (tib) impT = Quadrant.detectTibialSystem();
				
			if (impF != null || impT != null) {
				ImagePlus impF2 = (impF != null) ? impF.flatten() : null;
				ImagePlus impT2 = (impT != null) ? impT.flatten() : null;
				if (impF2 != null) impF2.show(); if (impT2 != null) impT2.show();
				
				ImagePlus impC = IJX.createCombinedImage("Quadrant System", impF2, impT2);
				impC.show(); 
				WindowManager.toFront(WindowManager.getFrame("Quadrant System"));
				WindowManager.setCurrentWindow(impC.getWindow());
				IJX.forceClose(impF2, impT2);
				
				String msg = "";
				if (fem && impF == null)
					msg += "Failed in auto-identifying femoral quadrant coord system.\n";
				if (tib && impT == null)
					msg += "Failed in auto-identifying tibial quadrant coord system.\n";
				msg += "Review the auto-identified quadrant coord system,\n";
				msg += "and check below when you agree.";
				
				boolean boo2[] = generalFTDialog("Choice", msg, "Quadrant System",
												(impF != null), (impT != null), new boolean[] {true,true});
				if (boo2 == null) return -1;
				
				if (fem && !boo2[0])
					Quadrant.systemCoordFem = null;
								
				if (tib && !boo2[1])
					Quadrant.systemCoordTib = null;
				
				IJX.forceClose(impC);
				
				if (boo2[0] || boo2[1])
					return 0;
			}
			return -1;
		}
		
		public static int detectTunnel2D() {
			boolean pre[] = new boolean[] { (Quadrant.systemCoordFem != null), (Quadrant.systemCoordTib != null) };
			boolean boo[] = generalFTDialog("Choice", "Check below for automatic tunnel detection.", 
											"Tunnel", pre, pre);
			if (boo == null)
				return -1;
			
			boolean fem = boo[0], tib = boo[1];
			
			IJ.run("Set Measurements...", "area centroid display redirect=None decimal=3");
			ResultsTable rtable = ResultsTable.getResultsTable();
			if (rtable != null) rtable.reset();
			
			int rf = 0, rt = 0;
			if (fem) rf = Quadrant.detectFemoralTunnel(notchRoofX);
			if (tib) rt = Quadrant.detectTibialTunnel();
			
			Quadrant.measurements2Coord();

			if (rf == 0 && rt == 0)
				return 0;
			return -1;
		}
		
		public static int refreshResults2D() {
			Quadrant.measurements2Coord();
			Quadrant.syncWithResults();
			return 0;
		}
		
		public static int determineSystem2D() {
			Quadrant.setSystem(notchRoofX);
			return 0;
		}
	}
}

abstract class IJIF_SwingWorker extends SwingWorker<Integer, String> {
	abstract public Integer doInBackground();
	abstract public void callback(String str);
}


class SliceChooseFilter implements ExtendedPlugInFilter, DialogListener {
	private int index;
	private final int[] sliceChoices;
	private ImagePlus imp;
	public boolean wasOK;
	
	public SliceChooseFilter(int[] sliceChoices) {
		this.sliceChoices = sliceChoices;
	}
	public int getChoice() {
		return index;
	}
	 
	@Override
	public int setup(String arg, ImagePlus imp) {
		this.index = 0;
		this.imp = imp;
		this.wasOK = false;
		
		return DOES_ALL | STACK_REQUIRED | NO_CHANGES;
	}

	@Override
	public void run(ImageProcessor ip) {
		imp.setSlice(sliceChoices[index] + 1);		
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		//index = gd.getNextChoiceIndex();
		Vector<Scrollbar> slider = gd.getSliders();
		index = slider.get(0).getValue() - 1;
		
		return true;
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		String items[] = new String[sliceChoices.length];
		for (int i = 0; i < items.length; i++)
			items[i] = Integer.toString(sliceChoices[i] + 1);
		
		GenericDialog gd = new GenericDialog("Select Epicondyle Slice");
		gd.addMessage("XYZ of femoral epicondyles are mandatory\n"+
					"for Auto-editting, but yet to be determined.\n"+
					 "In fact, Near-by points are satisfactory.\n" +
					 "Please choose one epicondyle-containing slice\n"+
					 "from machinary-identified candidates.\n");
		
		//gd.addChoice("Slice#: ", items, items[0]);
		gd.addSlider("Slice choice: ", 1, items.length, 1);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.getPreviewCheckbox().setState(true);
		gd.showDialog();
		
		if (gd.wasOKed())
			wasOK = true;
		
		return 0;
	}

	@Override public void setNPasses(int nPasses) {}
}

class SimpleRotateFilter implements ExtendedPlugInFilter, DialogListener {
	public boolean wasOK;
	public double angle = 0;
	
	public SimpleRotateFilter(double initialAngle) {
		angle = initialAngle;
	}
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		return DOES_ALL | SNAPSHOT;
	}

	@Override
	public void run(ImageProcessor ip) {
		ip.rotate(angle);
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		angle = gd.getNextNumber();
		return true;
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		GenericDialog gd = new GenericDialog("Rotate sagitally TibOnly");
		gd.addMessage("Rotate the sagittal projection of TibOnly\nto obtain horizontal tibial plateau.");
		gd.addNumericField("Angle: ", this.angle, 3);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.getPreviewCheckbox().setState(true);
		gd.showDialog();
		
		wasOK = gd.wasOKed();
		
		return 0;
	}

	@Override public void setNPasses(int nPasses) {}
}

