import java.util.*;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.*;
import javax.swing.*;

class Dashboard extends JLayeredPane implements ActionListener {
	private final String stay = "<html><font color='white'>%8d</font></html>";
	private final String adds = "<html><font color='white'>%8d</font><font color='aqua'>%+d</font></html>";
	private final String subs = "<html><font color='white'>%8d</font><font color='red'>%+d</font></html>";
	private final int doraBase = (Elem.WinS - Elem.cW * 7) / 2;
	private final int pointBar = Elem.WinS / 10;
	private final int yakuBase = 5;
	private JPanel summaryPanel;
	private JLabel[] pointLabel;
	private static Card[][] doraCards;	// 懸賞指示牌
	private static int[][] doraNumber;	// 懸賞牌數字
	private static int doraIndex;		// 目前開到第幾張
	private boolean tLoop;
	private JButton timer;
	
	public Dashboard() {
		this.setVisible(false);
		this.setBounds(0, 0, Elem.WinS, Elem.WinS);
		JPanel defaultPanel = new JPanel(null);
		defaultPanel.setOpaque(false);
		defaultPanel.setBounds(0, 0, Elem.WinS, Elem.WinS);
		this.add(defaultPanel, JLayeredPane.DEFAULT_LAYER);
		
		timer = new JButton();
		timer.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Elem.rTimeFont));
		timer.setForeground(Color.LIGHT_GRAY);
		timer.setBorderPainted(false);
		timer.setContentAreaFilled(false);
		timer.setBounds(Elem.WinC - Elem.rTimeSize / 2, Elem.WinS - Elem.rYakuSize * 4, Elem.rTimeSize, Elem.rTimeSize);
		timer.addActionListener(this);
		defaultPanel.add(timer);
		JLabel wallpaper = new JLabel(Elem.createBackground(new Color(32, 32, 32, 196), Elem.eBoard, Elem.eBoard));
		wallpaper.setBounds((Elem.WinS - Elem.eBoard) / 2, (Elem.WinS - Elem.eBoard) / 2, Elem.eBoard, Elem.eBoard);
		defaultPanel.add(wallpaper);
		// 結算數字的標籤位置
		final Font varyFont = new Font(Font.SANS_SERIF, Font.BOLD, Elem.rVaryFont);
		pointLabel = new JLabel[5];
		pointLabel[1] = new JLabel("", JLabel.CENTER);
		pointLabel[1].setFont(varyFont);
		pointLabel[1].setBounds(Elem.WinC - pointBar * 3, Elem.WinS - Elem.rYakuSize * 5, pointBar * 6, Elem.rYakuSize);
		pointLabel[2] = new JLabel("", JLabel.RIGHT);
		pointLabel[2].setFont(varyFont);
		pointLabel[2].setBounds(Elem.WinC - pointBar * 3, Elem.WinS - Elem.rYakuSize * 6, pointBar * 6, Elem.rYakuSize);
		pointLabel[3] = new JLabel("", JLabel.CENTER);
		pointLabel[3].setFont(varyFont);
		pointLabel[3].setBounds(Elem.WinC - pointBar * 3, Elem.WinS - Elem.rYakuSize * 7, pointBar * 6, Elem.rYakuSize);
		pointLabel[4] = new JLabel("", JLabel.LEFT);
		pointLabel[4].setFont(varyFont);
		pointLabel[4].setBounds(Elem.WinC - pointBar * 3, Elem.WinS - Elem.rYakuSize * 6, pointBar * 6, Elem.rYakuSize);
		
		summaryPanel = new JPanel(null);
		summaryPanel.setOpaque(false);
		summaryPanel.setBounds(0, 0, Elem.WinS, Elem.WinS);
		this.add(summaryPanel, JLayeredPane.PALETTE_LAYER);
		
		doraCards = new Card[2][5];
		doraNumber = new int[2][5];
		return;
	}
	
	// 每一局開始時的表裏懸賞設置
	public void doraSetup(LinkedList<Card> yama) {
		doraIndex = 0;
		ListIterator<Card> it = yama.listIterator(4);
		doraCards[1][0] = it.next();
		doraCards[0][0] = it.next();
		doraCards[1][1] = it.next();
		doraCards[0][1] = it.next();
		doraCards[1][2] = it.next();
		doraCards[0][2] = it.next();
		doraCards[1][3] = it.next();
		doraCards[0][3] = it.next();
		doraCards[1][4] = it.next();
		doraCards[0][4] = it.next();
		for (int i = 0; i < 5; ++i) {
			Card c = doraCards[0][i];
			doraNumber[0][i] = (c.vi == 0) ? ((c.vj == 4) ? 1 : ((c.vj == 7) ? 5 : (c.vj + 1))) : ((c.vj == 9) ? 1 : (c.vj + 1));
			Card d = doraCards[1][i];
			doraNumber[1][i] = (d.vi == 0) ? ((d.vj == 4) ? 1 : ((d.vj == 7) ? 5 : (d.vj + 1))) : ((d.vj == 9) ? 1 : (d.vj + 1));
		}
		return;
	}
	
	// 槓牌之後開懸賞
	public void doraOpen() {
		Card c = doraCards[0][doraIndex];
		c.setStable();
		++Game.kantsuCount[0];
		++Game.kantsuCount[Game.currentPlayer];
		++Game.appearance[c.vi][c.vj];
		++doraIndex;
		Game.suukansanra = (Game.kantsuCount[0] == 4)
				&& (Game.kantsuCount[1] < 4) && (Game.kantsuCount[2] < 4)
				&& (Game.kantsuCount[3] < 4) && (Game.kantsuCount[4] < 4);
		return;
	}
	
	// 此張牌表面上值多少懸賞飜數(表+赤)
	public static int doraWeight(Card x) {
		int weight = (x.value & 1);
		for (int i = 0; i < doraIndex; ++i) {
			if (x.vi == doraCards[0][i].vi && x.vj == doraNumber[0][i])
				++weight;
		}
		return weight;
	}
	
	// 計算目前持有的懸賞牌數量、focur可以是null [0]表 [1]裏 [2]赤
	public static int[] doraCount(int[][] hold, LinkedList<Furo> furo, ArrayList<Card> fuda, Card focus) {
		int[] doras = new int[3];
		if (focus != null)	++hold[focus.vi][focus.vj];
		for (int i = 0; i < doraIndex; ++i) {
			doras[0] += hold[doraCards[0][i].vi][doraNumber[0][i]];
			doras[1] += hold[doraCards[1][i].vi][doraNumber[1][i]];
		}
		if (focus != null)	--hold[focus.vi][focus.vj];
		for (Furo f: furo)	doras[2] += f.hasAkadora ? 1 : 0;
		for (Card c: fuda)	doras[2] += (c.value & 1);
		if (focus != null)	doras[2] += (focus.value & 1);
		return doras;
	}
	
	// 和牌之後的點數計算畫面
	public void printAgari(Player player, final boolean tsumo) {
		player.openFuda();
		Bunkai result = player.getBunkai(tsumo);
		this.setVisible(true);
		for (int i = 0; i < 7; ++i) {
			JLabel label = new JLabel((i >= 2 && i < (2 + doraIndex)) ? doraCards[0][i-2].showIcon() : Elem.cardPic[8][0][0]);
			label.setBounds(doraBase + Elem.cW * i, Elem.rYakuSize * (yakuBase - 2), Elem.cW, Elem.cH);
			summaryPanel.add(label);
		}
		summaryPanel.repaint();
		final Font yakuFont = new Font(Font.SERIF, Font.BOLD, Elem.rYakuFont);
		// fansuu如果大於512表示役滿
		boolean common = result.fansuu < 512;
		int index = 0;
		for (Iterator<Yaku> it = result.list.iterator(); it.hasNext(); ) {
			Yaku g = it.next();
			try {
				Thread.sleep((g == Yaku.UraDora) ? 1800 : 900);
			} catch (InterruptedException e) {}
			JLabel a = new JLabel(g.label,  JLabel.LEFT);
			a.setBounds(Elem.WinC - Elem.rYakuList, Elem.rYakuSize * (yakuBase + index), Elem.rYakuList * 2, Elem.rYakuSize);
			a.setFont(yakuFont);	a.setForeground(Color.WHITE);
			summaryPanel.add(a);
			JLabel b = new JLabel(g.text(), JLabel.RIGHT);
			b.setBounds(Elem.WinC - Elem.rYakuList, Elem.rYakuSize * (yakuBase + index), Elem.rYakuList * 2, Elem.rYakuSize);
			b.setFont(yakuFont);	b.setForeground(Color.WHITE);
			summaryPanel.add(b);
			summaryPanel.repaint();
			++index;
		}
		// 立直開裏懸賞
		if (player.richied()) {
			for (int i = 0; i < 7; ++i) {
				JLabel label = new JLabel((i >= 2 && i < (2 + doraIndex)) ? doraCards[1][i-2].showIcon() : Elem.cardPic[8][0][0]);
				label.setBounds(doraBase + Elem.cW * i, Elem.rYakuSize * (yakuBase + index + 1), Elem.cW, Elem.cH);
				summaryPanel.add(label);
			}
			summaryPanel.repaint();
		}
		try { Thread.sleep(720); } catch (InterruptedException e) {}
		
		int basicPoint = tensu(result.fansuu, result.fusuu);
		int wholePoint = carry(basicPoint * (Game.player[player.PID].isOya() ? 6 : 4));
		JLabel z = new JLabel("", JLabel.CENTER);
		if (common) {
			String ptsInfo = "%2d \u98DC%3d \u7B26            ";
			switch (result.fansuu) {
				case 1:
				case 2:
					ptsInfo += "\u3000\u3000\u3000\u3000";	break;
				case 3:
					ptsInfo += ((result.fusuu >= 70) ? "\u6E80\u8CAB\u3000\u3000" : "\u3000\u3000\u3000\u3000");	break;
				case 4:
					ptsInfo += ((result.fusuu >= 40) ? "\u6E80\u8CAB\u3000\u3000" : "\u3000\u3000\u3000\u3000");	break;
				case 5:
					ptsInfo += "\u6E80\u8CAB\u3000\u3000";	break;
				case 6:
				case 7:
					ptsInfo += "\u8DF3\u6E80\u3000\u3000";	break;
				case 8:
				case 9:
				case 10:
					ptsInfo += "\u500D\u6E80\u3000\u3000";	break;
				case 11:
				case 12:
					ptsInfo += "\u4E09\u500D\u6E80\u3000";	break;
				default:
					ptsInfo += "\u6570\u3048\u5F79\u6E80";	break;
			}
			ptsInfo += "%9d\u70B9";
			z.setText(String.format(ptsInfo, result.fansuu, result.fusuu, wholePoint));
		} else {
			z.setText(String.format("\u5F79\u3000\u6E80\u3000%9d\u70B9", wholePoint));
		}
		
		z.setFont(yakuFont);
		z.setForeground(Color.WHITE);
		z.setBounds(Elem.WinC - Elem.rYakuList, Elem.rYakuSize * (yakuBase + index + 2), Elem.rYakuList * 2, Elem.rYakuSize * 2);
		summaryPanel.add(z);
		
		int[] diffPoints = new int[5];
		if (!common && (result.sekinin > 0)) {
			basicPoint = tensu(result.sekinin, 0);
			wholePoint = carry(basicPoint * (Game.player[player.PID].isOya() ? 3 : 2));
			if (tsumo) {
				diffPoints[result.sekinin & 15] -= wholePoint << 1;
			} else {
				diffPoints[result.sekinin & 15] -= wholePoint;
				diffPoints[Game.currentPlayer] -= wholePoint;
				diffPoints[player.PID] += wholePoint << 1;
			}
			basicPoint = tensu(result.fansuu + 9 - result.sekinin, 0);
			wholePoint = carry(basicPoint * (Game.player[player.PID].isOya() ? 6 : 4));
		}
		
		int ratio = (player.isOya()) ? 2 : 1;
		if (tsumo) {
			switch (player.PID) {
				case 1:
					diffPoints[2] -= (carry(basicPoint * (Game.player[2].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[3] -= (carry(basicPoint * (Game.player[3].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[4] -= (carry(basicPoint * (Game.player[4].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[1] += (Game.tableRichi * 1000 - diffPoints[2] - diffPoints[3] - diffPoints[4]);
					break;
				case 2:
					diffPoints[3] -= (carry(basicPoint * (Game.player[3].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[4] -= (carry(basicPoint * (Game.player[4].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[1] -= (carry(basicPoint * (Game.player[1].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[2] += (Game.tableRichi * 1000 - diffPoints[3] - diffPoints[4] - diffPoints[1]);
					break;
				case 3:
					diffPoints[4] -= (carry(basicPoint * (Game.player[4].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[1] -= (carry(basicPoint * (Game.player[1].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[2] -= (carry(basicPoint * (Game.player[2].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[3] += (Game.tableRichi * 1000 - diffPoints[4] - diffPoints[1] - diffPoints[2]);
					break;
				case 4:
					diffPoints[1] -= (carry(basicPoint * (Game.player[1].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[2] -= (carry(basicPoint * (Game.player[2].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[3] -= (carry(basicPoint * (Game.player[4].isOya() ? 2 : ratio)) + Game.tableHonba * 100);
					diffPoints[4] += (Game.tableRichi * 1000 - diffPoints[1] - diffPoints[2] - diffPoints[3]);
					break;
			}
		} else {
			diffPoints[player.PID] += (wholePoint + Game.tableHonba * 300 + Game.tableRichi * 1000);
			diffPoints[Game.currentPlayer] += (-wholePoint - Game.tableHonba * 300);
		}
		Game.tableRichi = 0;
		
		for (int i = 1; i < 5; ++i) {
			if (diffPoints[i] == 0) {
				pointLabel[i].setText(String.format(stay, Game.pts[i]));
			} else {
				pointLabel[i].setText(String.format((diffPoints[i] > 0) ? adds : subs, Game.pts[i], diffPoints[i]));
			}
			Game.pts[i] += diffPoints[i];
		}
		
		summaryPanel.add(pointLabel[1]);
		summaryPanel.add(pointLabel[2]);
		summaryPanel.add(pointLabel[3]);
		summaryPanel.add(pointLabel[4]);
		summaryPanel.repaint();
		startTimer();
		return;
	}
	
	// 流局狀況
	public void printNagashi() {
		this.setVisible(true);
		
		int numOfTenpai = 0;
		String message;
		boolean isNagashimangan = false;
		
		int[] diffPoints = new int[5];
		for (int i = 1; i < 5; ++i) {
			if (Game.player[i].isTenpai()) {
				Game.player[i].openFuda();
				++numOfTenpai;
			}
			if (Game.player[i].isNagashimangan()) {
				isNagashimangan = true;
				if (Game.player[i].isOya()) {
					diffPoints[i] += 4000 * Game.PlayerNum;
					diffPoints[1] -= 4000;
					diffPoints[2] -= 4000;
					diffPoints[3] -= 4000;
					diffPoints[4] -= 4000;
				} else {
					diffPoints[i] += 2000 * (Game.PlayerNum + 1);
					diffPoints[1] -= (Game.player[1].isOya()) ? 4000 : 2000;
					diffPoints[2] -= (Game.player[2].isOya()) ? 4000 : 2000;
					diffPoints[3] -= (Game.player[3].isOya()) ? 4000 : 2000;
					diffPoints[4] -= (Game.player[4].isOya()) ? 4000 : 2000;
				}
			}
		}
		
		if (isNagashimangan) {
			message = "\u6D41\u3000\u3000\u3057\u3000\u3000\u6E80\u3000\u3000\u8CAB";
		} else {
			message = "\u6D41\u3000\u5C40";
			switch (numOfTenpai) {
				case 1:
					diffPoints[1] += Game.player[1].isTenpai() ? 3000 : -1000;
					diffPoints[2] += Game.player[2].isTenpai() ? 3000 : -1000;
					diffPoints[3] += Game.player[3].isTenpai() ? 3000 : -1000;
					diffPoints[4] += Game.player[4].isTenpai() ? 3000 : -1000;
					break;
				case 2:
					diffPoints[1] += Game.player[1].isTenpai() ? 1500 : -1500;
					diffPoints[2] += Game.player[2].isTenpai() ? 1500 : -1500;
					diffPoints[3] += Game.player[3].isTenpai() ? 1500 : -1500;
					diffPoints[4] += Game.player[4].isTenpai() ? 1500 : -1500;
					break;
				case 3:
					diffPoints[1] += Game.player[1].isTenpai() ? 1000 : -3000;
					diffPoints[2] += Game.player[2].isTenpai() ? 1000 : -3000;
					diffPoints[3] += Game.player[3].isTenpai() ? 1000 : -3000;
					diffPoints[4] += Game.player[4].isTenpai() ? 1000 : -3000;
					break;
				default:	;
			}
		}
		
		JLabel z = new JLabel(message, JLabel.CENTER);
		z.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Elem.rTokuFont));
		z.setForeground(Color.WHITE);
		z.setBounds(Elem.WinC - Elem.rYakuList, Elem.rYakuSize * (yakuBase + 4), Elem.rYakuList * 2, Elem.rYakuSize * 2);
		summaryPanel.add(z);
		
		for (int i = 1; i < 5; ++i) {
			if (diffPoints[i] == 0) {
				pointLabel[i].setText(String.format(stay, Game.pts[i]));
			} else {
				pointLabel[i].setText(String.format((diffPoints[i] > 0) ? adds : subs, Game.pts[i], diffPoints[i]));
			}
			Game.pts[i] += diffPoints[i];
		}
		
		summaryPanel.add(pointLabel[1]);
		summaryPanel.add(pointLabel[2]);
		summaryPanel.add(pointLabel[3]);
		summaryPanel.add(pointLabel[4]);
		
		startTimer();
		return;
	}
	
	// 其他狀況
	public void printSpecial(String s) {
		this.setVisible(true);
		
		JLabel z = new JLabel(s, JLabel.CENTER);
		z.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Elem.rTokuFont));
		z.setForeground(Color.WHITE);
		z.setBounds(0, Elem.rYakuSize * (yakuBase + 3), Elem.WinS, Elem.rYakuSize * 4);
		summaryPanel.add(z);
		
		startTimer();
		return;
	}
	
	private int carry(int point) {
		return ((point % 100 > 0) ? ((point / 100 + 1) * 100) : point);
	}
	
	public static int tensu(int fansuu, int fusuu) {
		if (fansuu < 512) {
			switch (fansuu) {
				case  0:	return (fusuu *  4);
				case  1:	return (fusuu *  8);
				case  2:	return (fusuu * 16);
				case  3:	return ((fusuu > 60) ? 2000 : (fusuu * 32));
				case  4:	return ((fusuu > 30) ? 2000 : (fusuu * 64));
				case  5:	return 2000;
				case  6:
				case  7:	return 3000;
				case  8:
				case  9:
				case 10:	return 4000;
				case 11:
				case 12:	return 6000;
				default:
					if (fansuu < 26)	return  8000;
					if (fansuu < 39)	return 16000;
					if (fansuu < 52)	return 24000;
					else				return 32000;
			}
		} else {
			return ((fansuu >> 9) * 8000);
		}
	}
	
	// GUI倒數按鈕開始
	public void startTimer() {
		int i = 99;
		timer.setText("OK" + i);
		tLoop = true;
		summaryPanel.repaint();
		synchronized (this) {
			try {
				while (tLoop && (i > 0)) {
					timer.setText("OK" + (--i));
					this.wait(999);
				}
			} catch (InterruptedException e) {}
		}
		summaryPanel.removeAll();
		this.setVisible(false);
		return;
	}
	
	public void actionPerformed(ActionEvent e) {
		synchronized (this) {
			tLoop = false;
			this.notify();
		}
		return;
	}
	
}
