import java.util.ArrayList;
import java.util.List;

abstract class Player {
    protected static final int[] REMAIN = new int[] { 11, 8, 5, 2 };
    protected static Game game;
    /** ========================= open information ========================= */
    protected final String name;
    protected final int seat;
    protected final int prevSeat;
    protected final List<Furo> furo   = new ArrayList<>(4);
    protected final List<Card> kawa   = new ArrayList<>(24);
    protected final List<Card> nakare = new ArrayList<>(12);
    protected boolean wrichi;
    protected boolean ippatsu;
    protected boolean menchin;
    protected boolean[][] genbuu;   // 他人視角的安牌
    protected int richiIndex;       // 立直時的河底位置、 -1 表示沒立直
    protected int jifuu;
    protected int point;
    /** ==================================================================== */
    protected Analyze analyze;      // 確定為和牌型後才會使用
    protected final List<Card> fuda = new ArrayList<>(16);
    protected int[][] hand;         // 手牌 array表示
    protected int[][] hold;         // 手牌 + 副露牌     index11是該花色牌數總和
    protected boolean furiten;
    protected boolean[][] tenpai;   // 可以聽的牌、只要有聽牌[0][0]就會是true
    
    protected Player(int seat, String name, int initPoint) {
        this.seat = seat;
        this.name = name;
        point = initPoint;
        prevSeat = Game.SEQUENCE[seat + 3];
    }
    
    protected void newRoundReset(int newJifuu) {
        furo.clear();
        kawa.clear();
        nakare.clear();
        wrichi = false;
        menchin = true;
        genbuu = new boolean[4][10];
        richiIndex = -1;
        jifuu = newJifuu;
        fuda.clear();
        hand = new int[4][12];
        hold = new int[4][12];
        furiten = false;
        menchin = true;
        tenpai = new boolean[4][10];
        return;
    }
    
    private void insertHold(Card c) {
        ++hold[c.vi][c.vj];
        ++hold[c.vi][Card.TOTAL];
        return;
    }
    
    private void insertHand(Card c) {
        ++hand[c.vi][c.vj];
        ++hand[c.vi][Card.TOTAL];
        return;
    }
    
    private void removeHand(Card c) {
        --hand[c.vi][c.vj];
        --hand[c.vi][Card.TOTAL];
        return;
    }
    
    private void insertBoth(Card c) {
        ++hand[c.vi][c.vj];
        ++hand[c.vi][Card.TOTAL];
        ++hold[c.vi][c.vj];
        ++hold[c.vi][Card.TOTAL];
        return;
    }
    
    private void removeBoth(Card c) {
        --hand[c.vi][c.vj];
        --hand[c.vi][Card.TOTAL];
        --hold[c.vi][c.vj];
        --hold[c.vi][Card.TOTAL];
        return;
    }
    
    protected void dealCard(Card c) {
        fuda.add(c);
        insertBoth(c);
        return;
    }
    
    protected void sortFuda(boolean recomputeTenpai) {
        fuda.sort(null);
        if (recomputeTenpai) {
            tenpai = computeTenpai(hand, menchin);
        }
        return;
    }
    
    protected Card discardCard(React ra) {
        switch (ra.type) {
            case KIRU:
                fuda.remove(ra.drop);
                removeBoth(ra.drop);
                if (ra.join != null) {
                    fuda.add(ra.join);
                    insertBoth(ra.join);
                }
                sortFuda(true);
                break;
            case KRGR:
                fuda.set(fuda.indexOf(ra.drop), ra.join);
                sortFuda(false);
                break;
            case TMGR:
                break;
            default:
                System.err.println("Unexpected discardCard: " + ra.type);
        }
        
        kawa.add(ra.drop);
        genbuu[ra.drop.vi][ra.drop.vj] = true;
        furiten = false;
    FURITEN_LOOP:
        for (int i = 3; i >= 0; --i) {
            for (int j = Card.INDEX_BOUND[i]; j >= 1; --j) {
                if (tenpai[i][j] && genbuu[i][j]) {
                    furiten = true;
                    break FURITEN_LOOP;
                }
            }
        }
        return ra.drop;
    }
    
    protected abstract React getDrawReact(List<React> reactList);
    
    /** 摸牌之後可以做什麼、index0是ツモ切る */
    protected final List<React> getDrawReactList(Card focus) {
        List<React> reactList = new ArrayList<>(20);
        reactList.add(React.tmgr(focus));
        
        if (tenpai[focus.vi][focus.vj] && (analyze = new Analyze(this, focus)).ok()) {
            reactList.add(React.tsumo(focus));
            // 振聽會在discardCard判定
        }
        
        insertHand(focus);
        
        if (richiIndex < 0) {   // 還沒立直
            // 打出其他手牌、若id與focus的一樣是空切る
            for (Card c: fuda) {
                reactList.add((focus.id == c.id) ?
                    React.krgr(focus, c) :
                    React.kiru(focus, c));
            }
            if (menchin && game.richiable() && point >= 1000) {
                List<Card> thrownable = new ArrayList<>(16);
                // 如果原本就有聽牌、那一定可以丟摸到的牌立直
                if (tenpai[0][0]) {
                    thrownable.add(focus);
                }
                // 如果去掉某張牌還可以聽牌表示那張牌可以被丟掉
                for (Card c: fuda) {
                    removeHand(c);
                    if (computeTenpai(hand, true)[0][0]) {
                        thrownable.add(c);
                    }
                    insertHand(c);
                }
                if (!thrownable.isEmpty()) {
                    reactList.add(React.richi(focus, thrownable));
                }
            }
            if (game.kanable()) {
                for (Card c: fuda) {
                    if (hand[c.vi][c.vj] == 4) {
                        reactList.add(React.ankan(focus, findFuda(c)));
                    }
                }
                int index = 0;
                for (Furo f: furo) {
                    if (hand[f.vi][f.vj] == 1 && f.type == Furo.Type.MINKOUTSU) {
                        reactList.add(React.kakan(focus, findFuda(f.focus), index));
                    }
                    ++index;
                }
            }
        } else if (hand[focus.vi][focus.vj] == 4 &&
                   richiKanable(focus) && game.kanable()) {
            // 立直後能做的事只剩下用focus暗槓
            reactList.add(React.ankan(focus, findFuda(focus)));
        }
        
        if (game.firstTurn && kindOfYaochu() >= 9) {
            reactList.add(React.kind9(focus));
        }
        removeHand(focus);
        return reactList;
    }
    
    protected abstract React getRichiReact(List<React> reactList);
    
    /** 宣告立直之後要打哪一張 */
    protected final List<React> getRichiReactList(React ra) {
        richiIndex = kawa.size();
        List<React> reactList = new ArrayList<>(ra.cardList.size());
        for (Card c: ra.cardList) {
            reactList.add((ra.join    == c   ) ? React.tmgr(c) : (
                          (ra.join.id == c.id) ? React.krgr(ra.join, c) :
                                                 React.kiru(ra.join, c)));
        }
        return reactList;
    }
    
    protected void doRichi() {
        ippatsu = true;
        wrichi = game.firstTurn;  // 雙立直一定是在還可以中途流局的時候
        point -= 1000;
        return;
    }
    
    protected Card doAnkan(React ra) {
        fuda.removeAll(ra.cardList);
        ra.cardList.forEach(c -> removeHand(c));
        if (ra.join != null) {  // 第四張原本就在手牌裡
            fuda.add(ra.join);
            insertBoth(ra.join);
        }
        furo.add(Furo.ankan(ra.drop));
        sortFuda(true);
        return ra.drop;
    }
    
    protected Card doKakan(React ra) {
        if (ra.join != null) {  // 用手牌加槓
            fuda.add(ra.join);
            insertBoth(ra.join);
        }
        furo.set(ra.index, Furo.kakan(furo.get(ra.index)));
        sortFuda(true);
        return ra.drop;
    }
    
    protected abstract React getNormalReact(List<React> reactList);
    
    /** 看到別人丟的牌可以有哪些行動、回傳順序為優先度高到低 */
    protected final List<React> getNormalReactList(Card focus) {
        final int si = focus.vi;
        final int sj = focus.vj;
        List<React> reactList = new ArrayList<>(8);
        
        // 如果自己已立直那麼這張牌會變成安全牌
        genbuu[si][sj] |= richiIndex >= 0;
        
        if (tenpai[si][sj]) {
            if (!furiten && (analyze = new Analyze(this, focus)).ok()) {
                reactList.add(React.ron(focus));
            }
            furiten = true;
        }
        
        // 自己還沒立直且不是河底牌才可以叫牌
        if (richiIndex < 0 && !game.lastCard()) {
            // 吃上家、不能是字牌
            if (game.cp.seat == prevSeat && si > 0) {
                Card l2 = null, l1 = null, r1 = null, r2 = null;
                for (int i = fuda.size() - 1; i >= 0; --i) {
                    Card c = fuda.get(i);
                    switch (c.id - focus.id) {
                        case -2: l2 = c; break;
                        case -1: l1 = c; break;
                        case +1: r1 = c; break;
                        case +2: r2 = c; break;
                        default: // nouse
                    }
                }
                if (l2 != null && l1 != null && // (x-2)(x-1)[x]
                        REMAIN[furo.size()] != hand[si][sj] + hand[si][sj - 3]) {
                    reactList.add(React.chi(focus, l2, l1, -3));
                }
                if (l1 != null && r1 != null && // (x-1)[x](x+1)
                        REMAIN[furo.size()] != hand[si][sj]) {
                    reactList.add(React.chi(focus, l1, r1,  0));
                }
                if (r1 != null && r2 != null && // [x](x+1)(x+2)
                        REMAIN[furo.size()] != hand[si][sj] + hand[si][sj + 3]) {
                    reactList.add(React.chi(focus, r1, r2, +3));
                }
            }
            if (hand[si][sj] >= 2) {
                List<Card> sameCard = findFuda(focus);
                reactList.add(React.pon(focus, sameCard.subList(0, 2)));
                if (hand[si][sj] == 3 && game.kanable()) {
                    reactList.add(React.kan(focus, sameCard));
                }
            }
        }
        
        reactList.add(React.pass(focus));
        return reactList;
    }
    
    protected abstract React getChiponReact(List<React> reactList);
    
    /** 吃碰完後要丟哪一張 */
    protected final List<React> getChiponReactList(React ra) {
        List<React> reactList = new ArrayList<React>(12);
        for (Card c: fuda) {    // 相同的牌與+index的牌規則上不能於此時打出
            if (c.id != ra.drop.id && c.id != ra.drop.id + ra.index) {
                reactList.add(React.kiru(null, c));
            }
        };
        return reactList;
    }
    
    protected void doChipon(React ra) {
        menchin = false;
        insertHold(ra.drop);
        fuda.removeAll(ra.cardList);
        ra.cardList.forEach(c -> removeHand(c));
        furo.add(ra.type == React.Type.PON ?
                 Furo.pon(ra.drop) : Furo.chi(ra.drop, ra.index));
        // sortFuda in discardCard
        return;
    }
    
    protected void doKan(React ra) {
        menchin = false;
        insertHold(ra.drop);
        fuda.removeAll(ra.cardList);
        ra.cardList.forEach(c -> removeHand(c));
        furo.add(Furo.kan(ra.drop));
        sortFuda(true);
        return;
    }
    
    protected abstract React getChankanReact(List<React> reactList);
    
    /** 別人暗槓或加槓的時候可以有哪些行動 */
    protected final List<React> getChankanReactList(Card focus, boolean isKakan) {
        // 如果自己已立直那麼這張牌會變成安全牌
        genbuu[focus.vi][focus.vj] |= richiIndex >= 0;
        
        boolean ronable = false;
        if (tenpai[focus.vi][focus.vj]) {
            if (!furiten && (analyze = new Analyze(this, focus)).chankanOk(isKakan)) {
                ronable = true;
            }
            furiten = true;
        }
        return ronable ? List.of(React.ron(focus), React.pass(focus)) :
                         List.of(React.pass(focus));
    }
    
    /** 回傳所有一樣id的手牌 */
    private List<Card> findFuda(Card x) {
        List<Card> res = new ArrayList<Card>(4);
        for (Card c: fuda) {
            if (c.id == x.id)
                res.add(c);
        }
        return res;
    }
    
    protected final void nakareru() {
        nakare.add(kawa.remove(kawa.size() - 1));
        return;
    }
    
    /** 確認目標牌是否來自此玩家 */
    protected final boolean isFromPlayer(Card target) {
        return nakare.contains(target);
    }
    
    protected final boolean isTenpai() {
        return tenpai[0][0];
    }
    
    protected final boolean hasRichied() {
        return richiIndex >= 0;
    }
    
    protected final boolean hasIppatsu() {
        return hasRichied() && ippatsu;
    }
    
    protected final boolean isNagashimangan() {
        if (nakare.isEmpty()) {
            for (Card c: kawa) {
                if (!c.yao9)
                    return false;
            }
            return true;
        }
        return false;
    }
    
    /** Game檢測四風連打用 東0b00010 南0b00100 西0b01000 北0b10000 */
    protected final int getKawaFirst() {
        int cid = kawa.isEmpty() ? 5 : kawa.get(0).id;
        return (cid < 5) ? (1 << cid) : 0;
    }
    
    protected final int kindOfYaochu() {
        int cnt = 0;
        for (int[] z: Card.YAOCHU_INDEXES) {
            if (hand[z[0]][z[1]] > 0)
                ++cnt;
        }
        return cnt;
    }
    
    protected final String getHoldingState() {
        StringBuilder infoBuilder = new StringBuilder(64);
        fuda.forEach(c -> infoBuilder.append(c));
        furo.forEach(f -> infoBuilder.append(' ').append(f));
        return infoBuilder.toString();
    }
    
    protected final String cheatInformation() {
        StringBuilder sb = new StringBuilder(192);
        sb.append(String.format("`%s'   furiten=%s%n", name, furiten));
        sb.append("__|_1__2__3__4__5__6__7__8__9_");
        for (int i = 0; i <= 3; ++i) {
            sb.append(String.format("%n%2d|", hand[i][Card.TOTAL]));
            for (int j = 1; j <= 9; ++j) {
                if (tenpai[i][j]) {
                    sb.append('(').append(hand[i][j]).append(')');
                } else {
                    sb.append(' ').append(hand[i][j]).append(' ');
                }
            }
        }
        return sb.toString();
    }
    
    /** https://ja.wikipedia.org/wiki/%E7%AB%8B%E7%9B%B4
        4.1 リーチ後の暗槓が認められないケース
    */
    private boolean richiKanable(Card focus) {
        final int fi = focus.vi;
        final int fj = focus.vj;
        if (fi == 0) {          // 字牌一定可
            return true;
        }
        if (tenpai[fi][fj]) {   // 聽牌之一不可
            return false;
        }
        
        // [3N+1] 如果作為雀頭還有聽牌→此牌參與某組順子→不可
        if (hand[fi][Card.TOTAL] % 3 == 1) {
            boolean stillTenpai = false;
            --hand[fi][Card.TOTAL];
            hand[fi][fj] -= 2;
            for (int j = 1; j <= 9; ++j) {
                ++hand[fi][j];
                stillTenpai |= testJuntsu(hand[fi], 8);
                --hand[fi][j];
            }
            hand[fi][fj] += 2;
            ++hand[fi][Card.TOTAL];
            return !stillTenpai;
        }
        
        // [3N] 抽掉此牌組成的順子後可以完整分解→此牌參與某組順子→不可
        if (hand[fi][Card.TOTAL] % 3 == 0) {
            boolean stillTenpai = false;
            hand[fi][Card.TOTAL] -= 3;
            --hand[fi][fj];
            if (fj - 2 >= 1 && hand[fi][fj - 2] > 0 && hand[fi][fj - 1] > 0) {
                --hand[fi][fj - 2];
                --hand[fi][fj - 1];
                stillTenpai |= testJuntsu(hand[fi], 8);
                ++hand[fi][fj - 1];
                ++hand[fi][fj - 2];
            }
            if (hand[fi][fj - 1] > 0 && hand[fi][fj + 1] > 0) {
                --hand[fi][fj - 1];
                --hand[fi][fj + 1];
                stillTenpai |= testJuntsu(hand[fi], 8);
                ++hand[fi][fj + 1];
                ++hand[fi][fj - 1];
            }
            if (fj + 2 <= 9 && hand[fi][fj + 1] > 0 && hand[fi][fj + 2] > 0) {
                --hand[fi][fj + 1];
                --hand[fi][fj + 2];
                stillTenpai |= testJuntsu(hand[fi], 8);
                ++hand[fi][fj + 2];
                ++hand[fi][fj + 1];
            }
            ++hand[fi][fj];
            hand[fi][Card.TOTAL] += 3;
            return !stillTenpai;
        }
        
        boolean stillTenpai = false;
        hand[fi][Card.TOTAL] -= 2;
        // 作為雀頭時
        --hand[fi][fj];
        stillTenpai |= testJuntsu(hand[fi], 8);
        ++hand[fi][fj];
        // 作為順子時
        if (fj - 2 >= 1 && hand[fi][fj - 2] > 0 && hand[fi][fj - 1] > 0) {
            --hand[fi][fj - 2];
            --hand[fi][fj - 1];
            for (int j = 1; j <= 9; ++j) {
                ++hand[fi][j];
                stillTenpai |= testJuntsu(hand[fi], 8);
                --hand[fi][j];
            }
            ++hand[fi][fj - 1];
            ++hand[fi][fj - 2];
        }
        if (hand[fi][fj - 1] > 0 && hand[fi][fj + 1] > 0) {
            --hand[fi][fj - 1];
            --hand[fi][fj + 1];
            for (int j = 1; j <= 9; ++j) {
                ++hand[fi][j];
                stillTenpai |= testJuntsu(hand[fi], 8);
                --hand[fi][j];
            }
            ++hand[fi][fj + 1];
            ++hand[fi][fj - 1];
        }
        if (fj + 2 <= 9 && hand[fi][fj + 1] > 0 && hand[fi][fj + 2] > 0) {
            --hand[fi][fj + 1];
            --hand[fi][fj + 2];
            for (int j = 1; j <= 9; ++j) {
                ++hand[fi][j];
                stillTenpai |= testJuntsu(hand[fi], 8);
                --hand[fi][j];
            }
            ++hand[fi][fj + 2];
            ++hand[fi][fj + 1];
        }
        hand[fi][Card.TOTAL] += 2;
        return !stillTenpai;
    }
    
    /** 計算當前手牌的聽牌、若有聽牌 result[0][0] == true */
    protected static boolean[][] computeTenpai(final int[][] pool, boolean menchin) {
        boolean[][] result = new boolean[4][10];
        
        // 特殊和牌型必須門清
        if (menchin) {
            // 国士無双只需要測試各種么九牌
            for (int[] z: Card.YAOCHU_INDEXES) {
                ++pool[z[0]][z[1]];
                boolean res = Yaku.Kokushimusou.check(pool);
                --pool[z[0]][z[1]];
                result[0][0] |= (result[z[0]][z[1]] = res);
            }
            // 七対子只能有一個單張且其他牌只能是0或2張
            int[] pair = null;
        CHITOI_LOOP:
            for (int i = 0; i <= 3; ++i) {
                for (int j = Card.INDEX_BOUND[i]; j >= 1; --j) {
                    if (pool[i][j] == 1) {
                        if (pair == null) {
                            pair = new int[] { i, j };
                        } else {
                            pair = null;
                            break CHITOI_LOOP;
                        }
                    } else if (pool[i][j] >= 3) {
                        pair = null;
                        break CHITOI_LOOP;
                    }
                }
            }
            if (pair != null) {
                result[0][0] = result[pair[0]][pair[1]] = true;
            }
        }
        
        // 正常和牌型最多兩種花色無法完全分解
        List<Integer> noPassList = new ArrayList<Integer>(4);
        if (pool[0][Card.TOTAL] != 0 && !testKoutsu(pool[0], 7))
            noPassList.add(0);
        if (pool[1][Card.TOTAL] != 0 && !testJuntsu(pool[1], 8))
            noPassList.add(1);
        if (pool[2][Card.TOTAL] != 0 && !testJuntsu(pool[2], 8))
            noPassList.add(2);
        if (pool[3][Card.TOTAL] != 0 && !testJuntsu(pool[3], 8))
            noPassList.add(3);
        if (noPassList.size() > 2)
            return result;
        
        int kind1 = noPassList.get(0);
        // 只有一種的情況雀頭一定是此種花色
        if (noPassList.size() == 1) {
            ++pool[kind1][Card.TOTAL];
            // 每個位置補一張從雀頭開始分解
            for (int j = Card.INDEX_BOUND[kind1]; j >= 1; --j) {
                ++pool[kind1][j];
                if (testJyantou(pool[kind1], Card.INDEX_BOUND[kind1])) {
                    result[0][0] = result[kind1][j] = true;
                }
                --pool[kind1][j];
            }
            --pool[kind1][Card.TOTAL];
            return result;
        }
        // 剩的兩種花色只能各是3n+2張
        if (pool[kind1][Card.TOTAL] % 3 != 2) {
            return result;
        }
        
        int kind2 = noPassList.get(1);
        // 雀頭為kindX並且可以完全分解則在kindY每個位置補一張測試
        if (testJyantou(pool[kind1], Card.INDEX_BOUND[kind1])) {
            ++pool[kind2][Card.TOTAL];
            for (int j = Card.INDEX_BOUND[kind2]; j >= 1; --j) {
                ++pool[kind2][j];
                if (testJuntsu(pool[kind2], 8)) {   // kind2 != 0
                    result[0][0] = result[kind2][j] = true;
                }
                --pool[kind2][j];
            }
            --pool[kind2][Card.TOTAL];
        }
        if (testJyantou(pool[kind2], Card.INDEX_BOUND[kind2])) {
            ++pool[kind1][Card.TOTAL];
            for (int j = Card.INDEX_BOUND[kind1]; j >= 1; --j) {
                ++pool[kind1][j];
                if (kind1 == 0 ? testKoutsu(pool[0], 7) :
                                 testJuntsu(pool[kind1], 8)) {
                    result[0][0] = result[kind1][j] = true;
                }
                --pool[kind1][j];
            }
            --pool[kind1][Card.TOTAL];
        }
        return result;
    }
    
    /** 嘗試能否完整分解某種花色
        testJyantou 從雀頭開始
        testJuntsu  從順子開始
        testKoutsu  從刻子開始
        @p          待檢測花色的手牌
        @startJ     開始檢測的index
    */
    private static boolean testJyantou(int[] p, int startJ) {
        for (int j = startJ; j >= 1; --j) {
            if (p[j] >= 2) {
                p[j] -= 2;
                p[Card.TOTAL] -= 2;
                boolean res = startJ == 7 ?
                              testKoutsu(p, 7) :
                              testJuntsu(p, 8);
                p[Card.TOTAL] += 2;
                p[j] += 2;
                if (res)
                    return true;
            }
        }
        return false;
    }
    
    private static boolean testJuntsu(int[] p, int j) {
        for ( ; j >= 2; --j) {
            if (p[j - 1] > 0 && p[j] > 0 && p[j + 1] > 0) {
                --p[j - 1];
                --p[j];
                --p[j + 1];
                p[Card.TOTAL] -= 3;
                boolean res = testJuntsu(p, j);
                p[Card.TOTAL] += 3;
                ++p[j + 1];
                ++p[j];
                ++p[j - 1];
                if (res)
                    return true;
            }
        }
        return p[Card.TOTAL] == 0 || testKoutsu(p, 9);
    }
    
    private static boolean testKoutsu(int[] p, int j) {
        for ( ; j >= 1; --j) {
            if (p[j] >= 3) {
                p[j] -= 3;
                p[Card.TOTAL] -= 3;
                boolean res = testKoutsu(p, j - 1);
                p[Card.TOTAL] += 3;
                p[j] += 3;
                return res;
            }
        }
        return p[Card.TOTAL] == 0;
    }
    
}
