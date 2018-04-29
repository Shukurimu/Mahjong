import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class Game extends JFrame implements ActionListener, KeyEventDispatcher {
	private final int[] takeS = { 0, 102, 68, 34, 0, 102, 68, 34 };
	private final int[] unitX = { 0, Elem.cW, 0, -Elem.cW, 0 };
	private final int[] unitY = { 0, 0, Elem.cW, 0, -Elem.cW };
	private final int[] anchorX =
	{ 0, Elem.cW*2 + Elem.eCM, Elem.WinS - Elem.cW*2 - Elem.cH - Elem.eCM, Elem.WinS - Elem.cW*3 - Elem.eCM, Elem.cW*2 + Elem.eCM };
	private final int[] anchorY =
	{ 0, Elem.cW*2 + Elem.eCM, Elem.cW*2 + Elem.eCM, Elem.WinS - Elem.cW*2 - Elem.cH - Elem.eCM, Elem.WinS - Elem.cW*3 - Elem.eCM };
	private final int[][] seat2menfonn = { {0}, {0, 1, 2, 3, 4}, {0, 4, 1, 2, 3}, {0, 3, 4, 1, 2}, {0, 2, 3, 4, 1} };
	
	private Dashboard calcLayer;
	private CenterBlock info;
	private JPanel cardLayer;
	private JLabel signHonba;
	private JLabel signRichi;
	private JDialog dialog;
	private int currentOya;
	private int seatMapping;
	public static React[] reaction = new React[5];	// 玩家做出的行動
	public static Player[] player = new Player[5];
	public static boolean[]     isAI = { false, false,  true,  true,  true };	// 要不要讓AI打
	public static boolean[] showFuda = { false,  true, false, false, false };	// 要不要顯示手牌
	
	/* 以下static變數是每位玩家都可以知道的資訊 */
	public static int PlayerNum;	// 玩家數量
	public static int tableHonba;	// 桌面本場棒
	public static int tableRichi;	// 桌面點棒
	public static int remainCard;	// 剩餘牌數
	public static int allLast;		// 0-結束 1-AllLast 2-還早
	public static boolean[] ippatsu;	// 玩家目前是不是一發
	public static boolean calcLocal;	// 是否計算地方役
	public static boolean suukansanra;	// 是否達成四槓散了條件
	public static boolean restartable;	// 可不可以重新此局
	public static int currentBakaze;	// 當前場風
	public static int currentPlayer;	// 當前玩家ID
	public static int[]         pts;	// 玩家的點數
	public static int[] kantsuCount;	// 槓子數 [0]全部 [1~4]各家
	public static int[][] appearance;	// 目前出現過的牌數量(包含懸賞指示牌)
	public static int duration;	// 玩的局數(開始程式時的選項)
	
	public Game(String inputName, boolean l, int p, int d) {
		calcLocal = l;
		PlayerNum = p;
		duration = d;
		JLayeredPane layeredPane = new JLayeredPane();
		this.setLayeredPane(layeredPane);
		this.setTitle("\u30cb\u30bb\u5929\u9cf3");
		this.setSize(Elem.WinS + 6, Elem.WinS + 27 + Elem.hCtrlSize);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JLabel wallpaper = new JLabel(Elem.createBackground(Color.GRAY, Elem.WinS, Elem.WinS + Elem.hCtrlSize));
		wallpaper.setBounds(0, 0, Elem.WinS, Elem.WinS + Elem.hCtrlSize);
		wallpaper.setOpaque(true);
		this.getRootPane().add(wallpaper);
		
		JPanel deskLayer = new JPanel(null);
		deskLayer.setBounds(0, 0, Elem.WinS, Elem.WinS);
		deskLayer.setOpaque(false);
		layeredPane.add(deskLayer, JLayeredPane.DEFAULT_LAYER);
		
		signHonba = new JLabel(String.format("%2d", 0), Elem.signhPic, JLabel.CENTER);
		signHonba.setFont(new Font(Font.SERIF, Font.BOLD, Elem.eAccuFont));
		signHonba.setForeground(Color.WHITE);
		signHonba.setBounds(Elem.WinC - Elem.ptsSW, Elem.WinC - Elem.eDice / 2 - Elem.ptsSH * 2, Elem.ptsSW * 2, Elem.ptsSH);
		deskLayer.add(signHonba);
		signRichi = new JLabel(String.format("%2d", 0), Elem.signrPic, JLabel.CENTER);
		signRichi.setFont(new Font(Font.SERIF, Font.BOLD, Elem.eAccuFont));
		signRichi.setForeground(Color.WHITE);
		signRichi.setBounds(Elem.WinC - Elem.ptsSW, Elem.WinC + Elem.eDice / 2 + Elem.ptsSH, Elem.ptsSW * 2, Elem.ptsSH);
		deskLayer.add(signRichi);
		
		cardLayer = new JPanel(null);
		cardLayer.setBounds(0, 0, Elem.WinS, Elem.WinS);
		cardLayer.setOpaque(false);
		layeredPane.add(cardLayer, JLayeredPane.PALETTE_LAYER);
		layeredPane.add(info = new CenterBlock(), JLayeredPane.MODAL_LAYER);
		
		layeredPane.add(calcLayer = new Dashboard(), JLayeredPane.POPUP_LAYER);
		layeredPane.add(player[1] = new Player(1, deskLayer, inputName), new Integer(259));
		layeredPane.add(player[2] = new Player(2, deskLayer, "Com - 1"), new Integer(257));
		layeredPane.add(player[3] = new Player(3, deskLayer, "Com - 2"), new Integer(255));
		layeredPane.add(player[4] = new Player(4, deskLayer, "Com - 3"), new Integer(253));
		
		dialog = new JDialog(this, "");
		dialog.setVisible(false);
		allLast = 2;
		currentBakaze = 1;
		tableHonba = 0;
		seatMapping = 1;
		tableRichi = 0;
		pts = new int[5];
		Arrays.fill(pts, 25000);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
	}
	
	public void gameProgress(int startWind) {
		currentOya = (((startWind & 1) == 1) ? startWind : (startWind ^ 6));
		this.setVisible(true);
		// Game Start
		ArrayList<Card> deck = new ArrayList<Card>(136);
		int[] cardNumber = { 1, 2, 3, 4, 6, 7, 8, 9 };
		for (int j = 1; j < 8; ++j) {
			deck.add(new Card(0, j));
			deck.add(new Card(0, j));
			deck.add(new Card(0, j));
			deck.add(new Card(0, j));
		}
		for (int i = 1; i < 4; ++i) {
			for (int j: cardNumber) {
				deck.add(new Card(i, j));
				deck.add(new Card(i, j));
				deck.add(new Card(i, j));
				deck.add(new Card(i, j));
			}
			deck.add(new Card(i, 0));
			deck.add(new Card(i, 5));
			deck.add(new Card(i, 5));
			deck.add(new Card(i, 5));
		}
		do {
			eachRound(deck);
			System.out.println("new round");
		} while ((pts[1] >= 0) && (pts[2] >= 0) && (pts[3] >= 0) && (pts[4] >= 0) && (allLast > 0));
		
		// Game Over
		JLayeredPane layeredPane = this.getLayeredPane();
		layeredPane.removeAll();
		player[Elem.seq[startWind + 0]].cumux = pts[Elem.seq[startWind + 0]] + 4;
		player[Elem.seq[startWind + 1]].cumux = pts[Elem.seq[startWind + 1]] + 3;
		player[Elem.seq[startWind + 2]].cumux = pts[Elem.seq[startWind + 2]] + 2;
		player[Elem.seq[startWind + 3]].cumux = pts[Elem.seq[startWind + 3]] + 1;
		Arrays.sort(player, 1, 5);
		
		float[] floating = new float[5];
		pts[player[1].PID] += tableRichi * 1000;
		boolean even = (pts[player[1].PID] == pts[player[2].PID]);
		floating[1] = pts[player[1].PID] - 30000 + (even ? 30000 : 40000);
		floating[2] = pts[player[2].PID] - 30000 + (even ? 20000 : 10000);
		floating[3] = pts[player[3].PID] - 30000 - 10000;
		floating[4] = pts[player[4].PID] - 30000 - 20000;
		
		final Font finalFont = new Font(Font.SANS_SERIF, Font.BOLD, Elem.pNakuFont);
		JLabel[] namelb = new JLabel[5];
		JLabel[] ending = new JLabel[5];
		JLabel[] points = new JLabel[5];
		ending[0] = new JLabel("\u7D42\u3000\u5C40", JLabel.CENTER);
		ending[0].setFont(new Font(Font.MONOSPACED, Font.BOLD, Elem.rTokuFont));
		ending[0].setForeground(Color.WHITE);
		ending[0].setBounds(Elem.WinC - Elem.eFinSizeW / 2, Elem.eFinSizeH, Elem.eFinSizeW, Elem.eFinSizeH);
		layeredPane.add(ending[0], JLayeredPane.POPUP_LAYER);
		for (int i = 1; i < 5; ++i) {
			namelb[i] = new JLabel(player[i].name, JLabel.LEFT);
			namelb[i].setFont(finalFont);
			namelb[i].setForeground(Color.WHITE);
			namelb[i].setBounds(Elem.WinC - Elem.eFinSizeW / 2, Elem.eFinSizeH * (1 + i), Elem.eFinSizeW, Elem.eFinSizeH);
			layeredPane.add(namelb[i], JLayeredPane.MODAL_LAYER);
			ending[i] = new JLabel(String.format("%7d", pts[player[i].PID]), JLabel.CENTER);
			ending[i].setFont(finalFont);
			ending[i].setForeground(Color.WHITE);
			ending[i].setBounds(Elem.WinC - Elem.eFinSizeW / 2, Elem.eFinSizeH * (1 + i), Elem.eFinSizeW, Elem.eFinSizeH);
			layeredPane.add(ending[i], JLayeredPane.MODAL_LAYER);
			points[i] = new JLabel(String.format("%+6.1f", floating[i] / 1000), JLabel.RIGHT);
			points[i].setFont(finalFont);
			points[i].setForeground((floating[i] >= 0) ? Color.CYAN : Color.RED);
			points[i].setBounds(Elem.WinC - Elem.eFinSizeW / 2, Elem.eFinSizeH * (1 + i), Elem.eFinSizeW, Elem.eFinSizeH);
			layeredPane.add(points[i], JLayeredPane.MODAL_LAYER);
		}
		
		JButton endb = new JButton("<<< END >>>");
		endb.setFont(finalFont);
		endb.setForeground(Color.PINK);
		endb.setBounds(Elem.WinC - 200, Elem.WinS - 200, 400, 100);
		endb.setContentAreaFilled(false);
		endb.setFocusable(false);
		endb.addActionListener(this);
		layeredPane.add(endb, JLayeredPane.PALETTE_LAYER);
		layeredPane.repaint();
		return;
	}
	
	private final int RULESUFON = 1000;
	private final int RULESUCHA = 1100;
	private final int RULESUKAN = 1200;
	
	private void eachRound(ArrayList<Card> deck) {
		if ((seatMapping == PlayerNum) && (currentBakaze == duration))
			allLast = 1;
		ippatsu = new boolean[5];
		kantsuCount = new int[5];
		appearance = new int[4][10];
		
		signHonba.setText(String.format("%2d", tableHonba));
		signRichi.setText(String.format("%2d", tableRichi));
		info.hideInfo();
		info.updatePoints();
		for (int i = 1; i < 5; ++i)
			player[i].resetPlayer(seat2menfonn[currentOya][i]);
		
		restartable = true;
		suukansanra = false;
		currentPlayer = currentOya;
		int startIndex = info.diceRandom();
		// 洗牌
		Collections.shuffle(deck);
		// 是否配牌
		if (false) {
			boolean cheatSuccess = true;
			for (Card c: deck)	c.owner = -1;
			int[][] available = {{67, 4, 4, 4, 4, 4, 4, 4, 0, 0 },
								{  1, 4, 4, 4, 4, 3, 4, 4, 4, 4 },
								{  1, 4, 4, 4, 4, 3, 4, 4, 4, 4 },
								{  1, 4, 4, 4, 4, 3, 4, 4, 4, 4 }};
			final int[] sequence = { 0, 1, 2, 3,16,17,18,19,32,33,34,35,48,52,
									 4, 5, 6, 7,20,21,22,23,36,37,38,39,49,
									 8, 9,10,11,24,25,26,27,40,41,42,43,50,
									12,13,14,15,28,29,30,31,44,45,46,47,51,
									134,135,132,133,130,131,128,129,126,127,124,125,122,123};
			int[] specifiedOrder = new int[136];
			try (Scanner scanner = new Scanner(new java.io.File("cheat.txt"), "utf-8")) {
				for (int s: sequence) {
					int x = s + startIndex;
					int i = scanner.nextInt();
					cheatSuccess &= (--available[i / 10][i % 10]) >= 0;
					specifiedOrder[(x > 135) ? (x - 136) : x] = i;
				}
			} catch (Exception ex) {
				cheatSuccess = false;
			}
			if (cheatSuccess) {
				for (int i = 0; i < 136; ++i)
					if (specifiedOrder[i] != 0) {
						for (int j = 0; j < 136; ++j) {
							if (deck.get(j).cpath == specifiedOrder[i] && deck.get(j).owner == -1) {
								deck.get(j).owner = 0;
								Collections.swap(deck, j, i);
								break;
							}
						}
					}
			} else
				System.out.println("Wrong or Illegal format in cheat.txt");
		}
		for (ListIterator<Card> it = deck.listIterator(); it.hasNext(); ) {
			int i = it.nextIndex();
			int j = (i / 34) + 1;
			int k = (i % 34) / 2;
			it.next().setAsYama(cardLayer, j, anchorX[j] + unitX[j] * k, anchorY[j] + unitY[j] * k);
		}
		// GUI嶺上牌
		Collections.swap(deck, startIndex - 1, startIndex - 2);
		Collections.swap(deck, startIndex - 3, startIndex - 4);
		// 牌山
		LinkedList<Card> yama = new LinkedList<Card>();
		for (ListIterator<Card> it = deck.listIterator(startIndex); it.hasPrevious(); yama.add(it.previous()))	;
		for (ListIterator<Card> it = deck.listIterator(deck.size()); yama.size() < 136; yama.add(it.previous()));
		calcLayer.doraSetup(yama);
		calcLayer.doraOpen();
		cardLayer.repaint();
		// 發牌
		try {
			Thread.sleep(640);
			for (int i = 0; i < 4; ++i) {
				for (int j = currentOya; j < currentOya + 4; ++j) {
					for (int k = 0; k < ((i < 3) ? 4 : 1); k++)
						player[Elem.seq[j]].distribute(yama.removeLast());
					Thread.sleep(120);
				}
			}
			remainCard = (PlayerNum == 4) ? 70 : 55;
			Thread.sleep(720);
		} catch (InterruptedException ie) {}
		
		player[1].sortFuda();
		player[2].sortFuda();
		player[3].sortFuda();
		player[4].sortFuda();
		info.showInfo();
		kantsuCount[0] = 0;
		int  endByRule = 0;
		int drawStatus = 0;
		boolean  turnEnded = false;
		boolean kanPending = false;
		Arrays.fill(reaction, React.doNothing);
		do {
			info.changeIcon(currentOya, currentPlayer);
			// 四風連打
			if (restartable && (remainCard == (((PlayerNum == 4) ? 66 : 51)))) {
				if ((player[1].firstKawa() & player[2].firstKawa() & player[3].firstKawa() & player[4].firstKawa()) != 0) {
					endByRule = RULESUFON;
					break;
				}
				restartable = false;
			}
			// 本人回合的動作
			switch (drawStatus) {
				case 0:	// 正常
					--remainCard;
					player[currentPlayer].drawCard(yama.removeLast(), false);
					player[currentPlayer].selfdecide();
					break;
				case 1:	// 吃碰後捨牌
					player[currentPlayer].chiponDiscard();
					break;
				case 2:	// 加槓、開槓
					kanPending = true;
					--remainCard;
					player[currentPlayer].drawCard(yama.removeFirst(), true);
					player[currentPlayer].selfdecide();
					break;
				case 3:	// 暗槓 直接開dora
					calcLayer.doraOpen();
					kanPending = false;
					--remainCard;
					player[currentPlayer].drawCard(yama.removeFirst(), true);
					player[currentPlayer].selfdecide();
					break;
			}
			drawStatus = 0;
			
			React cp = reaction[currentPlayer];
			switch (cp.kind) {
				case React.PASSS:
				case React.KIRII:
					ippatsu[currentPlayer] = false;
					break;
				case React.RICHI:
					ippatsu[currentPlayer] = true;
					player[cp.who].actionLabel.action(5);
					player[currentPlayer].doRichiAction();
					cp = reaction[currentPlayer];
					break;
				case React.ANKAN:
					ippatsu[currentPlayer] = false;
					player[cp.who].actionLabel.action(2);
					Arrays.fill(reaction, React.doNothing);
					drawStatus = 3;
					restartable = false;
					turnEnded |= checkChankan(player[currentPlayer].doAnkan(cp), true);
					break;
				case React.KAKAN:
					player[cp.who].actionLabel.action(2);
					Arrays.fill(reaction, React.doNothing);
					drawStatus = 2;
					turnEnded |= checkChankan(player[currentPlayer].doKakan(cp), false);
					break;
				case React.TSUMO:
					player[cp.who].actionLabel.action(4);
				case React.NAGAS:
					turnEnded = true;
			}
			if (turnEnded)	break;
			// 有人槓牌且沒人搶槓的話就不必考慮其他人的反應
			if (drawStatus != 0) {
				if (kanPending) {
					kanPending = false;
					calcLayer.doraOpen();
				}
				continue;
			}
			if (kanPending) {
				kanPending = false;
				calcLayer.doraOpen();
			}
			Arrays.fill(reaction, React.doNothing);
			player[currentPlayer].doThrowCard(cp);
			
			// 他人回合的回應
			Arrays.sort(reaction, 1, 5);
			final React ra = reaction[1];
			if (ra.kind == React.RONNN) {
				for (int i = 1; i <= PlayerNum; ++i)
					if (reaction[i].kind == React.RONNN)
						player[reaction[i].who].actionLabel.action(3);
				break;
			}
			// 第四個槓發生後如果沒自摸沒搶槓、打出後沒人和牌 == 四槓散了
			if (suukansanra) {
				endByRule = RULESUKAN;
				break;
			}
			// 立直後扣點
			if (cp.kind == React.RICHI) {
				player[currentPlayer].richibou.richiSenkoku();
				pts[currentPlayer] -= 1000;
				signRichi.setText(String.format("%2d", ++tableRichi));
				info.updatePoints();
			}
			// 四家立直
			if (player[1].richied() && player[2].richied() && player[3].richied() && player[4].richied()) {
				endByRule = RULESUCHA;
				break;
			}
			
			switch (ra.kind) {
				case React.CHII3:
				case React.CHII2:
				case React.CHII1:
					player[ra.who].actionLabel.action(0);
					player[currentPlayer].nakareru();
					player[ra.who].doChiPon(ra);
					drawStatus = 1;
					break;
				case React.PONNN:
					player[ra.who].actionLabel.action(1);
					player[currentPlayer].nakareru();
					player[ra.who].doChiPon(ra);
					drawStatus = 1;
					break;
				case React.KANNN:
					player[ra.who].actionLabel.action(2);
					player[currentPlayer].nakareru();
					player[ra.who].doMinkan(ra);
					drawStatus = 2;
					break;
			}
			
			// 如果有人叫牌則變成他的回合
			if (ra.kind == React.PASSS) {
				currentPlayer = ((currentPlayer == PlayerNum) ? 1 : (currentPlayer + 1));
			} else {
				restartable = false;
				Arrays.fill(ippatsu, false);
				currentPlayer = ra.who;
			}
		} while (!turnEnded && (remainCard > 0));
		try { Thread.sleep(1536); } catch (InterruptedException ie) {}
		
		boolean renchan = false;
		if ((endByRule != 0) || (reaction[3].kind == React.RONNN)) {
			// 如果有三人和牌 == 流局
			switch (endByRule) {
				case RULESUCHA:
					calcLayer.printSpecial("\u56DB\u3000\u5BB6\u3000\u7ACB\u3000\u76F4");	break;
				case RULESUFON:
					calcLayer.printSpecial("\u56DB\u3000\u98A8\u3000\u9023\u3000\u6253");	break;
				case RULESUKAN:
					calcLayer.printSpecial("\u56DB\u3000\u69D3\u3000\u6563\u3000\u4E86");	break;
				case 0:
					for (int i = 1; i <= PlayerNum; ++i)
						if (i != currentPlayer)
							player[currentPlayer].openFuda();
					calcLayer.printSpecial("\u4e09\u3000\u3000\u5bb6\u3000\u3000\u548c");	break;
			}
			renchan = true;
			++tableHonba;
		} else if (reaction[1].kind == React.RONNN) {
			calcLayer.printAgari(player[reaction[1].who], false);
			renchan |= (player[reaction[1].who].isOya());
			if (reaction[2].kind == React.RONNN) {
				calcLayer.printAgari(player[reaction[2].who], false);
				renchan |= (player[reaction[2].who].isOya());
			}
			tableHonba = renchan ? (tableHonba + 1) : 0;
		} else if (reaction[currentPlayer].kind == React.TSUMO) {
			calcLayer.printAgari(player[currentPlayer], true);
			if (player[currentPlayer].isOya()) {
				renchan = true;
				++tableHonba;
			} else
				tableHonba = 0;
		} else if (reaction[1].kind == React.CHANK) {
			int ronCount = 1;
			if (reaction[2].kind == React.CHANK)	++ronCount;
			if (reaction[3].kind == React.CHANK)	++ronCount;
			for (int i = 1; i <= ronCount; ++i) {
				calcLayer.printAgari(player[reaction[i].who], false);
				renchan |= (player[reaction[i].who].isOya());
			}
			tableHonba = renchan ? (tableHonba + 1) : 0;
		} else if (remainCard == 0) {
			calcLayer.printNagashi();
			renchan = player[currentOya].isTenpai();
			++tableHonba;
		} else if (reaction[1].kind == React.NAGAS) {
			player[currentPlayer].openFuda();
			calcLayer.printSpecial("\u4E5D\u3000\u7A2E\u3000\u4E5D\u3000\u724C");
			renchan = true;
			++tableHonba;
		} else {
			System.out.println("** Unknown END **");
		}
		
		if (renchan) {
			if (allLast == 1)	allLast = allLastTest(30001, false);
		} else {
			if (allLast == 1)	allLast = allLastTest(30001,  true);
			currentOya = (currentOya == PlayerNum) ? 1 : (currentOya + 1);
			++seatMapping;
			if (seatMapping == PlayerNum + 1) {
				seatMapping = 1;
				++currentBakaze;
			}
		}
		cardLayer.removeAll();
		return;
	}
	
	// 如果是暗槓則必須役滿才可以叫牌
	private boolean checkChankan(Card tar, boolean needYakuman) {
		tar.testChankan(needYakuman);
		boolean somebodyChankan = false;
		for (int i = 1; i <= PlayerNum; ++i)
			somebodyChankan |= (reaction[i].kind == React.CHANK);
		if (somebodyChankan)	Arrays.sort(reaction, 1, 5);
		return somebodyChankan;
	}
	
	private int allLastTest(final int limit, boolean renchan) {
		if (currentBakaze > duration + 1)
			return 0;
		if ((pts[1] < limit) && (pts[2] < limit) && (pts[3] < limit) && (pts[4] < limit))
			return 1;
		if (renchan)
			return 0;
		switch (currentOya) {
			case 1:
			return ((pts[1] > pts[2] && pts[1] > pts[3] && pts[1] > pts[4]) ? 0 : 1);
			case 2:
			return ((pts[2] > pts[1] && pts[2] > pts[3] && pts[2] > pts[4]) ? 0 : 1);
			case 3:
			return ((pts[3] > pts[1] && pts[3] > pts[2] && pts[3] > pts[4]) ? 0 : 1);
			case 4:
			return ((pts[4] > pts[1] && pts[4] > pts[2] && pts[4] > pts[3]) ? 0 : 1);
		}
		return 0;
	}
	
	// GUI遊戲結束按鈕行為
	public void actionPerformed(ActionEvent e) {
		this.dispose();
		return;
	}
	
	/* 此部分為除錯用、觀看玩家who的資訊 ========================================================== */
	public void showInfo(int who) {
		String insText = "<html>\u2190 hold<br />avail \u2192</html>";
		dialog = new JDialog(this, "information: PID" + who);
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(player[who].cheatShowBlock(true), BorderLayout.LINE_START);
		contentPane.add(player[who].cheatShowBlock(false),  BorderLayout.LINE_END);
		contentPane.add(player[who].cheatShowFuda(), BorderLayout.PAGE_END);
		contentPane.add(new JLabel(insText, JLabel.CENTER), BorderLayout.CENTER);
		contentPane.setOpaque(true);
		dialog.setContentPane(contentPane);
		dialog.setSize(new Dimension(360, 180));
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
		this.requestFocus();
		return;
	}
	
	// 使用按鍵1~4
	public boolean dispatchKeyEvent(KeyEvent e) {
		if (e.getID() != KeyEvent.KEY_PRESSED)
			return true;
		dialog.setVisible(false);
		switch (e.getKeyCode()) {
			case KeyEvent.VK_1:
				showInfo(1);
				break;
			case KeyEvent.VK_2:
				showInfo(2);
				break;
			case KeyEvent.VK_3:
				showInfo(3);
				break;
			case KeyEvent.VK_4:
				showInfo(4);
				break;
		}
		return true;
	}
	/* ====================================================================================== */
	
	// GUI中間的座位及場況資訊
	class CenterBlock extends JPanel {
		private final Font diceFont = new Font(Font.SANS_SERIF, Font.BOLD, Elem.eDiceFont);
		private final Font infoFont = new Font(Font.SANS_SERIF, Font.BOLD, Elem.eInfoFont);
		private final Font lifeFont = new Font(Font.SERIF, Font.BOLD, Elem.pLifeFont);
		private final int labelSize = Elem.pLifeFont + 4;
		private final char[] cInfo1 = { 'y', '\u6771', '\u5357', '\u897f', '\u5317' };
		private final char[] cInfo2 = { 'a', '\u2160', '\u2161', '\u2162', '\u2163' };
		private final JLabel[] ptsLabel;
		private final JLabel seat;
		private final JLabel cube;
		
		public CenterBlock() {
			this.setLayout(null);
			this.setOpaque(false);
			this.setBounds(Elem.WinC - (Elem.eSeats >> 1), Elem.WinC - (Elem.eSeats >> 1), Elem.eSeats, Elem.eSeats);
			this.setVisible(true);
			seat = new JLabel();
			seat.setVisible(false);
			seat.setBounds(0, 0, Elem.eSeats, Elem.eSeats);
			this.add(seat);
			cube = new JLabel("", JLabel.CENTER);
			cube.setVisible(true);
			cube.setBounds((Elem.eSeats - Elem.eDice) >> 1, (Elem.eSeats - Elem.eDice) >> 1, Elem.eDice, Elem.eDice);
			this.add(cube);
			ptsLabel = new JLabel[5];
			for (int i = 1; i <= PlayerNum; ++i) {
				ptsLabel[i] = new JLabel("", JLabel.CENTER);
				ptsLabel[i].setFont(lifeFont);
				ptsLabel[i].setForeground(Color.WHITE);
				ptsLabel[i].setSize(Elem.eSeats, labelSize);
				this.add(ptsLabel[i]);
			}
			ptsLabel[1].setLocation(0,  Elem.eSeats - labelSize - Elem.ptsH);
			ptsLabel[2].setLocation(0, (Elem.eSeats - labelSize) >> 1);
			ptsLabel[2].setHorizontalAlignment(JLabel.RIGHT);
			ptsLabel[3].setLocation(0, Elem.ptsH);
			ptsLabel[4].setLocation(0, (Elem.eSeats - labelSize) >> 1);
			ptsLabel[4].setHorizontalAlignment(JLabel.LEFT);
		}
		
		public int diceRandom() {
			cube.setText("?");
			cube.setFont(diceFont);
			cube.setForeground(Color.ORANGE);
			try { Thread.sleep(600); } catch (InterruptedException ie) {}
			int diceValue = (int)(Math.random() * 6) + (int)(Math.random() * 6) + 2;
			cube.setText(Integer.toHexString(diceValue).toUpperCase());
			return ((takeS[diceValue % 4 + currentOya] + 2 * diceValue) % 136);
		}
		
		public void hideInfo() {
			seat.setVisible(false);
			return;
		}
		
		public void updatePoints() {
			for (int i = 1; i <= PlayerNum; ++i)
				ptsLabel[i].setText(String.format("   %d   ", pts[i]));
			return;
		}
		
		public void showInfo() {
			seat.setVisible(true);
			cube.setVisible(false);
			cube.setFont(infoFont);
			cube.setForeground((allLast == 2) ? Color.YELLOW : Color.MAGENTA);
			cube.setText(String.format("<html>%c<br />%c</html>", cInfo1[currentBakaze], cInfo2[seatMapping]));
			cube.setVisible(true);
			return;
		}
		
		public void changeIcon(int currentOya, int currentPlayer) {
			seat.setIcon(Elem.seatPic[currentOya][currentPlayer]);
			return;
		}
		
	}
	
}
