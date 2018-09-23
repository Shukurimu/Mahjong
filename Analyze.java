import java.util.Arrays;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;

final class Analyze {
    public static final String SEPARATOR = "#";
    public static final int YAKUMAN_SHIFT = 20;
    public static final int YAKUMAN_THRESHOLD = (1 << YAKUMAN_SHIFT);
    public static final Bunkai EMPTY = new Analyze().new Bunkai(0);
    public static final int[][] DORA_TO = new int[][] {
        { 0, 2, 3, 4, 1, 6, 7, 5, 0, 0 },
        { 0, 2, 3, 4, 5, 6, 7, 8, 9, 1 },
        { 0, 2, 3, 4, 5, 6, 7, 8, 9, 1 },
        { 0, 2, 3, 4, 5, 6, 7, 8, 9, 1 }
    };
    
    final class Bunkai implements Comparable<Bunkai> {
        private EnumMap<Yaku, Integer> content = new EnumMap<>(Yaku.class);
        private int pts = -1;
        private int fan =  0;
        private int fu  = 20;
        
        private Bunkai(int baseFu) {
            fu = baseFu;
            if (fu == 20) {
                if (player.menchin) {
                    content.put(Yaku.Pinfu, 1);
                    ++fan;
                } else {// 喰い平和
                    fu = 30;
                }
            } else {
                if (tsumo)
                    fu += 2;
                else if (player.menchin)
                    fu += 10;
                fu = (fu + 9) / 10 * 10;
            }
        }
        
        /** Chitoitsu.Kokushimusou.Daichisei use */
        private Bunkai(Yaku y, int v) {
            content.put(y, v);
            fu = 25;
            fan = y.yakuman ? y.getValue(v) << YAKUMAN_SHIFT : y.getValue(v);
        }
        
        private void tryInsert(Yaku y) {
            if (content.containsKey(y.upper))
                return;
            int v = y.check(Analyze.this);
            if (v != 0) {
                content.put(y, v);
                fan += y.yakuman ? y.getValue(v) << YAKUMAN_SHIFT : y.getValue(v);
                pts = -1;
            }
            return;
        }
        
        private void insertDora(Yaku y, int v) {
            content.put(y, v);
            fan += v;
            pts = -1;
            return;
        }
        
        public boolean containsUraDora() {
            return content.containsKey(Yaku.UraDora);
        }
        
        public int getPoint() {
            if (pts < 0) {
                if (fan >= YAKUMAN_THRESHOLD) {
                    content.keySet().removeIf(k -> !k.yakuman);
                }
                pts = calcPoint(fan, fu);
            }
            return pts;
        }
        
        public List<String[]> getYakuList() {
            List<String[]> res = new ArrayList<>(content.size());
            for (Entry<Yaku, Integer> e: content.entrySet()) {
                res.add(new String[] {
                    e.getKey().getLabel1(e.getValue()),
                    e.getKey().getLabel2(e.getValue())
                });
            }
            return res;
        }
        
        public String getSummaryString() {
            return content.isEmpty() ? "" : (
               (fan >= YAKUMAN_THRESHOLD ?
                    String.format("%dx", fan >> YAKUMAN_SHIFT) :
                    String.format("%d%s%d%s", fu, Yaku.FU, fan, Yaku.FAN)) +
                String.format("　%s%d点", calcTitle(fan, fu),
                    ((player.jifuu == 1 ? 6 : 4) * getPoint() + 99) / 100 * 100)
            );
        }
        
        @Override
        public String toString() {
            return getSummaryString();
        }
        
        @Override
        public int compareTo(Bunkai o) {    // reversed order
            return Integer.compare(o.getPoint(), this.getPoint());
        }
        
    }
    
    protected List<Bunkai> bunkaiList = new ArrayList<>();
    protected Furo[] furo = new Furo[4];
    protected int fIndex = 0;       // FuroIndex
    protected int pairId;           // 雀頭
    protected final Game game;
    protected final Card focus;
    protected final Player player;
    protected final boolean tsumo;
    protected final int yao9;       // 么九牌數量
    protected final int[][] hand;   // 直接參照不可改動
    protected final int[][] hold;   // 直接參照不可改動
    protected final int[][] pool;   // 複製分解用、會加入和了牌
    protected final int fi, fj;     // 和了牌
    
    private Analyze() {
        game = null;
        focus = null;
        player = null;
        tsumo = true;
        hand = null;
        hold = null;
        pool = null;
        fi = fj = yao9 = 0;
    }
    
    public Analyze(Player player, Card focus) {
        this.game = Game.getCurrentGame();
        this.focus = focus;
        this.player = player;
        tsumo = game.cp == player;
        fi = focus.vi;
        fj = focus.vj;
        hand = player.hand;
        hold = player.hold;
        pool = new int[][] {
            hand[0].clone(),
            hand[1].clone(),
            hand[2].clone(),
            hand[3].clone()
        };
        ++pool[fi][fj];
        ++pool[fi][11];
        int yao9count = focus.yao9 ? 1 : 0;
        for (int[] z: Card.YAOCHU_INDEXES) {
            yao9count += hold[z[0]][z[1]];
        }
        yao9 = yao9count;
        for (Furo f: player.furo) {
            furo[fIndex++] = f;
        }
    }
    
    private boolean satisfyShibari() {
        ++hand[fi][fj];
        ++hand[fi][11];
        ++hold[fi][fj];
        ++hold[fi][11];
        for (Yaku sp: Yaku.specialSet) {
            int v = sp.check(this);
            if (v != 0) {
                Bunkai temp = new Bunkai(sp, v);
                Yaku.stack7Set.forEach(y -> temp.tryInsert(y));
                bunkaiList.add(temp);
            }
        }
        int pairKind = 0;
        for (int i = 3; i >= 0; --i) {
            if (pool[i][11] % 3 == 2) {
                pairKind = i;
                break;
            }
        }
        
        // 雀頭的情況
        if (pairKind == fi && pool[fi][fj] >= 2) {
            pool[fi][fj] -= 2;
            pool[fi][11] -= 2;
            pairId = focus.id;
            int pairFu = 2;
            if (fi == 0) {
                if (fj == game.bafuu || fj >= 5)
                    pairFu += 2;
                if (fj == player.jifuu)
                    pairFu += 2;
            }
            deKoutsu(pairFu, 0, 7);
            pool[fi][11] += 2;
            pool[fi][fj] += 2;
        }
        
        // 以下和了牌的花色都會少3張一次減去
        pool[fi][11] -= 3;
        
        // 刻子的情況
        if (pool[fi][fj] >= 3) {
            pool[fi][fj] -= 3;
            furo[fIndex++] = Furo.makeKoutsu(tsumo, fi, fj);
            deJyantou(0, pairKind);
            --fIndex;
            pool[fi][fj] += 3;
        }
        
        // 順子的情況
        if (fi != 0) {
            // [x-2][x-1][x]
            if ((fj >= 3) &&
                    (pool[fi][fj - 2] > 0) &&
                    (pool[fi][fj - 1] > 0) &&
                    (pool[fi][fj]     > 0)) {
                --pool[fi][fj - 2];
                --pool[fi][fj - 1];
                --pool[fi][fj];
                furo[fIndex++] = Furo.makeJuntsu(fi, fj - 1);
                deJyantou((fj > 3) ? 0 : 2, pairKind);
                --fIndex;
                ++pool[fi][fj];
                ++pool[fi][fj - 1];
                ++pool[fi][fj - 2];
            }
            // [x-1][x][x+1]
            if ((fj >= 2 && fj <= 8) &&
                    (pool[fi][fj - 1] > 0) &&
                    (pool[fi][fj]     > 0) &&
                    (pool[fi][fj + 1] > 0)) {
                --pool[fi][fj - 1];
                --pool[fi][fj];
                --pool[fi][fj + 1];
                furo[fIndex++] = Furo.makeJuntsu(fi, fj);
                deJyantou(2, pairKind);
                --fIndex;
                ++pool[fi][fj + 1];
                ++pool[fi][fj];
                ++pool[fi][fj - 1];
            }
            // [x][x+1][x+2]
            if ((fj <= 7) &&
                    (pool[fi][fj]     > 0) &&
                    (pool[fi][fj + 1] > 0) &&
                    (pool[fi][fj + 2] > 0)) {
                --pool[fi][fj];
                --pool[fi][fj + 1];
                --pool[fi][fj + 2];
                furo[fIndex++] = Furo.makeJuntsu(fi, fj + 1);
                deJyantou((fj < 7) ? 0 : 2, pairKind);
                --fIndex;
                ++pool[fi][fj + 2];
                ++pool[fi][fj + 1];
                ++pool[fi][fj];
            }
        }
        
        --hand[fi][fj];
        --hand[fi][11];
        --hold[fi][fj];
        --hold[fi][11];
        bunkaiList.sort(null);
        bunkaiList.forEach(System.out::println);
        if (bunkaiList.isEmpty()) {
            System.err.println("bunkaiList.isEmpty()   focus: " + focus);
            return false;
        }
        return bunkaiList.get(0).fan >= game.shibariBase;
    }
    
    public boolean chankanOk(boolean isKakan) {
        return satisfyShibari() &&
              (isKakan || (bunkaiList.get(0).fan >= YAKUMAN_THRESHOLD));
    }
    
    public boolean ok() {
        return satisfyShibari();
    }
    
    /** pair + deJyantou -> deKoutsu(07~01)
      -> deJuntsu(18~12) -> deKoutsu(19~11)
      -> deJuntsu(28~22) -> deKoutsu(29~21)
      -> deJuntsu(38~32) -> deKoutsu(39~31)
    */
    private void deJyantou(final int baseFu, int i /* excepted kind of pair */) {
        if (i == 0) {
            for (int j = 1; j <= 7; ++j) {
                if (pool[0][j] == 2) {
                    pool[0][j]  = 0;
                    pool[0][11]-= 2;
                    pairId = Card.getId(0, j);
                    int pairFu = 0;
                    if (j == game.bafuu || j >= 5)
                        pairFu += 2;
                    if (j == player.jifuu)
                        pairFu += 2;
                    deKoutsu(baseFu + pairFu, 0, 7);
                    pool[0][11]+= 2;
                    pool[0][j]  = 2;
                }
            }
        } else {
            for (int j = 1; j <= 9; ++j) {
                if (pool[i][j] >= 2) {
                    pool[i][j] -= 2;
                    pool[i][11]-= 2;
                    pairId = Card.getId(i, j);
                    deKoutsu(baseFu, 0, 7);
                    pool[i][11]+= 2;
                    pool[i][j] += 2;
                }
            }
        }
        return;
    }
    
    private void deKoutsu(final int baseFu, final int i, int j) {
        for ( ; j >= 1; --j) {
            if (pool[i][j] >= 3) {
                pool[i][j] -= 3;
                pool[i][11]-= 3;
                furo[fIndex++] = Furo.makeKoutsu(true, i, j);
                deKoutsu(baseFu, i, j - 1);
                --fIndex;
                pool[i][11]+= 3;
                pool[i][j] += 3;
            }
        }
        if (pool[i][11] == 0) {
            if (i == 3)
                deFinal(baseFu);
            else
                deJuntsu(baseFu, i + 1, 8);
        }
        return;
    }
    
    private void deJuntsu(final int baseFu, final int i, int j) {
        for ( ; j >= 2; --j) {
            if (pool[i][j - 1] > 0 && pool[i][j] > 0 && pool[i][j + 1] > 0) {
                --pool[i][j - 1];
                --pool[i][j];
                --pool[i][j + 1];
                pool[i][11]-= 3;
                furo[fIndex++] = Furo.makeJuntsu(i, j);
                deJuntsu(baseFu, i, j);
                --fIndex;
                pool[i][11]+= 3;
                ++pool[i][j + 1];
                ++pool[i][j];
                ++pool[i][j - 1];
            }
        }
        deKoutsu(baseFu, i, 9);
        return;
    }
    
    private void deFinal(int baseFu) {
        Furo[] origin = furo.clone();
        Arrays.sort(furo, null);
        baseFu += furo[0].getFu() + furo[1].getFu() +
                  furo[2].getFu() + furo[3].getFu();
        Bunkai temp = new Bunkai(baseFu + 20);
        Yaku.normalSet.forEach(y -> temp.tryInsert(y));
        bunkaiList.add(temp);
        furo = origin;
        return;
    }
    
    /** 確定和牌之後使用、會加入和了牌計算dora */
    public Bunkai summarize(List<Card> doraList) {
        player.fuda.add(focus);
        ++hold[fi][fj];
        int omote = 0;
        int ura = 0;
        int index = 0;
        for (Card c: doraList) {
            if ((++index & 1) == 1) {
                ura   += hold[c.vi][DORA_TO[c.vi][c.vj]];
            } else {
                omote += hold[c.vi][DORA_TO[c.vi][c.vj]];
            }
        }
        int aka = 0;
        for (Card c: player.fuda) {
            aka += c.order & 1;
        }
        // 取最高分數的分解方式
        Bunkai res = bunkaiList.get(0);
        if (omote > 0) {
            res.insertDora(Yaku.Dora, omote);
        }
        if (player.hasRichied()) {
            res.insertDora(Yaku.UraDora, ura);
        }
        if (aka > 0) {
            res.insertDora(Yaku.AkaDora, aka);
        }
        return res;
    }
    
    public static String calcTitle(int totalFan, int totalFu) {
        if (totalFan < YAKUMAN_THRESHOLD) {
            switch (totalFan) {
                case  0:
                case  1:
                case  2:
                    return "";
                case  3:
                    return (totalFu > 60) ? "満貫" : "";
                case  4:
                    return (totalFu > 30) ? "満貫" : "";
                case  5:
                    return "満貫";
                case  6:
                case  7:
                    return "跳満";
                case  8:
                case  9:
                case 10:
                    return "倍満";
                case 11:
                case 12:
                    return "三倍満";
                default:
                    return "数え役満";
            }
        } else {
            return "役満";
        }
    }
    
    public static int calcPoint(int totalFan, int totalFu) {
        if (totalFan < YAKUMAN_THRESHOLD) {
            switch (totalFan) {
                case  0:
                    return totalFu * 4;
                case  1:
                    return totalFu * 8;
                case  2:
                    return totalFu * 16;
                case  3:
                    return (totalFu > 60) ? 2000 : (totalFu * 32);
                case  4:
                    return (totalFu > 30) ? 2000 : (totalFu * 64);
                case  5:
                    return 2000;
                case  6:
                case  7:
                    return 3000;
                case  8:
                case  9:
                case 10:
                    return 4000;
                case 11:
                case 12:
                    return 6000;
                default:
                    return totalFan / 13 * 8000;
            }
        } else {
            return (totalFan >> YAKUMAN_SHIFT) * 8000;
        }
    }
    
}
