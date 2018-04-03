package imagejplugin.kneectanalyzer;

import java.awt.Choice;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import imagejplugin.kneectanalyzer.Quadrant.SysCoord;

public class QuadrantGrafter implements PlugIn, ItemListener {
	private Vector<TextField> numFields;
	private int resultsFT;
	private TunnelCoordList tcl;
	private XY femQ, tibQ;
	private String name;
	
	@Override public void run(String arg) {
		
	}
	
	public int directrun(String arg) {
		if (!IJX.Util.checkClass("mcib_plugins.Line3D_")) {
			return IJX.error("Line3D_ not found. Abort.", -1);
		}
			
		if (dialog() == -1) return -1;
		
		XYZ femP = get3DPointFromQuadCoordFem(femQ);
		XYZ tibP = get3DPointFromQuadCoordTib(tibQ);
		
		ImagePlus imp = callLine3D(femP, tibP), imp2;
		IJ.run(imp, "8-bit", ""); IJ.wait(50);
		IJ.run(imp, "Invert LUT", ""); IJ.wait(50);
		
		if ((imp2 = WindowManager.getImage(name)) != null) {
			(new ImageCalculator()).run("OR stack", imp2, imp);
			IJX.forceClose(imp);
		} else
			IJX.rename(imp, name);
		
		return 0;
	}
	
	private int dialog() {
		GenericDialog gd = new GenericDialog("Quadrant Grafting");
		gd.addStringField("Window title: Graft-", "AM");
		gd.setInsets(5, 0, 0);
		
		tcl = new TunnelCoordList();
		tcl.fromResultsTable("Results");
		tcl.fromResultsTable("Results3D");
		
		String fems[] = tcl.getLabels(Quadrant.FEM);
		String tibs[] = tcl.getLabels(Quadrant.TIB);
		if (fems != null) resultsFT |= Quadrant.FEM;
		if (tibs != null) resultsFT |= Quadrant.TIB;
		
		XY xy = new XY(0, 0);
		if (fems != null) {
			gd.addChoice("imported femoral tunnels:", fems, fems[0]);
			xy = tcl.findDefault(Quadrant.FEM).xy;
		}		
		gd.addNumericField("Fem Depth%", xy.x, 4);
		gd.addNumericField("Fem Height%", xy.y, 4);
		gd.setInsets(10, 0, 0);
		if (tibs != null) {
			gd.addChoice("imported tibial tunnels:", tibs, tibs[0]);
			xy = tcl.findDefault(Quadrant.TIB).xy;
		}
		gd.addNumericField("Tib M-L%", xy.x, 4);
		gd.addNumericField("Tib A-P%", xy.y, 4);
		
		Vector<Choice> choices = gd.getChoices();
		numFields = gd.getNumericFields();
		
		if (resultsFT > 0)
			choices.get(0).addItemListener(this);
		if (resultsFT == 3)
			choices.get(1).addItemListener(this);
		
		gd.showDialog();
		if (gd.wasOKed()) {
			name = "Graft-" + gd.getNextString();
			femQ = new XY(gd.getNextNumber(), gd.getNextNumber());
			tibQ = new XY(gd.getNextNumber(), gd.getNextNumber());
			return 0;
		}
		return -1;
	}
	
	public static XYZ get3DPointFromQuadCoordFem(XY quad) {
		Calibration cal = IJX.getBaseCalibration();
		
		XY pnt2D = Quadrant.calc2DPoint(Quadrant.FEM, quad);
		XY yzPx = pnt2D.clonePixelized(cal.pixelHeight, cal.pixelDepth);
		int x0 = RTBoundary.getSplitX();
		
		ImagePlus imps[] = new ImagePlus[2];
		imps[0] = WindowManager.getImage("FemOnly");
		imps[1] = WindowManager.getImage("TunOnlyFem");
		
		int y = (int)yzPx.x, w = imps[0].getWidth();;
		int xP = Integer.MAX_VALUE;
		for (ImagePlus imp: imps) { 
			if (imp != null) {
				ByteProcessor ip = (ByteProcessor) imp.getImageStack().getProcessor((int)yzPx.y + 1);
				
				for (int x = x0; x < w; x++)
					if (ip.getPixel(x, y) != 0) {
						xP = Math.min(xP, x); x = w;
					}
			}
		}
		
		if (xP == Integer.MAX_VALUE)
			return null;
		
		XYZ pnt3D = new XYZ(xP, y, yzPx.y);
		//pnt3D.px2real(cal);
		
		return pnt3D;
	}
	
	public static XYZ get3DPointFromQuadCoordTib(XY quad) {
		Calibration cal = IJX.getBaseCalibration();
		
		XY pnt2D = Quadrant.calc2DPoint(Quadrant.TIB, quad);
		XY xyPx = pnt2D.clonePixelized(cal);
		int z0 = RTBoundary.getProximalZ(BoundaryData.TIB);
		
		ImagePlus imps[] = new ImagePlus[2];
		imps[0] = WindowManager.getImage("TibOnly");
		imps[1] = WindowManager.getImage("TunOnlyTib");
		
		int zL = imps[0].getNSlices(), zP = Integer.MAX_VALUE;
		for (ImagePlus imp: imps) {
			
			for (int z = z0; z < zL; z++) {
				imp.setSliceWithoutUpdate(z + 1);
				ByteProcessor ip = (ByteProcessor) imp.getProcessor();
				
				if (ip.getPixel((int)xyPx.x, (int)xyPx.y) != 0) {
					zP = Math.min(zP, z); z = zL;
				}
			}
		}
		
		if (zP == Integer.MAX_VALUE)
			return null;
		
		XYZ pnt3D = new XYZ(xyPx.x, xyPx.y, zP);
		//pnt3D.px2real(cal);
		
		return pnt3D;
	}

	public ImagePlus callLine3D(XYZ fem, XYZ tib) {
		ImagePlus impBase = IJX.getBasal();
		int w = impBase.getWidth(), h = impBase.getHeight(), d = impBase.getNSlices();
		
		String size = "size_x="+w+" size_y="+h+" size_z="+d;
		String c0 = "x0="+Integer.toString((int)fem.x)+" y0="+Integer.toString((int)fem.y)+" z0="+Integer.toString((int)fem.z);
		String c1 = "x1="+Integer.toString((int)tib.x)+" y1="+Integer.toString((int)tib.y)+" z1="+Integer.toString((int)tib.z);
		String others = "thickness=5 value=65535 display=[New stack]";
		
		String arg = size + " " + c0 + " " + c1 + " " + others;
		
		IJ.run("3D Draw Line", arg);
		IJ.wait(50);
		return WindowManager.getImage("Line3D");
	}
	
	@Override public void itemStateChanged(ItemEvent e) {
		Choice c = (Choice)e.getSource();
		String l = c.getSelectedItem();
		
		TunnelCoord tc = tcl.find(l);
		if (tc != null) {
			int j = (tc.ft == Quadrant.TIB) ? 2:0;
			numFields.get(j).setText(IJ.d2s(tc.xy.x, 3));
			numFields.get(j+1).setText(IJ.d2s(tc.xy.y, 3));
		}
	}
	
}

class TunnelCoord {
	int ft;
	String label;
	XY xy;
	
	public TunnelCoord(int ft, String l, XY xy) {
		this.ft = ft;
		this.label = l;
		this.xy = xy;
	}
	
	public TunnelCoord(int ft, String l, double x, double y) {
		this.ft = ft;
		this.label = l;
		this.xy = new XY(x, y);
	}
}

class TunnelCoordList {
	ArrayList<TunnelCoord> list = new ArrayList();
	
	public TunnelCoordList() {}
	
	public int getCounts(int ft) {
		int n = 0;
		for (TunnelCoord tc: list) {
			if (tc.ft == ft)
				n++;
		}
		
		return n;
	}
	
	public String[] getLabels(int ft) {
		int n = getCounts(ft);
		if (n == 0) return null;
		
		String ret[] = new String[n];
		int i = 0;
		for (TunnelCoord tc: list) {
			if (tc.ft == ft)
				ret[i++] = tc.label;
		}
		
		return ret;
	}
	
	public void fromResultsTable(String rttitle) {
		ResultsTable rt = IJX.getResultsTable(rttitle);
		if (rt == null || rt.getColumnIndex("QuadX") == ResultsTable.COLUMN_NOT_FOUND)
			return;
		
		for (int i = 0; i < rt.size(); i++) {
			int ft = 0;
			
			String l = rt.getLabel(i);
			if (l.startsWith(Quadrant.WINTITLE_FEM2D)) ft = Quadrant.FEM;
			else if (l.startsWith(Quadrant.WINTITLE_TIB2D)) ft = Quadrant.TIB;
			
			if (ft != 0) {
				double x = rt.getValue("QuadX", i);
				double y = rt.getValue("QuadY", i);
				String label = rttitle + " #" + (i + 1) + ":" + l;
				list.add(new TunnelCoord(ft, label, x, y));
			}
		}
	}
	
	public TunnelCoord find(String label) {
		for (TunnelCoord tc: list)
			if (tc.label.equals(label))
				return tc;
		return null;
	}
	
	public TunnelCoord findDefault(int ft) {
		for (TunnelCoord tc: list)
			if (tc.ft == ft)
				return tc;
		return null;
	}
	
	

}