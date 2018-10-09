import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.OutputStream;
import java.io.PrintWriter;

abstract class Game {
    enum EndReason {
        TSUMO   (false, false, ""),
        RON_1   (false, false, ""),
        RON_2   (false, false, ""),
        HOWANPAI(false, true,  "流　　　局"),
        KIND9   (true,  true,  "九　種　九　牌"),
        FUU_4   (true,  true,  "四　風　連　打"),
        RICHI_4 (true,  true,  "四　家　立　直"),
        KAN_4   (true,  true,  "四　槓　散　了"),
        RON_3   (true,  true,  "三　　家　　和");
        
        public final boolean midgameEnd;
        public final boolean increaseBonba;
        public final String text;
        
        private EndReason(boolean e, boolean ib, String t) {
            midgameEnd = e;
            increaseBonba = ib;
            text = t;
        }
        
    }
    
    enum RoundPhase {
        NORMAL_DRAW_DISCARD(true,  false, false),
        ANKAN_DRAW_DISCARD (true,  true,  false),
        KAN_DRAW_DISCARD   (true,  true,  false),
        RICHI_DISCARD      (true,  false, false),
        CHIPON_DISCARD     (true,  false, false),
        NORMAL_REACT       (false, false, false),
        ANKAN_REACT        (false, false, true),
        KAKAN_REACT        (false, false, true);
        
        public final boolean selfTurn;
        public final boolean rinshanable;
        public final boolean chankanable;
        
        private RoundPhase(boolean s, boolean r, boolean c) {
            selfTurn = s;
            rinshanable = r;
            chankanable = c;
        }
        
    }
    
    private static Game currentGameInstance = null;
    private static final PrintWriter logger;
    private static final HashMap<String, Integer> gameInfo = new HashMap<>(16);
    public  static final char[][] INFO_CHAR = {
        { '？', '東', '南', '西', '北' }, { '？', '１', '２', '３', '４' }
    };
    public  static final int[] SEQUENCE = { 4, 1, 2, 3, 4, 1, 2, 3 };
    private static final int[] dealCardArray = {
        0, 1, 2, 3, // deal 3 tiles
        0, 1, 2, 3, // deal 3 tiles
        0, 1, 2, 3, // deal 3 tiles
        0, 1, 2, 3, // deal 1 tiles
        0
    };
    
    protected final Player[] player;
    protected final LinkedList<Card> yama;
    protected final int bafuu;
    protected final int shibari;
    protected final int oyaSeat;
    protected int richibouOnTable;
    protected int[][] appearance = new int[4][10];  // 場上已知出現牌數
    protected boolean firstTurn = true;
    protected RoundPhase phase = RoundPhase.NORMAL_DRAW_DISCARD;
    protected Player cp = null; // CurrentPlayer
    
    private final boolean[] richiPending = new boolean[5];
    private final int[] kantsuCount = new int[5];
    private final List<ReactManager> managerList = new ArrayList<>(3);
    private final List<Card> doraList = new ArrayList<>(10);
    private int doraCount = 0;
    private int dealCardIndex = 0;
    private boolean gameRenchan;// set right before returning from startRound()
    private EndReason endReason;//   and used in calculate() & updateGameInfo()
    
    static {
        OutputStream logOS = System.out;
        String logFileName = Long.toString(System.currentTimeMillis()) + ".txt";
        try {
            logOS = new java.io.FileOutputStream(logFileName);
        } catch (Exception ouch) {
            ouch.printStackTrace();
            System.err.println("Failed to create log file.");
        }
        logger = new PrintWriter(logOS, true, java.nio.charset.Charset.forName("UTF-8"));
        logger.println("(This is just a log file and thus can be deleted.)");
    }
    
    /** 處理其他玩家對當前玩家丟牌或槓牌的反應
        於constructor記下玩家(who)及可執行的最高優先度行動種類(priority)
        玩家真正決定行動後會執行setReact並通知processReact進行判斷
    */
    final class ReactManager implements Comparable<ReactManager> {
        private final React.Type priority;
        private final Player who;
        private React dicision = React.defaultPass;
        private boolean done = false;
        
        public ReactManager(Player p, List<React> reactList) {
            priority = reactList.get(0).type;
            who = p;
        }
        
        public void decide(AtomicInteger counter, React ra) {
            dicision = ra;
            done = true;
            synchronized (counter) {
                counter.decrementAndGet();
                counter.notify();
            }
            return;
        }
        
        public boolean hasPrivilege(ReactManager o) {
            return done && (this == o ||
                dicision.type.compareTo(o.done ? o.dicision.type : o.priority) < 0);
        }
        
        @Override
        public int compareTo(ReactManager o) {
            return dicision.compareTo(o.dicision);
        }
        
    }
    
    /** 當某玩家做出的決定足以優先過其他玩家的決定便可中斷其他玩家思考 */
    private SimpleEntry<Player, React> waitNext(AtomicInteger counter,
            ThreadGroup managerThreadGroup, RoundPhase nextRoundPhase) {
    THINKING_LOOP:
        while (counter.get() > 0) {
            synchronized (counter) {
                try {
                    counter.wait(500);
                } catch (Exception ouch) {
                    ouch.printStackTrace();
                }
            }
            int size = managerList.size();
            for (ReactManager p: managerList) {
                int inturruptRequirement = size;
                for (ReactManager q: managerList) {
                    if (p.hasPrivilege(q))
                        --inturruptRequirement;
                }
                if (inturruptRequirement == 0) {
                    managerThreadGroup.interrupt();
                    counter.set(0);
                    break THINKING_LOOP;
                }
            }
        }
        phase = nextRoundPhase;
        managerList.sort(null);
        React finalReact = managerList.isEmpty() ?
                           React.defaultPass : managerList.get(0).dicision;
        if (finalReact.type == React.Type.PASS) {
            return new SimpleEntry<>(
                phase == RoundPhase.NORMAL_DRAW_DISCARD ?
                         player[SEQUENCE[cp.seat + 1]] : cp,
                React.defaultPass);
        }
        Player reactPlayer = managerList.get(0).who;
        if (finalReact.type == React.Type.RON) {
            // 如果有玩家和牌、移除非和牌類型
            Iterator<ReactManager> it = managerList.listIterator();
            while (it.hasNext()) {
                ReactManager rm = it.next();
                if (rm.dicision.type != React.Type.RON) {
                    it.remove();
                } else {
                    recordPlayerAction(rm.who, rm.dicision);
                }
            }
        } else {
            recordPlayerAction(reactPlayer, finalReact);
        }
        return new SimpleEntry<>(reactPlayer, finalReact);
    }
    
    /** called in FxApplicationThread */
    protected Game(Player[] player, LinkedList<Card> shuffledYama) {
        Player.game = currentGameInstance = this;
        this.player = player;
        yama = shuffledYama;
        bafuu = gameInfo.get("bafuu");
        shibari = gameInfo.get("shibariBase") +
                 (gameInfo.get("bonba") > 4 ? gameInfo.get("shibariPlus") : 0);
        oyaSeat = gameInfo.get("currOyaSeat");
        richibouOnTable = gameInfo.get("richi");
        cp = player[SEQUENCE[oyaSeat + 3]];
        logger.printf(" *** %s *** %n", getKyokuInfo());
        gameInfo.forEach((k ,v) -> logger.printf(" * %s: %d%n", k, v));
        
        for (int i = 1; i <= 4; ++i) {
            player[i].newRoundReset(SEQUENCE[(5 - oyaSeat + i) & 3]);
        }
        // doraList: [02468]UraDora [13579]OmoteDora
        doraList.addAll(yama.subList(4, 14));
    }
    
    protected final String getKyokuInfo() {
        return String.format("%c%c局",
            INFO_CHAR[0][gameInfo.get("bafuu")],
            INFO_CHAR[1][gameInfo.get("kyoku")]);
    }
    
    protected final void dealCard() {
        if (dealCardIndex < 12) {
            int i = SEQUENCE[oyaSeat + dealCardArray[dealCardIndex]];
            player[i].dealCard(yama.removeLast());
            player[i].dealCard(yama.removeLast());
            player[i].dealCard(yama.removeLast());
            player[i].dealCard(yama.removeLast());
        } else if (dealCardIndex < 16) {
            int i = SEQUENCE[oyaSeat + dealCardArray[dealCardIndex]];
            player[i].dealCard(yama.removeLast());
        } else {
            player[SEQUENCE[oyaSeat + 0]].sortFuda(true);
            player[SEQUENCE[oyaSeat + 1]].sortFuda(true);
            player[SEQUENCE[oyaSeat + 2]].sortFuda(true);
            player[SEQUENCE[oyaSeat + 3]].sortFuda(true);
        }
        ++dealCardIndex;
        return;
    }
    
    protected final boolean isReadyToStart() {
        return dealCardIndex == dealCardArray.length;
    }
    
    protected final void startRound() {
        flipDoraIndicator();
        changeCurrentPlayer(player[oyaSeat]);
        React react = null; // reused after Richi & ChiPon
    PHASE_LOOP:
        while (yama.size() > 14) {
            
            /** ======================== self side ======================== */
            
            switch (phase) {
                case NORMAL_DRAW_DISCARD:
                    react = cp.getDrawReact(cp.getDrawReactList(yama.removeLast()));
                    recordPlayerAction(cp, react);
                    break;
                case ANKAN_DRAW_DISCARD:
                    flipDoraIndicator();
                    react = cp.getDrawReact(cp.getDrawReactList(yama.removeFirst()));
                    recordPlayerAction(cp, react);
                    break;
                case KAN_DRAW_DISCARD:
                    react = cp.getDrawReact(cp.getDrawReactList(yama.removeFirst()));
                    recordPlayerAction(cp, react);
                    flipDoraIndicator();
                    break;
                case RICHI_DISCARD:
                    react = cp.getRichiReact(cp.getRichiReactList(react));
                    recordPlayerAction(cp, react);
                    richiPending[cp.seat] = true;
                    break;
                case CHIPON_DISCARD:
                    react = cp.getChiponReact(cp.getChiponReactList(react));
                    recordPlayerAction(cp, react);
                    break;
                default:
                    System.err.println("Unexpected Phase1: " + phase);
            }
            
            /** ======================== do action ======================== */
            
            switch (react.type) {
                case KIND9:
                    gameRenchan = true;
                    endReason = EndReason.KIND9;
                    return;
                case TSUMO:
                    gameRenchan = (cp.seat == oyaSeat);
                    endReason = EndReason.TSUMO;
                    return;
                case KIRU:
                case KRGR:
                case TMGR:
                    cp.discardCard(react);
                    phase = RoundPhase.NORMAL_REACT;
                    break;
                case ANKAN:
                    cp.doAnkan(react);
                    phase = RoundPhase.ANKAN_REACT;
                    break;
                case KAKAN:
                    cp.doKakan(react);
                    phase = RoundPhase.KAKAN_REACT;
                    break;
                case RICHI:
                    phase = RoundPhase.RICHI_DISCARD;
                    continue PHASE_LOOP;
                default:
                    System.err.println("Unexpected SelfReact: " + react.type);
            }
            
            /** ======================== else side ======================== */
            
            managerList.clear();
            AtomicInteger counter = new AtomicInteger(0);
            ThreadGroup managerTG = new ThreadGroup("mtg");
            final Card focus = react.drop;
            SimpleEntry<Player, React> result = null;
            
            switch (phase) {
                case NORMAL_REACT:
                    ++appearance[focus.vi][focus.vj];
                    for (int i = 1; i <= 3; ++i) {
                        Player p = player[SEQUENCE[cp.seat + i]];
                        List<React> reactList = p.getNormalReactList(focus);
                        if (reactList.size() == 1)  // pass
                            continue;
                        ReactManager rm = new ReactManager(p, reactList);
                        managerList.add(rm);
                        counter.incrementAndGet();
                        new Thread(managerTG, () -> {
                            rm.decide(counter, p.getNormalReact(reactList));
                        }).start();
                    }
                    result = waitNext(counter, managerTG,
                                      RoundPhase.NORMAL_DRAW_DISCARD);
                    break;
                case ANKAN_REACT:
                    appearance[focus.vi][focus.vj] = 4;
                    for (int i = 1; i <= 3; ++i) {
                        Player p = player[SEQUENCE[cp.seat + i]];
                        List<React> reactList = p.getChankanReactList(focus, false);
                        if (reactList.size() == 1)  // pass
                            continue;
                        ReactManager rm = new ReactManager(p, reactList);
                        managerList.add(rm);
                        counter.incrementAndGet();
                        new Thread(managerTG, () -> {
                            rm.decide(counter, p.getChankanReact(reactList));
                        }).start();
                    }
                    result = waitNext(counter, managerTG,
                                      RoundPhase.ANKAN_DRAW_DISCARD);
                    deIppatsu();
                    break;
                case KAKAN_REACT:
                    appearance[focus.vi][focus.vj] = 4;
                    for (int i = 1; i <= 3; ++i) {
                        Player p = player[SEQUENCE[cp.seat + i]];
                        List<React> reactList = p.getChankanReactList(focus, true);
                        if (reactList.size() == 1)  // pass
                            continue;
                        counter.incrementAndGet();
                        ReactManager rm = new ReactManager(p, reactList);
                        managerList.add(rm);
                        new Thread(managerTG, () -> {
                            rm.decide(counter, p.getChankanReact(reactList));
                        }).start();
                    }
                    result = waitNext(counter, managerTG,
                                      RoundPhase.KAN_DRAW_DISCARD);
                    deIppatsu();
                    break;
                default:
                    System.err.println("Unexpected Phase2: " + phase);
            }
            
            /** ========================== check ========================== */
            
            react = result.getValue();
            if (react.type == React.Type.RON) {
                gameRenchan = false;
                for (ReactManager rm: managerList) {
                    gameRenchan |= (rm.who == player[oyaSeat]);
                }
                endReason = (managerList.size() == 3) ? EndReason.RON_3 :
                           ((managerList.size() == 2) ? EndReason.RON_2 :
                                                        EndReason.RON_1);
                return;
            }
            if (phase != RoundPhase.NORMAL_DRAW_DISCARD &&
                    ++kantsuCount[cp.seat] < 4 &&
                    ++kantsuCount[0] >= 4) {
                gameRenchan = true;
                endReason = EndReason.KAN_4;
                return;
            }
            
            /** ======================== do action ======================== */
            
            Player nextPlayer = result.getKey();
            if (react.type != React.Type.PASS) {
                deIppatsu();
                cp.nakareru();
                for (Card c: react.cardList) {
                    ++appearance[c.vi][c.vj];
                }
                switch (react.type) {
                    case KAN:
                        nextPlayer.doKan(react);
                        phase = RoundPhase.KAN_DRAW_DISCARD;
                        break;
                    case CHI:
                    case PON:
                        nextPlayer.doChipon(react);
                        phase = RoundPhase.CHIPON_DISCARD;
                        break;
                    default:
                        System.err.println("Unexpected ElseReact: " + react.type);
                }
            }
            
            /** ===================== result & update ===================== */
            
            if (richiPending[cp.seat]) {
                richiPending[cp.seat] = false;
                cp.doRichi();
                increaseRichibou();
                if (player[1].hasRichied() & player[2].hasRichied() &
                    player[3].hasRichied() & player[4].hasRichied()) {
                    gameRenchan = true;
                    endReason = EndReason.RICHI_4;
                    return;
                }
            } else {
                player[cp.seat].ippatsu = false;
            }
            
            if (firstTurn && yama.size() == 80) { // 136 - 13 * 4 - 4
                if ((0b11110 & player[1].getKawaFirst() &
                               player[2].getKawaFirst() &
                               player[3].getKawaFirst() &
                               player[4].getKawaFirst()) != 0) {
                    gameRenchan = true;
                    endReason = EndReason.FUU_4;
                    return;
                }
                firstTurn = false;
            }
            
            changeCurrentPlayer(nextPlayer);
        }
        gameRenchan = player[oyaSeat].isTenpai();
        endReason = EndReason.HOWANPAI;
        return;
    }
    
    /** should be overridden for UI */
    protected int increaseRichibou() {
        return ++richibouOnTable;
    }
    
    /** should be overridden for UI */
    protected void changeCurrentPlayer(Player nextPlayer) {
        cp = nextPlayer;
        logger.printf("`%s' %s%n", cp.name, cp.getHoldingState());
        return;
    }
    
    /** should be overridden for UI */
    protected Card flipDoraIndicator() {
        Card indicator = doraList.get(doraCount + 1);
        ++appearance[indicator.vi][indicator.vj];
        doraCount += 2;
        logger.printf(" * DoraIndicator: %s%n", indicator);
        return indicator;
    }
    
    protected void recordPlayerAction(Player who, React ra) {
        logger.printf("`%s' %s%n", who.name, ra);
        return;
    }
    
    private final void deIppatsu() {
        firstTurn = false;
        player[1].ippatsu = false;
        player[2].ippatsu = false;
        player[3].ippatsu = false;
        player[4].ippatsu = false;
        return;
    }
    
    protected final boolean richiable() {
        return yama.size() > 18;
    }
    
    protected final boolean kanable() {
        return yama.size() > 14 && kantsuCount[0] < 4;
    }
    
    protected final boolean lastCard() {
        return yama.size() == 14;
    }
    
    /** 計算本局結果、會更新player.point
        可以兩家和牌、故使用List
        @return 流局方式標題
                空字串表示有和牌
            @result     非中途流局狀況才會有物件
                @key    此次和牌役種、或Analyze.EMPTY表示流局
                @value  本局各家點數變化、其中index0為桌面上的立直棒數量
            @finalDoraList 前半為ドラ、後半為裏ドラ
    */
    protected final String scoring(
                List<SimpleEntry<Analyze.Bunkai, int[]>> result,
                List<Card> finalDoraList) {
        if (endReason.midgameEnd) {
            logger.printf(" * %s%n", endReason.text);
            return endReason.text;
        }
        int[] diff = new int[] { richibouOnTable, 0, 0, 0, 0 };
        if (endReason == EndReason.HOWANPAI) {
            int tenpaiCount = 0;
            List<Integer> nagaman = new ArrayList<>();
            for (int i = 1; i <= 4; ++i) {
                if (player[i].isTenpai()) {
                    ++tenpaiCount;
                    diff[i] = +1;
                } else {
                    diff[i] = -1;
                }
                if (player[i].isNagashimangan()) {
                    nagaman.add(i);
                }
            }
            if (nagaman.isEmpty()) {
                if ((tenpaiCount & 3) == 0) {   // 0 or 4
                    Arrays.fill(diff, 1, 5, 0);
                } else {
                    int add = 3000 / tenpaiCount;
                    int sub = 3000 / (tenpaiCount - 4);
                    for (int i = 1; i <= 4; ++i) {
                        diff[i] = diff[i] > 0 ? add : sub;
                        player[i].point += diff[i];
                    }
                }
            } else {
                Arrays.fill(diff, 1, 5, 0);
                for (Integer i: nagaman) {
                    if (i == oyaSeat) {
                        diff[1] -= 4000;
                        diff[2] -= 4000;
                        diff[3] -= 4000;
                        diff[4] -= 4000;
                        diff[i] += 4000 + 12000;
                    } else {
                        diff[oyaSeat] -= 2000;
                        diff[1] -= 2000;
                        diff[2] -= 2000;
                        diff[3] -= 2000;
                        diff[4] -= 2000;
                        diff[i] += 2000 + 8000;
                    }
                }
                for (int i = 1; i <= 4; ++i) {
                    player[i].point += diff[i];
                }
            }
            result.add(new SimpleEntry<Analyze.Bunkai, int[]>(Analyze.EMPTY, diff));
            String endText = nagaman.isEmpty() ? endReason.text : "流　し　満　貫";
            logger.printf(" * %s%n%s%n", endText, getPlayerPointString());
            return endText;
        }
        // remove nouse DoraIndicators
        doraList.subList(doraCount, 10).clear();
        StringBuilder doraString = new StringBuilder(64);
        doraString.append("表ドラ表示牌：");
        for (int i = 1; i < doraCount; i += 2) {
            finalDoraList.add(doraList.get(i));
            doraString.append(doraList.get(i));
        }
        doraString.append(System.lineSeparator());
        doraString.append("裏ドラ表示牌：");
        for (int i = 0; i < doraCount; i += 2) {
            finalDoraList.add(doraList.get(i));
            doraString.append(doraList.get(i));
        }
        logger.println(doraString.toString());
        
        if (endReason == EndReason.TSUMO) {
            Analyze.Bunkai bunkai = cp.analyze.summarize(doraList);
            int bonba = gameInfo.get("bonba") * 100;
            int ratio = cp.seat == oyaSeat ? 2 : 1;
            int basic = bunkai.getPoint();
            
            Card pao = bunkai.getPao();
            if (pao == null) {
                for (int i = 1; i <= 3; ++i) {
                    int k = SEQUENCE[cp.seat + i];
                    int p = carry100(basic * (k == oyaSeat ? 2 : ratio)) + bonba;
                    diff[k] -= p;
                    diff[cp.seat] += p;
                }
            } else {
                int whole = carry100(basic * (cp.seat == oyaSeat ? 6 : 4));
                int total = whole + bonba * 3;
                diff[findSource(pao)] -= total;
                diff[cp.seat] += total;
            }
            
            diff[cp.seat] += richibouOnTable * 1000;
            richibouOnTable = 0;
            for (int i = 1; i <= 4; ++i) {
                player[i].point += diff[i];
            }
            result.add(new SimpleEntry<Analyze.Bunkai, int[]>(bunkai, diff));
            logger.println(bunkai.getYakuString() + getPlayerPointString());
            return endReason.text;
        }
        
        int bonba = gameInfo.get("bonba") * 300;
        for (ReactManager rm: managerList) {
            Player op = rm.who;
            Analyze.Bunkai bunkai = op.analyze.summarize(doraList);
            int basic = bunkai.getPoint();
            int whole = carry100(basic * (op.seat == oyaSeat ? 6 : 4));
            
            Card pao = bunkai.getPao();
            if (pao == null) {
                diff[cp.seat] -= bonba + whole;
            } else {
                diff[cp.seat] -= bonba + whole / 2;
                diff[findSource(pao)] -= whole / 2;
            }
            
            diff[op.seat] += bonba + whole + richibouOnTable * 1000;
            bonba = 0;  // calculate once
            richibouOnTable = 0;
            for (int i = 1; i <= 4; ++i) {
                player[i].point += diff[i];
            }
            result.add(new SimpleEntry<Analyze.Bunkai, int[]>(bunkai, diff));
            logger.println(bunkai.getYakuString() + getPlayerPointString());
            diff = new int[5];
        }
        return endReason.text;
    }
    
    protected String getPlayerPointString() {
        return String.format("`%s' %d%n`%s' %d%n`%s' %d%n`%s' %d%n",
            player[1].name, player[1].point,
            player[2].name, player[2].point,
            player[3].name, player[3].point,
            player[4].name, player[4].point
        );
    }
    
    /** 更新各種資訊並回傳是否可繼續下一局遊戲、一定要在每局結束後呼叫一次 */
    protected final boolean updateGameInfo() {
        gameInfo.put("richi", richibouOnTable);
        
        for (int i = 1; i <= 4; ++i) {
            if (player[i].point < 0)
                return false;
        }
        int allLast = gameInfo.get("allLast");
        if (allLast != 0 && onePassPointLimit()) {
            if (allLast == 2 || oyaIsTheHighest() || !gameRenchan)
                return false;
        }
        gameInfo.put("bonba", (endReason.increaseBonba || gameRenchan) ?
                               gameInfo.get("bonba") + 1 : 0);
        if (!gameRenchan) {
            int initOyaSeat = gameInfo.get("initOyaSeat");
            int nextOyaSeat = SEQUENCE[oyaSeat + 1];
            gameInfo.put("kyoku", gameInfo.get("kyoku") + 1);
            
            if (nextOyaSeat == initOyaSeat) {
                gameInfo.put("kyoku", 1);
                int nextBafuu = gameInfo.get("bafuu") + 1;  // 不考慮東入
                int gameLimit = gameInfo.get("totalLength") + 1;
                if (nextBafuu > gameLimit)
                    return false;
                if (nextBafuu == gameLimit)
                    gameInfo.put("allLast", 2);
                gameInfo.put("bafuu", nextBafuu);
            } else if (SEQUENCE[nextOyaSeat + 1] == initOyaSeat &&
                       bafuu == gameInfo.get("totalLength")) {
                gameInfo.put("allLast", 1);
            }
            gameInfo.put("currOyaSeat", nextOyaSeat);
        }
        return true;
    }
    
    private final boolean oyaIsTheHighest() {
        for (int i = 1; i <= 4; ++i) {
            if (i != oyaSeat && player[i].point >= player[oyaSeat].point)
                return false;
        }
        return true;
    }
    
    private final boolean onePassPointLimit() {
        for (int i = 1; i <= 4; ++i) {
            if (player[i].point >= gameInfo.get("limitPoints"))
                return true;
        }
        return false;
    }
    
    /** 尋找目標牌是由哪個坐位的玩家丟的 */
    private final int findSource(Card target) {
        for (int i = 1; i <= 4; ++i) {
            if (player[i].isFromPlayer(target))
                return i;
        }
        System.err.println("Cannot find the source of " + target);
        return 0;
    }
    
    protected static int carry100(int originalValue) {
        return (originalValue + 99) / 100 * 100;
    }
    
    /** 遊戲結束得點計算、會改動Player[]順序並回傳符合順序的double[] */
    protected static double[] getFinalScore(Player[] pl) {
        final int[][] SEAT_BONUS = new int[][] { { 0 },
            { 0, 4, 3, 2, 1 },
            { 0, 1, 4, 3, 2 },
            { 0, 2, 1, 4, 3 },
            { 0, 3, 2, 1, 4 }
        };
        final int initOyaSeat = gameInfo.get("initOyaSeat");
        Arrays.sort(pl, 1, 5, (p, q) -> Integer.compare(
            (q.point << 3) + SEAT_BONUS[initOyaSeat][q.seat],
            (p.point << 3) + SEAT_BONUS[initOyaSeat][p.seat])
        );
        
        pl[1].point += gameInfo.get("richi") * 1000;
        int oka = gameInfo.get("oka");
        boolean even12 = pl[1].point == pl[2].point;
        double[] score = new double[] { 0,
            pl[1].point + 20000 + (even12 ? oka / 2 : oka),
            pl[2].point + 10000 + (even12 ? oka / 2 : 0),
            pl[3].point - 10000,
            pl[4].point - 20000
        };
        for (int i = 1; i <= 4; ++i) {
            score[i] = Math.rint((score[i] - gameInfo.get("limitPoints")) / 1000);
        }
        return score;
    }
    
    protected static HashMap<String, Integer> getGameInfo(
            int totalLength, int initOyaSeat,
            int giveBackPts, int startPoints,
            int shibariBase, int shibariPlus, int cheatEnabled) {
        gameInfo.putIfAbsent("allLast", 0); // [0]Not [1]AllLast [2]SuddenDeath
        gameInfo.putIfAbsent("bafuu", 1);
        gameInfo.putIfAbsent("kyoku", 1);
        gameInfo.putIfAbsent("bonba", 0);
        gameInfo.putIfAbsent("richi", 0);
        gameInfo.putIfAbsent("totalLength", totalLength);
        gameInfo.putIfAbsent("currOyaSeat", initOyaSeat);
        gameInfo.putIfAbsent("initOyaSeat", initOyaSeat);
        gameInfo.putIfAbsent("shibariPlus", shibariPlus);
        gameInfo.putIfAbsent("shibariBase", shibariBase);
        gameInfo.putIfAbsent("limitPoints", startPoints + giveBackPts);
        gameInfo.putIfAbsent("oka", giveBackPts * 4);
        gameInfo.putIfAbsent("cheatEnabled", cheatEnabled);
        gameInfo.putIfAbsent("cheatTakeIndex", 0);
        return gameInfo;
    }
    
    protected static Game getCurrentGame() {
        return currentGameInstance;
    }
    
}
