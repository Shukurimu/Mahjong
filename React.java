import java.util.List;

final class React implements Comparable<React> {
    enum Type {
        KIND9 (true,  false, "流局"),
        TSUMO (true,  true,  "ツモ"),
        RICHI (true,  true,  "リーチ"),
        ANKAN (false, true,  "カン"),
        KAKAN (false, true,  "カン"),
        KIRU  (false, false, ""),
        KRGR  (false, false, ""),   // 空切る
        TMGR  (false, false, ""),   // ツモ切る
        /** 以下五種回應、排愈下位者優先度愈低 */
        RON   (true,  true,  "ロン"),
        KAN   (false, true,  "カン"),
        PON   (false, true,  "ポン"),
        CHI   (false, true,  "チー"),
        PASS  (true,  false, "パス");
        
        public final boolean buttonType;
        public final boolean declType;
        public final String text;
        
        private Type(boolean bt, boolean dt, String t) {
            buttonType = bt;
            declType = dt;
            text = t;
        }
        
    }
    
    public static final React defaultPass = pass(null);
    public static final List<Card> NOCARD = List.of();
    public final Type type;
    public final Card drop;
    public final Card join;
    public final List<Card> cardList;
    public final int index;
    
    private React(Type type, Card drop, Card join, List<Card> cList, int index) {
        this.type = type;
        this.drop = drop;
        this.join = join;
        this.index = index;
        cardList = cList == null ? NOCARD : cList;
    }
    
    /** 對他人出牌反應時drop指向focus */
    public static React pass(Card focus) {
        return new React(Type.PASS, focus, null, null, 0);
    }
    
    public static React ron(Card focus) {
        return new React(Type.RON, focus, null, null, 0);
    }
    
    public static React chi(Card focus, Card c1, Card c2, int diff) {
        return new React(Type.CHI, focus, null, List.of(c1, c2), diff);
    }
    
    public static React pon(Card focus, List<Card> sameList) {
        return new React(Type.PON, focus, null, sameList, 0);
    }
    
    public static React kan(Card focus, List<Card> sameList) {
        return new React(Type.KAN, focus, null, sameList, 0);
    }
    
    /** 自己回合時join為摸牌、drop為捨牌 */
    public static React tsumo(Card focus) {
        return new React(Type.TSUMO, null, focus, null, 0);
    }
    
    public static React kind9(Card focus) {
        return new React(Type.KIND9, null, focus, null, 0);
    }
    
    public static React tmgr(Card focus) {
        return new React(Type.TMGR, focus, focus, null, 0);
    }
    
    public static React krgr(Card focus, Card selected) {
        return new React(Type.KRGR, selected, focus, null, 0);
    }
    
    public static React kiru(Card focus, Card selected) {
        return new React(Type.KIRU, selected, focus, null, 0);
    }
    
    public static React richi(Card focus, List<Card> thrownableList) {
        return new React(Type.RICHI, null, focus, thrownableList, 0);
    }
    
    /** 自己回合槓牌類的drop就是要槓的牌、join可能是null */
    public static React ankan(Card focus, List<Card> sameList) {
        return sameList.size() == 3 ? // 原本只有3張
            new React(Type.ANKAN,           focus,  null, sameList, 0) :
            new React(Type.ANKAN, sameList.get(0), focus, sameList, 0);
    }
    
    /** 於furo[index]加槓 */
    public static React kakan(Card focus, List<Card> sameList, int index) {
        return sameList.isEmpty() ?   // 手中沒有加槓牌
            new React(Type.KAKAN,           focus,  null, sameList, index) :
            new React(Type.KAKAN, sameList.get(0), focus, sameList, index);
    }
    
    @Override
    public String toString() {
        return String.format("React.%s(drop=%s, join=%d)", type, drop, join);
    }
    
    @Override
    public int compareTo(React o) {
        return type.compareTo(o.type);
    }
    
}
