package imagejplugin.kneectanalyzer;


import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;

public class BoundaryOverlayer implements ImageListener {
	private ImagePlus imp;
	private BoundaryTool bt;
	private int flag, lastSlice;
	private RoiManager rm;
	
	public BoundaryOverlayer(ImagePlus imp, BoundaryTool btPx, int... types) {
		this.rm = new RoiManager();
		rm.reset();
		
		this.bt = btPx;
		this.imp = imp;
		this.lastSlice = imp.getSlice();
		
		ImagePlus.addImageListener(this);
		
		setVisible(types);
		refreshView(lastSlice);
	}
	
	public void setVisible(int... types) {
		flag = 0;
		for (int type: types)
			flag |= 1 << type;
	}
	
	public void setInvisible(int... types) {
		flag = 0xff;
		for (int type: types)
			flag ^= 1 << type;
	}
	
	private void updateData() {
		int cnt = rm.getCount();
		
		if (cnt == 0)
			for (int i = bt.bdlist.size() - 1; i >= 0; i--) {
				BoundaryData bd = bt.bdlist.get(i);
				if (bd.z == lastSlice - 1)
					bt.bdlist.remove(i);
			}
		
		for (int j = 0; j < cnt; j++) {
			Roi roi = rm.getRoi(j);
			
			boolean updated = false;
			for (int i = bt.bdlist.size() - 1; i >= 0; i--) {
				BoundaryData bd = bt.bdlist.get(i);
				
				if (bd.z == lastSlice - 1 && bd.getTypeString().equals(roi.getName())) {
					java.awt.Rectangle r = roi.getBounds();
					if (!bd.equals(r)) {
						System.out.println("updated roi "+bd.getTypeString());
						bd.set(r);
					}
					
					i = 0;
					updated = true;
				}
			}
			
			if (!updated) {
				java.awt.Rectangle r = roi.getBounds();
				int type = BoundaryData.getType(roi.getName());
				BoundaryData bd = new BoundaryData(type, lastSlice - 1, r);
				bt.bdlist.add(bd);
				System.out.println("new roi "+bd.getTypeString());
			}
						
		}
	}
	
	public void refreshView(int slice) {
		rm.reset();
		
		int z = slice - 1;
		
		for (BoundaryData bd: bt.bdlist) {
			if (bd.z == z) {
				int typeflag = 1 << bd.type;
				if ((typeflag & flag) != 0) {
					Roi rect = new Roi(bd.x, bd.y, bd.w, bd.h);
					rect.setName(bd.getTypeString());
					rm.addRoi(rect);
				}
			}
		}
		
		rm.runCommand("UseNames", "true");
		rm.runCommand(imp, "Show All with labels");
		
	}

	@Override public void imageOpened(ImagePlus imp) {}

	@Override public void imageClosed(ImagePlus imp) {
		if (this.imp == imp) {
			
		}
		// TODO Auto-generated method stub
		
	}

	@Override public void imageUpdated(ImagePlus imp) {
		if (this.imp == imp) {
			int slice = imp.getSlice();
			if (slice != lastSlice) {
				updateData();
				refreshView(slice);
				lastSlice = slice;
			} 
		}
	}

	
}
