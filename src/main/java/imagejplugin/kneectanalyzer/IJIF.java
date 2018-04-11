package imagejplugin.kneectanalyzer;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
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
import java.awt.TextField;
import java.awt.Panel;
import java.awt.Button;
import java.awt.Font;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.SwingWorker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class IJIF implements Measurements {
	public static boolean ij3d = false;
	private static IJIF_SwingWorker callbackSwingWorker;
	
	//private static mPointList fec;
	
	static double minThreshold, maxThreshold;
	
	//private static double TPangle;
	
	private static boolean initializedFlag;
	private static String nonKCAtitle;
	
	//private static final String FILENAME_FEC = "fec.points";
	private static final String FILENAME_BOUNDARY = "boundary.txt"; 
	
	private static final String MD_THRESHOLD = "Threshold";
	public static final String WINTITLE_BOUNDARY = AnatomyDetector.WINTITLE_BOUNDARY;
	
	
	private static final String BD_TYPE[] = BoundaryData.TYPESTRING;
	
	public static void initIJIF() {
		if (IJ.getInstance() == null)
			new ij.ImageJ();
		
		if (initializedFlag)
			return;
		
		//fec = null;
		//TPangle = 0;
		initializedFlag = true;
		minThreshold = maxThreshold = 0;
		
		ij3d = IJX.Util.checkClass("ij3d.Image3DUniverse");	
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
	
	public static boolean hasTunnelCoords() {
		boolean ret = false;
		ResultsTable rt;
		if ((rt = IJX.getResultsTable("Results")) != null) {
			if (rt.getColumnIndex("QuadX") != ResultsTable.COLUMN_NOT_FOUND)
				ret = true;
		}
		if ((rt = IJX.getResultsTable("Results3D")) != null) {
			if (rt.getColumnIndex("QuadX") != ResultsTable.COLUMN_NOT_FOUND)
				ret = true;
		}
		
		return ret;
	}
	
	public static boolean hasGrafts() {
		String wins[] = IJX.getWindowTitles("Graft-.*");
		return (wins != null);
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
	
	public static boolean isKCADirectory(String dir) {
		return (IJX.Util.doesFileExist(dir, "Base.avi") && 
			(IJX.Util.doesFileExist(dir, "Voxel.txt") || IJX.Util.doesFileExist(dir, "metadata.txt")));
			
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
	
	
	/*
	 * Interface for dialog buttons 
	 * 
	 */
	
	
	public static int openKCADirectory(String... models) {
		nonKCAtitle = null;
		String dir = openDirectoryDialog("Select directory");
		
		if (dir == null)
			return -1;
		
		if (isKCADirectory(dir)) {
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
		for (String win: wintitles) {
			if (win != null && win.endsWith(".*")) {
				String wins[] = IJX.getWindowTitles(win);
				IJX.forceClose(wins);
			} else
				IJX.forceClose(win);
		}
		
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
		
		return 0;
	}
	
	static class Property {
		public static final int ALIGN_FEMORALSHAFT = 1, ALIGN_TIBIALPLATEAU = 2;
		public static int sagAlignMode = ALIGN_TIBIALPLATEAU;
		private static final String ALIGNMENTS[] = { "Femoral shaft", "Tibial plateau" };
		public static double condyleSize = 225;
		public static double condyleRatioMin = 0.5, condyleRatioMax = 1.5;
		public static double fibX1 = 0.6, fibX2 = 1.0;
		public static double fibY1 = 0.8, fibY2 = 1.2;
		public static double notchSize = 100;
		public static double proxNotchAngle = 110;
		public static double minSagProjFemSize = 100;
		public static double blumenOutlinerR1 = 10;
		public static double blumenOutlinerR2 = 5;
		public static double blumenOutlinerSD = 2;
		public static final int FEMQUAD_DS0BY_CONDYLE = 1, FEMQUAD_DS0BY_BLUMENSAAT = 2;
		public static int femQuadDS0Mode = FEMQUAD_DS0BY_CONDYLE;
		private static final String FEMQUAD_DS0[] = { "Tangent to Condyle", "End of Blumensaat's line" };  
		
		private static Vector<TextField> numFields, strFields;
		
		public static void settingDialog() {
			Calibration cal = IJX.getBaseCalibration();
			String calUnit = cal != null ? cal.getUnit() : "pixel";
			String calUnit2 = calUnit + "2";
			String unit = " (in " + calUnit + "2)";
			Font font = new Font("SansSerif", Font.PLAIN, 14);
			
			String condyleRangeStr = IJ.d2s(condyleRatioMin, 2) + "-" + IJ.d2s(condyleRatioMax, 2);
			String fibxRangeStr = IJ.d2s(fibX1, 2) + "-" + IJ.d2s(fibX2, 2);
			String fibyRangeStr = IJ.d2s(fibY1, 2) + "-" + IJ.d2s(fibY2, 2);
						
			GenericDialog gd = new GenericDialog("About and Settings");
			
			String msg1 = "KneeCT Analyzer\n";
			msg1 += "Copyright (C) 2018 Yasuhiro Take\n";
			msg1 += "License: GPLv3\n\n";
			msg1 += "Icons are downloaded from http://icons8.com.\n";
			gd.addMessage(msg1, font);
			
			gd.setInsets(5, 20, 0);
			gd.addRadioButtonGroup("Alignment", ALIGNMENTS, 1, 2, ALIGNMENTS[sagAlignMode - 1]);
			gd.addMessage("Detect Anatomy");
			gd.addNumericField("min condyle size", condyleSize, 0, 4, calUnit2);										// 0
			gd.addStringField("M/L condyle size diff. (M/L ratio)", condyleRangeStr, 12);								
			gd.addStringField("               fibula ML location (relative to tibia)" , fibxRangeStr, 12);
			gd.addStringField("               fibula AP location (relative to tibia)" , fibyRangeStr, 12);
			gd.addNumericField("min notch size", notchSize, 0, 4, calUnit2);											// 1
			gd.addNumericField("                               max prox notch angle", proxNotchAngle, 0, 4, "degree");	// 2
			gd.addMessage("Detect Quadrant System (fem)");
			gd.addNumericField("  min sag. femur size", minSagProjFemSize, 0, 4, calUnit2);								// 3
			gd.addMessage("+Outliner for Blumensaat's line");
			gd.addNumericField("SD > 5" + calUnit + " & residual >", blumenOutlinerR1, 1, 4, calUnit);					// 4
			gd.addNumericField("SD > 2.5" + calUnit + " & residual >", blumenOutlinerR2, 1, 4, calUnit);				// 5
			gd.addNumericField("SD > 2.5" + calUnit + " & residual >", blumenOutlinerSD, 2, 4, "x SD");					// 6
			gd.setInsets(5, 20, 0);
			gd.addRadioButtonGroup("+DS0 by", FEMQUAD_DS0, 1, 2, FEMQUAD_DS0[femQuadDS0Mode - 1]);
			gd.addMessage("Create Graft Model");
			
			Panel p = new Panel();
			p.setLayout(new BorderLayout());
			Button btn = new Button("Reset");
			btn.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					int i = 0;
					numFields.get(i++).setText(Integer.toString(225));
					numFields.get(i++).setText(Integer.toString(100));
					numFields.get(i++).setText(Integer.toString(110));
					numFields.get(i++).setText(Integer.toString(100));
					numFields.get(i++).setText(Double.toString(10));
					numFields.get(i++).setText(Double.toString(5));
					numFields.get(i++).setText(Double.toString(2));
					
					i = 0;
					strFields.get(i++).setText("0.5-1.5");
					strFields.get(i++).setText("0.6-1.0");
					strFields.get(i++).setText("0.8-1.2");
				} 
			});
			p.add("East", btn);
			gd.addPanel(p);
			
			numFields = gd.getNumericFields();
			strFields = gd.getStringFields();
			
			gd.showDialog();
			
			if (!gd.wasOKed()) return;
			
			String alignment = gd.getNextRadioButton();
			sagAlignMode = IJX.Util.getIndexOf(ALIGNMENTS, alignment) + 1;
			String femquad = gd.getNextRadioButton();
			femQuadDS0Mode = IJX.Util.getIndexOf(FEMQUAD_DS0, femquad) + 1;
			
			condyleSize = gd.getNextNumber();
			notchSize = gd.getNextNumber();
			proxNotchAngle = gd.getNextNumber();
			minSagProjFemSize = gd.getNextNumber();
			blumenOutlinerR1 = gd.getNextNumber();
			blumenOutlinerR2 = gd.getNextNumber();
			blumenOutlinerSD = gd.getNextNumber();
			
			double range[] = IJX.Util.stringRange2double(gd.getNextString());
			condyleRatioMin = range[0]; condyleRatioMax = range[1];
			
			range = IJX.Util.stringRange2double(gd.getNextString());
			fibX1 = range[0]; fibX2 = range[1];
			
			range = IJX.Util.stringRange2double(gd.getNextString());
			fibY1 = range[0]; fibY2 = range[1];
		}
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
			ad.setMinimumCondyleSize(Property.condyleSize);
			ad.setCondyleRatio(Property.condyleRatioMin, Property.condyleRatioMax);
			ad.setMinimumNotchSize(Property.notchSize);
			ad.setMaximumProxNotchAngle(Property.proxNotchAngle);
			ad.setFibularHeadLocation(Property.fibX1, Property.fibX2, Property.fibY1, Property.fibY2);
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
			
			IJIF.Modeler.save("TunOnlyFem", "TunOnlyTib");
			
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
				FemoralQuadrantDetector qd = new FemoralQuadrantDetector();
				qd.setSagittalFemoralProjectionSize(Property.minSagProjFemSize);
				qd.setBlumensaatDetectionParams(Property.blumenOutlinerR1, Property.blumenOutlinerR2, Property.blumenOutlinerSD);
				qd.setDS0ByCondyle(Property.femQuadDS0Mode == Property.FEMQUAD_DS0BY_CONDYLE);
				qd.directRun();
			}
			if (tib) {
				IJIF.notice("Analyzing tibia...This may take some time...");
				(new TibialQuadrantDetector(Property.sagAlignMode == Property.ALIGN_TIBIALPLATEAU, Property.condyleSize)).directRun();
			}
			
			return 0;
		}
		
		public static String detectTunnel2D() {
			TunnelDetector td = new TunnelDetector();
			return td.directrun();
		}
		
		public static int determineSystem2D() {
			return (new ManualQuadrantDefiner()).directrun();
		}
		
		public static int refreshResults() {
			Quadrant.updateResults();
			return 0;
		}
		
		
	}
	
	static class Grafter {
		public static int open(String... wins) {
			int r = openKCADirectory(wins);
			if (r > 0) {
				String dir = getBaseDirectory();
				
				if (IJX.Util.doesFileExist(dir, Quadrant.FILENAME_RESULTS) ||
					IJX.Util.doesFileExist(dir, Quadrant.FILENAME_RESULTS3D))
					importData(dir);
			}
			
			return r;
		}
		public static int save() {
			String basepath = getBaseDirectory();
			if (basepath == null)
				return -1;
			
			String snapshots[] = IJX.getWindowTitles("3D-.*");
			
			if (snapshots != null)
				for (String win: snapshots)
					IJX.savePNG(WindowManager.getImage(win), basepath, win);
			
			String grafts[] = IJX.getWindowTitles("Graft-.*");
			if (grafts != null)
				IJIF.Modeler.save(grafts);
			
			return 0;
		}
		
		public static int importData() {
			String dir = openDirectoryDialog("Select directory");
			
			int r  = importData(dir);
			
			if (r == -1)
				IJX.error("File not found: " + Quadrant.FILENAME_RESULTS); 
			
			return r;
		}
		
		public static int importData(String dir) {
			if (dir == null || !isKCADirectory(dir))
				return -1;
			
			String f1 = Quadrant.FILENAME_RESULTS, f2 = Quadrant.FILENAME_RESULTS3D;
			boolean loaded = false;
			if (IJX.Util.doesFileExist(dir, f1)) {
				IJX.closeResultsTable("Results");
				ResultsTable rt = ResultsTable.open2(IJX.Util.createPath(dir, f1));
				if (rt != null) {
					rt.show("Results");
					loaded = true;
				}
			}
			
			if (IJX.Util.doesFileExist(dir, f2)) {
				IJX.closeResultsTable("Results3D");
				ResultsTable rt = ResultsTable.open2(IJX.Util.createPath(dir, f2));
				if (rt != null) {
					rt.show("Results3D");
					loaded = true;
				}
			}
			
			if (!loaded) return -1;
			
			return 0;
		}
		
		public static int createGraft() {
			int r = (new QuadrantGrafter()).directrun(null);
			return r;
		}
	}
}

abstract class IJIF_SwingWorker extends SwingWorker<Integer, String> {
	abstract public Integer doInBackground();
	abstract public void callback(String str);
}

