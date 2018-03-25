package imagejplugin.kneectanalyzer;

import java.awt.Button;
import java.awt.Canvas;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;






//import javax.vecmath.Color3f;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.java3d.View;

import vib.BenesNamedPoint;
import vib.PointList;
import vib.PointList.PointListListener;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.MultiLineLabel;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Analyzer;
import ij.text.TextPanel;
import ij3d.Content;
import ij3d.DefaultUniverse;
import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;
import ij3d.UniverseListener;
import ij3d.behaviors.InteractiveBehavior;
import ij3d.behaviors.Picker;

public class IJIF3D {
	public static Image3DUniverse univ;
	//public static ArrayList<String> contentNames;
	private static ResultsTable rt3d;
	private static final int IJ3D_WINSIZE = 512;
	private static int Resampling = 0; 
	
	private static float transparency;
	
	public static Image3DUniverse open3D() {
		if (IJIF.ij3d == true && univ == null) {
			univ = new Image3DUniverse(IJ3D_WINSIZE, IJ3D_WINSIZE);
			univ.show();
			univ.addUniverseListener(new IJIFUnivListener());
			univ.addInteractiveBehavior(new IJIFBehavior(univ));
			
			return univ;
		}
		return univ;
	}
	
	public static Content add3D(ImagePlus imp) {
		Color3f color = new Color3f(1f, 1f, 1f);
		return add3D(imp, color);
	}
	public static Content add3D(ImagePlus imp, Color3f color) {
		Content c;
		String name = imp.getTitle();
		
		if ((c = univ.getContent(name)) != null) {
			String contentWintitle = c.getImage().getTitle();
			if (contentWintitle.equals(name)) {
				c.setVisible(true);
				
				return c;
			} else {
				univ.removeContent(name);
			}
		}
		
		//boolean[] channels = new boolean[]{ true, true, true };
		//c = univ.addMesh(grey, color, name, 50, channels, 2);
		
		c = (Resampling > 0) ? univ.addMesh(imp, Resampling) : univ.addMesh(imp); 
		c.setColor(color);
		c.setLandmarkColor(new Color3f(1f, 1f, 0));
		c.setLandmarkPointSize(1);
		System.out.println("resampling: " + c.getResamplingFactor());
		
		return c;
	}
	
	public static void close() {
		if (univ != null)
			univ.cleanup();
		univ = null;
	}
	
	public static int getVisibleFT() {
		return Dialog.flag2ft(Dialog.content2flag(true));
	}
	
	public static void hideAllContents() {
		if (univ != null) {
			Collection<Content> clist = univ.getContents();
			for (Content c: clist)
				c.setVisible(false);
		}
	}
	
	public static void removeAllContents() {
		if (univ != null) {
			Collection<Content> clist = univ.getContents();
			for (Content c: clist)
				univ.removeContent(c.getName());
		}
	}
	
	private static mPointList convertPLtoMy(PointList pl) {
		if (pl == null) return null;
		
		mPointList mpl = new mPointList();
		for (int i = 0; i < pl.size(); i++)
			mpl.add(pl.get(i).getName(), pl.get(i).x, pl.get(i).y, pl.get(i).z);
		
		return mpl;
	} 
	
	static mPointList sumOfPointList(mPointList pl1, mPointList pl2) {
		mPointList pl = new mPointList();
		
		if (pl1 != null)
			for (int i = 0; i < pl1.size(); i++)
				pl.add(pl1.get(i));
		if (pl2 != null)
			for (int i = 0; i < pl2.size(); i++)
				pl.add(pl2.get(i));
		
		return pl;
	}
	static XY get3DRoiCentroid() {
		ImageCanvas3D canvas = (ImageCanvas3D)univ.getCanvas();
		Roi roi = canvas.getRoi();
		if (roi != null) {
			XY centroid = new XY(roi.getContourCentroid());
			
			if (!Double.isNaN(centroid.x))
				return centroid;
		}
		
		return null;
	}
	
	static XYZ get3DCoord(int x, int y, Picker picker, Content c) {
		if (picker != null && c != null) {
			org.scijava.vecmath.Point3d pnt = picker.getPickPointGeometry(c, x, y);
			if (pnt != null)
				return new XYZ(pnt.x, pnt.y, pnt.z);
		}
		
		return null;
	}
	
	static XYZ get3DCoord(XY xyPx) {
		if (xyPx != null) {
			int x = (int)xyPx.x, y = (int)xyPx.y;
			
			Picker picker = univ.getPicker();
			Content c = picker.getPickedContent(x, y);
		
			return get3DCoord(x, y, picker, c);
		}
		return null;
	}
	
	static void addPoint(Content c, XYZ pnt) {
		if (c != null && pnt != null) {
			float tol = c.getLandmarkPointSize();
			PointList pl = c.getPointList();
			if (pl.pointAt(pnt.x, pnt.y, pnt.z, tol) == null)
				pl.add(pnt.x, pnt.y, pnt.z);
			
			//picker.addPoint(c, x, y);
			c.showPointList(true);
		}
	}
	static void addPointAtScreen(XY xyPx) {
		if (xyPx != null) {
			int x = (int)xyPx.x, y = (int)xyPx.y;
				
			Picker picker = univ.getPicker();
			Content c = picker.getPickedContent(x, y);
			XYZ pnt = get3DCoord(x, y, picker, c);
				
			addPoint(c, pnt);
		} 
	}
	
	static void addPointFromResults(Content c, int ft, ResultsTable rt) {
		if (rt == null) return;
		
		for (int i = 0; i < rt.size(); i++) {
			XYZ pnt = null;
			if (ft == Quadrant.FEM && rt.getLabel(i).startsWith(Quadrant.WINTITLE_FEM2D)) {
				pnt = new XYZ(rt.getValue("Z", i), rt.getValue("X", i), rt.getValue("Y", i));
			} else if (ft == Quadrant.TIB && rt.getLabel(i).startsWith(Quadrant.WINTITLE_TIB2D)) {
				pnt = new XYZ(rt.getValue("X", i), rt.getValue("Y", i), rt.getValue("Z", i));
			}
			
			if (pnt != null)
				addPoint(c, pnt);
		}
	}
	
	static XYZ complementFromCentroid(XYZ centroid3d, XY centroid, XY rectPnts) {
		Picker picker = univ.getPicker();
		
		XY diff = XY.sub(rectPnts, centroid);
		double len = diff.getLength();
		diff.x /= len; diff.y /= len;
				
		double lastj = 0, inc = Math.max(2, len / 20);
		for (double j = 1; j < len; j+= inc) {
			int x = (int)(centroid.x + diff.x * j);
			int y = (int)(centroid.y + diff.y * j);
					
			Content c = picker.getPickedContent(x, y);
			if (c != null) {
				XYZ p = get3DCoord(x, y, picker, c);
				if (p == null)
					j = len;
				else
					lastj = j;
			} else
				j = len;
		}
				
		if (lastj > 0) {
			XY p2d = new XY(centroid.x + diff.x * lastj, centroid.y + diff.y * lastj);
			XYZ p3d = get3DCoord(p2d);
			XYZ diff3d = XYZ.sub(p3d, centroid3d);
			diff3d.multiply(len / lastj);
			
			return XYZ.add(centroid3d, diff3d);
		}
				
		return null;
	}
	
	static class Dialog {
		private static final String WINTITLES_BONE[] = { "FemOnly", "LFCOnly", "TibOnly" };
		private static final int BONEINDEX2FT[] = { 1, 1, 2 };
		private static final String WINTITLES_TUNNEL[] = { "TunOnlyFem", "TunOnlyTib", "TunOnlyBoth" };
		
		private static final int FEMONLY=1, LFCONLY=2, TIBONLY=4, TUNONLYFEM=8,TUNONLYTIB=16;
		private static final int FLAGS[] = { FEMONLY, LFCONLY, TIBONLY, TUNONLYFEM, TUNONLYTIB };
		private static int FEM = 1, TIB = 2; 
		
		private static boolean addMisc(GenericDialog gd, boolean enableTunnel) {
			gd.addSlider("Transparency", 0, 100, 0);
			
			boolean hasTun = (Quadrant.tunnelDetermined() != 0 && enableTunnel);
			if (hasTun) {
				boolean tun3D = ((content2flag(true) & (TUNONLYFEM|TUNONLYTIB)) != 0);
				gd.addCheckbox("Add Tunnel(s)", tun3D);
			}
			
			//TODO: grafts...
			
			return hasTun;
		}
		
		private static int getMisc(GenericDialog gd, boolean hasTun, int ftNew) {
			Vector<Scrollbar> slider = gd.getSliders();
			//transparency = slider.get(0).getValue();
			transparency = (float)gd.getNextNumber();
			
			int ret = 0;
			if (hasTun)
				if (gd.getNextBoolean())
					switch (ftNew) {
					case 1: ret = TUNONLYFEM; break;
					case 2: ret = TUNONLYTIB; break;
					case 3: ret = TUNONLYFEM | TUNONLYTIB; break;
					}
			
			return ret;
		}
		
		private static int oneBone(String title, String msg, boolean enableTunnel) {
			if (univ == null) return 0;
			
			int ft = flag2ft(content2flag(true)); 
			if (ft == 3 || ft == 0) ft = FEM;
			
			GenericDialog gd = new GenericDialog(title);
			gd.addMessage(msg);
			
			String bonetitles[] = new String[] { WINTITLES_BONE[1], WINTITLES_BONE[2] };
			gd.addRadioButtonGroup("Bone model to show", bonetitles, 2, 1, bonetitles[ft - 1]);
			boolean hasTun = addMisc(gd, enableTunnel);
			
			gd.showDialog();
			if (!gd.wasOKed()) return 0;
			
			int ftNew = 0, flag = 0;
			String ftStr = gd.getNextRadioButton();
			ftNew = (ftStr == bonetitles[0]) ? FEM : TIB;
			flag = FLAGS[ftNew];
			
			flag |= getMisc(gd, hasTun, ftNew);
			
			return flag;
		}
			
		private static int multiBones(String title, String msg) {
			if (univ == null) return 0;
			
			Content cFem, cTib;
			int contentFlag = content2flag(true);
			int ft = flag2ft(contentFlag);
			
			GenericDialog gd = new GenericDialog(title);
			gd.addMessage(msg);
			
			boolean defaults[] = new boolean[WINTITLES_BONE.length];
			defaults[0] = ((contentFlag & FEMONLY) != 0) ? true : false;
			defaults[1] = ((contentFlag & LFCONLY) != 0) ? true : false;
			defaults[2] = ((contentFlag & TIBONLY) != 0) ? true : false;
			
			String labels[] = new String[] { "Bone model to show" };
			gd.addCheckboxGroup(WINTITLES_BONE.length, 1, WINTITLES_BONE, defaults, labels);
			
			boolean hasTun = addMisc(gd, true);
			
			gd.showDialog();
			if (!gd.wasOKed()) return 0;
			
			int ftNew = 0, flag = 0; 
			for (int i = 0; i < WINTITLES_BONE.length; i++)
				if (gd.getNextBoolean()) {
					flag |= FLAGS[i];
					ftNew |= BONEINDEX2FT[i];
				}
			
			flag |= getMisc(gd, hasTun, ftNew);
			
			return flag;
		}
		
		static ImagePlus[] flag2imp(int flag) {
			ImagePlus[] imp = new ImagePlus[3]; // 0..fem, 1..tib, 2..tun; 3..grafts...
			
			if ((flag & LFCONLY) != 0) {
				if ((imp[0] = WindowManager.getImage("LFCOnly")) == null) {
					int nrx = RTBoundary.getSplitX();
					imp[0] = IJX.createLFCOnly(nrx);
				}
			} else if ((flag & FEMONLY) != 0) {
				if ((imp[0] = WindowManager.getImage("FemOnly")) == null)
					return null;
			}
			if ((flag & TIBONLY) != 0) {
				if ((imp[1] = WindowManager.getImage("TibOnly")) == null)
					return null;
			}
			
			ImagePlus tunFem, tunTib; tunFem = tunTib = null;
			if ((flag & TUNONLYFEM) != 0)
				imp[2] = tunFem = WindowManager.getImage("TunOnlyFem");
			if ((flag & TUNONLYTIB) != 0)
				imp[2] = tunTib = WindowManager.getImage("TunOnlyTib");
			if (tunFem != null && tunTib != null) {
				if ((imp[2] = WindowManager.getImage("TunOnlyBoth")) == null) {
					ImageCalculator ic = new ImageCalculator();
				
					imp[2] = ic.run("OR create stack", tunFem, tunTib);
					IJX.rename(imp[2], "TunOnlyBoth");
				}
			}
			
			return imp;
		}
		
		private static int flag2ft(int flag) {
			int ret = 0;
			if ((flag & (FEMONLY | LFCONLY)) != 0) ret |= FEM;
			if ((flag & TIBONLY) != 0) ret |= TIB;
			return ret;
		}
		
		public static String flag2string(int flag) {
			String ret = "";
			
			if ((flag & FEMONLY) != 0) ret += "Fem";
			if ((flag & LFCONLY) != 0) ret += "LFC";
			if ((flag & TIBONLY) != 0) ret += "Tib";
			if ((flag & (TUNONLYFEM | TUNONLYTIB)) != 0)
				ret += "+Tun";
			
			return ret;
		}
		
		public static int content2flag(boolean onlyVisible) {
			int ret = 0;
			Content c;
			
			if (univ == null) return 0;
			
			if ((c = univ.getContent("FemOnly")) != null)
				if (!onlyVisible || c.isVisible()) ret |= FEMONLY;
			if ((c = univ.getContent("LFCOnly")) != null)
				if (!onlyVisible || c.isVisible()) ret |= LFCONLY;
			if ((c = univ.getContent("TibOnly")) != null)
				if (!onlyVisible || c.isVisible()) ret |= TIBONLY;
			if ((c = univ.getContent("TunOnlyFem")) != null)
				if (!onlyVisible || c.isVisible()) ret |= TUNONLYFEM;
			if ((c = univ.getContent("TunOnlyTib")) != null)
				if (!onlyVisible || c.isVisible()) ret |= TUNONLYTIB;
			
			return ret;
		}
	}
	
	static class Quad {
		private static final String FILENAME_RESULTS3D = "tunnelCoords3D.txt";
		
		public static int view3dOne(boolean enableTunnel) {
			if (open3D() == null)
				return IJX.error("3D Viewer not available.", -1);
			
			hideAllContents();
			int showFlag = Dialog.oneBone("Choice", "Select below for 3D model to create.", enableTunnel);
			if (showFlag == 0) return 0;
			
			int ft = Dialog.flag2ft(showFlag);
			ImagePlus imps[] = Dialog.flag2imp(showFlag);
			if (imps == null) return 0;
			
			IJIF.notice("creating 3D surface rendered model...This takes some moments.");
			
			Content cBone = add3D(imps[ft - 1]);
			cBone.setTransparency(transparency);
			
			if (imps[2] != null) {
				Content cTun = add3D(imps[2], new Color3f(1f,0,0));
				
				ResultsTable rt = Analyzer.getResultsTable();
				if (rt != null)
					addPointFromResults(cTun, ft, rt);
			}
			
			univ.resetView();
			
			if (ft == 1)
				univ.rotateToPositiveXZ();
			
			return ft;
		}
		
		private static int createSystemDetermineView() {
			int ft = view3dOne(false);
			if (ft > 0) {
				// if ft==1, rotate...
				
				IJ.setTool("rotrect");
				int r = IJX.WaitForUser("Use Rotated-Rectangle selection tool\n"+
						"to fit the Quadrant system.\n"+"Then click OK.");
				if (r == -1) return 0;
				return ft;
			}
			
			return 0;
		} 
		
		public static int determineSystem3D() {
			int ft = Dialog.flag2ft(Dialog.content2flag(true));
			if (ft == 0 || ft == 3)
				if ((ft = createSystemDetermineView()) == 0)
					return -1;
			
			// TODO: componentize-from here
			
			if (univ == null) return -1;
			
			RoiAnalyzer3D ra3d = new RoiAnalyzer3D();
			ra3d.directrun(univ);
			if (!ra3d.isRectangle()) return -1;
			
			XY coords2d[] = ra3d.coords;
			XY centroid2d = ra3d.centroid;
			XYZ centroid3d = get3DCoord(centroid2d);
			
			if (centroid3d == null)
				return IJX.error("No bone at centroid!", -1);
			
			XYZ pnts[] = new XYZ[4]; int n = 0;
			for (int i = 0; i < 4; i++)
				if ((pnts[i] = get3DCoord(coords2d[i])) == null)
					if ((pnts[i] = complementFromCentroid(centroid3d, centroid2d, coords2d[i])) == null)
						return -1;
			
			coords2d = new XY[4];
			for (int i = 0; i < 4; i++)
				coords2d[i] = pnts[i].getXYZ(ft == 1 ? "YZ" : "XY");
			centroid2d = centroid3d.getXYZ(ft == 1 ? "YZ" : "XY");
			
			RotRectTool rrt = new RotRectTool(coords2d);
			rrt.setCentroid(centroid2d);
			XY qxy[] = rrt.toSysCoords(ft);
			Quadrant.SysCoord.output(ft, qxy);
			
			// TODO: componentize-to here
			
			return 0;
		}
		
		public static int refreshResults3D() {
			if (univ == null) return -1;
			
			int contentFlag = Dialog.content2flag(true);
			int ft = Dialog.flag2ft(contentFlag);
			if (ft == 0 || ft == 3) return -1;
			
			Content cBone, cTun;
			if (ft == Dialog.FEM) {
				if ((cBone = univ.getContent("LFCOnly")) != null)
					cBone = univ.getContent("FemOnly");
				cTun = univ.getContent("TunOnlyFem");
			} else {
				cBone = univ.getContent("TibOnly");
				cTun = univ.getContent("TunOnlyTib");
			}
			
			addPointAtScreen(get3DRoiCentroid());
			
			mPointList plTun = (cTun != null) ? convertPLtoMy(cTun.getPointList()) : null;
			mPointList plBone = (cBone != null) ? convertPLtoMy(cBone.getPointList()) : null;
			
			mPointList pl = sumOfPointList(plBone, plTun);
			
			if (rt3d == null)
				rt3d = new ResultsTable();
			else
				rt3d.reset();
			
			for (int i = 0; i < pl.size(); i++) {
				double x, y, z;
				if (ft == Dialog.FEM) {
					x = pl.get(i).y;
					y = pl.get(i).z;
					z = pl.get(i).x;
				} else {
					x = pl.get(i).x;
					y = pl.get(i).y;
					z = pl.get(i).z;
				}
				
				rt3d.incrementCounter();
				rt3d.addLabel(Quadrant.WINTITLES3D[ft]);
				rt3d.addValue("X", x);
				rt3d.addValue("Y", y);
				rt3d.addValue("Z", z);	
			}
			
			Quadrant.updateResults(rt3d);
			rt3d.show("Results3D");
			
			return 0;
		}
		
		public static int snapshot() {
			if (univ == null) return -1;
			
			ImagePlus imp3D = univ.takeSnapshot(IJ3D_WINSIZE, IJ3D_WINSIZE);
			
			String fname = "3D-" + Dialog.flag2string(Dialog.content2flag(true));
			String basepath = IJIF.getBaseDirectory();
			if (basepath == null)
				return -1;
			
			int cnt = 1;
			while (IJX.Util.doesFileExist(basepath, fname + "-" + cnt + ".png") ||
					WindowManager.getImage(fname) != null) {
				cnt++;
			}
			
			IJX.rename(imp3D, fname);
			imp3D.show();
			
			return 0;
		}
		
		public static int save(String basepath) {
			if (rt3d != null && rt3d.size() > 0)
				rt3d.save(IJX.Util.createPath(basepath, FILENAME_RESULTS3D));
			
			String snapshots[] = IJX.getWindowTitles("3D-.*");
			
			if (snapshots != null)
				for (String win: snapshots)
					IJX.savePNG(WindowManager.getImage(win), basepath, win);
			
			return 0;
		}
	}
}

class IJIFBehavior extends InteractiveBehavior {
	DefaultUniverse univ;
	
	public IJIFBehavior(DefaultUniverse univ) {
		super(univ);
		this.univ = univ;
	}
	
	@Override public void doProcess(KeyEvent e) {
		int id = e.getID();
		char code = e.getKeyChar();
		
		if (id == KeyEvent.KEY_TYPED && code == 'm')
			IJIF3D.addPointAtScreen(IJIF3D.get3DRoiCentroid());
	}
}

class IJIFUnivListener implements UniverseListener {
	@Override 	public void transformationStarted(View view) {}
	@Override	public void transformationUpdated(View view) {}
	@Override	public void transformationFinished(View view) {}
	@Override	public void contentAdded(Content c) {}
	@Override 	public void contentRemoved(Content c) {}
	@Override	public void contentChanged(Content c) {}
	@Override	public void contentSelected(Content c) {}
	@Override	public void canvasResized() {}
	
	@Override	public void universeClosed() {
		IJIF3D.univ = null;
	}
	
}

/*
 public static int snapshot(String basepath) {
			if (univ == null) return -1;
			
			int ft = 0;
			if (univ.getContent("Fem") != null) ft |= 1;
			if (univ.getContent("Tib") != null) ft |= 2;
			
			if (ft == 0) return -1;
			
			ImagePlus imp3D = univ.takeSnapshot(IJ3D_WINSIZE, IJ3D_WINSIZE);
		
			int cnt = 0;
			String fname;
			do {
				cnt++;
				fname = "3D-" + CONTENTNAMES[ft] + "-" + cnt;
			} while (IJX.Util.doesFileExist(basepath, fname + ".png"));
			
			IJX.savePNG(imp3D, basepath, fname);
			
			return 0;
		}
		
 */

