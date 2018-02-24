package imagejplugin.kneectanalyzer;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Roi;

public class QCanvas implements MouseListener, MouseMotionListener {
	protected static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
	protected static Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
	protected static Cursor moveCursor = new Cursor(Cursor.MOVE_CURSOR);
	protected static Cursor crosshairCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
	protected static Cursor resizeCursorFem[] = new Cursor[] { new Cursor(Cursor.NW_RESIZE_CURSOR), 
		new Cursor(Cursor.SE_RESIZE_CURSOR), new Cursor(Cursor.SW_RESIZE_CURSOR), new Cursor(Cursor.NE_RESIZE_CURSOR) };
	protected static Cursor resizeCursorTib[] = new Cursor[] { new Cursor(Cursor.N_RESIZE_CURSOR),
		new Cursor(Cursor.S_RESIZE_CURSOR), new Cursor(Cursor.W_RESIZE_CURSOR), new Cursor(Cursor.E_RESIZE_CURSOR) };
	
	
	
	ImagePlus imp;
	ImageCanvas ic;
	public QCanvas(ImagePlus imp) {
		this.imp = imp;
		this.ic = imp.getCanvas();
	}
	
	@Override
	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		
		Cursor cursor0 = ic.getCursor();
		Cursor cursorN = null;
		
		System.out.println(x + " " + y);
		
		Overlay ol = imp.getOverlay();
		for (int i = 0; i < ol.size(); i++) {
			Roi roi = ol.get(i);
			
			if (roi.getType() == Roi.LINE) {
				Line l = (Line)roi;
				String name = l.getName();
				
				if (name != null && name.startsWith("edge")) {
					Rectangle r = l.getBounds();
					if (r.contains(x, y)) {
						Line lM = new Line(l.x1, l.y1, x, y);
						if (Math.abs(l.getAngle() - lM.getAngle()) < 2) {
							int j = Character.getNumericValue(name.charAt(4));
							cursorN = resizeCursorFem[j];
						}
					}
				}
			}	
		}
		
		cursorN = (cursorN == null) ? crosshairCursor : cursorN;
		if (!cursor0.equals(cursorN))
			ic.setCursor(cursorN);
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		
		Overlay ol = imp.getOverlay();
		for (int i = 0; i < ol.size(); i++) {
			Roi roi = ol.get(i);
			
			if (roi.contains(x, y)) 
				System.out.println("Roi "+roi.toString()+" contains.");
			
		}
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	
}