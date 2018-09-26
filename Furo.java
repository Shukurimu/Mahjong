
final class Furo implements Comparable<Furo> {
    enum Type {
        ANKANTSU (1, 1, 1, 16),
        MINKANTSU(1, 1, 0,  8),
        ANKOUTSU (0, 1, 1,  4),
        MINKOUTSU(0, 1, 0,  2),
        JUNTSU   (0, 0, 0,  0);
        
        public final int kantsu;
        public final int koutsu;
        public final int anko;
        public final int baseFu;
        
        private Type(int ka, int ko, int an, int fu) {
            kantsu = ka;
            koutsu = ko;
            anko   = an;
            baseFu = fu;
        }
        
    }
    
    public final Type type;
    public final Card focus;// null(自己的牌) or 來源牌(叫牌後橫擺的那張)
    public final int vi;    // 花色
    public final int vj;    // 數字、順子用中間張
    public final int id;    // Card.getId(vi, vj) 順子再*-1、Analyze排序比較用
    
    private Furo(Type t, Card f, int vi, int vj, int id) {
        type = t;
        focus = f;
        this.vi = vi;
        this.vj = vj;
        this.id = id;
    }
    
    /** 吃的情況
        [x](x+1)(x+2)<x+restrict(+3)>
        (x-1)[x = x+restrict(0)](x+1)
        <x+restrict(-3)>(x-2)(x-1)[x]
    */
    public static Furo chi(Card focus, int restrict) {
        restrict /= 3;
        return new Furo(Type.JUNTSU, focus,
            focus.vi, focus.vj + restrict, -(focus.id + restrict));
    }
    
    public static Furo pon(Card focus) {
        return new Furo(Type.MINKOUTSU, focus, focus.vi, focus.vj, focus.id);
    }
    
    public static Furo kan(Card focus) {
        return new Furo(Type.MINKANTSU, focus, focus.vi, focus.vj, focus.id);
    }
    
    public static Furo ankan(Card anyone) {
        return new Furo(Type.ANKANTSU, null, anyone.vi, anyone.vj, anyone.id);
    }
    
    public static Furo kakan(Furo minkou) {
        return new Furo(Type.MINKANTSU, minkou.focus, minkou.vi, minkou.vj, minkou.id);
    }
    
    /** makeXXX為Analyze中計算分解用、故focus均為null */
    public static Furo makeJuntsu(int i, int j) {
        return new Furo(Type.JUNTSU, null, i, j, -Card.getId(i, j));
    }
    
    public static Furo makeKoutsu(boolean tsumo, int i, int j) {
        return new Furo(tsumo ? Type.ANKOUTSU : Type.MINKOUTSU,
                                     null, i, j, +Card.getId(i, j));
    }
    
    public boolean containsYao9() {
        return (type == Type.JUNTSU) ? (vj == 2 || vj == 8) : Card.isYao9(id);
    }
    
    public int getFu() {
        return Card.isYao9(id) ? (type.baseFu << 1) : type.baseFu;
    }
    
    @Override
    public String toString() {
        return String.format("Furo.%s(%d)", type, id);
    }
    
    @Override
    public int compareTo(Furo o) {
        return Integer.compare(id, o.id);
    }
    
}
