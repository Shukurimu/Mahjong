import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;

public class Hello extends JFrame implements Runnable, ActionListener, PropertyChangeListener {
	private final Font dialogFont = new Font(Font.MONOSPACED, Font.BOLD, 24);
	private JTextField nameField;
	private JCheckBox  localYaku;
	private JLayeredPane layeredPane;
	private JProgressBar progressBar;
	private int duration;
	
	public Hello() {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		this.setTitle("POI");
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLayout(null);
		this.setBounds(screen.width / 2 - 240, screen.height / 2 - 200, 480, 400);
		
		layeredPane = new JLayeredPane();
		this.setLayeredPane(layeredPane);
		
		nameField = new JTextField("Player", 18);
		nameField.setFont(dialogFont);
		nameField.setHorizontalAlignment(JTextField.CENTER);
		nameField.setBounds(60, 40, 360, 40);
		layeredPane.add(nameField);
		
		localYaku = new JCheckBox("\u8a08\u7b97\u5730\u65b9\u5f79", false);
		localYaku.setFont(dialogFont);
		localYaku.setBounds(40, 120, 400, 40);
		layeredPane.add(localYaku);
		
		JButton duration1 = new JButton("\u6771\u98a8\u6230");
		duration1.setFont(dialogFont);
		duration1.setBounds( 40, 240, 180, 40);
		duration1.setActionCommand("1");
		duration1.addActionListener(this);
		layeredPane.add(duration1);
		
		JButton duration2 = new JButton("\u6771\u5357\u6230");
		duration2.setFont(dialogFont);
		duration2.setBounds(260, 240, 180, 40);
		duration2.setActionCommand("2");
		duration2.addActionListener(this);
		layeredPane.add(duration2);
		
		this.setVisible(true);
	}
	
	public void run() {
		layeredPane.removeAll();
		int startSeat = (int)(Math.random() * 4) + 1;
		JLabel label = new JLabel("", JLabel.CENTER);
		label.setFont(dialogFont);
		label.setBounds(40, 40, 400, 80);
		layeredPane.add(label);
		switch (startSeat) {
			case 1:	label.setText("\u8d77\u59cb\u5ea7\u4f4d\uff1a\u6771");	break;
			case 2:	label.setText("\u8d77\u59cb\u5ea7\u4f4d\uff1a\u5357");	break;
			case 3:	label.setText("\u8d77\u59cb\u5ea7\u4f4d\uff1a\u897f");	break;
			case 4:	label.setText("\u8d77\u59cb\u5ea7\u4f4d\uff1a\u5317");	break;
		}
		
		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setBounds(40, 200, 400, 60);
		layeredPane.add(progressBar);
		layeredPane.repaint();
		
		Elem stdData = new Elem();
		stdData.addPropertyChangeListener(this);
		stdData.execute();
		try {
			System.out.println("Loading Time: " + stdData.get());
		} catch (Exception e) {}
		
		Game game = new Game(nameField.getText(), localYaku.isSelected(), 4, duration);
		progressBar.setValue(100);
		this.dispose();
		game.gameProgress(startSeat);
		return;
	}
	
	public void actionPerformed(ActionEvent e) {
		duration = Integer.parseInt(e.getActionCommand());
		(new Thread(this)).start();
		return;
	}
	
	public void propertyChange(PropertyChangeEvent pce) {
		if ("progress".equals(pce.getPropertyName())) {
			progressBar.setValue(((Integer) pce.getNewValue()).intValue());
		}
		return;
	}
	
	public static void main(String[] args) {
		// try {
			// System.setProperty("file.encoding", "shift-jis");
			// java.lang.reflect.Field charset = java.nio.charset.Charset.class.getDeclaredField("defaultCharset");
			// charset.setAccessible(true);
			// charset.set(null, null);
		// } catch (Exception ex) {}
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Hello();
				return;
			}
		});
		return;
	}
	
}
