package imagejplugin.kneectanalyzer;

import ij.IJ;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.gui.RotatedRectRoi;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;

public class RoiAnalyzer3D implements PlugIn {
	XY centroid;
	Rect rect;
	XY coords[];
	int type;
	private static final int RECT=1, ROTRECT=2, OVAL=3, LINE=4; 

	@Override public void run(String arg) {
		// check if 'ImageJ 3D Viewer' is open, and if yes, get universe, and invoke run
		
		
	}
	
	public int directrun(Image3DUniverse univ) {
		ImageCanvas3D canvas = (ImageCanvas3D)univ.getCanvas();
		Roi roi = canvas.getRoi();
		if (roi == null) {
			IJ.error("Make selection on the 3D Viewer, using a selection tool.");
			return -1;
		}
		
		int type = 0;
		switch (Toolbar.getToolId()) {
		case Toolbar.RECTANGLE:
			switch (Toolbar.getRectToolType()) {
			case Toolbar.RECT_ROI:
			case Toolbar.ROUNDED_RECT_ROI:
				type = RECT;
				analyzeNormalRectangle(roi); break;				
			case Toolbar.ROTATED_RECT_ROI:
				type = ROTRECT;
				analyzeRotatedRectangle((RotatedRectRoi)roi); break;
			}
			break;
		case Toolbar.OVAL:
			switch (Toolbar.getOvalToolType()) {
			case Toolbar.OVAL_ROI:
			case Toolbar.ELLIPSE_ROI:
				type = OVAL;
				measureCentroid(roi); break;
			}
		case Toolbar.LINE:
			type = LINE;
			analyzeLine((Line)roi); break;
				
		}
		this.type = type;
		
		return type;
	}
	
	public boolean isRectangle() {
		return (type == RECT || type == ROTRECT);
	}
	
	private void measureCentroid(Roi roi) {
		centroid = new XY(roi.getContourCentroid());
	}
	
	private void analyzeNormalRectangle(Roi roi) {
		measureCentroid(roi);
		rect = new Rect(roi.getBounds());
		coords = rect.toCoordsPx();
	}
	
	private void analyzeRotatedRectangle(RotatedRectRoi roi) {
		measureCentroid(roi);
		RotRectTool rrt = new RotRectTool(roi);
		coords = rrt.getRect();
	}
	
	private void analyzeLine(Line roi) {
		coords = new XY[2];
		coords[0] = new XY(roi.x1, roi.y1);
		coords[1] = new XY(roi.x2, roi.y2);
		centroid = XY.midstOf(coords[0],  coords[1]);
	}
}