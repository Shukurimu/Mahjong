import java.util.*;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;

class Player extends JPanel implements MouseListener, Runnable, Comparable<Player> {
	private static final int  startZ = (Elem.WinS - 16 * Elem.cW) >> 1;
	private static final int[] previousID = { 0, 4, 1, 2, 3, 4, 1, 2, 3 };	// 上家的ID
	private static final int[] furoX = { 0, Elem.WinS, Elem.WinS, 0, 0 };
	private static final int[] furoY = { 0, Elem.WinS, 0, 0, Elem.WinS };
	private static final int[] fudaX = { 0, startZ, Elem.WinS - Elem.cH, Elem.WinS - Elem.cW - startZ, 0 };
	private static final int[] fudaY = { 0, Elem.WinS - Elem.cH, Elem.WinS - Elem.cW - startZ, 0, startZ };
	private static final int[] incsX = { 0, 1, 0, -1, 0 };
	private static final int[] incsY = { 0, 0, -1, 0, 1 };
	private static final int[] slctX = { 0, 0, -1, 0, 1 };
	private static final int[] slctY = { 0, -1, 0, 1, 0 };
	private static final int[] usedC = { 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
	private static final int[] usedR = { 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 };
	private static final int[] usedX =
	{ 0, Elem.WinC - Elem.cW * 3, Elem.WinC + Elem.cW * 3, Elem.WinC + Elem.cW * 2, Elem.WinC - Elem.cW * 3 - Elem.cH };
	private static final int[] usedY =
	{ 0, Elem.WinC + Elem.cW * 3, Elem.WinC + Elem.cW * 2, Elem.WinC - Elem.cW * 3 - Elem.cH, Elem.WinC - Elem.cW * 3 };
	private boolean sLoop = false;
	private ThreadGroup actGroup = null;
	private ActionButton buttonTsumo;	// GUI自摸按鈕
	private ActionButton buttonRichi;	// GUI立直按鈕
	private ActionButton buttonNagas;	// GUI流局按鈕
	private ActionButton buttonPasss;	// GUIPASS按鈕
	private ActionButton buttonRonnn;	// GUI和牌按鈕
	public  ActionLabel  actionLabel;	// GUI行動宣告文字
	public final String name;	// GUI玩家名稱
	public final int PID;		// GUI玩家座位
	public final int WDX, WDY;	// GUI牌的長寬
	public final int GPX, GPY;	// GUI鳴牌標示位移
	public final int KWX, KWY;	// GUI河底放牌位移
	public int cumux, cumuy;	// GUI副露牌的位置
	public int richiIndex;		// GUI立直位置(此張牌應擺橫的)
	public Card richibou;		// GUI立直丟的點棒
	
	/* 此部分是可以被其他玩家知道的資訊 ================================================================ */
	private LinkedList<Card> kawa;	// 河底
	private LinkedList<Furo> furo;	// 副露
	private int     menfonn;	// 門風 - 1東 2南 3西 4北
	private boolean menchin;	// 是否門清
	private boolean   richi;	// 有無立直
	private boolean dbrichi;	// 是否雙立直
	private boolean rinshan;	// 現在是否在摸嶺上牌
	private boolean nakareta;	// 是否被叫過牌
	private boolean[][] yasui;	// 安排：打過的牌、立直後別人打出而沒有和了的牌
	/* ============================================================================================ */
	
	private ArrayList<Card> fuda;	// 從左到右的手牌
	private Analyze genre;		// 目前的手牌分解狀況、只有在已經聽牌的情況下才會是正確有效的
	private boolean[][] avail;	// 可以聽的牌 >> 其中[0][0]是只要有聽牌就會是true
	private int[][] hold;	// 目前持有(包含副露)的牌、和pool都是自己回合時包含摸到的牌
	private int[][] pool;	// 手牌狀況、不考慮赤懸賞 >> 其中[0][0]保留給國士無雙測試使用
	private Card    focus;	// (自己回合)摸到的牌 (別人回合)被打出去的牌、就是可以被吃碰槓和的牌
	private Card[][] head;	// 每一種牌的最左邊的牌、理論上GUI才會用到
	private int furiten;	// 0無振聽 1同巡過水振聽 2自己打牌振聽
	
	
	// 遊戲結束時的排名用函式
	public int compareTo(Player x) {
		return Integer.compare(x.cumux, cumux);
	}
	
	public Player(int who, JPanel deskLayer, String s) {
		this.setLayout(null);
		this.setOpaque(false);
		this.setBounds(0, 0, Elem.WinS, Elem.WinS);
		
		PID = who;
		WDX = ((PID & 1) == 1) ? Elem.cW : Elem.cH;
		WDY = ((PID & 1) == 1) ? Elem.cH : Elem.cW;
		GPX = slctX[PID] * (WDX / 10);
		GPY = slctY[PID] * (WDY / 10);
		name = s;
		actionLabel = new ActionLabel();
		switch (PID) {
			case 1:
				KWX = +(WDX / 10);
				KWY = +(WDY / 10);
				richibou = new Card(PID, Elem.WinC - Elem.ptsW / 2, Elem.WinC + Elem.eSeats / 2 - Elem.ptsH);
				actionLabel.setLocation(Elem.WinC - (Elem.pDeclaraW >> 1), Elem.WinS - Elem.pDeclaraH);
				break;
			case 2:
				KWX = +(WDX / 10);
				KWY = -(WDY / 10);
				richibou = new Card(PID, Elem.WinC + Elem.eSeats / 2 - Elem.ptsH, Elem.WinC - Elem.ptsW / 2);
				actionLabel.setLocation(Elem.WinS - (Elem.pDeclaraW * 3 / 4), Elem.WinC - (Elem.pDeclaraH >> 1));
				break;
			case 3:
				KWX = -(WDX / 10);
				KWY = -(WDY / 10);
				richibou = new Card(PID, Elem.WinC - Elem.ptsW / 2, Elem.WinC - Elem.eSeats / 2);
				actionLabel.setLocation(Elem.WinC - (Elem.pDeclaraW >> 1), 0);
				break;
			case 4:
				KWX = -(WDX / 10);
				KWY = +(WDY / 10);
				richibou = new Card(PID, Elem.WinC - Elem.eSeats / 2, Elem.WinC - Elem.ptsW / 2);
				actionLabel.setLocation(-(Elem.pDeclaraW >> 2), Elem.WinC - (Elem.pDeclaraH >> 1));
				break;
			default:
				KWX = KWY = 0;
		}
		buttonTsumo = new ActionButton(declaration[4], new React(PID, React.TSUMO));
		buttonRichi = new ActionButton(declaration[5], new React(PID, React.RICHI));
		buttonNagas = new ActionButton(declaration[7], new React(PID, React.NAGAS));
		buttonPasss = new ActionButton(declaration[8], React.doNothing);
		buttonPasss.setLocation(Elem.pRbase - (Elem.pBtnWidth + Elem.btnGaps), Elem.WinS - Elem.pBbase * (PID + 1));
		buttonRonnn = new ActionButton(declaration[3], new React(PID, React.RONNN));
		buttonRonnn.setLocation(Elem.pRbase - (Elem.pBtnWidth + Elem.btnGaps) * 2, Elem.WinS - Elem.pBbase * (PID + 1));
		deskLayer.add(richibou);
		if (PID == 1 && !Game.isAI[1])	deskLayer.addMouseListener(this);
		this.add(actionLabel);
		this.add(buttonTsumo);
		this.add(buttonRichi);
		this.add(buttonNagas);
		this.add(buttonPasss);
		this.add(buttonRonnn);
	}
	
	// GUI等待玩家輸入行為
	private synchronized void actionWait() {
		sLoop = true;
		try {
			while (sLoop)	this.wait();
		} catch (InterruptedException e) {}
		return;
	}
	
	// GUI玩家輸入行為
	public synchronized void actionConfirm(React ra) {
		if (sLoop) {
			sLoop = false;
			clickActivated = false;
			Game.reaction[PID] = ra;
			this.notify();
			actGroup.interrupt();
		}
		return;
	}
	
	// 新的一局重置變數
	public void resetPlayer(int w) {
		richiIndex = 0;
		focus = null;
		pool = new int[4][10];
		hold = new int[4][10];
		head = new Card[4][10];
		fuda = new ArrayList<Card>(16);
		kawa = new LinkedList<Card>();
		furo = new LinkedList<Furo>();
		nakareta = false;
		menfonn = w;
		furiten = 0;
		cumux = furoX[PID];
		cumuy = furoY[PID];
		genre = null;
		menchin = true;
		rinshan = false;
		richi = dbrichi = false;
		richibou.setVisible(false);
		yasui = new boolean[4][10];
		return;
	}
	
	// 起始的發牌
	public void distribute(Card x) {
		++pool[x.vi][x.vj];
		++hold[x.vi][x.vj];
		x.order = 999;
		x.drawnIntoFuda(PID);
		x.newx = fudaX[PID] + incsX[PID] * WDX * fuda.size();
		x.newy = fudaY[PID] + incsY[PID] * WDY * fuda.size();
		(new Thread(x)).start();
		fuda.add(x);
		return;
	}
	
	// 每回合抽牌、再提醒此時pool和hold都會加入摸到的牌
	public void drawCard(Card x, boolean rinshanpai) {
		++pool[x.vi][x.vj];
		++hold[x.vi][x.vj];
		rinshan = rinshanpai;
		focus = x;
		focus.order = fuda.size();
		focus.drawnIntoFuda(PID);
		focus.newx = fudaX[PID] + incsX[PID] * (WDX * focus.order + WDX / 5);
		focus.newy = fudaY[PID] + incsY[PID] * (WDY * focus.order + WDY / 5);
		focus.run();
		fuda.add(focus);
		return;
	}
	
	// 打出選擇的牌[focus]、並且消去嶺上狀態
	private void discardFocus(boolean needSort) {
		rinshan = false;
		--pool[focus.vi][focus.vj];
		--hold[focus.vi][focus.vj];
		yasui[focus.vi][focus.vj] = true;
		int index = kawa.size();
		focus.newx = usedX[PID] + (incsX[PID] * usedC[index] - slctX[PID] * usedR[index]) * WDX;
		focus.newy = usedY[PID] + (incsY[PID] * usedC[index] - slctY[PID] * usedR[index]) * WDY;
		kawa.addLast(focus);
		if (richi) {
			if (richiIndex == index) {
				// GUI此張牌需橫擺
				focus.changeDirection(Elem.seq[PID + 1]);
				switch (PID) {
					case 1:
						focus.newy += (Elem.cH - Elem.cW);
						break;
					case 2:
						focus.newx += (Elem.cH - Elem.cW);
						focus.newy -= (Elem.cH - Elem.cW);
						break;
					case 3:
						focus.newx -= (Elem.cH - Elem.cW);
						break;
				}
			} else if (usedR[richiIndex] == usedR[index]) {
				// GUI同一排的其他張需要向右移動一點
				focus.newx += incsX[PID] * (Elem.cH - Elem.cW);
				focus.newy += incsY[PID] * (Elem.cH - Elem.cW);
			}
		}
		
		if (needSort)	sortFuda();
		focus.setStable();
		focus.discard(KWX, KWY);
		// 打牌之後就可以解振聽
		furiten = 0;
		for (int i = 0; i < 4; ++i) {
			for (int j = 1; j < 10; ++j) {
				if (avail[i][j] && yasui[i][j]) {
					// 如果打過的牌裡面有聽的牌就會振聽
					furiten = 2;
					System.out.printf("%s furiten: %d%d\n", name, i, j);
					i = 5;	break;
				}
			}
		}
		return;
	}
	
	// 排序手牌並且檢測有沒有聽牌、會更新head狀況
	public void sortFuda() {
		Arrays.fill(head[0], null);
		Arrays.fill(head[1], null);
		Arrays.fill(head[2], null);
		Arrays.fill(head[3], null);
		Collections.sort(fuda);
		// 反序是為了讓head指到最左邊的牌
		for (ListIterator<Card> it = fuda.listIterator(fuda.size()); it.hasPrevious(); ) {
			Card c = it.previous();
			int index = it.nextIndex();
			head[c.vi][c.vj] = c;
			if (c.order != index) {
				c.order  = index;
				c.newx = fudaX[PID] + incsX[PID] * WDX * index;
				c.newy = fudaY[PID] + incsY[PID] * WDY * index;
				(new Thread(c)).start();
			}
		}
		getTenpaiList();
		return;
	}
	
	// 自己回合的行動
	public void selfdecide() {
		ArrayList<React> reactList = selfTest();
		// 立直後就算沒事可做也要等一下、由於至少可以打出摸到的牌所以 size == 1
		if (reactList.size() == 1) {
			Game.reaction[PID] = reactList.get(0);
			try { Thread.sleep(300); } catch (InterruptedException e) {}
			return;
		}
		
		if (Game.isAI[PID]) {
			Game.reaction[PID] = aiSelfAction(reactList);
		} else {
			actGroup = new ThreadGroup("actGroup");
			int index = 0;
			for (Card c: fuda)	c.setUnselectable();
			for (React ra: reactList) {
				switch (ra.kind) {
					case React.KIRII:
						ra.setKirii();
						break;
					case React.PASSS:
						break;
					case React.TSUMO:
						buttonTsumo.toggle(++index);
						break;
					case React.RICHI:
						buttonRichi.toggle(++index);
						break;
					case React.NAGAS:
						buttonNagas.toggle(++index);
						break;
					case React.ANKAN:
						ra.setKannn();
						break;
					case React.KAKAN:
						ra.setKakan();
						break;
				}
			}
			setClickDefaultAction(reactList.get(0));
			actionWait();
			for (Card c: fuda)	c.setWaiting();
			for (Furo f: furo) {
				if (f.react != null)
					f.setFuroUnpressable();
			}
		}
		return;
	}
	
	// 自己回合時決定可以有哪些行動
	private ArrayList<React> selfTest() {
		ArrayList<React> reactList = new ArrayList<React>(20);
		boolean flagTsumo = false;
		// 一定可以打出摸到的牌
		reactList.add(new React(focus, React.KIRII));
		// 沒立直的話可以打出其他手牌
		if (!richi) {
			for (Card c: fuda)
				reactList.add(new React(c, React.KIRII));
		}
		if (Game.restartable) {
			// 九種九牌
			if (calc9() >= 9)
				reactList.add(new React(PID, React.NAGAS));
			// 十三不搭XD
			if ((Game.PlayerNum == 4) && is13()) {
				genre = Analyze.dealWith13(genre);
				reactList.add(new React(PID, React.TSUMO));
				flagTsumo = true;
			}
		}
		// 如果摸的牌在聽牌列表中
		if (avail[focus.vi][focus.vj] && (genre.calculate(focus, true) || richi || rinshan || (Game.remainCard == 0))) {
			reactList.add(new React(PID, React.TSUMO));
			flagTsumo = true;
		}
		// 如果已經四槓散了、除了自摸以外就沒其他選擇
		if (Game.suukansanra)	return reactList;
		
		if (richi) {
			// 立直後能做的事只剩下暗槓、若可以自摸規則上絕對無法暗槓
			if ((pool[focus.vi][focus.vj] == 4) && !flagTsumo && (Game.remainCard > 0) && (Game.kantsuCount[0] < 4)) {
				boolean flagKan = false;
				if (focus.vi == 0) {
					// 字牌一定可以暗槓
					flagKan = true;
				} else {
					if ((focus.vj >= 2 && focus.vj <= 8) && (pool[focus.vi][focus.vj - 1] == 0 && pool[focus.vi][focus.vj + 1] == 0)) {
						flagKan = true;
					} else if ((focus.vj == 1) && (pool[focus.vi][2] == 0)) {
						flagKan = true;
					} else if ((focus.vj == 9) && (pool[focus.vi][8] == 0)) {
						flagKan = true;
					} else {
						// 其他因為略複雜所以先放生...
					}
				}
				if (flagKan) {
					int o = head[focus.vi][focus.vj].order;
					reactList.add(new React(fuda.get(o), fuda.get(o+1), fuda.get(o+2), focus, React.ANKAN));
				}
			}
		} else {
			boolean flagRichi = false;
			// 立直測試：此時thrownable表示立直時可否打出此張牌
			if (menchin && (Game.remainCard >= Game.PlayerNum) && (Game.pts[PID] >= 1000)) {
				fuda.remove(fuda.size() - 1);
				// 如果原本就有聽牌(avail[0][0] == true)、那一定可以丟摸到的牌立直
				focus.thrownable = flagRichi = avail[0][0];
				// 看每一張牌、如果去掉某張牌還可以聽牌表示那張牌可以被丟掉
				for (Card c: fuda) {
					--pool[c.vi][c.vj];
					flagRichi |= (c.thrownable = tenpaiable());
					++pool[c.vi][c.vj];
				}
				fuda.add(focus);
			}
			if (flagRichi)	reactList.add(new React(PID, React.RICHI));
			
			if ((Game.remainCard > 0) && (Game.kantsuCount[0] < 4)) {
				// 暗槓
				for (int i = 0; i < 4; ++i)
					for (int j = 1; j < 10; ++j)
						if (pool[i][j] == 4) {
							int o = head[i][j].order;
							if (focus.vi == i && focus.vj == j) {
								// 摸到的牌是第四張
								reactList.add(new React(fuda.get(o), fuda.get(o+1), fuda.get(o+2), focus, React.ANKAN));
							} else {
								// 原本就有四張
								reactList.add(new React(fuda.get(o), fuda.get(o+1), fuda.get(o+2), fuda.get(o+3), React.ANKAN));
							}
						}
				// 加槓：副露要是明刻(符數 == 2) + 一張手中或摸到的牌
				for (Furo f: furo)
					if (f.stat == 2) {
						if (focus.vi == f.kind && focus.vj == f.numb)
							reactList.add(new React(focus, f));
						else if (pool[f.kind][f.numb] == 1)
							reactList.add(new React(f, head[f.kind][f.numb]));
					}
			}
		}
		return reactList;
	}
	
	// 宣告立直之後要打一張牌
	public void doRichiAction() {
		// 雙立直一定是在還可以流局的時候
		dbrichi = Game.restartable;
		richi = true;
		richiIndex = kawa.size();
		// c.thrownable、可打出的牌、是在selfTest就已經算好了
		if (Game.isAI[PID]) {
			ArrayList<React> reactList = new ArrayList<React>(14);
			for (Card c: fuda)
				if (c.thrownable)
					reactList.add(new React(c, React.RICHI));
			Game.reaction[PID] = aiRichiDiscard(reactList);
		} else {
			for (Card c: fuda) {
				if (c.thrownable)
					c.setKirii(new React(c, React.RICHI));
				else
					c.setUnselectable();
			}
			actionWait();
			for (Card c: fuda)	c.setWaiting();
		}
		return;
	}
	
	// GUI自己回合打牌的處理
	public void doThrowCard(React ra) {
		// 如果摸切可以不必重新排序手牌及分析
		if (ra.focus == focus) {
			fuda.remove(fuda.size() - 1);
			discardFocus(false);
		} else {
			focus = ra.focus;
			fuda.remove(focus.order);
			discardFocus(true);
		}
		return;
	}
	
	// GUI暗槓處理
	public Card doAnkan(React ra) {
		// 第四張原本不在手牌裡
		if (focus != ra.focus)
			focus  = ra.focus;
		fuda.remove(ra.focus.order);
		fuda.remove(ra.self3.order);
		fuda.remove(ra.self2.order);
		fuda.remove(ra.self1.order);
		furo.addLast(new Furo(PID, ra));
		hold[focus.vi][focus.vj] = 4;
		pool[focus.vi][focus.vj] = 0;
		sortFuda();
		return focus;
	}
	
	// GUI加槓處理
	public Card doKakan(React ra) {
		ra.furoo.upgrade(PID, focus = ((ra.self1 == null) ? ra.focus : ra.self1));
		fuda.remove(focus.order);
		hold[focus.vi][focus.vj] = 4;
		pool[focus.vi][focus.vj] = 0;
		sortFuda();
		return focus;
	}
	
	// 對別人出牌的行動
	public void elsedecide(ThreadGroup tgroup, Card x) {
		focus = x;
		(new Thread(tgroup, this)).start();
		return;
	}
	
	// elsedecide的start()會跳來這裡執行
	public void run() {
		ArrayList<React> reactList = elseTest();
		// 如果可以和牌那麼一定是在第一個
		boolean flagRon = (reactList.size() > 0) && (reactList.get(0).kind == React.RONNN);
		if (reactList.size() == 0) {
			// 沒事可做
		} else if (Game.isAI[PID]) {
			Game.reaction[PID] = aiElseAction(reactList);
		} else {
			actGroup = new ThreadGroup("actGroup");
			buttonPasss.toggle();
			for (Card c: fuda)	c.setUnselectable();
			for (React ra: reactList) {
				switch (ra.kind) {
					case React.PASSS:
						break;
					case React.RONNN:
						buttonRonnn.toggle();
						break;
					case React.CHII1:
					case React.CHII2:
					case React.CHII3:
					case React.PONNN:
						ra.setChiPon();
						break;
					case React.KANNN:
						ra.setKannn();
				}
			}
			setClickDefaultAction(React.doNothing);
			actionWait();
			for (Card c: fuda)	c.setWaiting();
		}
		// 如果可以和牌但選擇PASS的話會振聽
		if (flagRon && (Game.reaction[PID].kind == React.PASSS))
			furiten = richi ? 2 : 1;
		return;
	}
	
	// 別人回合時決定可以有哪些行動
	private ArrayList<React> elseTest() {
		int si = focus.vi;
		int sj = focus.vj;
		// 如果自己已經立直那麼這張牌會變成安全牌
		yasui[si][sj] |= richi;
		ArrayList<React> reactList = new ArrayList<React>(8);
		// 如果在聽牌名單裡且沒振聽就可以和牌
		if (avail[si][sj] && (furiten == 0)) {
			if (genre.calculate(focus, false) || richi || (Game.remainCard == 0)) {
				reactList.add(new React(PID, React.RONNN));
			} else
				furiten = 1;
		}
		
		if (!richi && (Game.remainCard > 0) && !Game.suukansanra) {
			// 要上一家的牌並且不是字牌才可以吃
			if ((Game.currentPlayer == previousID[PID]) && (si > 0)) {
				// 有一二吃三
				if ((sj >= 3) && (pool[si][sj-2] > 0) && (pool[si][sj-1] > 0)) {
					reactList.add(new React(React.CHII3, head[si][sj-2], head[si][sj-1], focus));
				}
				// 有一三吃二
				if ((sj >= 2) && (sj <= 8) && (pool[si][sj-1] > 0) && (pool[si][sj+1] > 0)) {
					reactList.add(new React(React.CHII2, head[si][sj-1], head[si][sj+1], focus));
				}
				// 有二三吃一
				if ((sj <= 7) && (pool[si][sj+1] > 0) && (pool[si][sj+2] > 0)) {
					reactList.add(new React(React.CHII1, head[si][sj+1], head[si][sj+2], focus));
				}
			}
			// 碰
			if (pool[si][sj] >= 2) {
				reactList.add(new React(React.PONNN, head[si][sj], fuda.get(head[si][sj].order + 1), focus));
			}
			// 槓
			if ((pool[si][sj] == 3) && (Game.kantsuCount[0] < 4)) {
				int o = head[si][sj].order;
				reactList.add(new React(fuda.get(o), fuda.get(o+1), fuda.get(o+2), focus, React.KANNN));
			}
		}
		// 如果可以行動、那也可以PASS
		if (reactList.size() > 0)
			reactList.add(React.doNothing);
		return reactList;
	}
	
	// ai自己回合的行動
	private React aiSelfAction(ArrayList<React> reactList) {
		return reactList.get((int)(Math.random() * reactList.size()));
	}
	
	// ai立直之後要打的牌
	private React aiRichiDiscard(ArrayList<React> reactList) {
		return reactList.get((int)(Math.random() * reactList.size()));
	}
	
	// ai別人回合的反應
	private React aiElseAction(ArrayList<React> reactList) {
		return reactList.get((int)(Math.random() * reactList.size()));
	}
	
	// ai吃碰牌以後的捨牌
	private React aiChiponDiscard(ArrayList<React> reactList) {
		return reactList.get((int)(Math.random() * reactList.size()));
	}
	
	// GUI明槓處理
	public void doMinkan(React ra) {
		menchin = false;
		fuda.remove(ra.self3.order);
		fuda.remove(ra.self2.order);
		fuda.remove(ra.self1.order);
		furo.addLast(new Furo(PID, ra, previousID[PID]));
		hold[ra.focus.vi][ra.focus.vj] = 4;
		pool[ra.focus.vi][ra.focus.vj] = 0;
		sortFuda();
		return;
	}
	
	// GUI吃碰處理
	public void doChiPon(React ra) {
		menchin = false;
		fuda.remove(ra.self2.order);
		fuda.remove(ra.self1.order);
		furo.addLast((ra.kind == React.PONNN) ? (new Furo(ra, PID, previousID[PID])) : (new Furo(ra, PID)));
		++hold[ra.focus.vi][ra.focus.vj];
		--pool[ra.self1.vi][ra.self1.vj];
		--pool[ra.self2.vi][ra.self2.vj];
		int index = 0;
		for (Card d: fuda) {
			if (d.order != index) {
				d.order  = index;
				d.newx = fudaX[PID] + incsX[PID] * WDX * index;
				d.newy = fudaY[PID] + incsY[PID] * WDY * index;
				(new Thread(d)).start();
			}
			++index;
		}
		return;
	}
	
	// 吃碰後捨牌
	public void chiponDiscard() {
		// 一定是跟最後一個副露相關
		Furo f = furo.peekLast();
		int vi0 = f.kind;		// 不可以打的花色
		int vj1 = f.restrict1;	// 不可以打數字1
		int vj2 = f.restrict2;	// 不可以打數字2
		ArrayList<React> reactList = new ArrayList<React>(12);
		for (Card c: fuda) {
			if (c.thrownable = ((c.vi != vi0) || ((c.vj != vj1) && (c.vj != vj2))))
				reactList.add(new React(c, React.KIRII));
		}
		// TODO: 目前是如果剛好都不能打就隨便了 例如7899不和牌但吃9
		if (reactList.size() == 0)
			for (Card c: fuda) {
				c.thrownable = true;
				reactList.add(new React(c, React.KIRII));
			}
		
		if (Game.isAI[PID]) {
			Game.reaction[PID] = aiChiponDiscard(reactList);
		} else {
			for (Card c: fuda) {
				if (c.thrownable)
					c.setKirii(new React(c, React.KIRII));
				else
					c.setUnselectable();
			}
			actionWait();
			for (Card c: fuda)	c.setWaiting();
		}
		focus = null;
		return;
	}
	
	// GUI被叫牌
	public void nakareru() {
		nakareta = true;
		kawa.removeLast();
		return;
	}
	
	// 搶槓
	public void runChankan(final ThreadGroup tgroup, final boolean needYakuman, Card cpf) {
		focus = cpf;
		final int si = cpf.vi;
		final int sj = cpf.vj;
		// 如果沒有聽就不可能搶槓
		if (!avail[si][sj]) {
			Game.reaction[PID] = React.doNothing;
			return;
		}
		Thread cyk = new Thread(new Runnable() {
			@Override
			public void run() {
				genre.calculate(focus, false);
				// 不需要役滿 或 和牌可役滿
				if (!needYakuman || !genre.isCommon()) {
					if (Game.isAI[PID]) {
						// ai的搶槓應對... 應該都是和牌吧
						// 如果不是就改成 React.doNothing
						Game.reaction[PID] = new React(PID, React.CHANK);
					} else {
						actGroup = new ThreadGroup("actGroup");
						buttonRonnn.toggle();
						buttonPasss.toggle();
						setClickDefaultAction(React.doNothing);
						actionWait();
					}
					// PASS一定振聽
					if (Game.reaction[PID].kind == React.CHANK)
						genre.addChankan();
					else
						furiten = richi ? 2 : 1;
				}
				return;
			}
		});
		(new Thread(tgroup, cyk)).start();
		return;
	}
	
	// 是不是莊家
	public boolean isOya() {
		return (menfonn == 1);
	}
	
	// 有沒有立直
	public boolean richied() {
		return richi;
	}
	
	// 給主程式看有沒有四風連打用的
	public int firstKawa() {
		Card c = kawa.getFirst();
		return (((c.vi > 0) || (c.vj > 4)) ? 0 : (1 << c.vj));
	}
	
	// GUI可以鳴牌時滑鼠移入的位置調整
	public void highLight(React ra) {
		if ((ra.kind >= React.CHII1 && ra.kind <= React.CHII3) || (ra.kind == React.PONNN)) {
			ra.self1.setLocation(ra.self1.oldx + GPX, ra.self1.oldy + GPY);
			ra.self2.setLocation(ra.self2.oldx + GPX, ra.self2.oldy + GPY);
		} else if (ra.kind == React.KANNN) {
			ra.self1.setLocation(ra.self1.oldx + GPX, ra.self1.oldy + GPY);
			ra.self2.setLocation(ra.self2.oldx + GPX, ra.self2.oldy + GPY);
			ra.self3.setLocation(ra.self3.oldx + GPX, ra.self3.oldy + GPY);
		} else if (ra.kind == React.ANKAN) {
			ra.self1.setLocation(ra.self1.oldx + GPX, ra.self1.oldy + GPY);
			ra.self2.setLocation(ra.self2.oldx + GPX, ra.self2.oldy + GPY);
			ra.self3.setLocation(ra.self3.oldx + GPX, ra.self3.oldy + GPY);
			ra.focus.setLocation(ra.focus.oldx + GPX, ra.focus.oldy + GPY);
		}
		return;
	}
	
	// GUI可以鳴牌時滑鼠離開的位置調整
	public void unhighLight(React ra) {
		if ((ra.kind >= React.CHII1 && ra.kind <= React.CHII3) || (ra.kind == React.PONNN)) {
			ra.self1.setLocation(ra.self1.oldx, ra.self1.oldy);
			ra.self2.setLocation(ra.self2.oldx, ra.self2.oldy);
		} else if (ra.kind == React.KANNN) {
			ra.self1.setLocation(ra.self1.oldx, ra.self1.oldy);
			ra.self2.setLocation(ra.self2.oldx, ra.self2.oldy);
			ra.self3.setLocation(ra.self3.oldx, ra.self3.oldy);
		} else if (ra.kind == React.ANKAN) {
			ra.self1.setLocation(ra.self1.oldx, ra.self1.oldy);
			ra.self2.setLocation(ra.self2.oldx, ra.self2.oldy);
			ra.self3.setLocation(ra.self3.oldx, ra.self3.oldy);
			ra.focus.setLocation(ra.focus.oldx, ra.focus.oldy);
		}
		return;
	}
	
	// GUI一局結束時打開手牌 TODO:副露
	public void openFuda() {
		for (Card c: fuda) {
			c.setStable();
		}
		focus.setStable();
		return;
	}
	
	// 和了時取得剩下的資訊
	public Bunkai getBunkai(boolean tsumo) {
		int[] doras = Dashboard.doraCount(hold, furo, fuda, tsumo ? null : focus);
		return genre.agari(richi, dbrichi, Game.ippatsu[PID], rinshan, doras);
	}
	
	// 看手牌
	public JLabel cheatShowFuda() {
		String contents = "";
		for (Card c: fuda)	contents += String.format("%d%d ", c.vi, c.vj);
		JLabel cheat = new JLabel(contents, JLabel.CENTER);
		cheat.setFont(new Font(Font.SERIF, Font.BOLD, 18));
		return cheat;
	}
	
	// 看持有牌和聽牌
	public JTextArea cheatShowBlock(boolean p) {
		String contents = "";
		for (int i = 0; i < 10; ++i)
			contents += (" " + i);
		for (int i = 0; i < 4; ++i) {
			contents += ("\n " + i);
			for (int j = 1; j < 10; ++j) {
				contents += (" " + (p ? hold[i][j] : (avail[i][j] ? 1 : 0)));
			}
			contents += " ";
		}
		JTextArea cheat = new JTextArea(contents);
		cheat.setEditable(false);
		cheat.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
		cheat.setBackground(new Color(240, 240, 240));
		return cheat;
	}
	
	// 目前是不是流局滿貫的狀態
	public boolean isNagashimangan() {
		if (nakareta)	return false;
		for (Card c: kawa) {
			if ((c.vi > 0) && (c.vj > 1) && (c.vj < 9))
				return false;
		}
		return true;
	}
	
	// 有沒有十三不搭
	private boolean is13() {
		int k = 1;
		for (int j = 1; j < 8; ++j)
			if (pool[0][j] > 0)
				k *= pool[0][j];
		for (int i = 1; i < 4; ++i) {
			for (int j = 1; j < 10; ++j)
				if (pool[i][j] > 0) {
					if ((j < 8) && ((pool[i][j + 1] != 0) || (pool[i][j + 2] != 0))) {
						k = 4;	i = 5;	break;
					} else if ((j == 8) && (pool[i][9] != 0)) {
						k = 4;	i = 5;	break;
					} else {
						k *= pool[i][j];
					}
					j += 2;
				}
		}
		return (k == 2);
	}
	
	// 計算么九牌種類數
	private int calc9() {
		int i = 0;
		i += ((pool[0][1] == 0) ? 0 : 1);
		i += ((pool[0][2] == 0) ? 0 : 1);
		i += ((pool[0][3] == 0) ? 0 : 1);
		i += ((pool[0][4] == 0) ? 0 : 1);
		i += ((pool[0][5] == 0) ? 0 : 1);
		i += ((pool[0][6] == 0) ? 0 : 1);
		i += ((pool[0][7] == 0) ? 0 : 1);
		i += ((pool[1][1] == 0) ? 0 : 1);
		i += ((pool[1][9] == 0) ? 0 : 1);
		i += ((pool[2][1] == 0) ? 0 : 1);
		i += ((pool[2][9] == 0) ? 0 : 1);
		i += ((pool[3][1] == 0) ? 0 : 1);
		i += ((pool[3][9] == 0) ? 0 : 1);
		return i;
	}
	
	// 檢測此人是否聽牌
	public boolean isTenpai() {
		return avail[0][0];
	}
	
	// 測試聽的牌寫入avail中、注意此函式是用pool裡的資訊
	public void getTenpaiList() {
		avail = new boolean[4][10];
		if (menchin) {
			for (int j = 1; j < 8; ++j) {
				++pool[0][j];
				testJyantou(false, 0, j);
				testChitoii(false, 0, j);
				testKokushi(false, 0, j);
				--pool[0][j];
			}
			for (int i = 1; i < 4; ++i)
				for (int j = 1; j < 10; ++j) {
					++pool[i][j];
					testJyantou(false, i, j);
					testChitoii(false, i, j);
					if (j == 1 || j == 9)
					testKokushi(false, i, j);
					--pool[i][j];
				}
		} else {
			for (int j = 1; j < 8; ++j) {
				++pool[0][j];
				testJyantou(false, 0, j);
				--pool[0][j];
			}
			for (int i = 1; i < 4; ++i)
				for (int j = 1; j < 10; ++j) {
					++pool[i][j];
					testJyantou(false, i, j);
					--pool[i][j];
				}
		}
		if (avail[0][0])
			genre = new Analyze(pool, hold, furo, menfonn, menchin);
		return;
	}
	
	private boolean richiable;	// 可不可以立直 (此變數由tenpaiable函數使用)
	// 目前pool可不可以聽牌、一樣使用pool資訊、但不會更動avail
	private boolean tenpaiable() {
		richiable = false;
		for (int i = 1; i < 4; ++i) {
			for (int j = 1; j < 10; ++j) {
				++pool[i][j];
				testJyantou(true, i, j);
				testChitoii(true, i, j);
				if (j == 1 || j == 9)
				testKokushi(true, i, j);
				--pool[i][j];
			}
			if (richiable)	return true;
		}
		for (int j = 1; j < 8; ++j) {
			++pool[0][j];
			testJyantou(true, 0, j);
			testChitoii(true, 0, j);
			testKokushi(true, 0, j);
			--pool[0][j];
		}
		return richiable;
	}
	
	private void testChitoii(boolean richiTest, int x, int y) {
		for (int i = 0; i < 4; ++i)
			for (int j = 1; j < 10; ++j) {
				if ((pool[i][j] & 5) != 0)	return;
			}
		if (richiTest)	richiable = true;
		else	avail[0][0] = avail[x][y] = true;
		return;
	}
	
	private void testKokushi(boolean richiTest, int x, int y) {
		int k = pool[0][5] * pool[0][6] * pool[0][7];
		if ((k == 0) || (k > 2))	return;
		k *= pool[0][1] * pool[0][2] * pool[0][3] * pool[0][4];
		k *= pool[1][1] * pool[1][9];
		k *= pool[2][1] * pool[2][9];
		k *= pool[3][1] * pool[3][9];
		if (k == 2) {
			if (richiTest)		richiable = true;
			else	avail[0][0] = avail[x][y] = true;
			pool[0][0] = 99;	// 給Analyze用的
		}
		return;
	}
	
	private void testJyantou(boolean richiTest, int x, int y) {
		for (int j = 1; j < 8; ++j) {
			if ((pool[0][j] % 3) == 1)	return;
			if (pool[0][j] == 2) {
				pool[0][j] = 0;
				testTsupai0(richiTest, 1, x, y);
				pool[0][j] = 2;
			}
		}
		for (int i = 1; i < 4; ++i)
			for (int j = 1; j < 10; ++j) {
				if (pool[i][j] >= 2) {
					pool[i][j] -= 2;
					testTsupai0(richiTest, 1, x, y);
					pool[i][j] += 2;
				}
			}
		return;
	}
	
	private void testTsupai0(boolean richiTest, int start, int x, int y) {
		for (int i = start; i < 8; ++i)
			if (pool[0][i] == 3) {
				pool[0][i] = 0;
				testTsupai0(richiTest, start + 1, x, y);
				pool[0][i] = 3;
			}
		// 如果字牌還有剩那一定聽不了
		if (sum(0) == 0)	testSuupai1(richiTest, 1, 2, x, y);
		return;
	}
	
	private void testSuupai1(boolean richiTest, int kind, int start, int x, int y) {
		for (int i = start; i < 9; ++i)
			if ((pool[kind][i-1] > 0) && (pool[kind][i] > 0) && (pool[kind][i+1] > 0)) {
				--pool[kind][i-1];
				--pool[kind][i];
				--pool[kind][i+1];
				testSuupai1(richiTest, kind, i, x, y);
				++pool[kind][i-1];
				++pool[kind][i];
				++pool[kind][i+1];
			}
		testSuupai2(richiTest, kind, 1, x, y);
		return;
	}
	
	private void testSuupai2(boolean richiTest, int kind, int start, int x, int y) {
		for (int i = 1; i < 10; ++i)
			if (pool[kind][i] == 3) {
				pool[kind][i] = 0;
				testSuupai2(richiTest, kind, i + 1, x, y);
				pool[kind][i] = 3;
			}
		if (sum(kind) == 0) {
			if (kind < 3) {
				testSuupai1(richiTest, kind + 1, 2, x, y);
			} else {
				if (richiTest)		richiable = true;
				else	avail[0][0] = avail[x][y] = true;
			}
		}
		return;
	}
	
	// 計算kind花色有的牌數
	private int sum(int kind) {
		return (pool[kind][1] + pool[kind][2] + pool[kind][3] +
				pool[kind][4] + pool[kind][5] + pool[kind][6] +
				pool[kind][7] + pool[kind][8] + pool[kind][9]);
	}
	
	// GUI點空白處兩下機制、只有自己摸牌和可以叫牌時有作用
	private long lastClick = 0L;
	private boolean clickActivated = false;
	private React clickDefaultAction = React.doNothing;
	
	private void setClickDefaultAction(React ra) {
		clickDefaultAction = ra;
		clickActivated = true;
		return;
	}
	
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {
		if (!clickActivated)	return;
		long thisClick = System.currentTimeMillis();
		if (thisClick - lastClick < 486L)
			actionConfirm(clickDefaultAction);
		lastClick = thisClick;
		return;
	}
	
	// GUI的文字 [0]吃 [1]碰 [2]槓 [3]和 [4]自摸 [5]立直
	private static final String[] declaration = { "\u30C1\uFF0D", "\u30DD\u30F3", "\u30AB\u30F3",
	"\u30ED\u30F3", "\u30C4\u30E2", "\u30EA\u30FC\u30C1", "\u629C\u304F", "\u6D41\u5C40", "\u30D1\u30B9" };
	// GUI各種宣言用的文字標示
	class ActionLabel extends JLabel implements Runnable {
		
		public ActionLabel() {
			this.setOpaque(false);
			this.setVisible(false);
			this.setForeground(Color.WHITE);
			this.setSize(Elem.pDeclaraW, Elem.pDeclaraH);
			this.setHorizontalAlignment(JLabel.CENTER);
		}
		
		public void action(int kind) {
			this.setText(declaration[kind]);
			(new Thread(this)).start();
			return;
		}
		
		public void run() {
			this.setVisible(true);
			try {
				for (int i = Elem.pDeclFont * 3; i > Elem.pDeclFont; i -= 4) {
					this.setFont(new Font(Font.SANS_SERIF, Font.BOLD, i));
					Thread.sleep(16);
				}
				Thread.sleep(400);
			} catch (InterruptedException e) {}
			this.setVisible(false);
			return;
		}
		
	}
	
	// GUI產生諸如 PASS、和牌、立直、自摸等的按鈕以及按下反應
	class ActionButton extends JButton implements Runnable {
		private final React react;
		private boolean pressed;
		private boolean entered;
		
		public ActionButton(String s, React ra) {
			react = ra;
			this.setText(s);
			this.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Elem.pNakuFont));
			this.setSize(Elem.pBtnWidth, Elem.pBtnHeight);
			this.setContentAreaFilled(false);
			this.setOpaque(false);
			this.setVisible(false);
		}
		
		public void toggle(int index) {
			this.setLocation(Elem.pRbase - (Elem.pBtnWidth + Elem.btnGaps) * index, Elem.WinS - Elem.pBbase * (PID + 1));
			(new Thread(actGroup, this)).start();
			return;
		}
		
		public void toggle() {
			(new Thread(actGroup, this)).start();
			return;
		}
		
		public void run() {
			this.setForeground(Color.LIGHT_GRAY);
			this.setVisible(true);
			try {
				do {
					this.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
					Thread.sleep(900);
					this.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
					Thread.sleep(100);
				} while (true);
			} catch (InterruptedException e) {}
			this.setVisible(false);
			return;
		}
		
		@Override
		protected void processMouseEvent(MouseEvent e) {
			switch (e.getID()) {
				case MouseEvent.MOUSE_ENTERED:
					entered = true;
					this.setForeground(Color.WHITE);
					this.setBackground(Color.LIGHT_GRAY);
					break;
				case MouseEvent.MOUSE_EXITED:
					entered = false;
					this.setForeground(Color.LIGHT_GRAY);
					this.setBackground(null);
					break;
				case MouseEvent.MOUSE_PRESSED:
					pressed = true;
					break;
				case MouseEvent.MOUSE_RELEASED:
					if (entered && pressed)
						actionConfirm(react);
					pressed = false;
					break;
			}
			return;
		}
		
	}
	
}
