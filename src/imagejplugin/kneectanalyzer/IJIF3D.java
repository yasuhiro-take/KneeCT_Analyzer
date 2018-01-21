package imagejplugin.kneectanalyzer;

import java.awt.Scrollbar;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import vib.BenesNamedPoint;
import vib.PointList;
import vib.PointList.PointListListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Analyzer;
import ij3d.Content;
import ij3d.DefaultUniverse;
import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;
import ij3d.UniverseListener;
import ij3d.behaviors.InteractiveBehavior;
import ij3d.behaviors.Picker;

public class IJIF3D {
	public static Image3DUniverse univ;
	public static ArrayList<String> contentNames;
	private static ResultsTable rt3d;
	private static final int IJ3D_WINSIZE = 512;
	
	private static final String CONTENT_BONES[] = { null, "Fem", "Tib", "FemTib" };
	
	private static float transparency;
	private static int lastShowFlag; 
	
	public static Image3DUniverse open3D() {
		if (IJIF.ij3d == true && univ == null) {
			univ = new Image3DUniverse(IJ3D_WINSIZE, IJ3D_WINSIZE);
			univ.show();
			univ.addUniverseListener(new IJIFUnivListener());
			univ.addInteractiveBehavior(new IJIFBehavior(univ));
			contentNames = new ArrayList<String>();
			
			return univ;
		}
		return univ;
	}
	
	public static Content add3D(ImagePlus grey, String wintitle) {
		Color3f color = new Color3f(1f, 1f, 1f);
		return add3D(grey, wintitle, color);
	}
	public static Content add3D(ImagePlus grey, Color3f color) {
		return add3D(grey, grey.getTitle(), color);
	}
	public static Content add3D(ImagePlus grey) {
		Color3f color = new Color3f(1f, 1f, 1f);
		return add3D(grey, grey.getTitle(), color);
	}
	
	public static Content add3D(ImagePlus grey, String name, Color3f color) {
		boolean[] channels = new boolean[]{ true, true, true };
		
		Content c;
		
		if ((c = univ.getContent(name)) != null) {
			String contentWintitle = c.getImage().getTitle();
			if (contentWintitle.startsWith(grey.getTitle())) {
				c.setVisible(true);
				
				if (!contentNames.contains(name))
					contentNames.add(name);
				
				return c;
			} else {
				univ.removeContent(name);
			}
		}
		
		c = univ.addMesh(grey, color, name, 50, channels, 2);
		
		if (c != null && !contentNames.contains(name))
			contentNames.add(name);
		
		return c;
	}
	
	public static void close() {
		if (univ != null)
			univ.cleanup();
		univ = null;
		contentNames = null;
	}
	
	public static void hideAllContents() {
		if (univ != null && contentNames != null) {
			for (String name: contentNames) {
				Content c = univ.getContent(name);
				if (c != null)
					c.setVisible(false);
			}
		}
	}
	
	public static void removeAllContents() {
		if (univ != null && contentNames != null) {
			for (String name: contentNames) {
				univ.removeContent(name);
			}
		}
	}
	
	private static mPointList convertPLtoMy(PointList pl) {
		mPointList mpl = new mPointList();
		for (int i = 0; i < pl.size(); i++)
			mpl.add(pl.get(i).getName(), pl.get(i).x, pl.get(i).y, pl.get(i).z);
		
		return mpl;
	} 
	
	static PointList sumOfPointList(PointList pl1, PointList pl2) {
		PointList pl = new PointList();
		
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
	
	static void addPoint(XY xyPx) {
		if (xyPx != null) {
			int x = (int)xyPx.x, y = (int)xyPx.y;
				
			Picker picker = univ.getPicker();
			Content c = picker.getPickedContent(x, y);
				
			if (c != null) {
				Point3d pnt = picker.getPickPointGeometry(c, x, y);
				float tol = c.getLandmarkPointSize();
				PointList pl = c.getPointList();
				if (pl.pointAt(pnt.x, pnt.y, pnt.z, tol) == null)
					pl.add(pnt.x, pnt.y, pnt.z);
				
				//picker.addPoint(c, x, y);
				c.showPointList(true);
			}
		} 
	}
	
	static class Dialog {
		private static final String WINTITLES_BONE[] = { "FemOnly", "LFCOnly", "TibOnly" };
		private static final int BONEINDEX2FT[] = { 1, 1, 2 };
		private static final String WINTITLES_TUNNEL[] = { "TunOnlyFem", "TunOnlyTib", "TunOnlyBoth" };
		
		private static final int FEMONLY=1, LFCONLY=2, TIBONLY=4, TUNONLYFEM=8,TUNONLYTIB=16;
		private static final int FLAGS[] = { FEMONLY, LFCONLY, TIBONLY, TUNONLYFEM, TUNONLYTIB };
		
		private static int flag2ft(int flag) {
			int ret = 0;
			if ((flag & (FEMONLY | LFCONLY)) != 0) ret |= 1;
			if ((flag & TIBONLY) != 0) ret |= 2;
			return ret;
		}
	
		private static boolean addMisc(GenericDialog gd) {
			gd.addSlider("Transparency", 0, 100, 100);
			
			boolean hasTun = (Quadrant.tunnelDetermined() != 0);
			if (hasTun) {
				Content cTun = univ.getContent("Tun"); 
				boolean tun3D = (cTun != null && cTun.isVisible());
				gd.addCheckbox("Add Tunnel(s)", tun3D);
			}
			
			//TODO: grafts...
			
			return hasTun;
		}
		
		private static int getMisc(GenericDialog gd, boolean hasTun, int ftNew) {
			Vector<Scrollbar> slider = gd.getSliders();
			transparency = slider.get(0).getValue();
			
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
		
		private static int oneBone(String title, String msg) {
			if (univ == null) return 0;
			
			int ft = 0; 
			if ((univ.getContent("Fem")) != null) ft |= 1;
			if ((univ.getContent("Tib")) != null) ft |= 2;
			
			if (ft == 3 || ft == 0) ft = 1;
			
			GenericDialog gd = new GenericDialog(title);
			gd.addMessage(msg);
			
			String bonetitles[] = new String[] { WINTITLES_BONE[1], WINTITLES_BONE[2] };
			gd.addRadioButtonGroup("Bone model to show", bonetitles, 2, 1, bonetitles[ft - 1]);
			boolean hasTun = addMisc(gd);
			
			gd.showDialog();
			if (!gd.wasOKed()) return 0;
			
			int ftNew = 0, flag = 0;
			String ftStr = gd.getNextRadioButton();
			ftNew = (ftStr == bonetitles[0]) ? 1:2;
			flag = FLAGS[ftNew];
			
			flag |= getMisc(gd, hasTun, ftNew);
			
			return flag;
		}
			
		private static int multiBones(String title, String msg) {
			if (univ == null) return 0;
			
			int ft = 0; Content cFem, cTib;
			if ((cFem = univ.getContent("Fem")) != null) ft |= 1;
			if ((cTib = univ.getContent("Tib")) != null) ft |= 2;
			
			GenericDialog gd = new GenericDialog(title);
			gd.addMessage(msg);
			
			boolean defaults[] = new boolean[WINTITLES_BONE.length];
				
			String femtitle = ((ft & 1) != 0 && cFem.isVisible()) ? cFem.getImage().getTitle() : null;
			String tibtitle = ((ft & 2) != 0 && cTib.isVisible()) ? cTib.getImage().getTitle() : null;
				
			for (int i = 0; i < WINTITLES_BONE.length; i++) {
				defaults[i] = false;
				if (femtitle != null && WINTITLES_BONE[i].equals(femtitle)) defaults[i] = true;
				if (tibtitle != null && WINTITLES_BONE[i].equals(tibtitle)) defaults[i] = true;
			}	
			
			String labels[] = new String[] { "Bone model to show" };
			gd.addCheckboxGroup(WINTITLES_BONE.length, 1, WINTITLES_BONE, defaults, labels);
			
			boolean hasTun = addMisc(gd);
			
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
				if ((imp[0] = WindowManager.getImage("LFCOnly")) == null)
					imp[0] = IJX.createLFCOnly(IJIF.notchRoofX);
			} else if ((flag & FEMONLY) != 0) {
				if ((imp[0] = WindowManager.getImage("FemOnly")) == null)
					return null;
			}
			if ((flag & TIBONLY) != 0) {
				if ((imp[1] = WindowManager.getImage("TibOnly")) == null)
					return null;
			}
			
			ImagePlus tunFem, tunTib; tunFem = tunTib = null;
			if ((flag & TUNONLYFEM) != 0) {
				if ((tunFem = WindowManager.getImage("TunOnlyFem")) == null) {
					IJIF.notice("creating tunnel model...This takes some moments.");
					tunFem = IJX.createTunOnlyFem(imp[0], Quadrant.tunnelRoisFem);
				}
				imp[2] = tunFem;
			}
			if ((flag & TUNONLYTIB) != 0) {
				if ((tunTib = WindowManager.getImage("TunOnlyTib")) == null)
					tunTib = IJX.createTunOnlyTib(imp[1], Quadrant.tunnelRoisTib);
				imp[2] = tunTib;
			}
			if (tunFem != null && tunTib != null) {
				if ((imp[2] = WindowManager.getImage("TunOnlyBoth")) == null) {
					ImageCalculator ic = new ImageCalculator();
				
					imp[2] = ic.run("OR create stack", tunFem, tunTib);
					IJX.rename(imp[2], "TunOnlyBoth");
				}
			}
			
			return imp;
		}
		
		public static String flag2string(int flag) {
			int ft = flag2ft(flag);
			String ret = CONTENT_BONES[ft];
			
			if ((flag & (TUNONLYFEM | TUNONLYTIB)) != 0)
				ret += "+Tun";
			
			return ret;
		}
	}
	
	static class Modeler {		
		public static int determineFEC(String wintitle) {
			if (IJIF3D.open3D() == null)
				return -1;
		
		//GUI.center(univ.getWindow());
		
			ImagePlus grey = WindowManager.getImage(wintitle);
		
			IJIF.notice("creating 3D surface rendered model...This takes some moments.");
			Content c = add3D(grey, wintitle);
		
		/*
		IJIFPointListListener pll = new IJIFPointListListener();
		c.getPointList().addPointListListener(pll);
		*/
			IJIF.notice("Rotate the 3D image by the hand tool, and registrate the medial and lateral femoral epicondyles by the point tool.");
			
			do {
				c = (univ != null) ? univ.getContent(wintitle) : null;
			
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
				//} while (c != null && univ != null && pll.pointNum < 2);
			} while (c != null && univ != null && c.getPointList().size() < 2);
		
			if (c == null) // 3D-window closed, or object removed
				return -1;
		//if (pll.pointNum < 2 || c.getPointList().size() < 2) return null;
			
			IJIF.setFEC(convertPLtoMy(c.getPointList()));
		
			return -1;
		}
		
		public static void updateFEC(String wintitle) {
			if (univ != null) {
				Content c = univ.getContent(wintitle);
				if (c != null)
					IJIF.setFEC(convertPLtoMy(c.getPointList())); //update fec for the case when a point is moved after determineFECBy3D().
			}
		}
		
		public static void showModelAndPoints(ImagePlus imp, mPointList pl) {
			removeAllContents();
			Content c = add3D(imp, imp.getTitle());
			
			for (int i = 0; i < pl.size(); i++)
				c.getPointList().add(pl.get(i).name, pl.get(i).x, pl.get(i).y, pl.get(i).z);
			c.showPointList(true);
		}
	}
	
	static class Quad {
		private static final String FILENAME_RESULTS3D = "tunnelCoords3D.txt";
		
		public static int view3D() {
			if (open3D() == null)
				return IJX.error("3D Viewer not available.", -1);
			
			hideAllContents();
			int showFlag = Dialog.oneBone("Choice", "Select below for 3D model to create.");
			if (showFlag == 0) return -1;
			lastShowFlag = showFlag;
			
			int ft = Dialog.flag2ft(showFlag);
			ImagePlus imps[] = Dialog.flag2imp(showFlag);
			if (imps == null) return -1;
			
			IJIF.notice("creating 3D surface rendered model...This takes some moments.");
			
			add3D(imps[ft - 1], CONTENT_BONES[ft]);
			
			if (imps[2] != null)
				add3D(imps[2], "Tun", new Color3f(1f,0,0));
			
			univ.resetView();
			
			if (ft == 1)
				univ.rotateToPositiveXZ();
			
			return 0;
		}
		
		public static int refreshResults3D() {
			if (univ == null) return -1;
			
			int ft = 0;
			Content cTun = null, cBones[] = new Content[3];
			if ((cBones[1] = univ.getContent("Fem")) != null) ft |= 1;
			if ((cBones[2] = univ.getContent("Tib")) != null) ft |= 2;
			cTun = univ.getContent("Tun");
			
			if (ft == 0 || ft == 3) return -1;
			
			addPoint(get3DRoiCentroid());
			
			PointList plTun = (cTun != null) ? cTun.getPointList() : null;
			PointList pl = sumOfPointList(cBones[ft].getPointList(), plTun);
			
			if (rt3d == null)
				rt3d = new ResultsTable();
			else
				rt3d.reset();
			
			for (int i = 0; i < pl.size(); i++) {
				double x, y, z;
				if (ft == 1) {
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
			
			Quadrant.measurements2Coord(rt3d);
			
			//rt.updateResults();
			rt3d.show("Results3D");
			
			return 0;
		}
		
		public static int snapshot() {
			if (univ == null) return -1;
			
			ImagePlus imp3D = univ.takeSnapshot(IJ3D_WINSIZE, IJ3D_WINSIZE);
			
			String fname = "3D-" + Dialog.flag2string(lastShowFlag);
			String basepath = IJIF.getBaseDirectory();
			if (basepath == null)
				return -1;
			
			int cnt = 1;
			while (IJX.Util.doesFileExist(basepath, fname + ".png") ||
					WindowManager.getImage(fname) != null) {
				fname += "-" + cnt++;
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
			IJIF3D.addPoint(IJIF3D.get3DRoiCentroid());
	}
}

class IJIFPointListListener implements PointListListener {
	public int pointNum;
	public IJIFPointListListener() {
		pointNum = 0;
	}
	@Override public void added(BenesNamedPoint p) { pointNum++; }
	@Override public void removed(BenesNamedPoint p) { pointNum--; }
	@Override 	public void renamed(BenesNamedPoint p) {}
	@Override 	public void moved(BenesNamedPoint p) {}
	@Override	public void highlighted(BenesNamedPoint p) {}
	@Override	public void reordered() {}
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
		IJIF3D.contentNames = null;
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

