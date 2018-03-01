package imagejplugin.kneectanalyzer;

import ij.*;
import ij.process.*;
import ij.measure.*;
import ij.gui.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import ij.plugin.PlugIn;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;

public class OrthogonalTransformer implements PlugIn {
	private ImagePlus lastImpAx;
	private ImagePlus impZ[] = new ImagePlus[3];
	
	private static final int I_AXIAL = 0;
	private static final int I_CORONAL = 1;
	private static final int I_SAGITTAL = 2;
	
	@Override public void run(String arg) { 
		ImagePlus imp = WindowManager.getCurrentImage();   
		if (imp == null) {
			IJ.noImage();
			return;
		}
		
		directRun(imp, arg);
	}
	
	public ImagePlus getTransformedImage() {
		return lastImpAx;
	}
	
	public int directRun(ImagePlus impOriginal, String arg) {
		ImagePlus impstack[] = new ImagePlus[3];
		
		lastImpAx = impOriginal;
		
		notice("Creating orthogonal projections... \nThis takes some moments.");
		
		Calibration cal = impOriginal.getCalibration();
		impstack[I_AXIAL] = impOriginal.duplicate(); // axial
		impstack[I_AXIAL].show();
		impstack[I_CORONAL] = IJX.reslice(impstack[0], cal.pixelHeight, "Top"); 
		impstack[I_SAGITTAL] = IJX.reslice(impstack[0], cal.pixelDepth, "Left");
		
		this.orthoStacksToZProjects(impstack); // this.impZ updated, impstack[1,2] closed.
		
		OrthogonalTransformerFilter mf = new OrthogonalTransformerFilter(this.impZ);
		mf.impOrthoView.show();
		mf.impOrthoView.getWindow().toFront();
		
		this.lastImpAx = impstack[0];
		
		int r = loop(mf, impstack[0], arg);
			
		if (r == -1) {
			IJX.forceClose(impstack[0]);
			lastImpAx = null;
		}
		IJX.forceClose(mf.impOrthoView);
		IJX.rename(lastImpAx, "Aligned " + impOriginal.getTitle());
		
		return r;
	}
	
	private int loop(OrthogonalTransformerFilter mf, ImagePlus impAx, String arg) {
		for (;;) {
			WindowManager.setCurrentWindow(mf.impOrthoView.getWindow());
			
			new PlugInFilterRunner(mf, null, arg);
			
			if (mf.endStatus == mf.END_CANCEL)
				return -1;
			else { // END_APPLY | END_OK
				if (mf.isChanged()) {
					notice(null);
					notice("Transforming stacks...\nThis takes some moments.");
					double a[] = mf.getAngles();
					boolean f[] = mf.getFlipflags();
					
					ImagePlus impstack[] = createTransformedStacks(impAx, a, f); 
					
					//this.transformPoints(a, f, impstack); // this.plConv is transformed
					this.orthoStacksToZProjects(impstack); // this.impZ updated, impstack[1,2] closed.
					
					impAx = impstack[I_AXIAL];
					this.lastImpAx = impAx;
					
					mf.clearInput();
					mf.updateOrthoView(this.impZ);
					mf.impOrthoView.getWindow().toFront();
				}
				
				if (mf.endStatus == mf.END_OK)
					return 0;
				else
					arg = null;
			}
		}
	}
	
	static private ImagePlus[] createTransformedStacks(ImagePlus impSrc, double angles[], boolean flip[]) {
		ImagePlus impstack[] = new ImagePlus[3];
		Calibration cal = impSrc.getCalibration();
				
		// rotate ax
		IJX.rotate(impSrc, angles[I_AXIAL]);
		
		if (flip[I_AXIAL])
			IJ.run(impSrc, "Flip Horizontally", "stack");
		
		// ax->cor, & rotate
		impstack[I_CORONAL] = IJX.reslice(impSrc, cal.pixelHeight, "Top"); 
		IJX.rotate(impstack[I_CORONAL], angles[I_CORONAL]);
		if (flip[I_CORONAL]) 
			IJ.run(impstack[I_CORONAL], "Flip Vertically", "stack");
		
		// cor->sag, & rotate
		impstack[I_SAGITTAL] = IJX.reslice(impstack[I_CORONAL], cal.pixelWidth, "Left rotate");
		IJX.rotate(impstack[I_SAGITTAL], angles[I_SAGITTAL]);
		if (flip[I_SAGITTAL])
			IJ.run(impstack[I_SAGITTAL], "Flip Horizontally", "stack");
				
		// sag->ax
		IJX.forceClose(impSrc);
		impstack[I_AXIAL] = IJX.reslice(impstack[I_SAGITTAL], cal.pixelDepth, "Top rotate");
		impstack[I_AXIAL].setTitle("KCAwork-3Drotated");
		
		return impstack;
	}
	
	private void orthoStacksToZProjects(ImagePlus impstack[]) {
		for (int i = 0; i < 3; i++) {
			IJX.forceClose(this.impZ[i]);
			
			this.impZ[i] = IJX.zproject(impstack[i]); // TODO: start and end slice
			
			if (i > 0) {
				IJX.forceClose(impstack[i]);
				impstack[i] = null;
			}
		}
	}
	
	static private void notice(String message) {
		//IJ.log(message);
		IJIF.notice(message);
	}
	
	static private double[] getAnglesBy2P(mPointList pl) {
		double ret[] = new double[3];
		
		Line l1 = new Line(pl.get(0).x, pl.get(0).y, pl.get(1).x, pl.get(1).y);
		ret[0] = l1.getAngle(); // Axial angle
		
		Line l2 = new Line(pl.get(0).x, pl.get(0).z, pl.get(1).x, pl.get(1).z);
		ret[1] = l2.getAngle(); // Coronal angle
		
		ret[2] = 0;
		
		return ret;
	}
}

/**
* class OrthogonalTransformerFilter - impl. ExtendedPluginFilter, DialogListener 
* 
* This class visualizes projections in three orthogonal plane, i.e., axial, coronal      
* and sagittal planes, and provide an interactive interface to rotate and flip them
* with preview.
* 
* Input (at constructor)
*   impZ[]: three non-stack images as ImagePlus (projections of orthogonal stacks)
*   angles[]: three initial angles for rotation
* Output: (partly as class member variables).
*   impOrthoView - ImagePlus; an image showing three impZ[] in one, which may be 
*     transformed according to user-inputs through the dialog (when preview is ON).   
*   angle[] - double; user-specified values through the dialog
*   flip[] - boolean; user-specified values through the dialog
* 
* Overall, the original stack of axial slices is supposed to be three-dimensionally
* transformed according to the user-inputs, while this plugin-filter only provides     
* UI to show projections of orthogonal stacks with a temporal transformation.
* The plugin-filter invoker should transform the stacks according to the user-inputs.
* When this plugin-filter ends with END_APPLY (when APPLY button is clicked),  
* the invoker should reconstruct projections of transformed orthogonal stacks, and
* re-run this plugin-filter.  
* 
* @author Yasuhiro Take, M.D., Ph.D. - Osaka Univ. Graduate School of Medicine
* 
* License: GPL v3. ABSOLUTELY NO WARRANTY.
*
*/

class OrthogonalTransformerFilter implements ExtendedPlugInFilter, DialogListener {
	
	private double angle[] = new double[4];
	private boolean flip[] = new boolean[3];
	
	private ImagePlus impZ[];
	private int px[] = new int[4], width[] = new int[4], height[] = new int[4];

	public final int END_OK = 1, END_APPLY=2, END_CANCEL=3;
	public int endStatus;
	public ImagePlus impOrthoView;
	public double minPx, maxPx;
	public double yscale;
	
	private void init(ImagePlus impZ[]) {
		this.impZ = impZ;
		
		this.impOrthoView = createOrthoView(impZ);
		this.minPx = impOrthoView.getProcessor().getMin();
		this.maxPx = impOrthoView.getProcessor().getMax();
		this.yscale = impOrthoView.getWidth() / impOrthoView.getHeight();
	}
	
	public OrthogonalTransformerFilter(ImagePlus impZ[], final double angle[], final boolean flip[]) {
		init(impZ);
		
		for (int i = 0; i < 3; i++) {
			this.angle[i] = (angle != null) ? angle[i] : 0;
			this.flip[i] = (flip != null) ? flip[i] : false;
		}
	}
	
	public OrthogonalTransformerFilter(ImagePlus impZ[]) {
		init(impZ);
		
		for (int i = 0; i < 3; i++) {
			this.angle[i] = 0;
			this.flip[i] = false;
		}
	}
	
	@Override public int setup(String arg, ImagePlus imp) {
		return DOES_ALL;
	}

	@Override public void run(ImageProcessor ip) {
		refreshOrthoView();	
	}
	
	@Override public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.endStatus = 0;
		
		OTMouseListener otml = new OTMouseListener(this);
		ImageCanvas ic = imp.getCanvas();
		ic.addMouseMotionListener(otml);
		ic.addMouseListener(otml);
		
		IJ.setTool(Toolbar.HAND);
		
		String items[] = { "Axial", "Coronal", "Sagittal" };
				
		NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Orthogonal Transformation Dialog");
		
		gd.addMessage("Correct alignment by rotation and flipping in orhtogonal planes.\n" +
				"You can repeat correction by 'Apply' button, until pressing 'Finish' button.\n"+
				"Preview shows a temporally rotated/flipped images in 3-plane projections.");
		
		for (int i = 0; i < 3; i++)
			gd.addNumericField(items[i] + " angle:", angle[i], 3);
		
		gd.setInsets(10, 20, 0);

		gd.addCheckbox("Flip Medial-Lateral", false);
		gd.addCheckbox("Flip Proximal-Distal", false);
		gd.addCheckbox("Flip Anterior-Posterior", false);
		
		gd.setInsets(10, 40, 0);
		
		gd.addPreviewCheckbox(pfr);
		// pfr.setDialog(gd); --- called at gd.showDialog() 
		// at pfr.setDIalog(), gd.AddDialogListener(this) is called.
		gd.addDialogListener(this);
		gd.enableYesNoCancel("Finish", "Apply");
		gd.getPreviewCheckbox().setState(true);
		gd.showDialog();
		
		ic.removeMouseMotionListener(otml);
		ic.removeMouseListener(otml);
		
		if (gd.wasOKed())
			this.endStatus = END_OK;		
		 else if (gd.wasCanceled())
			 this.endStatus = END_CANCEL;
		 else
			 this.endStatus = END_APPLY;
		
		return 0;
	}

	@Override public void setNPasses(int nPasses) {}
	
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		
		for (int i = 0; i < 3; i++) {
			double v = gd.getNextNumber();
			this.angle[i] = Double.isNaN(v) ? 0 : v;
		}

		for (int i = 0; i < 3; i++)
			this.flip[i] = gd.getNextBoolean();
		
		return true;
	}
	
	public double[] getAngles() {
		return angle;
	}
	
	public boolean[] getFlipflags() {
		return flip;
	}


	public boolean isChanged() {
		if (angle[0] != 0 || angle[1] != 0 || angle[2] != 0 ||
			flip[0] || flip[1] || flip[2])
			return true;
		else
			return false;
	}		
	
	public void clearInput() {
		for (int i = 0;i < 3; i++) {
			angle[i] = 0.0;
			flip[i] = false;
		}
	}
	
	private ImagePlus createOrthoView(ImagePlus impZ[]) {
		int px0 = 0;
			
		for (int i = 0; i < 3; i++) {
			int w = impZ[i].getWidth();
			int h = impZ[i].getHeight();
			
			this.px[i] = px0;
			this.width[i] = w;
			this.height[i] = h;
			
			px0 += w; 			
		}
		
		return IJX.createCombinedImage("Orthogonal Projection", impZ);
	}
	
	private void refreshOrthoView() {
		for (int i = 0; i < 3; i++) {
			ImageProcessor ipZ = impZ[i].getProcessor();
			ipZ.reset();
			
			switch(i) {
				case 0:
					if (flip[0]) ipZ.flipHorizontal();
					if (flip[2]) ipZ.flipVertical();
					break;
				case 1:
					if (flip[0]) ipZ.flipHorizontal();
					if (flip[1]) ipZ.flipVertical();
					break;
				case 2:
					if (flip[1]) ipZ.flipVertical();
					if (flip[2]) ipZ.flipHorizontal(); 
					break;
			}
			
			ipZ.rotate(angle[i]);
			
			impZ[i].killRoi();
			impZ[i].copy();
			IJ.wait(50);
			
			impOrthoView.setRoi(this.px[i], 0, this.width[i], this.height[i]);
			impOrthoView.paste();
			impOrthoView.killRoi();
		}
		
		//impOrthoView.getProcessor().setMinAndMax(minPx, maxPx);
		
		impOrthoView.updateAndDraw();
	}
	
	public void updateOrthoView(ImagePlus impZ[]) {
		this.impZ = impZ;
		for (int i = 0; i < 3; i++)
			impZ[i].getProcessor().snapshot();
		IJ.wait(50);
		
		refreshOrthoView();
	
	}
}

class OTMouseListener implements MouseMotionListener, MouseListener {
	private int startX, startY;
	private OrthogonalTransformerFilter otf;
	
	OTMouseListener(OrthogonalTransformerFilter otf) {
		this.otf = otf;
	}
	
	@Override public void mouseDragged(MouseEvent e) {
		double wl = (otf.maxPx + otf.minPx) / 2;
		double ww = (otf.maxPx - otf.minPx);
		
		int x = e.getX();
		int y = e.getY();
		int dx = x - startX; int dy = y - startY;  
		wl += dx;
		ww += dy * otf.yscale;
		
		otf.maxPx = wl + ww / 2;
		otf.minPx = wl - ww / 2;
		
		startX = x;
		startY = y;
		
		otf.impOrthoView.getProcessor().setMinAndMax(otf.minPx, otf.maxPx);
		otf.impOrthoView.updateAndDraw();
		
		e.consume();
	}

	@Override public void mouseMoved(MouseEvent e) {}

	@Override public void mouseClicked(MouseEvent e) {}

	@Override public void mousePressed(MouseEvent e) {
		startX = e.getX();
		startY = e.getY();
		e.consume();
	}

	@Override public void mouseReleased(MouseEvent e) {}
	@Override public void mouseEntered(MouseEvent e) {}
	@Override public void mouseExited(MouseEvent e) {}
}

/*
  private void transformPoints(final double angle[], final boolean flip[], final ImagePlus impstack[]) {
		if (plConv == null) 
			return;
		
		double x[], y[], z[], xN, yN, zN, xc, yc, zc, f_xc, f_yc, f_zc;
		int i, plsize = plConv.size();
		Calibration cal;
		double cos, sin;
		
		x = new double[plsize]; y = new double[plsize]; z = new double[plsize]; 
		for (i = 0; i < plsize; i++) {
			x[i] = plConv.get(i).x; y[i] = plConv.get(i).y; z[i] = plConv.get(i).z;
		}
		
		// -------- rotate on axial slice -----------
		int index = I_AXIAL;
		
		cal = impstack[index].getCalibration();
		xc = (impstack[index].getWidth() - 1) / 2 * cal.pixelWidth;
		yc = (impstack[index].getHeight() - 1) / 2 * cal.pixelHeight;
		f_xc = xc;
		cos = IJX.Util.cosA(angle[index]); sin = IJX.Util.sinA(angle[index]);
		 
		for (i = 0; i < plsize; i++) {
			xN = (x[i] - xc) * cos - (y[i] - yc) * sin + xc;
			yN = (y[i] - yc) * cos + (x[i] - xc) * sin + yc;
			
			x[i] = xN; y[i] = yN;
		}
		
		// -------- rotate on coronal slice -----------
		index = I_CORONAL;
		
		cal = impstack[index].getCalibration();
		xc = (impstack[index].getWidth() - 1) / 2 * cal.pixelWidth;
		zc = (impstack[index].getHeight() - 1) / 2 * cal.pixelHeight;
		f_zc = zc;
		cos = IJX.Util.cosA(angle[index]); sin = IJX.Util.sinA(angle[index]);
		
		for (i = 0; i < plsize; i++) {
			xN = (x[i] - xc) * cos - (z[i] - zc) * sin + xc;
			zN = (z[i] - zc) * cos + (x[i] - xc) * sin + zc;
			
			x[i] = xN; z[i] = zN;
		}
		
		// -------- rotate on sagittal slice -----------
		index = I_SAGITTAL;
		
		cal = impstack[index].getCalibration();
		yc = (impstack[index].getWidth() - 1) / 2 * cal.pixelWidth;
		zc = (impstack[index].getHeight() - 1) / 2 * cal.pixelHeight;
		f_yc = yc;
		cos = IJX.Util.cosA(angle[index]); sin = IJX.Util.sinA(angle[index]); 
		
		for (i = 0; i < plsize; i++) {
			yN = (y[i] - yc) * cos - (z[i] - zc) * sin + yc;
			zN = (z[i] - zc) * cos + (y[i] - yc) * sin + zc;
			
			y[i] = yN; z[i] = zN;
		}
		
		// ---------- flip -------------
		
		for (i = 0; i < plsize; i++) {
			xN = x[i]; yN = y[i]; zN = z[i];
			
			if (flip[I_AXIAL])
				xN = 2 * f_xc - x[i];
			if (flip[I_CORONAL])
				zN = 2 * f_zc - z[i];
			if (flip[I_SAGITTAL])
				yN = 2 * f_yc - y[i];
			
			x[i] = xN; y[i] = yN; z[i] = zN;
		}
		
		// ----------- substitution --------------
		
		String name[] = new String[plsize];
		for (i = 0; i < plsize; i++)
			name[i] = plConv.get(i).getName();
		
		plConv.clear();
		for (i = 0; i < plsize; i++)
			plConv.add(name[i], x[i], y[i], z[i]);
	}
	
	*/


