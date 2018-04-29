
class React implements Comparable<React> {
	// 各家之間的優先順序、用於多人和牌的情況
	public static final int[][] correspond = { {0}, {0, 4, 3, 2, 1}, {0, 1, 4, 3, 2}, {0, 2, 1, 4, 3}, {0, 3, 2, 1, 4} };
	public static final int NAGAS = 900;	// 流局(宣告)
	public static final int TSUMO = 800;	// 自摸(宣告)
	public static final int ANKAN = 760;	// 暗槓
	public static final int KAKAN = 730;	// 加槓
	public static final int RICHI = 600;	// 立直(宣告)
	public static final int RONNN = 500;	// 和牌(宣告)
	public static final int CHANK = 400;	// 搶槓
	public static final int KANNN = 300;	// 槓
	public static final int PONNN = 200;	// 碰
	public static final int CHII3 = 103;	// 吃 (x-2)(x-1)x
	public static final int CHII2 = 102;	// 吃 (x-1)x(x+1)
	public static final int CHII1 = 101;	// 吃 x(x+1)(x+2)
	public static final int KIRII =  50;	// 普通出牌
	public static final int PASSS =   0;	// PASS(宣告)
	public static final React doNothing = new React();	// 相當於PASS
	/* 以上數值愈大表示愈優先、表示"宣告"的動作與牌無關 */
	public final int kind;	// 動作的類型、其值為上面的常數之一
	public final int who;	// 動作者
	public final Card self1;	// 吃、碰、明槓、暗槓的第一張牌(自己)
	public final Card self2;	// 吃、碰、明槓、暗槓的第二張牌(自己)
	public final Card self3;	// 明槓、暗槓的第三張牌(自己)
	public final Card focus;	// 吃、碰、明槓、和牌的對象牌(別人) 加槓、暗槓的第四張牌(自己) 自己回合打出或叫牌後打出的牌
	public final Furo furoo;	// 用於自己回合可以加槓時的刻子
	
	// 主程式判斷處理速度用
	public int compareTo(React x) {
		return Integer.compare(x.kind + correspond[Game.currentPlayer][x.who], kind + correspond[Game.currentPlayer][who]);
	}
	
	// PASS、預設動作
	private React() {
		kind = who = 0;
		self1 = self2 = self3 = focus = null;
		furoo = null;
	}
	
	// 流局、自摸、立直、和牌(+搶槓)
	public React(int w, int k) {
		who = w;
		kind = k;
		self1 = self2 = self3 = focus = null;
		furoo = null;
	}
	
	// 基本出牌、立直出牌、吃碰捨牌
	public React(Card c, int k) {
		who = c.owner;
		kind = k;
		self1 = self2 = self3 = focus = c;
		furoo = null;
		c.react = this;
	}
	
	// GUI設定反應
	public void setKirii() {
		focus.setKirii(this);
		return;
	}
	
	// 暗槓、明槓
	public React(Card c1, Card c2, Card c3, Card f, int k) {
		who = c1.owner;
		kind = k;
		self1 = c1;
		self2 = c2;
		self3 = c3;
		focus = f;
		furoo = null;
	}
	
	// GUI設定反應
	public void setKannn() {
		self2.setChiPonKan(this);
		self3.setChiPonKan(this);
		return;
	}
	
	// 加槓(用摸到的牌)
	public React(Card c, Furo f) {
		who = c.owner;
		kind = KAKAN;
		self1 = self2 = self3 = null;
		focus = c;
		furoo = f;
	}
	
	// 加槓(用手裡的牌)
	public React(Furo f, Card c) {
		who = c.owner;
		kind = KAKAN;
		self1 = self2 = self3 = focus = c;
		furoo = f;
	}
	
	// GUI設定反應
	public void setKakan() {
		furoo.setFuroPressable(this);
		return;
	}
	
	// 吃、碰
	public React(int k, Card c1, Card c2, Card f) {
		who = c1.owner;
		kind = k;
		self1 = c1;
		self2 = c2;
		self3 = null;
		focus = f;
		furoo = null;
	}
	
	// GUI設定反應
	public void setChiPon() {
		self1.setChiPonKan(this);
		self2.setChiPonKan(this);
		return;
	}
	
}
