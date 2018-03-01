package imagejplugin.kneectanalyzer;


import java.awt.AWTEvent;
import java.awt.Button;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;



public class AnatomyDetector implements PlugIn, ImageListener {
	public static final String WINTITLE_BOUNDARY = "Anatomic Boundary";
	private ImagePlus imp;
	private BoundaryTool bt;
	private int flag, lastSlice, nrx;
	private RoiManager rm;
	
	@Override public void run(String arg) {
	}
	
	public int directRun(ImagePlus imp) {
		this.imp = imp;
				
		BoundaryTool bt = new BoundaryTool(imp);
		bt.scanFEC();
		bt.scanFemur();
		bt.scanProxTibia();
		bt.scanDistalTibia();
		bt.scanProxNotch();
		
		nrx = bt.getMeanNotchRoofX();
		
		bt.clear(BoundaryData.NOTCHROOF);
		bt.real2px(imp.getCalibration());
		bt.close();
		
		imp.getWindow().toFront();
		WindowManager.setCurrentWindow(imp.getWindow());
		
		IJ.setTool(Toolbar.RECTANGLE);
		
		startBoundaryManager(bt, BoundaryData.FEM, BoundaryData.MFC, BoundaryData.LFC, BoundaryData.TIB, BoundaryData.FIB, BoundaryData.NOTCH);
		
		String msg = "Review Image w/ detected Anatomic Boundaries.\n";
		msg += "Edit the boundary ROI if necessary.\n";
		msg += "When finished, click OK of this dialog.";
		
		AnatomyDetectorDialog add = new AnatomyDetectorDialog(msg, imp, rm, bt);
		new PlugInFilterRunner(add, null, null);
		
		updateData(imp.getSlice());

		ImagePlus.removeImageListener(this);
		imp.killRoi();
		rm.reset();
		rm.close();
		
		if (!add.wasOK) return -1;
		
		bt.toResults(WINTITLE_BOUNDARY, nrx, true);
		
		return 0;
	}
	
	private void startBoundaryManager(BoundaryTool btPx, int... types) {
		this.rm = new RoiManager(false); rm.reset();
		this.bt = btPx;
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
		flag = 0xffff;
		for (int type: types)
			flag ^= 1 << type;
	}
	
	private void refreshView(int slice) {
		rm.reset();
		//overlay.clear();
		
		int z = slice - 1;
		
		for (BoundaryData bd: bt.bdlist) {
			if (bd.z == z) {
				int typeflag = 1 << bd.type;
				if ((typeflag & flag) != 0) {
					Roi rect = new Roi(bd.x, bd.y, bd.w, bd.h);
					String label = bd.getTypeString(); 
					rect.setName(label); rm.addRoi(rect);
					//overlay.add(rect, label);
				}
			}
		}
		
		BoundaryData bdatNE = bt.findDistal(BoundaryData.NOTCHEND);
		if (z <= bdatNE.z) {
			Line l = new Line(nrx, 0, nrx, imp.getHeight() - 1);
			l.setName("FemSplit");
			rm.addRoi(l);
			//overlay.add(l);
		}
		
		rm.runCommand("UseNames", "true"); 
		rm.runCommand(imp, "Show All with labels");
		
		//imp.setOverlay(overlay);
		
		// if (add != null) add.roiManagerUpdated();
	}
	
	private void updateData(int slice) {
		int cnt = rm.getCount();
		int z = slice - 1;
		
		for (int i = bt.bdlist.size() - 1; i >= 0; i--) {
			BoundaryData bd = bt.bdlist.get(i);
			if (bd.z == z) {
				int typeflag = 1 << bd.type;
				if ((typeflag & flag) != 0) {
					boolean inRM = false;
					for (int j = 0; j < cnt; j++) 
						if (bd.getTypeString().equals(rm.getName(j)))
							inRM = true;
					
					if (!inRM) {
						System.out.println("removed roi " + bd.getTypeString());
						bt.bdlist.remove(i);
					}
				}
			}
		}

		for (int j = 0; j < cnt; j++) {
			Roi roi = rm.getRoi(j);
			
			boolean updated = false;
			if (!roi.isLine()) {
				for (int i = bt.bdlist.size() - 1; i >= 0; i--) {
					BoundaryData bd = bt.bdlist.get(i);
					
					if (bd.z == z && bd.getTypeString().equals(roi.getName())) {
						java.awt.Rectangle r = roi.getBounds();
						if (!bd.equals(r)) {
							System.out.println("updated roi "+bd.getTypeString());
							bd.set(r);
						}
						
						i = 0;
						updated = true;
					}
				}
			} else {
				Line l = (Line)roi;
				if (l.x1 == l.x2 && l.x1 != nrx) {
					nrx = l.x1;
				}
				updated = true;
			}
			
			if (!updated) {
				java.awt.Rectangle r = roi.getBounds();
				int type = BoundaryData.getType(roi.getName());
				BoundaryData bd = new BoundaryData(type, z, r);
				bt.bdlist.add(bd);
				System.out.println("new roi "+bd.getTypeString());
			}
						
		}
	}
	
	@Override public void imageUpdated(ImagePlus imp) {
		if (this.imp == imp) {
			int slice = imp.getSlice();
			if (slice != lastSlice) {
				updateData(lastSlice);
				refreshView(slice);
				lastSlice = slice;
			} 
		}
	}
	
	@Override public void imageClosed(ImagePlus imp) {
		if (this.imp == imp) {
			
		}
		// TODO Auto-generated method stub
		
	}
	
	@Override public void imageOpened(ImagePlus imp) {}

	
}

class AnatomyDetectorDialog implements ExtendedPlugInFilter, DialogListener, ListSelectionListener, KeyListener, ActionListener {
	private ImagePlus imp;
	private String msg;
	private RoiManager rm;
	private BoundaryList bl;
	private int slice;
	private NonBlockingGenericDialog gd;
	private JList list;
	private DefaultListModel<String> listModel;
	public boolean wasOK;
	private Button[] btns = new Button[7];
	private int[] selected;
	
	private static final String[] BLABEL = new String[] { "mfc", "lfc", "tib", "fib", "notch" };
	private static final int[] BTYPE = new int[] { BoundaryData.MFC, BoundaryData.LFC, BoundaryData.TIB, BoundaryData.FIB, BoundaryData.NOTCH };
	
	public AnatomyDetectorDialog(String msg, ImagePlus imp, RoiManager rm, BoundaryList bl) {
		this.imp = imp;
		this.msg = msg;	
		this.rm = rm;
		this.bl = bl;
		this.selected = null;
		listModel = new DefaultListModel<String>();
	}
	
	@Override public int setup(String arg, ImagePlus imp) {
		return STACK_REQUIRED | DOES_ALL | NO_CHANGES;
	}

	@Override public void run(ImageProcessor ip) {
		if (slice > 0) imp.setSlice(slice);
		imp.updateAndDraw();
		
		IJ.wait(50);
		
		updateList();
		
	}

	@Override public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		gd = new NonBlockingGenericDialog("Anatomy Detector Dialog");
		gd.addMessage(msg);
		gd.addSlider("Slice: ", 1, imp.getNSlices(), imp.getSlice());
		gd.addPanel(createListPanel(), GridBagConstraints.CENTER, new Insets(5, 20, 0, 0));
		
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.getPreviewCheckbox().setState(true);
		
		//gd.addKeyListener(this);
		Vector<Scrollbar> slider = gd.getSliders();
		slider.get(0).addKeyListener(this);
		
		gd.showDialog();
		
		wasOK = gd.wasOKed();
		
		return 0;
	}

	@Override public void setNPasses(int nPasses) {}
	
	private Panel createListPanel() {
		list = new JList();
		list.setModel(listModel);
		list.setPrototypeCellValue("xxxxxxxxx ");		
		list.addListSelectionListener(this);
		//list.addKeyListener(ij);
		//list.addMouseListener(this);
		//list.addMouseWheelListener(this);
		if (IJ.isLinux()) list.setBackground(Color.white);
		JScrollPane scrollPane = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		for (int i = 0; i < 5; i++)
			btns[i] = new Button("Add as " + BLABEL[i]);
		btns[5] = new Button("Delete");
		btns[6] = new Button("Delete...");
		
		Panel panel = new Panel();
		GridBagLayout pgrid = new GridBagLayout();
		GridBagConstraints pc  = new GridBagConstraints();
		panel.setLayout(pgrid);
		pc.gridx = 0; pc.gridy = 0;
		pc.gridwidth = 1; pc.gridheight = btns.length;
		pc.fill = GridBagConstraints.VERTICAL;
		pgrid.setConstraints(scrollPane, pc);
		panel.add(scrollPane);
		
		for (int i = 0; i < btns.length; i++) {
			pc.gridx = 1;
			pc.gridy = i;
			pc.gridwidth = 1; pc.gridheight = 1;
			pc.anchor = GridBagConstraints.WEST;
			pc.fill = GridBagConstraints.HORIZONTAL;
			pgrid.setConstraints(btns[i], pc);
	    	panel.add(btns[i]);
	    	btns[i].addActionListener(this);
		}
    	
		return panel;
	}
	
	private int addRoi(int btn) {
		Roi roi = imp.getRoi();
		
		if (roi == null)
			return IJX.error("Specify ROI in the Image.");
		
		if (listModel.contains(BLABEL[btn]))
			return IJX.error(BLABEL[btn] + ": already specified.");
		
		if (rm.getRoiIndex(roi) != -1)
			return IJX.error("Duplicated ROI.");
		
		roi.setName(BLABEL[btn]);
		rm.addRoi(roi);
		
		listModel.addElement(BLABEL[btn]);
		
		bl.bdlist.add(new BoundaryData(BTYPE[btn], slice - 1, new Rect(roi.getBounds())));
		
		return 0;
	}
	
	private void removeRoi() {
		if (selected != null) {
			rm.deselect();
			rm.setSelectedIndexes(selected);
			rm.runCommand(imp, "Delete");
			
			String strs[] = new String[selected.length];
			for (int i = 0; i < selected.length; i++)
				strs[i] = listModel.get(selected[i]);
			
			for (int i = 0; i < strs.length; i++) {
				listModel.removeElement(strs[i]);
				
				BoundaryData bd = bl.find(BoundaryData.getType(strs[i]), slice - 1);
				bl.bdlist.remove(bd);
			}
			
			selected = null;
		}
	}
	
	private void removeRoiDialog() {
		GenericDialog gd = new GenericDialog("Remove ROIs");
		gd.addChoice("ROI type to remove: ", BLABEL, BLABEL[0]);
		gd.addNumericField("start slice:", slice, 0);
		gd.addNumericField("end slice:", slice, 0);
		gd.showDialog();
		
		if (gd.wasOKed()) {
			String typeStr = gd.getNextChoice();
			int type = BoundaryData.getType(typeStr);
			
			int start = (int)gd.getNextNumber();
			int end = (int)gd.getNextNumber();
			
			for (int i = bl.bdlist.size() - 1; i >= 0; i--) {
				BoundaryData bd = bl.bdlist.get(i);
				int z = bd.z + 1;
				if (bd.type == type && z >= start && z <= end)
					bl.bdlist.remove(i);
			}
					
			if (start <= slice && slice <= end && listModel.contains(typeStr)) {
				for (int i = rm.getCount() - 1; i >= 0; i--)
					if (typeStr.equals(rm.getName(i))) {
						rm.deselect();
						rm.select(i);
						rm.runCommand(imp, "Delete");
					}
				
				listModel.removeElement(typeStr);
			}
		}
	}
	
	@Override public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		slice = (int)gd.getNextNumber(); 
		return true;
	}

	@Override public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
			return;

		selected = list.getSelectedIndices();
	}
	
	@Override public void actionPerformed(ActionEvent e) {
		int btn = -1;
		for (int i = 0; i < btns.length; i++)
			if (btns[i] == e.getSource()) {
				btn = i; i = btns.length;
			}
		
		if (btn == -1) return;
		
		if (btn == 5) {
			System.out.println("DELETE " + selected);
			removeRoi();
		} else if (btn == 6) {
			removeRoiDialog();
		} else if (btn >= 0 && btn <= 4) {
			addRoi(btn);
		}	
	}

	@Override public void keyTyped(KeyEvent e) {}

	@Override public void keyPressed(KeyEvent e) {
		System.out.println("keyp");
		
		int kc = e.getKeyCode(); 
		int ds = 0;
		switch (kc) {
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_KP_LEFT:
			if (slice > 1) ds = -1;
			break;
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_KP_RIGHT:
			if (slice < imp.getNSlices()) ds = 1;
			break;
		}
		
		if (ds != 0) {
			Vector<TextField> numField = gd.getNumericFields();
			numField.get(0).setText(Integer.toString(slice + ds));
			//Scrollbar slider = (Scrollbar)e.getSource();
			//slider.setValue(slice + ds);
		}
	}
	@Override public void keyReleased(KeyEvent e) {}

	public void updateList() {
		listModel.removeAllElements();
		
		for (int i = 0; i < rm.getCount(); i++) {
			String label = rm.getName(i);
			listModel.addElement(label);
		}
		
		selected = null;
	}
}

