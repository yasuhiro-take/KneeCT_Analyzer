package imagejplugin.kneectanalyzer;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;

public class CommonGUI implements ActionListener {
	Frame frame;
	JButton btn_open, btn_save, btn_close, btn_q;
	Button btn_1, btn_2, btn_3, btn_4;
	JTextPane messageBox;
	JLabel label_open, label_save, label_close;
	JLabel label_1L, label_2L, label_3L, label_4L;
	JToggleButton tbtn1, tbtn2;
	
	private static ImageIcon icons[];
	static ImageIcon arrowR, arrowD, empty;
	
	int toggleSwitch = 0;
	private String[] btntitles;
	
	public static void setup(ImageIcon icons[]) {
		CommonGUI.icons = icons;
		arrowD = icons[4]; arrowR = icons[5];
	}
	
	public CommonGUI(Frame frame, String btntitles[]) {
		this.frame = frame;
		this.btntitles = btntitles;
		int y = 0;
		boolean togglebutton = (btntitles.length == 10);
		
		GridBagLayout grid = new GridBagLayout();
		GridBagConstraints c  = new GridBagConstraints();
		frame.setLayout(grid);
		
		label_open = new JLabel(""); label_open.setIcon(arrowD);
		c.gridx = 0; c.gridy = y++; c.gridwidth = 1; c.gridheight = 1; c.anchor = GridBagConstraints.NORTH;
		grid.setConstraints(label_open, c);
		//frame.getContentPane().add(label_open, "2, 2, center, default");
		frame.add(label_open);
		
		label_save = new JLabel(""); label_save.setIcon(arrowD);
		c.gridx = 1; 
		grid.setConstraints(label_save, c);
		//frame.getContentPane().add(label_save, "4, 2, center, default");
		frame.add(label_save);
		
		c.gridx = 2;
		label_close = new JLabel(""); label_close.setIcon(arrowD);
		//frame.getContentPane().add(label_close, "6, 2, center, default");
		grid.setConstraints(label_close, c);
		frame.add(label_close);
		
		c.gridx = 0; c.gridy = y++;
		btn_open = new JButton("");
		btn_open.setIcon(icons[0]);
		//frame.getContentPane().add(btn_open, "2, 4");
		grid.setConstraints(btn_open, c);
		frame.add(btn_open);
		btn_open.addActionListener(this);
		
		c.gridx = 1;
		btn_save = new JButton("");
		btn_save.setIcon(icons[1]);
		//frame.getContentPane().add(btn_save, "4, 4");
		grid.setConstraints(btn_save, c);
		frame.add(btn_save);
		btn_save.addActionListener(this);
		
		c.gridx = 2;
		btn_close = new JButton("");		
		btn_close.setIcon(icons[2]);
		//frame.getContentPane().add(btn_close, "6, 4");
		grid.setConstraints(btn_close, c);
		frame.add(btn_close);
		btn_close.addActionListener(this);
		
		c.gridx = 5; 
		btn_q = new JButton("");
		btn_q.setIcon(icons[3]);
		//frame.getContentPane().add(button_q, "10, 4");
		grid.setConstraints(btn_q, c);
		frame.add(btn_q);
		
		c.gridx = 0; c.gridy = y++; c.gridwidth = 6; c.ipady = 4; //c.weighty = 5; 
		JSeparator separator = new JSeparator();
		//frame.getContentPane().add(separator, "2, 6, 9, 1");
		grid.setConstraints(separator, c);
		frame.add(separator);
		
		
		if (togglebutton) {
			c.gridx = 0; c.gridy = y++; c.gridwidth = 3; c.ipady = 0; c.fill = GridBagConstraints.HORIZONTAL;
			tbtn1 = new JToggleButton(btntitles[8]);
			tbtn1.setSelected(true);
			grid.setConstraints(tbtn1, c);
			frame.add(tbtn1);
			tbtn1.addActionListener(this);
			
			c.gridx = 3; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL;
			tbtn2 = new JToggleButton(btntitles[9]);
			tbtn2.setSelected(false);
			grid.setConstraints(tbtn2, c);
			frame.add(tbtn2);
			tbtn2.addActionListener(this);
			
			toggleSwitch = 1;
		}
		
		c.gridx = 0; c.gridy = y++; c.gridwidth = 1;c.ipady = 8; c.fill = GridBagConstraints.HORIZONTAL; 
		label_1L = new JLabel(" "); label_1L.setIcon(arrowR);
		//frame.getContentPane().add(label_1L, "2, 8, center, default");
		grid.setConstraints(label_1L, c);
		frame.add(label_1L);
		
		c.gridx = 1; c.gridwidth = 4; c.fill = GridBagConstraints.BOTH;
		btn_1 = new Button(btntitles[0]);
		/* btn_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});*/
		//frame.getContentPane().add(btn_1, "4, 8, 5, 1");
		grid.setConstraints(btn_1, c);
		frame.add(btn_1);
		btn_1.addActionListener(this);
		
		c.gridx = 0; c.gridy = y++; c.gridwidth = 1;
		label_2L = new JLabel(""); label_2L.setIcon(arrowR);
		grid.setConstraints(label_2L, c);
		frame.add(label_2L);
		//frame.getContentPane().add(label_2L, "2, 10");
		
		c.gridx = 1; c.gridwidth = 4; c.fill = GridBagConstraints.BOTH;
		btn_2 = new Button(btntitles[1]);
		grid.setConstraints(btn_2, c);
		frame.add(btn_2);
		//frame.getContentPane().add(btn_2, "4, 10, 5, 1");
		btn_2.addActionListener(this);
		
		c.gridx = 0; c.gridy = y++; c.gridwidth = 1;
		label_3L = new JLabel(""); label_3L.setIcon(arrowR);
		grid.setConstraints(label_3L, c);
		frame.add(label_3L);
		//frame.getContentPane().add(label_3L, "2, 12");
		
		c.gridx = 1; c.gridwidth = 4; c.fill = GridBagConstraints.BOTH;
		btn_3 = new Button(btntitles[2]);
		grid.setConstraints(btn_3, c);
		frame.add(btn_3);
		//frame.getContentPane().add(btn_3, "4, 12, 5, 1");
		btn_3.addActionListener(this);
		
		c.gridx = 0; c.gridy = y++; c.gridwidth = 1;
		label_4L = new JLabel("");
		grid.setConstraints(label_4L, c); label_4L.setIcon(arrowR);
		frame.add(label_4L);
		//frame.getContentPane().add(label_4L, "2, 14");
		
		c.gridx = 1; c.gridwidth = 4; c.fill = GridBagConstraints.BOTH;
		btn_4 = new Button(btntitles[3]);
		grid.setConstraints(btn_4, c);
		frame.add(btn_4);
		//frame.getContentPane().add(btn_4, "4, 14, 5, 1");
		btn_4.addActionListener(this);
		
		/*
		c.gridx = 0; c.gridy = y++; c.gridwidth = 5; c.ipady = 4;
		JSeparator separator_1 = new JSeparator();
		grid.setConstraints(separator_1, c);
		frame.add(separator_1);
		//frame.getContentPane().add(separator_1, "2, 16, 9, 1");
		 */
		
		String spaces = "                      \n\n\n\n\n";
		
		c.gridx = 0; c.gridy = 8; c.gridwidth = 6; c.fill = GridBagConstraints.BOTH; c.anchor = GridBagConstraints.CENTER;
		c.ipadx = 0;
		messageBox = new JTextPane(); 
		messageBox.setText(spaces);
		messageBox.setEditable(false);
		
		grid.setConstraints(messageBox, c);
		frame.add(messageBox);
		//frame.getContentPane().add(messageBox, "2, 18, 9, 1, fill, fill");
		
		//frame.addWindowListener(new frameWindowListener());
		frame.pack();
		
		Dimension d = label_open.getSize();
		label_open.setMinimumSize(d); 
		d = messageBox.getSize();
		messageBox.setMinimumSize(d);
		
		/*
		d = btn_1.getMinimumSize();
		d.height = d.height * 3 / 2;
		btn_1.setMinimumSize(d);
		btn_2.setMinimumSize(d);
		btn_3.setMinimumSize(d);
		btn_4.setMinimumSize(d);
		*/
		
	}

	@Override public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		char btn = ' ';
		
		if (o == btn_open) btn = 'o';
		else if (o == btn_save) btn = 's';
		else if (o == btn_close) btn = 'c';
		else if (o == btn_1) btn = '1';
		else if (o == btn_2) btn = '2';
		else if (o == btn_3) btn = '3';
		else if (o == btn_4) btn = '4';
		else {
			if (o == tbtn1 && toggleSwitch == 2) {
				toggleSwitch = 1;
				btn_1.setLabel(btntitles[0]);
				btn_2.setLabel(btntitles[1]);
				btn_3.setLabel(btntitles[2]);
				btn_4.setLabel(btntitles[3]);
				
				tbtn1.setSelected(true);
				tbtn2.setSelected(false);
				btn = 't';
				
			} else if (o == tbtn2 && toggleSwitch == 1) {
				toggleSwitch = 2;
				btn_1.setLabel(btntitles[4]);
				btn_2.setLabel(btntitles[5]);
				btn_3.setLabel(btntitles[6]);
				btn_4.setLabel(btntitles[7]);
				
				tbtn1.setSelected(false);
				tbtn2.setSelected(true);
				btn = 't';
			}
		}
		
		if (btn != 't') {
			btnSwingWorker sw = new btnSwingWorker(btn);
			sw.execute();
		} else {
			afterActionPerformed(e);
		}
	}
	
	void resetLabelIcons()
	{
		
		label_open.setIcon(null);
		label_save.setIcon(null);
		label_close.setIcon(null);
		label_1L.setIcon(null);
		label_2L.setIcon(null);
		label_3L.setIcon(null);
		label_4L.setIcon(null);
		/*
		label_open.setVisible(false);
		label_save.setVisible(false);
		label_close.setVisible(false);
		label_1L.setVisible(false);
		label_2L.setVisible(false);
		label_3L.setVisible(false);
		label_4L.setVisible(false);
		*/
	}
	
	class btnSwingWorker extends IJIF_SwingWorker {
		private char btn;
		private String message;
		
		public btnSwingWorker(char btnType) 
		{
			btn = btnType;
			message = "";
			/*
			frame.getGlassPane().addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					e.consume();
				}
			});
			frame.getGlassPane().setVisible(true);
			*/
			btn_1.setEnabled(false);
			btn_2.setEnabled(false);
			btn_3.setEnabled(false);
			btn_4.setEnabled(false);
			btn_open.setEnabled(false);
			btn_save.setEnabled(false);
			btn_close.setEnabled(false);
			btn_q.setEnabled(false);
			if (tbtn1 != null) {
				tbtn1.setEnabled(false);
				tbtn2.setEnabled(false);
			}
			
		}
		
		@Override public Integer doInBackground() {
			int r = -1;
			IJIF.setCallback(this);
			
			r = executeCommand(btn);
			
			return (Integer)(r);
		}
		
		public void callback(String str) {
			if (str == null)
				message = "";
			else {
				message += str + "\n";
				publish(message);
			}
		}
		
		@Override protected void process(List<String> l) {
			messageBox.setText(l.get(0));
			frame.toFront();
		}
		
		@Override protected void done() {
			Integer r;
			
			try {
				r = get();
			} catch (Exception ex) {
				r = -1;
				IJX.error(ex.toString(), 0);
				ex.printStackTrace();
			}
			
			IJIF.setCallback(null);
			
			afterCommand(btn, r);
			/*
			frame.getGlassPane().setVisible(false);
            MouseListener[] listeners = frame.getGlassPane().getMouseListeners();
            
            for (MouseListener listener: listeners) {
                frame.getGlassPane().removeMouseListener(listener);
            }
            */
			btn_1.setEnabled(true);
			btn_2.setEnabled(true);
			btn_3.setEnabled(true);
			btn_4.setEnabled(true);
			btn_open.setEnabled(true);
			btn_save.setEnabled(true);
			btn_close.setEnabled(true);
			btn_q.setEnabled(true);
			if (tbtn1 != null) {
				tbtn1.setEnabled(true);
				tbtn2.setEnabled(true);
			}
		}
		
		
	}
	
	int executeCommand(char btn) { return 0; }
	void afterCommand(char btn, int r) {}
	void afterActionPerformed(ActionEvent e) {}

}