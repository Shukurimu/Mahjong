import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.SwingWorker;
import javax.swing.ImageIcon;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioSystem;

class Elem extends SwingWorker<String, Void> {
	public static final int[] seq = { 0, 1, 2, 3, 4, 1, 2, 3 };
	public static final int cW = 40;
	public static final int cH = 50;
	public static final int eCM = 5;
	public static final int WinS = 900;
	public static final int WinC = 450;
	public static final int eDice = 80;
	public static final int eBoard = 720;
	public static final int eSeats = 240;
	public static final int eFinFont = 36;
	public static final int eAccuFont = 16;
	public static final int eDiceFont = 48;
	public static final int eInfoFont = 24;
	public static final int eFinSizeW = 600;
	public static final int eFinSizeH = 100;
	public static final int ptsW = 96;
	public static final int ptsH = 12;
	public static final int ptsSW = 48;
	public static final int ptsSH = 12;
	public static final int pDeclFont = 54;
	public static final int pDeclaraW = 800;
	public static final int pDeclaraH = 200;
	public static final int hCtrlSize = 20;
	public static final int hSlctFont = 16;
	public static final int rYakuSize = 36;
	public static final int rYakuList = 300;
	public static final int rYakuFont = 26;
	public static final int rTokuFont = 64;
	public static final int rVaryFont = 26;
	public static final int rTimeFont = 18;
	public static final int rTimeSize = 80;
	public static final int pLifeFont = 24;
	public static final int pNakuFont = 32;
	public static final int pBbase = 100;
	public static final int pRbase = WinS - 60;
	public static final int pBtnWidth = 200;
	public static final int pBtnHeight = 50;
	public static final int btnGaps = 20;
	public static ImageIcon signhPic;
	public static ImageIcon signrPic;
	public static ImageIcon[] pt1000Pic;
	public static ImageIcon[][] seatPic;
	public static ImageIcon[][][] cardPic;
	
	@Override
	public String doInBackground() {
		int pro;
		System.out.println("Now Loading. Please Wait ...");
		long t0 = System.currentTimeMillis();
		java.net.URL iURL;
		pt1000Pic = new ImageIcon[5];
		seatPic = new ImageIcon[5][5];
		cardPic = new ImageIcon[40][4][3];
		BufferedImage img = null;
		this.setProgress(pro = 10);
		
		iURL = Game.class.getResource("pic/pt10001.png");
		pt1000Pic[1] = new ImageIcon((new ImageIcon(iURL)).getImage().getScaledInstance(ptsW, ptsH, Image.SCALE_DEFAULT));
		iURL = Game.class.getResource("pic/pt10002.png");
		pt1000Pic[2] = new ImageIcon((new ImageIcon(iURL)).getImage().getScaledInstance(ptsH, ptsW, Image.SCALE_DEFAULT));
		iURL = Game.class.getResource("pic/pt10003.png");
		pt1000Pic[3] = new ImageIcon((new ImageIcon(iURL)).getImage().getScaledInstance(ptsW, ptsH, Image.SCALE_DEFAULT));
		iURL = Game.class.getResource("pic/pt10004.png");
		pt1000Pic[4] = new ImageIcon((new ImageIcon(iURL)).getImage().getScaledInstance(ptsH, ptsW, Image.SCALE_DEFAULT));
		iURL = Game.class.getResource("pic/pt1000.png");
		signrPic = new ImageIcon((new ImageIcon(iURL)).getImage().getScaledInstance(ptsSW, ptsSH, Image.SCALE_DEFAULT));
		iURL = Game.class.getResource("pic/pt100.png");
		signhPic = new ImageIcon((new ImageIcon(iURL)).getImage().getScaledInstance(ptsSW, ptsSH, Image.SCALE_DEFAULT));
		this.setProgress(pro += 2);
		
		for (int i = 1; i < 5; ++i) {
			iURL = Game.class.getResource("pic/k" + i + "1.png");
			seatPic[i][1] = new ImageIcon((new ImageIcon(iURL)).getImage().getScaledInstance(eSeats, eSeats, Image.SCALE_DEFAULT));
			iURL = Game.class.getResource("pic/k" + i + "2.png");
			seatPic[i][2] = new ImageIcon((new ImageIcon(iURL)).getImage().getScaledInstance(eSeats, eSeats, Image.SCALE_DEFAULT));
			iURL = Game.class.getResource("pic/k" + i + "3.png");
			seatPic[i][3] = new ImageIcon((new ImageIcon(iURL)).getImage().getScaledInstance(eSeats, eSeats, Image.SCALE_DEFAULT));
			iURL = Game.class.getResource("pic/k" + i + "4.png");
			seatPic[i][4] = new ImageIcon((new ImageIcon(iURL)).getImage().getScaledInstance(eSeats, eSeats, Image.SCALE_DEFAULT));
			this.setProgress(pro += 3);
		}
		
		Color white = new Color(255, 255, 255, 128);
		Color black = new Color(  0,   0,   0,  64);
		for (int i = 10; i < 40; ++i) {
			iURL = Game.class.getResource("pic/" + (i*10 + 1) + ".png");
			try { img = ImageIO.read(iURL); } catch (IOException ex) { System.err.printf("Error: pic/%d.png\n", (i*10 + 1)); }
			cardPic[i][0][0] = new ImageIcon(img.getScaledInstance(cW, cH, Image.SCALE_DEFAULT));
			cardPic[i][0][1] = changeFilter(img, white, cW, cH);
			cardPic[i][0][2] = changeFilter(img, black, cW, cH);
			iURL = Game.class.getResource("pic/" + (i*10 + 2) + ".png");
			try { img = ImageIO.read(iURL); } catch (IOException ex) { System.err.printf("Error: pic/%d.png\n", (i*10 + 2)); }
			cardPic[i][1][0] = new ImageIcon(img.getScaledInstance(cH, cW, Image.SCALE_DEFAULT));
			cardPic[i][1][1] = changeFilter(img, white, cH, cW);
			cardPic[i][1][2] = changeFilter(img, black, cH, cW);
			iURL = Game.class.getResource("pic/" + (i*10 + 3) + ".png");
			try { img = ImageIO.read(iURL); } catch (IOException ex) { System.err.printf("Error: pic/%d.png\n", (i*10 + 3)); }
			cardPic[i][2][0] = new ImageIcon(img.getScaledInstance(cW, cH, Image.SCALE_DEFAULT));
			cardPic[i][2][1] = changeFilter(img, white, cW, cH);
			cardPic[i][2][2] = changeFilter(img, black, cW, cH);
			iURL = Game.class.getResource("pic/" + (i*10 + 4) + ".png");
			try { img = ImageIO.read(iURL); } catch (IOException ex) { System.err.printf("Error: pic/%d.png\n", (i*10 + 4)); }
			cardPic[i][3][0] = new ImageIcon(img.getScaledInstance(cH, cW, Image.SCALE_DEFAULT));
			cardPic[i][3][1] = changeFilter(img, white, cH, cW);
			cardPic[i][3][2] = changeFilter(img, black, cH, cW);
			this.setProgress(pro += 2);
		}
		
		for (int i = 1; i < 9; ++i) {
			iURL = Game.class.getResource("pic/" + (i*10 + 401) + ".png");
			try { img = ImageIO.read(iURL); } catch (IOException ex) { System.err.printf("Error: pic/%d.png\n", (i*10 + 401)); }
			cardPic[i][0][0] = new ImageIcon(img.getScaledInstance(cW, cH, Image.SCALE_DEFAULT));
			cardPic[i][0][1] = changeFilter(img, white, cW, cH);
			cardPic[i][0][2] = changeFilter(img, black, cW, cH);
			iURL = Game.class.getResource("pic/" + (i*10 + 402) + ".png");
			try { img = ImageIO.read(iURL); } catch (IOException ex) { System.err.printf("Error: pic/%d.png\n", (i*10 + 402)); }
			cardPic[i][1][0] = new ImageIcon(img.getScaledInstance(cH, cW, Image.SCALE_DEFAULT));
			cardPic[i][1][1] = changeFilter(img, white, cH, cW);
			cardPic[i][1][2] = changeFilter(img, black, cH, cW);
			iURL = Game.class.getResource("pic/" + (i*10 + 403) + ".png");
			try { img = ImageIO.read(iURL); } catch (IOException ex) { System.err.printf("Error: pic/%d.png\n", (i*10 + 403)); }
			cardPic[i][2][0] = new ImageIcon(img.getScaledInstance(cW, cH, Image.SCALE_DEFAULT));
			cardPic[i][2][1] = changeFilter(img, white, cW, cH);
			cardPic[i][2][2] = changeFilter(img, black, cW, cH);
			iURL = Game.class.getResource("pic/" + (i*10 + 404) + ".png");
			try { img = ImageIO.read(iURL); } catch (IOException ex) { System.err.printf("Error: pic/%d.png\n", (i*10 + 404)); }
			cardPic[i][3][0] = new ImageIcon(img.getScaledInstance(cH, cW, Image.SCALE_DEFAULT));
			cardPic[i][3][1] = changeFilter(img, white, cH, cW);
			cardPic[i][3][2] = changeFilter(img, black, cH, cW);
			this.setProgress(pro += 2);
		}
		
		return String.valueOf(System.currentTimeMillis() - t0);
	}
	
	@Override
	public void done() {
		return;
	}
	
	private static ImageIcon changeFilter(BufferedImage img, Color c, final int width, final int height) {
		java.awt.Graphics2D g2d = (java.awt.Graphics2D)img.getGraphics();
		g2d.setColor(c);
		g2d.fillRoundRect(0, 0, img.getWidth(), img.getHeight(), 5, 5);
		g2d.dispose();
		return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_DEFAULT));
	}
	
	public static ImageIcon createBackground(final Color c, final int width, final int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g2d = image.createGraphics();
		g2d.setColor(c);
		g2d.fillRect(0, 0, width, height);
		g2d.dispose();
		return new ImageIcon(image);
	}
	
	private static String[] wavs = { "pck01.wav" };
	public static void playAudio(final int which) {
		java.net.URL url = Game.class.getResource("pic/" + wavs[which]);
		try {
			Clip clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(url));
			clip.loop(1);
		} catch (Exception ex) {}
		return;
	}
	
}
