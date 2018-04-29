import java.util.ArrayList;

class Bunkai implements Comparable<Bunkai> {
	public ArrayList<Yaku> list;
	public int sekinin;
	public int fansuu;
	public int fusuu;
	
	public Bunkai(int x) {
		list = new ArrayList<Yaku>(16);
		fusuu = x;
		fansuu = 0;
		sekinin = 0;
	}
	
	public Bunkai(Yaku x) {
		list = new ArrayList<Yaku>(1);
		fusuu = 0;
		fansuu = x.getValue();
		sekinin = 0;
		list.add(x);
	}
	
	public int compareTo(Bunkai x) {
		return Integer.compare(Dashboard.tensu(x.fansuu, x.fusuu), Dashboard.tensu(fansuu, fusuu));
	}
	
	public void addList(Yaku x) {
		fansuu += x.getValue();
		list.add(x);
		return;
	}
	
	public void addDora(Yaku x, int n) {
		fansuu += x.doraSet(n);
		list.add(x);
		return;
	}
	
}

enum Yaku {
	// Yakuman
	Tenhou			(1 << 9, "\u5929\u548C"),
	Chihou			(1 << 9, "\u5730\u548C"),
	Renhou			(1 << 9, "\u4EBA\u548C"),
	Shisanbutou		(1 << 9, "\u5341\u4E09\u4E0D\u5854"),
	KokushimusouR	(2 << 9, "\u7D14\u6B63\u56FD\u58EB\u7121\u53CC"),
	Kokushimusou	(1 << 9, "\u56FD\u58EB\u7121\u53CC"),
	ChurenpoutoR	(2 << 9, "\u7D14\u6B63\u4E5D\u84EE\u5B9D\u71C8"),
	Churenpouto		(1 << 9, "\u4E5D\u84EE\u5B9D\u71C8"),
	Daisuurin		(1 << 9, "\u5927\u6570\u96A3"),
	Daichikurin		(1 << 9, "\u5927\u7AF9\u6797"),
	Daisyarin		(1 << 9, "\u5927\u8ECA\u8F2A"),
	SuuankoR		(2 << 9, "\u56DB\u6697\u523B\u5358\u9A0E"),
	Suuanko			(1 << 9, "\u56DB\u6697\u523B"),
	Suukantsu		(1 << 9, "\u56DB\u69D3\u5B50"),
	Daichisei		(2 << 9, "\u5927\u4E03\u661F"),
	Tsuiso			(1 << 9, "\u5B57\u4E00\u8272"),
	Daisangen		(1 << 9, "\u5927\u4E09\u5143"),
	Daisushi		(2 << 9, "\u5927\u56DB\u559C"),
	Shousushi		(1 << 9, "\u5C0F\u56DB\u559C"),
	Ryuiso			(1 << 9, "\u7DD1\u4E00\u8272"),
	Chinroto		(1 << 9, "\u6E05\u8001\u982D"),
	Surenko			(1 << 9, "\u56DB\u9023\u523B"),
	Issyokuyonjun	(1 << 9, "\u4e00\u8272\u56db\u9806"),
	// Common
	Ippatsu			(1, "\u4E00\u767A"),
	Richi			(1, "\u7ACB\u76F4"),
	RichiW			(2, "\u4E21\u7ACB\u76F4"),
	Rinsyankaihou	(1, "\u5DBA\u4E0A\u958B\u82B1"),
	Chankan			(1, "\u69CD\u69D3"),
	Haiteiraoyue	(1, "\u6D77\u5E95\u6488\u6708"),
	Houteiraoyui	(1, "\u6CB3\u5E95\u6488\u9B5A"),
	Chinitsu		(6, "\u6E05\u4E00\u8272"),
	ChinitsuX		(5, "\u6E05\u4E00\u8272\u25BD"),
	Honitsu			(3, "\u6DF7\u4E00\u8272"),
	HonitsuX		(2, "\u6DF7\u4E00\u8272\u25BD"),
	Tanyaochu		(1, "\u65AD\u5E7A\u4E5D"),
	Honrouto		(2, "\u6DF7\u8001\u982D"),
	Shousangen		(2, "\u5C0F\u4E09\u5143"),
	Yakuhai5		(1, "\u5F79\u724C \u767D"),
	Yakuhai6		(1, "\u5F79\u724C \u767C"),
	Yakuhai7		(1, "\u5F79\u724C \u4E2D"),
	Jikaze1			(1, "\u81EA\u98A8 \u6771"),
	Jikaze2			(1, "\u81EA\u98A8 \u5357"),
	Jikaze3			(1, "\u81EA\u98A8 \u897F"),
	Jikaze4			(1, "\u81EA\u98A8 \u5317"),
	Bakaze1			(1, "\u5834\u98A8 \u6771"),
	Bakaze2			(1, "\u5834\u98A8 \u5357"),
	Bakaze3			(1, "\u5834\u98A8 \u897F"),
	Bakaze4			(1, "\u5834\u98A8 \u5317"),
	Toitoiho		(2, "\u5BFE\u3005\u548C"),
	Junchantaiyao	(3, "\u7D14\u5168\u5E2F\u5E7A\u4E5D"),
	JunchantaiyaoX	(2, "\u7D14\u5168\u5E2F\u5E7A\u4E5D\u25BD"),
	Honchantaiyao	(2, "\u6DF7\u5168\u5E2F\u5E7A\u4E5D"),
	HonchantaiyaoX	(1, "\u6DF7\u5168\u5E2F\u5E7A\u4E5D\u25BD"),
	Sankantsu		(2, "\u4E09\u69D3\u5B50"),
	Sananko			(2, "\u4E09\u6697\u523B"),
	Kofonsanko		(2, "\u5BA2\u98A8\u4E09\u523B"),
	Ryanpeko		(3, "\u4E8C\u76C3\u53E3"),
	Ipeko			(1, "\u4E00\u76C3\u53E3"),
	Ikkitsuukan		(2, "\u4E00\u6C17\u901A\u8CAB"),
	IkkitsuukanX	(1, "\u4E00\u6C17\u901A\u8CAB\u25BD"),
	Sanshokudoujun	(2, "\u4E09\u8272\u540C\u9806"),
	SanshokudoujunX	(1, "\u4E09\u8272\u540C\u9806\u25BD"),
	Sanshokudoko	(2, "\u4E09\u8272\u540C\u523B"),
	Isshokusanjun	(2, "\u4E00\u8272\u4E09\u9806"),
	Sanreko			(2, "\u4E09\u9023\u523B"),
	Sushichitoi		(3, "\u56DB\u559C\u4E03\u5BFE\u5B50"),
	Sangenchitoi	(3, "\u4E09\u5143\u4E03\u5BFE\u5B50"),
	Chitoitsu		(2, "\u4E03\u5BFE\u5B50"),
	Menzenchintsumo	(1, "\u9580\u524D\u6E05\u81EA\u6478\u548C"),
	Pinfu			(1, "\u5E73\u548C"),
	// bones
	Dora		(1, "\u30C9\u30E9"),
	UraDora		(1, "\u88CF\u30C9\u30E9"),
	AkaDora		(1, "\u8D64\u30C9\u30E9"),
	NukiDora	(1, "\u629C\u304D\u30C9\u30E9");
	
	private int value;
	public final String label;
	
	private Yaku(int v, String l) {
		value = v;
		label = l;
	}
	
	public int getValue() {
		return value;
	}
	
	public int doraSet(int n) {
		return (value = n);
	}
	
	public String text() {
		return ("x" + ((value < 64) ? value : (value >> 9)));
	}
	
}
