import java.awt.event.MouseEvent;
import javax.swing.*;

class Card extends JButton implements Runnable, Comparable<Card> {
	private static final int[] ribouX = { 0, Elem.WinS - Elem.ptsW, Elem.WinS - Elem.ptsH, 0, 0 };
	private static final int[] ribouY = { 0, Elem.WinS - Elem.ptsH, 0, 0, Elem.WinS - Elem.ptsW };
	private static final int[] csizeX = { 0, Elem.cW, Elem.cH, Elem.cW, Elem.cH };
	private static final int[] csizeY = { 0, Elem.cH, Elem.cW, Elem.cH, Elem.cW };
	
	private ImageIcon[] icon = { null, null, null };	// [0]normal [1]bright [2]dark
	private int state;		// [front][furo][fuda][kirii][rollover]
	public React react;
	public int newx, newy;
	public int oldx, oldy;
	public final int cpath;
	public final int value;
	/* 以上為GUI使用 */
	
	public final int vi;	// 花色
	public final int vj;	// 真實數字
	public int order;	// 在手牌裡的順位
	public int owner;	// 持有者
	public boolean thrownable;	// 立直時、叫牌後 可不可以被打出
	
	public int compareTo(Card x) {
		return Integer.compare(value, x.value);
	}
	
	// 立直點棒用
	public Card(int who, int x, int y) {
		owner = who;
		cpath = value = state = 0;
		vi = x;
		vj = y;
		this.setSize(((who & 1) == 1) ? Elem.ptsW : Elem.ptsH, ((who & 1) == 1) ? Elem.ptsH : Elem.ptsW);
		this.setEnabled(false);
		this.setIcon(Elem.pt1000Pic[who]);
		this.setDisabledIcon(Elem.pt1000Pic[who]);
		this.setVisible(false);
		this.setBorderPainted(false);
		this.setContentAreaFilled(false);
	}
	
	// 宣告立直丟出動畫
	public void richiSenkoku() {
		newx = vi;
		newy = vj;
		this.setLocation(oldx = ribouX[owner], oldy = ribouY[owner]);
		this.setVisible(true);
		(new Thread(this)).start();
		return;
	}
	
	// 麻將牌
	public Card(int i, int j) {
		vi = i;
		vj = (j > 0) ? j : 5;
		cpath = 10 * i + j;
		value = (i > 0) ? ((j > 0) ? (cpath * 2) : (cpath * 2 + 9)) : (j * 2 + 80);
		this.setBorderPainted(false);
		this.setContentAreaFilled(false);
	}
	
	// 開局堆牌
	public void setAsYama(JPanel cardLayer, int pdir, int x, int y) {
		owner = state = 0;
		newx = oldx = x;
		newy = oldy = y;
		this.setBounds(x, y, csizeX[pdir], csizeY[pdir]);
		this.setIcon(Elem.cardPic[ 8][pdir - 1][0]);
		icon[0] = Elem.cardPic[cpath][pdir - 1][0];
		cardLayer.add(this);
		return;
	}
	
	// GUI結束顯示
	public Icon showIcon() {
		return Elem.cardPic[cpath][0][0];
	}
	
	// GUI每一巡摸牌
	public void drawnIntoFuda(int o) {
		icon[0] = Elem.cardPic[cpath][o - 1][0];
		icon[1] = Elem.cardPic[cpath][o - 1][1];
		icon[2] = Elem.cardPic[cpath][o - 1][2];
		if (Game.showFuda[owner = o]) {
			state = 0b10011;
			this.setIcon(icon[0]);
		} else {
			this.setIcon(Elem.cardPic[8][o - 1][0]);
		}
		this.setSize(csizeX[o], csizeY[o]);
		return;
	}
	
	// GUI被丟出手牌後的方向改變
	public void changeDirection(int o) {
		this.setSize(csizeX[o], csizeY[o]);
		icon[0] = Elem.cardPic[cpath][o - 1][0];
		icon[1] = Elem.cardPic[cpath][o - 1][1];
		return;
	}
	
	// GUI暗槓蓋著的牌
	public void setAnkann() {
		state = 0;
		this.setIcon(Elem.cardPic[8][owner - 1][0]);
		return;
	}
	
	// 懸賞指示牌及已經打出或成為副露不會再動到的牌、正面朝上
	public void setStable() {
		state = 0;
		this.setIcon(icon[0]);
		return;
	}
	
	// GUI設為待機狀態、有滑過發亮特效
	public void setWaiting() {
		state = 0b10001;
		react = null;
		this.setIcon(icon[0]);
		return;
	}
	
	// GUI設為無法選取
	public void setUnselectable() {
		state = 0b10000;
		react = null;
		this.setIcon(icon[2]);
		return;
	}
	
	// GUI普通打牌的處理
	public void setKirii(React ra) {
		react = ra;
		state = 0b10011;
		this.setIcon(icon[0]);
		return;
	}
	
	// GUI可以叫牌的處理
	public void setChiPonKan(React ra) {
		react = ra;
		state = 0b10101;
		this.setIcon(icon[0]);
		return;
	}
	
	// GUI可以加槓時的處理
	public void furoEnable(React ra) {
		react = ra;
		state = 0b11001;
		return;
	}
	
	// GUI取消可以加槓
	public void furoDisable() {
		react = null;
		state = 0b10000;
		this.setIcon(icon[0]);
		return;
	}
	
	// 等待其他玩家反應
	private void waitReactions(ThreadGroup tgroup) {
		Thread[] thinking = new Thread[4];
		int num = tgroup.enumerate(thinking, false);
		try {
			for (int i = 0; i < num; ++i)
				thinking[i].join();
			// TODO: 如果高優先度的人決定了高優先度的行動就可以打斷其他人思考
		} catch (InterruptedException e) {}
		return;
	}
	
	// 從手牌打出、此函式會呼叫其他玩家的 run()
	public void discard(int tempx, int tempy) {
		++Game.appearance[vi][vj];
		double dx = (double)(newx + tempx - oldx) / 12.0;
		double dy = (double)(newy + tempy - oldy) / 12.0;
		try {
			for (int i = 1; i < 12; ++i) {
				this.setLocation(oldx + (int)(dx * i), oldy + (int)(dy * i));
				Thread.sleep(16);
			}
		} catch (InterruptedException e) {}
		this.setLocation(newx + tempx, newy + tempy);
		Elem.playAudio(0);
		// 自己以外的玩家都去elsedecide
		ThreadGroup tgroup = new ThreadGroup("discard");
		if (owner != 1)	Game.player[1].elsedecide(tgroup, this);
		if (owner != 2)	Game.player[2].elsedecide(tgroup, this);
		if (owner != 3)	Game.player[3].elsedecide(tgroup, this);
		if (owner != 4)	Game.player[4].elsedecide(tgroup, this);
		waitReactions(tgroup);
		this.setLocation(oldx = newx, oldy = newy);
		return;
	}
	
	// 槓牌時測試搶槓
	public void testChankan(boolean needYakuman) {
		// 自己以外的玩家都去runChankan
		ThreadGroup tgroup = new ThreadGroup("testChankan");
		if (owner != 1)	Game.player[1].runChankan(tgroup, needYakuman, this);
		if (owner != 2)	Game.player[2].runChankan(tgroup, needYakuman, this);
		if (owner != 3)	Game.player[3].runChankan(tgroup, needYakuman, this);
		if (owner != 4)	Game.player[4].runChankan(tgroup, needYakuman, this);
		waitReactions(tgroup);
		return;
	}
	
	// GUI滑鼠反應
	private boolean entered = false;
	private boolean pressed = false;
	@Override
	protected void processMouseEvent(MouseEvent e) {
		if (state < 0b10000)	return;
		switch (e.getID()) {
			case MouseEvent.MOUSE_ENTERED:
				entered = true;
				if ((state & 0b00100) > 0)
					Game.player[owner].highLight(react);
				if ((state & 1) > 0)	this.setIcon(icon[1]);
				break;
			case MouseEvent.MOUSE_EXITED:
				entered = false;
				if ((state & 0b00100) > 0)
					Game.player[owner].unhighLight(react);
				if ((state & 1) > 0)	this.setIcon(icon[0]);
				break;
			case MouseEvent.MOUSE_PRESSED:
				pressed = true;
				break;
			case MouseEvent.MOUSE_RELEASED:
				if ((state & 0b00100) > 0)
					Game.player[owner].unhighLight(react);
				if (entered && pressed && ((state & 0b01110) > 0))
					Game.player[owner].actionConfirm(react);
				pressed = false;
				break;
		}
		return;
	}
	
	// GUI移動動畫
	public void run() {
		double dx = (double)(newx - oldx) / 12.0;
		double dy = (double)(newy - oldy) / 12.0;
		try {
			for (int i = 1; i < 12; ++i) {
				this.setLocation(oldx + (int)(dx * i), oldy + (int)(dy * i));
				Thread.sleep(16);
			}
		} catch (InterruptedException e) {}
		this.setLocation(oldx = newx, oldy = newy);
		return;
	}
	
}
