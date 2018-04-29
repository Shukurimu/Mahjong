import java.util.LinkedList;

class Furo {
	private static final int[] picx1 = { 0, -Elem.cW, -Elem.cH, 0, 0 };
	private static final int[] picy1 = { 0, -Elem.cH, 0, 0, -Elem.cW };
	private static final int[] picx2 = { 0, -Elem.cH, -Elem.cW, 0, 0 };
	private static final int[] picy2 = { 0, -Elem.cW, 0, 0, -Elem.cH };
	private static final int[] addx1 = { 0, -Elem.cW, 0, Elem.cW, 0 };
	private static final int[] addy1 = { 0, 0, Elem.cW, 0, -Elem.cW };
	private static final int[] addx2 = { 0, -Elem.cH, 0, Elem.cH, 0 };
	private static final int[] addy2 = { 0, 0, Elem.cH, 0, -Elem.cH };
	private static final int[][] direction = { {0}, {0,0,4,2,2}, {0,3,0,1,3}, {0,4,4,0,2}, {0,3,1,1,0} };
	/* 以上是GUI相對應的叫牌擺放方向及間距 */
	
	private LinkedList<Card> component = new LinkedList<Card>();	// 這組副露裡面有甚麼牌
	public boolean hasAkadora = false;	// 這個副露裏面有沒有赤懸賞
	public int stat;	// [0]x23 [2]4x4 [4]XXX [8]5555 [16]X55X
	public int kind;	// 花色
	public int numb;	// 數字 -若為面子則用其中間張的數字 (x-1)x(x+1)
	public int from;	// 叫牌的來源
	public int restrict1;	// 不可以打的牌
	public int restrict2;	// 吃的狀況下會是叫的牌 +3 或 -3
	public React react = null;	// GUI點選用
	
	// 吃
	public Furo(React ra, int who) {
		restrict1 = ra.focus.vj;
		restrict2 = ra.focus.vj + ((ra.kind == React.CHII2) ? 0 : ((ra.kind == React.CHII1) ? +3 : -3));
		stat = 0;
		from = ra.focus.owner;
		kind = ra.focus.vi;
		numb = (ra.focus.vj + ra.self1.vj + ra.self2.vj) / 3;
		ra.focus.changeDirection(direction[who][from]);
		ra.focus.setStable();
		ra.self1.setStable();
		ra.self2.setStable();
		++Game.appearance[ra.self1.vi][ra.self1.vj];
		++Game.appearance[ra.self2.vi][ra.self2.vj];
		setCard1(who, ra.self2);
		setCard1(who, ra.self1);
		setCard2(who, ra.focus);
	}
	
	// 碰
	public Furo(React ra, int who, int pre) {
		restrict1 = restrict2 = ra.focus.vj;
		stat = 2;
		from = ra.focus.owner;
		kind = ra.focus.vi;
		numb = ra.focus.vj;
		ra.focus.changeDirection(direction[who][from]);
		ra.focus.owner = who;
		ra.focus.setStable();
		ra.self1.setStable();
		ra.self2.setStable();
		Game.appearance[ra.focus.vi][ra.focus.vj] += 2;
		if (from == pre) {
			setCard2(who, ra.focus);
			setCard1(who, ra.self2);
			setCard1(who, ra.self1);
		} else if (Math.abs(from - who) == 2) {
			setCard1(who, ra.self2);
			setCard2(who, ra.focus);
			setCard1(who, ra.self1);
		} else {
			setCard1(who, ra.self2);
			setCard1(who, ra.self1);
			setCard2(who, ra.focus);
		}
	}
	
	// 明槓
	public Furo(int who, React ra, int pre) {
		stat = 8;
		from = ra.focus.owner;
		kind = ra.focus.vi;
		numb = ra.focus.vj;
		ra.focus.changeDirection(direction[who][from]);
		ra.focus.setStable();
		ra.self1.setStable();
		ra.self2.setStable();
		ra.self3.setStable();
		Game.appearance[ra.focus.vi][ra.focus.vj] = 4;
		if (from == pre) {
			setCard2(who, ra.focus);
			setCard1(who, ra.self3);
			setCard1(who, ra.self2);
			setCard1(who, ra.self1);
		} else if (Math.abs(from - who) == 2) {
			setCard1(who, ra.self3);
			setCard1(who, ra.self2);
			setCard2(who, ra.focus);
			setCard1(who, ra.self1);
		} else {
			setCard1(who, ra.self3);
			setCard1(who, ra.self2);
			setCard1(who, ra.self1);
			setCard2(who, ra.focus);
		}
	}
	
	// 暗槓
	public Furo(int who, React ra) {
		stat = 16;
		from = who;
		kind = ra.self1.vi;
		numb = ra.self1.vj;
		ra.self1.setStable();
		ra.self2.setStable();
		ra.self3.setAnkann();
		ra.focus.setAnkann();
		Game.appearance[ra.self1.vi][ra.self1.vj] = 4;
		setCard1(who, ra.self3);
		setCard1(who, ra.self1);
		setCard1(who, ra.self2);
		setCard1(who, ra.focus);
	}
	
	// 加槓
	public void upgrade(int who, Card c) {
		stat = 8;
		c.changeDirection(direction[who][from]);
		c.setStable();
		Game.appearance[c.vi][c.vj] = 4;
		component.add(c);
		hasAkadora |= ((c.value & 1) == 1);
		switch (who) {
			case 1:
				c.newx = component.getFirst().oldx;
				c.newy = component.getFirst().oldy - Elem.cW;
				break;
			case 2:
				c.newx = component.getFirst().oldx - Elem.cW;
				c.newy = component.getFirst().oldy;
				break;
			case 3:
				c.newx = component.getFirst().oldx;
				c.newy = component.getFirst().oldy + Elem.cW;
				break;
			case 4:
				c.newx = component.getFirst().oldx + Elem.cW;
				c.newy = component.getFirst().oldy;
				break;
		}
		(new Thread(c)).start();
		return;
	}
	
	// GUI設定為可以加槓
	public void setFuroPressable(React ra) {
		react = ra;
		for (Card c: component)
			c.furoEnable(ra);
		return;
	}
	
	// GUI回復普通狀態
	public void setFuroUnpressable() {
		react = null;
		for (Card c: component)
			c.furoDisable();
		return;
	}
	
	// GUI直向擺放在桌面
	private void setCard1(int who, Card c) {
		component.addLast(c);
		c.newx = Game.player[who].cumux + picx1[who];
		c.newy = Game.player[who].cumuy + picy1[who];
		(new Thread(c)).start();
		Game.player[who].cumux += addx1[who];
		Game.player[who].cumuy += addy1[who];
		hasAkadora |= ((c.value & 1) == 1);
		return;
	}
	
	// GUI橫向擺放在桌面
	private void setCard2(int who, Card c) {
		component.addFirst(c);
		c.newx = Game.player[who].cumux + picx2[who];
		c.newy = Game.player[who].cumuy + picy2[who];
		(new Thread(c)).start();
		Game.player[who].cumux += addx2[who];
		Game.player[who].cumuy += addy2[who];
		hasAkadora |= ((c.value & 1) == 1);
		return;
	}
	
}
