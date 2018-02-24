package imagejplugin.kneectanalyzer;

import ij.IJ;
import ij.WindowManager;
import ij.gui.MultiLineLabel;
import ij.plugin.frame.PlugInFrame;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;



public class AnatomyDetectorDialog extends PlugInFrame implements KeyListener, ItemListener, ListSelectionListener, MouseListener, MouseWheelListener, ActionListener {
	private static Frame instance;
	private GridBagLayout grid;
	private GridBagConstraints c;
	private Choice choice;
	private JList list;
	private DefaultListModel listModel;
	private Button finishBtn, cancelBtn;
	private static final String menu[] = new String[] { "This slice", "All mfc", "All lfc", "All notch", "All tib" }; 

	
	public AnatomyDetectorDialog() {
		super("Anatomy Detector Dialog");
		// TODO Auto-generated constructor stub
		if (instance != null) {
			instance.toFront();
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		
		createDialog();
	}
	
	
	private void createDialog() {
		grid = new GridBagLayout();
		c = new GridBagConstraints();
		setLayout(grid);
		
		String msg = "Review Image w/ detected Anatomic Boundaries.\n";
		msg += "Edit the boundary ROI if necessary.";
		MultiLineLabel label = new MultiLineLabel(msg);
		
		c.gridx = 0; c.gridy = 0;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(10, 10, 0, 0);
		c.fill = GridBagConstraints.HORIZONTAL;
		grid.setConstraints(label, c);
		add(label);
		c.fill = GridBagConstraints.NONE;
	
		choice = addChoice("ROIs shown: ", menu, 1);
		addList(2);
		addButtons(3);
		
		pack();
		show();
	}
		
	private Choice addChoice(String label, String[] items, int y) {
		// copied from ImageJ source
   		Label theLabel = new Label(label);
		
   		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		c.insets = new Insets(10, 10, 0, 0);
		grid.setConstraints(theLabel, c);
		add(theLabel);
		
		Choice thisChoice = new Choice();
		thisChoice.addItemListener(this);
		for (int i=0; i<items.length; i++)
			thisChoice.addItem(items[i]);
		thisChoice.select(0);
		
		c.gridx = 1; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		grid.setConstraints(thisChoice, c);
		add(thisChoice);
		
		return thisChoice;
    }
	
	private void addList(int y) {
		listModel = new DefaultListModel();
		list = new JList();
		list.setModel(listModel);
		list.setPrototypeCellValue("0000-xxxxx ");		
		list.addListSelectionListener(this);
		//list.addKeyListener(ij);
		list.addMouseListener(this);
		list.addMouseWheelListener(this);
		if (IJ.isLinux()) list.setBackground(Color.white);
		JScrollPane scrollPane = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.CENTER;
		grid.setConstraints(scrollPane, c);
		add(scrollPane);
	}

	private void addButtons(int y) {
		Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
		cancelBtn = new Button("Cancel");
		cancelBtn.addActionListener(this);
		//cancelBtn.addKeyListener(this);
		
		finishBtn = new Button("Finish");
		finishBtn.addActionListener(this);
		finishBtn.addKeyListener(this);
		
		buttons.add(cancelBtn);
		buttons.add(finishBtn);
		
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 2;
		c.insets = new Insets(5, 0, 0, 0);
		grid.setConstraints(buttons, c);
		add(buttons);
	}

	@Override public void itemStateChanged(ItemEvent e) {
		int stateChange = e.getStateChange();
		String item = e.paramString();
		
		//System.out.println("itemStateChanged "+arg0);
	}


	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		// TODO Auto-generated method stub
		
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
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("action performed "+arg0);
	}
   
	
}
