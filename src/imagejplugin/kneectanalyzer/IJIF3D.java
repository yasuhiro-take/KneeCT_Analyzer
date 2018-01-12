package imagejplugin.kneectanalyzer;

import java.awt.event.KeyEvent;

import javax.media.j3d.View;
import javax.vecmath.Color3f;

import vib.BenesNamedPoint;
import vib.PointList;
import vib.PointList.PointListListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.ResultsTable;
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
	private static final int IJ3D_WINSIZE = 512;
	
	public static Image3DUniverse open3D() {
		// TODO: classloader, catch exception...
		if (IJIF.ij3d == true && univ == null) {
			univ = new Image3DUniverse(IJ3D_WINSIZE, IJ3D_WINSIZE);
			univ.show();
			univ.addUniverseListener(new IJIFUnivListener());
			univ.addInteractiveBehavior(new IJIFBehavior(univ));
			return univ;
		}
		return univ;
	}
	
	public static Content add3D(Image3DUniverse u, ImagePlus grey, String wintitle) {
		Color3f color = new Color3f(1f, 1f, 1f);
		return add3D(u, grey, wintitle, color);
	}
	public static Content add3D(Image3DUniverse u, ImagePlus grey, Color3f color) {
		return add3D(u, grey, grey.getTitle(), color);
	}
	public static Content add3D(Image3DUniverse u, ImagePlus grey) {
		Color3f color = new Color3f(1f, 1f, 1f);
		return add3D(u, grey, grey.getTitle(), color);
	}
	
	public static Content add3D(Image3DUniverse univ, ImagePlus grey, String wintitle, Color3f color) {
		boolean[] channels = new boolean[]{ true, true, true };
		
		//Content c = univ.addContent(grey, color, wintitle, 50, channels, 2, 2);
		Content c = univ.addMesh(grey, color, wintitle, 50, channels, 2);
		//c.showBoundingBox(true);
		
		return c;
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
	
	static XY getRoiCentroid3D() {
		ImageCanvas3D canvas = (ImageCanvas3D)univ.getCanvas();
		Roi roi = canvas.getRoi();
		if (roi != null) {
			XY centroid = new XY(roi.getContourCentroid());
			
			if (!Double.isNaN(centroid.x))
				return centroid;
		}
		
		return null;
	}
	
	static void addPointByRoi() {
		XY centroid = getRoiCentroid3D();
		if (centroid != null) {
			int x = (int)centroid.x, y = (int)centroid.y;
				
			Picker picker = univ.getPicker();
			Content c = picker.getPickedContent(x, y);
				
			if (c != null) {
				picker.addPoint(c, x, y);
				c.showPointList(true);
			}
		} 
	}
	
	static class Modeler {		
		public static String determineFEC() {
			if (IJIF3D.open3D() == null)
				return null;
		
		//GUI.center(univ.getWindow());
		
			ImagePlus grey = WindowManager.getCurrentImage();
			String wintitle = grey.getTitle();
		
			IJIF.notice("creating 3D surface rendered model...This takes some moments.");
			Content c = add3D(univ, grey, wintitle);
		
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
				return null;
		//if (pll.pointNum < 2 || c.getPointList().size() < 2) return null;
			
			IJIF.setFEC(convertPLtoMy(c.getPointList()));
		
			return wintitle;
		}
		
		public static void updateFEC(String wintitle) {
			if (univ != null) {
				Content c = univ.getContent(wintitle);
				if (c != null)
					IJIF.setFEC(convertPLtoMy(c.getPointList())); //update fec for the case when a point is moved after determineFECBy3D().
			}
		}
		
		public static void showModelAndPoints(ImagePlus imp, mPointList pl) {
			univ.removeAllContents();
			Content c = add3D(univ, imp, imp.getTitle());
			
			for (int i = 0; i < pl.size(); i++)
				c.getPointList().add(pl.get(i).name, pl.get(i).x, pl.get(i).y, pl.get(i).z);
			c.showPointList(true);
		}
	}
	
	static class Quad {
		private static final String CONTENTNAMES[] = new String[] { null, "Fem", "Tib", "Fem+Tib" };
		
		public static int view3D(boolean tunnel) {
			if (open3D() == null)
				return IJX.error("3D Viewer not available.", -1);
			
			univ.removeAllContents();
			
			int ft = IJIF.radiobuttonFTDialog("Choice", "Select below for 3D model to create.", "Bone (+ Tunnel)"); 
			if (ft == 0) return -1;
			
			ImagePlus impBone, impTun = null;
			if (ft == 1) {
				impBone = WindowManager.getImage("LFCOnly");
				if (impBone == null)
					impBone = IJX.createLFCOnly(WindowManager.getImage("FemOnly"), IJIF.notchRoofX);
				impBone.show();
				
				if (tunnel && Quadrant.tunnelRoisFem != null) {
					impTun = WindowManager.getImage("TunOnlyFem");
					if (impTun == null) {
						IJIF.notice("creating tunnel model...This takes some moments.");
						ImagePlus impSag = IJX.createAx2Sag(impBone);
						IJIF.convertTunOnly(impSag, Quadrant.tunnelRoisFem);
						impTun = IJX.createSag2Ax(impSag);
						IJX.rename(impTun, "TunOnlyFem");
						IJX.forceClose(impSag);
					}
				}
				//pl = IJX.createPointListfromResults(Quadrant.WINTITLE_FEM2D, "Z", "X", "Y");
			} else if (ft == 2) {
				impBone = WindowManager.getImage("TibOnly");
				
				if (tunnel && Quadrant.tunnelRoisTib != null) {
					impTun = WindowManager.getImage("TunOnlyTib");
					if (impTun == null) {
						impTun = impBone.duplicate();
						impTun.show();
						IJIF.convertTunOnly(impTun, Quadrant.tunnelRoisTib);
						IJX.rename(impTun, "TunOnlyTib");
					}
				}
				//pl = IJX.createPointListfromResults(Quadrant.WINTITLE_TIB2D, "X", "Y", "Z");
			} else return -1;
			
			IJIF.notice("creating 3D surface rendered model...This takes some moments.");
			
			Content c = add3D(univ, impBone, CONTENTNAMES[ft]);
			
			if (tunnel) {
				if (impTun != null)
					c = add3D(univ, impTun, "Tun", new Color3f(1f,0,0));
			
				/*
				if (pl != null) {
					for (int i = 0; i < pl.size(); i++)
						c.getPointList().add(pl.get(i).name, pl.get(i).x, pl.get(i).y, pl.get(i).z);
					c.showPointList(true);
					c.setLandmarkPointSize(1);
				}
				*/
			}
			
			if (ft == 1)
				univ.rotateToPositiveXZ();
			/*
			ImagePlus imp2D = univ.takeSnapshot(IJ3D_WINSIZE, IJ3D_WINSIZE);
			imp2D.show();
			IJX.rename(imp2D, Quadrant.WINTITLES3D[ft]);
			*/
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
			
			addPointByRoi();
			
			PointList plTun = (cTun != null) ? cTun.getPointList() : null;
			PointList pl = sumOfPointList(cBones[ft].getPointList(), plTun);
			
			ResultsTable rt = new ResultsTable();
			
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
				
				rt.incrementCounter();
				rt.addLabel(Quadrant.WINTITLES[ft]);
				rt.addValue("X", x);
				rt.addValue("Y", y);
				rt.addValue("Z", z);	
			}
			
			Quadrant.measurements2Coord(rt);
			
			//rt.updateResults();
			rt.show("Results3D");
			
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
			IJIF3D.addPointByRoi();
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

