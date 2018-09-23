import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

enum Yaku {
    /** hold只在乎有沒有、hand強調需要在手牌
        furo依照id從小到大排序、負數為順子
        (fi,fj)為和了牌
    */
    Tenhou          (1, true, true, "天和", null) {
        @Override public int check(Analyze a) {
            return (a.game.restartable && a.tsumo && a.player.jifuu == 1) ? 1 : 0;
        }
    },
    Chihou          (1, true, true, "地和", null) {
        @Override public int check(Analyze a) {
            return (a.game.restartable && a.tsumo && a.player.jifuu != 1) ? 1 : 0;
        }
    },
    Renhou          (1, true, true, "人和", null) {
        @Override public int check(Analyze a) {
            return (a.game.restartable && !a.tsumo && a.player.kawa.isEmpty()) ? 1 : 0;
        }
    },
    Kokushimusou    (1, true, false, "国士無双", null) {
        @Override public int check(Analyze a) {
            return check(a.hand) ? (a.hand[a.fi][a.fj] == 2 ? -1 : 1) : 0;
        }
        @Override public boolean check(int[][] h) {
            int m07 = h[0][1] * h[0][2] * h[0][3] * h[0][4] * h[0][5] * h[0][6] * h[0][7];
            return (m07 != 0) &&
                m07 * h[1][1] * h[1][9] * h[2][1] * h[2][9] * h[3][1] * h[3][9] == 2;
        }
        @Override public int getValue(int cnt) {
            return cnt > 0 ? value : (value + 1);
        }
        @Override public String getLabel1(int cnt) {
            return cnt > 0 ? label : ("純正" + label);
        }
    },
    Churenpouto     (1, true, false, "九蓮宝燈", null) {
        @Override public int check(Analyze a) {
            int[] h = a.hand[a.fi];
            int m19 = h[1] * h[9];
            if (m19 ==  9) {// 19為3+3張、其餘1包含一張2
                if (h[2] * h[3] * h[4] * h[5] * h[6] * h[7] * h[8] == 2) {
                    return h[a.fj] == 2 ? -1 : 1;
                }
            }
            if (m19 == 12) {// 19為3+4張、其餘都是1
                if ((h[2] & h[3] & h[4] & h[5] & h[6] & h[7] & h[8]) == 1) {
                    return h[a.fj] == 4 ? -1 : 1;
                }
            }
            return 0;
        }
        @Override public int getValue(int cnt) {
            return cnt > 0 ? value : (value + 1);
        }
        @Override public String getLabel1(int cnt) {
            return cnt > 0 ? label : ("純正" + label);
        }
    },
    Daichisei       (2, true, false, "大七星", null) {
        @Override public int check(Analyze a) {
            return (a.hand[0][11] == 14 && testDai7(a.hand[0], 1)) ? 1 : 0;
        }
        @Override public boolean check(int[][] h) {
            return h[0][11] == 14 && testDai7(h[0], 1);
        }
    },
    Daisuurin       (1, true, false, "大数隣", null) {
        @Override public int check(Analyze a) {
            return (a.hand[1][11] == 14 && testDai7(a.hand[1], 2)) ? 1 : 0;
        }
    },
    Daichikurin     (1, true, false, "大竹林", null) {
        @Override public int check(Analyze a) {
            return (a.hand[2][11] == 14 && testDai7(a.hand[2], 2)) ? 1 : 0;
        }
    },
    Daisyarin       (1, true, false, "大車輪", null) {
        @Override public int check(Analyze a) {
            return (a.hand[3][11] == 14 && testDai7(a.hand[3], 2)) ? 1 : 0;
        }
    },
    Suuanko         (1, true, false, "四暗刻", null) {
        @Override public int check(Analyze a) {
            int count = a.furo[0].type.anko +
                        a.furo[1].type.anko +
                        a.furo[2].type.anko +
                        a.furo[3].type.anko;
            return count == 4 ? (a.hand[a.fi][a.fj] == 2 ? -1 : 1) : 0;
        }
        @Override public int getValue(int cnt) {
            return cnt > 0 ? value : (value + 1);
        }
        @Override public String getLabel1(int cnt) {
            return cnt > 0 ? label : (label + "単騎");
        }
    },
    Suukantsu       (1, true, false, "四槓子", null) {
        @Override public int check(Analyze a) {
            int count = a.furo[0].type.kantsu +
                        a.furo[1].type.kantsu +
                        a.furo[2].type.kantsu +
                        a.furo[3].type.kantsu;
            return count == 4 ? 1 : 0;
        }
    },
    Tsuiso          (1, true, false, "字一色", Daichisei) {
        @Override public int check(Analyze a) {
            return (a.hold[1][11] + a.hold[2][11] + a.hold[3][11] == 0) ? 1 : 0;
        }
    },
    Daisangen       (1, true, false, "大三元", null) {
        @Override public int check(Analyze a) {
            int count = 0;
            for (Furo f: a.furo) {
                if (f.id >= 5 && f.id <= 7)
                    ++count;
            }
            return count == 3 ? 1 : 0;
        }
    },
    Daisushi        (2, true, false, "大四喜", null) {
        @Override public int check(Analyze a) {
            int count = 0;
            for (Furo f: a.furo) {
                if (f.id >= 1 && f.id <= 4)
                    ++count;
            }
            return count == 4 ? 1 : 0;
        }
    },
    Shousushi       (1, true, false, "小四喜", null) {
        @Override public int check(Analyze a) {
            if (a.pairId >= 5)
                return 0;
            int count = 0;
            for (Furo f: a.furo) {
                if (f.id >= 1 && f.id <= 4)
                    ++count;
            }
            return count == 3 ? 1 : 0;
        }
    },
    Chinroto        (1, true, false, "清老頭", null) {
        @Override public int check(Analyze a) {
            return (a.hold[0][11] == 0 &&
                   (a.hold[1][11] + a.hold[2][11] + a.hold[3][11] == a.yao9)) ? 1 : 0;
        }
    },
    Ryuiso          (1, true, false, "緑一色", null) {
        @Override public int check(Analyze a) {
            return (a.hold[0][6] == a.hold[0][11] + a.hold[1][11] + a.hold[3][11] +
                    a.hold[2][1] + a.hold[2][5] + a.hold[2][7] + a.hold[2][9]) ? 1 : 0;
        }
    },
    Issyokuyonjun   (1, true, false, "一色四順", null) {
        @Override public int check(Analyze a) {
            return (a.furo[0].id == a.furo[1].id &&
                    a.furo[0].id == a.furo[2].id &&
                    a.furo[0].id == a.furo[3].id) ? 1 : 0;
        }
    },
    Surenko         (1, true, false, "四連刻", null) {
        @Override public int check(Analyze a) {
            return (a.furo[0].id > Card.UNIT &&// 字牌不會有四連刻
                    a.furo[0].id + 1 == a.furo[1].id &&
                    a.furo[0].id + 2 == a.furo[2].id &&
                    a.furo[0].id + 3 == a.furo[3].id) ? 1 : 0;
        }
    },
    
    WRichi          (2, false, true, "両立直", null) {
        @Override public int check(Analyze a) {
            return a.player.wrichi ? 1 : 0;
        }
    },
    Richi           (1, false, true, "立直", WRichi) {
        @Override public int check(Analyze a) {
            return a.player.hasRichied() ? 1 : 0;
        }
    },
    Ippatsu         (1, false, true, "一発", null) {
        @Override public int check(Analyze a) {
            return a.player.hasIppatsu() ? 1 : 0;
        }
    },
    Rinsyankaihou   (1, false, false, "嶺上開花", null) {
        @Override public int check(Analyze a) {
            return (a.game.phase.rinshanable &&  a.tsumo) ? 1 : 0;
        }
    },
    Chankan         (1, false, true, "槍槓", null) {
        @Override public int check(Analyze a) {
            return (a.game.phase.chankanable && !a.tsumo) ? 1 : 0;
        }
    },
    Haiteiraoyue    (1, false, true, "海底撈月", null) {
        @Override public int check(Analyze a) {
            return (a.game.lastCard() &&  a.tsumo) ? 1 : 0;
        }
    },
    Houteiraoyui    (1, false, true, "河底撈魚", null) {
        @Override public int check(Analyze a) {
            return (a.game.lastCard() && !a.tsumo) ? 1 : 0;
        }
    },
    Chinitsu        (6, false, true, "清一色", null) {
        @Override public int check(Analyze a) {
            return (a.hold[0][11] == 0 && (
                    a.hold[1][11] + a.hold[2][11] == 0 ||
                    a.hold[2][11] + a.hold[3][11] == 0 ||
                    a.hold[3][11] + a.hold[1][11] == 0)) ?
                    (a.player.menchin ? 1 : -1) : 0;
        }
    },
    Honitsu         (3, false, true, "混一色", Chinitsu) {
        @Override public int check(Analyze a) {
            return (a.hold[1][11] + a.hold[2][11] == 0 ||
                    a.hold[2][11] + a.hold[3][11] == 0 ||
                    a.hold[3][11] + a.hold[1][11] == 0)  ?
                    (a.player.menchin ? 1 : -1) : 0;
        }
    },
    Tanyaochu       (1, false, true, "断么九", null) {
        @Override public int check(Analyze a) {
            return (a.yao9 == 0) ? 1 : 0;
        }
    },
    Honrouto        (2, false, true, "混老頭", Chinroto) {
        @Override public int check(Analyze a) {
            return (a.hold[1][11] + a.hold[2][11] + a.hold[3][11] == a.yao9) ? 1 : 0;
        }
    },
    Shousangen      (2, false, false, "小三元", Daisangen) {
        @Override public int check(Analyze a) {
            if (a.pairId >= 7 || a.pairId <= 5)
                return 0;
            int count = 0;
            for (Furo f: a.furo) {
                if (f.id >= 5 && f.id <= 7)
                    ++count;
            }
            return count == 2 ? 1 : 0;
        }
    },
    Jifuu           (1, false, false, "自風 ", null) {
        @Override public int check(Analyze a) {
            for (Furo f: a.furo) {
                if (f.id == a.player.jifuu)
                    return f.vj;
            }
            return 0;
        }
        @Override public int getValue(int cnt) {
            return 1;
        }
        @Override public String getLabel1(int cnt) {
            switch (cnt) {
                case  1: return label + "東";
                case  2: return label + "南";
                case  3: return label + "西";
                case  4: return label + "北";
                default: return label + "？";
            }
        }
    },
    Bafuu           (1, false, false, "場風 ", null) {
        @Override public int check(Analyze a) {
            for (Furo f: a.furo) {
                if (f.id == a.game.bafuu)
                    return f.vj;
            }
            return 0;
        }
        @Override public int getValue(int cnt) {
            return 1;
        }
        @Override public String getLabel1(int cnt) {
            switch (cnt) {
                case  1: return label + "東";
                case  2: return label + "南";
                case  3: return label + "西";
                case  4: return label + "北";
                default: return label + "？";
            }
        }
    },
    Yakuhai5        (1, false, false, "役牌 白", null) {
        @Override public int check(Analyze a) {
            for (Furo f: a.furo) {
                if (f.id == 5)
                    return 1;
            }
            return 0;
        }
    },
    Yakuhai6        (1, false, false, "役牌 發", null) {
        @Override public int check(Analyze a) {
            for (Furo f: a.furo) {
                if (f.id == 6)
                    return 1;
            }
            return 0;
        }
    },
    Yakuhai7        (1, false, false, "役牌 中", null) {
        @Override public int check(Analyze a) {
            for (Furo f: a.furo) {
                if (f.id == 7)
                    return 1;
            }
            return 0;
        }
    },
    Toitoiho        (2, false, false, "対々和", null) {
        @Override public int check(Analyze a) {
            // id最小的副露就已經是刻子
            return a.furo[0].id > 0 ? 1 : 0;
        }
    },
    Junchantaiyao   (3, false, false, "純全帯么九", null) {
        @Override public int check(Analyze a) {
            if (a.hold[0][11] > 0 || Card.isYao9(a.pairId))
                return 0;
            for (Furo f: a.furo) {
                if (!f.containsYao9()) {
                    return 0;
                }
            }
            return a.player.menchin ? 1 : -1;
        }
    },
    Honchantaiyao   (2, false, false, "混全帯么九", Junchantaiyao) {
        @Override public int check(Analyze a) {// 不能跟混老頭複合
            if (Card.isYao9(a.pairId) || a.furo[0].id > 0)
                return 0;
            for (Furo f: a.furo) {
                if (!f.containsYao9()) {
                    return 0;
                }
            }
            return a.player.menchin ? 1 : -1;
        }
    },
    Sankantsu       (2, false, false, "三槓子", Suukantsu) {
        @Override public int check(Analyze a) {
            int count = a.furo[0].type.kantsu +
                        a.furo[1].type.kantsu +
                        a.furo[2].type.kantsu +
                        a.furo[3].type.kantsu;
            return count == 3 ? 1 : 0;
        }
    },
    Sananko         (2, false, false, "三暗刻", Suuanko) {
        @Override public int check(Analyze a) {
            int count = a.furo[0].type.anko +
                        a.furo[1].type.anko +
                        a.furo[2].type.anko +
                        a.furo[3].type.anko;
            return count == 3 ? 1 : 0;
        }
    },
    Kofonsanko      (2, false, false, "客風三刻", Daisushi) {
        @Override public int check(Analyze a) {
            int flag = 0b11110 ^ (a.game.bafuu & a.player.jifuu);
            for (Furo f: a.furo) {
                if (f.id >= 1 && f.id <= 4)
                    flag ^= f.id;
            }
            return flag == 0 ? 1 : 0;
        }
    },
    Ryanpeko        (3, false, false, "二盃口", Issyokuyonjun) {
        @Override public int check(Analyze a) {
            return (a.furo[0].id == a.furo[1].id &&
                    a.furo[2].id == a.furo[3].id) ? 1 : 0;
        }
    },
    Ipeko           (1, false, false, "一盃口", Ryanpeko) {
        @Override public int check(Analyze a) {
            return (a.furo[0].id == a.furo[1].id ||
                    a.furo[1].id == a.furo[2].id ||
                    a.furo[2].id == a.furo[3].id) ? 1 : 0;
        }
    },
    Ikkitsuukan     (2, false, false, "一気通貫", null) {
        @Override public int check(Analyze a) {
            boolean d12 = a.furo[0].id + 3 == a.furo[1].id;
            boolean d13 = a.furo[0].id + 3 == a.furo[2].id;
            boolean d23 = a.furo[1].id + 3 == a.furo[2].id;
            boolean d24 = a.furo[1].id + 3 == a.furo[3].id;
            boolean d34 = a.furo[2].id + 3 == a.furo[3].id;
            if (a.furo[0].id < 0 && a.furo[0].vj == 8) {
                return ((d12 && d23) || (d12 && d24) || (d13 && d34)) ?
                    (a.player.menchin ? 1 : -1) : 0;
            }
            if (a.furo[1].id < 0 && a.furo[1].vj == 8) {
                return (d23 && d34) ?
                    (a.player.menchin ? 1 : -1) : 0;
            }
            return 0;
        }
    },
    Sanshokudoko    (2, false, false, "三色同刻", null) {
        @Override public int check(Analyze a) {
            boolean d12 = a.furo[0].id + Card.UNIT == a.furo[1].id;
            boolean d13 = a.furo[0].id + Card.UNIT == a.furo[2].id;
            boolean d23 = a.furo[1].id + Card.UNIT == a.furo[2].id;
            boolean d24 = a.furo[1].id + Card.UNIT == a.furo[3].id;
            boolean d34 = a.furo[2].id + Card.UNIT == a.furo[3].id;
            if (a.furo[0].id > Card.UNIT) {
                return ((d12 && d23) || (d12 && d24) || (d13 && d34)) ?
                    (a.player.menchin ? 1 : -1) : 0;
            }
            if (a.furo[1].id > Card.UNIT) {
                return (d23 && d34) ?
                    (a.player.menchin ? 1 : -1) : 0;
            }
            return 0;
        }
    },
    Sanshokudoujun  (2, false, false, "三色同順", null) {
        @Override public int check(Analyze a) {
            boolean d12 = a.furo[0].id + Card.UNIT == a.furo[1].id;
            boolean d13 = a.furo[0].id + Card.UNIT == a.furo[2].id;
            boolean d23 = a.furo[1].id + Card.UNIT == a.furo[2].id;
            boolean d24 = a.furo[1].id + Card.UNIT == a.furo[3].id;
            boolean d34 = a.furo[2].id + Card.UNIT == a.furo[3].id;
            if (a.furo[3].id < 0) {
                return ((d23 && d34) || (d12 && d24) || (d13 && d34)) ?
                    (a.player.menchin ? 1 : -1) : 0;
            }
            if (a.furo[2].id < 0) {
                return (d12 && d23) ?
                    (a.player.menchin ? 1 : -1) : 0;
            }
            return 0;
        }
    },
    Isshokusanjun   (2, false, false, "一色三順", Issyokuyonjun) {
        @Override public int check(Analyze a) {
            return (a.furo[1].id == a.furo[2].id &&
                   (a.furo[0].id == a.furo[1].id ||
                    a.furo[2].id == a.furo[3].id)) ?
                   (a.player.menchin ? 1 : -1) : 0;
        }
    },
    Sanreko         (2, false, false, "三連刻", Surenko) {
        @Override public int check(Analyze a) {
            return (a.furo[1].id > Card.UNIT &&
                    a.furo[1].id + 1 == a.furo[2].id &&
                   (a.furo[0].id + 1 == a.furo[1].id ||
                    a.furo[2].id + 1 == a.furo[3].id)) ? 1 : 0;
        }
    },
    Chitoitsu       (2, false, false, "七対子", Ryanpeko) {
        @Override public int check(Analyze a) {
            return check(a.hand) ? 1 : 0;
        }
        @Override public boolean check(int[][] h) {
            for (int i = 3; i >= 0; --i) {
                for (int j = 1; j <= 9; ++j) {
                    if ((h[i][j] & 5) != 0)
                        return false;
                }
            }
            return true;
        }
    },
    Menzenchintsumo (1, false, true, "門前清自摸和", null) {
        @Override public int check(Analyze a) {
            return (a.tsumo && a.player.menchin) ? 1 : 0;
        }
    },
    Pinfu           (1, false, false, "平和", null) {
        @Override public int check(Analyze a) {
            return 0;
        }
    },
    Dora            (1, false, false, "ドラ", null) {
        @Override public int check(Analyze a) {
            return 0;
        }
        @Override public int getValue(int cnt) {
            return value * cnt;
        }
    },
    AkaDora         (1, false, false, "赤ドラ", null) {
        @Override public int check(Analyze a) {
            return 0;
        }
        @Override public int getValue(int cnt) {
            return value * cnt;
        }
    },
    UraDora         (1, false, false, "裏ドラ", null) {
        @Override public int check(Analyze a) {
            return 0;
        }
        @Override public int getValue(int cnt) {
            return value * cnt;
        }
        @Override public String getLabel1(int cnt) {
            return label;// 以0出現是正常的
        }
    };
    
    public static final String FAN = "飜";
    public static final String FU  = "符";
    public static final Set<Yaku> normalSet;
    public static final Set<Yaku> stack7Set;
    public static final Set<Yaku> specialSet;
    
    static {
        EnumSet<Yaku> specialTemp = EnumSet.of(Kokushimusou, Daichisei, Chitoitsu);
        specialSet = Collections.unmodifiableSet(specialTemp);
        
        EnumSet<Yaku> normalTemp = EnumSet.copyOf(specialTemp);
        normalTemp.addAll(EnumSet.range(Pinfu, UraDora));
        normalSet = Collections.unmodifiableSet(EnumSet.complementOf(normalTemp));
        
        EnumSet<Yaku> stack7Temp = EnumSet.allOf(Yaku.class);
        stack7Temp.removeIf(y -> !y.stackable);
        stack7Set = Collections.unmodifiableSet(EnumSet.complementOf(stack7Temp));
    }
    
    public final int value;
    public final boolean yakuman;
    public final boolean stackable;
    public final String label;
    public final Yaku upper;    // 上位役
    
    private Yaku(int v, boolean y, boolean s, String l, Yaku u) {
        value = v;
        yakuman = y;
        stackable = s;
        label = l;
        upper = u;
    }
    
    /** 檢測是否有此役種
        回傳==0 無
        回傳 >0 有
        回傳 <0 非門清減飜或其他變形(可能需要覆寫getValue及getName)
        正確的名稱及飜數用getValue及getName取得
    */
    public abstract int check(Analyze a);
    
    /** 符不符合此役牌型、至少需要實作specialSet牌型 */
    public boolean check(int[][] pool) {
        return false;
    }
    
    public int getValue(int cnt) {
        return cnt > 0 ? value : (value - 1);
    }
    
    public String getLabel1(int cnt) {
        return cnt > 0 ? label : (label + "▽");
    }
    
    public String getLabel2(int cnt) {
        return String.format("%d%s", getValue(cnt), yakuman ? "ｘ" : FAN);
    }
    
    private static boolean testDai7(int[] h, int index) {
        for (int i = 0; i < 7; ++i, ++index) {
            if (h[index] != 2)
                return false;
        }
        return true;
    }
    
}
