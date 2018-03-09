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
import ij.process.ImageStatistics;
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
	
	static double minThreshold, maxThreshold;
	
	//private static double TPangle;
	
	private static boolean initializedFlag;
	private static String nonKCAtitle;
	
	private static final String FILENAME_FEC = "fec.points";
	private static final String FILENAME_BOUNDARY = "boundary.txt";
	
	private static final String MD_THRESHOLD = "Threshold";
	public static final String WINTITLE_BOUNDARY = AnatomyDetector.WINTITLE_BOUNDARY;
	
	private static final String BD_TYPE[] = BoundaryData.TYPESTRING;
	
	public static void initIJIF() {
		if (IJ.getInstance() == null)
			new ij.ImageJ();
		
		if (initializedFlag)
			return;
		
		fec = null;
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
		return (IJX.getResultsTable(WINTITLE_BOUNDARY) != null);
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
	
	private static Calibration parseMetadata(String filedata, Calibration cal, boolean toResults) {
		
		//String lines[] = filedata.split(System.getProperty("line.separator"));
		String lines[] = filedata.split("\n");
		
		for (int i = 0; i < lines.length; i++) {
			if (i == 0) {		
				String voxeldata[] = lines[0].split(" ");
				
				String unit = voxeldata[3].replaceAll("[\r\n]", "");
							
				cal.setUnit(unit);
				cal.pixelWidth = Double.parseDouble(voxeldata[0]);
				cal.pixelHeight = Double.parseDouble(voxeldata[1]);
				cal.pixelDepth = Double.parseDouble(voxeldata[2]);
				
				lines[0] = null;
			} else {
				if (lines[i].matches("^[a-zA-Z].*")) {
					String l[] = lines[i].split(" ");
					if (l[0].equals(MD_THRESHOLD)) {
						minThreshold = Double.parseDouble(l[1]); maxThreshold = Double.parseDouble(l[2]);
					}
					lines[i] = null;
				} else {
					if (toResults) {
						BoundaryList bl = new BoundaryList(lines);
						int nrx = bl.getMeanNotchRoofX();
						bl.clear(BoundaryData.NOTCHROOF);
						bl.real2px(cal);
						bl.toResults(WINTITLE_BOUNDARY, nrx, true);
					}	
					
					i = lines.length;
				}
			}
		}
		
		return cal;
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
		String msg = "Please choose the most probable epicondyle-containing slice.";
		
		SliceChooseFilter scf = new SliceChooseFilter(slices, "Select Epicondyle Slice", msg);			 
		
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
	
	
	
	
	
	/*
	 * Interface for dialog buttons 
	 * 
	 */
	
	
	public static int openKCADirectory(String... models) {
		nonKCAtitle = null;
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
			
			boolean rtboundary = false;
			if (IJX.Util.doesFileExist(dir, FILENAME_BOUNDARY)) {
				IJX.closeResultsTable(WINTITLE_BOUNDARY);
				
				ResultsTable rt = ResultsTable.open2(IJX.Util.createPath(dir, FILENAME_BOUNDARY));
				if (rt != null) {
					rt.show(WINTITLE_BOUNDARY);
					rtboundary = true;
				}
			}
			
			String filedata;
			if (IJX.Util.doesFileExist(dir, "metadata.txt")) 
				filedata = IJ.openAsString(IJX.Util.createPath(dir, "metadata.txt"));
			 else 
				filedata = IJ.openAsString(IJX.Util.createPath(dir, "Voxel.txt"));
				
			Calibration cal = parseMetadata(filedata, imp.getCalibration(), rtboundary == false);
			imp.setCalibration(cal);
			
			if (rtboundary) {
				if (RTBoundary.isReal()) {
					System.out.println("old version of resultstable boundary data is converted.");
					RTBoundary.real2px(cal);
				}
			}
			
			//IJIF.fec = IJX.loadPointList(dir, IJIF.FILENAME_FEC);
			
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
			IJ.wait(50);
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp != null) {
				imp.show();
				nonKCAtitle = imp.getTitle();
				return 0;
			} else
				return -1;
		}
	}
	
	public static String getOpenedTitle() {
		return nonKCAtitle;
	}
	
	public static int closeWorkingFiles(String... wintitles) {
		IJIF.fec = null;
		
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
		
		IJX.closeResultsTable("Results3D");
		IJX.closeResultsTable(WINTITLE_BOUNDARY);
		IJX.closeResultsTable(Quadrant.WINTITLE_RTQUAD);
		
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
						/*
						if (IJIF.bdatList != null)
							outStr += bdatList.toString(); 
							*/
						IJ.saveString(outStr, IJX.Util.createPath(basepath, "metadata.txt"));
						
						ResultsTable rt = IJX.getResultsTable(WINTITLE_BOUNDARY);
						if (rt != null)
							rt.save(IJX.Util.createPath(basepath, FILENAME_BOUNDARY));
					
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
		
		public static int binarize() {
			ImagePlus imp;
			if ((imp = WindowManager.getCurrentImage()) == null)
				return -1;
			
			imp = imp.duplicate();
			imp.show();
			imp.killRoi();
			
			WindowManager.setCurrentWindow(imp.getWindow());
			
			Thresholder threr = new Thresholder ("Bone Thresholding", "Image is thresholded for bone density.\n"+
					"Please review and adjust the threshold if necessary;\nthen press OK of this dialog.");	
			new PlugInFilterRunner(threr, null, null);
			
			if (!threr.wasOK) return -1;
			
			IJ.setThreshold(imp, threr.min, threr.max);
			minThreshold = threr.min;
			maxThreshold = threr.max;
			
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
			
			return 0;
		}
		
		public static int align() {
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp == null) 
				return -1;
			
			OrthogonalTransformer ot = new OrthogonalTransformer();
			int r = ot.directRun(imp, null);
			
			return r;
		}
		
		public static int detectAnatomy() {
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp == null) return -1;
			
			AnatomyDetector ad = new AnatomyDetector();
			int r = ad.directRun(imp);
			
			if (r == -1) return -1;
			
			IJX.rename(imp, "Base");
			
			return 0;
		}

		public static int autoEdit() {
			ImagePlus imp = WindowManager.getImage("Base");
			if (imp == null) return -1;
		
			notice(null);
			notice("Creating isolated bone models...\nThis may take some time...");
			
			(new FTDivider()).directRun(imp);
			
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
			
			//IJIF.Modeler.save("TunOnlyFem", "TunOnlyTib");
			
			return 0;
		}
	
		public static int detectSystem2D() {
			int quadsys = Quadrant.SysCoord.getDetermined();
			boolean pre[] = new boolean[] { ((quadsys & 1) == 0), ((quadsys & 2) == 0) };
			boolean boo[] = IJX.generalFTDialog("Choice", "Check below for automatic identification of quadrant coord system.", 
											"Quadrant System", true, true, pre);
			if (boo == null) return -1;
			boolean fem = boo[0], tib = boo[1];
			
			if (fem) {
				IJIF.notice("Analyzing femur...This may take some time...");
				//Quadrant.detectFemoralSystem();
				(new FemoralQuadrantDetector()).directRun();
			}
			if (tib) {
				IJIF.notice("Analyzing tibia...This may take some time...");
				(new TibialQuadrantDetector()).directRun();
			}
				
			return 0;
		}
		
		public static int detectTunnel2D() {
			(new TunnelDetector()).run(null);
			return 0;
		}
		
		public static int determineSystem2D() {
			Quadrant.setSystem();
			return 0;
		}
		
		
	}
}

abstract class IJIF_SwingWorker extends SwingWorker<Integer, String> {
	abstract public Integer doInBackground();
	abstract public void callback(String str);
}

