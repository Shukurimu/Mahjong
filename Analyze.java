import java.util.*;

class Analyze {
	private ArrayList<Bunkai> bunkaiList;
	private final boolean menchin;
	private final int menfonn;
	private boolean tsumo;
	private int[][] pool;
	private int[][] hold;
	private int[][] furo;
	private int fi, fj;
	private int kans;	// number of kan
	private int jpair;	// jyantou
	private int fixed;
	private int unfixed;
	private int c0, c1, c2, c3;
	
	public Analyze(int[][] po, int[][] ho, LinkedList<Furo> ku, int wd, boolean mchn) {
		pool = po;
		hold = ho;
		furo = new int[4][4];
		kans = 0;
		fixed = 0;
		menfonn = wd;
		menchin = mchn;
		for (Furo f: ku) {
			furo[fixed][0] = f.stat;
			furo[fixed][1] = f.kind;
			furo[fixed][2] = f.numb;
			furo[fixed][3] = f.from;
			if (f.stat == 16)
				++kans;
			++fixed;
		}
		bunkaiList = new ArrayList<Bunkai>(10);
	}
	
	public Analyze() {
		bunkaiList = new ArrayList<Bunkai>(1);
		tsumo = true;
		menfonn = 0;
		menchin = true;
		bunkaiList.add(new Bunkai(Yaku.Shisanbutou));
	}
	
	public static Analyze dealWith13(Analyze origin) {
		if (origin == null)
			return (new Analyze());
		origin.bunkaiList.add(new Bunkai(Yaku.Shisanbutou));
		return origin;
	}
	
	public void addChankan() {
		bunkaiList.get(0).addList(Yaku.Chankan);
		return;
	}
	
	public boolean isCommon() {
		return (bunkaiList.get(0).fansuu < 512);
	}
	
	// return true only if > 0 fan
	public boolean calculate(Card focus, boolean rt) {
		bunkaiList = new ArrayList<Bunkai>(8);
		fi = focus.vi;
		fj = focus.vj;
		unfixed = 0;
		jpair = 0;
		if (!(tsumo = rt)) {
			++pool[fi][fj];
			++hold[fi][fj];
		}
		c0 = sum(hold[0]);
		c1 = sum(hold[1]);
		c2 = sum(hold[2]);
		c3 = sum(hold[3]);
		// Chitoitsu or Tenhou
		if (menchin) {
			Bunkai result = new Bunkai(25);
			if (Game.restartable) {
				if (tsumo)	result.addList((menfonn == 1) ? Yaku.Tenhou : Yaku.Chihou);
				else if (Game.calcLocal)	result.addList(Yaku.Renhou);
			}
			yakuman1(result);
			yakuStackable(result);
			// Yakuman: Kokushimusou
			if (pool[0][0] == 99)
				result.addList((pool[fi][fj] == 2) ? Yaku.KokushimusouR : Yaku.Kokushimusou);
			
			bunkaiList.add(result);
		}
		
		// as Jyantou
		if (pool[fi][fj] >= 2) {
			pool[fi][fj] -= 2;
			int moreFu = 2;
			if (fi > 0) {
				jpair = (fi << 4) + fj;
			} else {
				jpair = fj;
				if ((fj > 4) || (fj == Game.currentBakaze))
					moreFu += 2;
				if (fj == menfonn)
					moreFu += 2;
			}
			decomposeJihai(moreFu, 1);
			pool[fi][fj] += 2;
		}
		
		// as Kou
		if (pool[fi][fj] >= 3) {
			pool[fi][fj] -= 3;
			int moreFu = tsumo ? 4 : 2;
			furo[fixed][0] = moreFu;
			furo[fixed][1] = fi;
			furo[fixed][2] = fj;
			++unfixed;
			decomposeJyantou(((fi > 0) && (fj > 1) && (fj < 9)) ? moreFu : (moreFu << 1));
			--unfixed;
			pool[fi][fj] += 3;
		}
		
		// as Jun
		if ((fi > 0) && (fixed < 4)) {
			furo[fixed][0] = 0;
			furo[fixed][1] = fi;
			++unfixed;
			// [x-2][x-1][x]
			if ((fj >= 3) && (pool[fi][fj - 2] > 0) && (pool[fi][fj - 1] > 0) && (pool[fi][fj] > 0)) {
				--pool[fi][fj - 2];
				--pool[fi][fj - 1];
				--pool[fi][fj];
				furo[fixed][2] = fj - 1;
				decomposeJyantou((fj > 3) ? 0 : 2);
				++pool[fi][fj];
				++pool[fi][fj - 1];
				++pool[fi][fj - 2];
			}
			// [x-1][x][x+1]
			if ((fj >= 2) && (fj <= 8) && (pool[fi][fj - 1] > 0) && (pool[fi][fj] > 0) && (pool[fi][fj + 1] > 0)) {
				--pool[fi][fj - 1];
				--pool[fi][fj];
				--pool[fi][fj + 1];
				furo[fixed][2] = fj;
				decomposeJyantou(2);
				++pool[fi][fj + 1];
				++pool[fi][fj];
				++pool[fi][fj - 1];
			}
			// [x][x+1][x+2]
			if ((fj <= 7) && (pool[fi][fj] > 0) && (pool[fi][fj + 1] > 0) && (pool[fi][fj + 2] > 0)) {
				--pool[fi][fj];
				--pool[fi][fj + 1];
				--pool[fi][fj + 2];
				furo[fixed][2] = fj + 1;
				decomposeJyantou((fj < 7) ? 0 : 2);
				++pool[fi][fj + 2];
				++pool[fi][fj + 1];
				++pool[fi][fj];
			}
			--unfixed;
		}
		
		if (!tsumo) {
			--pool[fi][fj];
			--hold[fi][fj];
		}
		Collections.sort(bunkaiList);
		for (Bunkai i: bunkaiList) {
			System.out.printf("%3d-%3d\n", i.fansuu, i.fusuu);
		}
		return (bunkaiList.get(0).fansuu > 0);
	}
	
	// Yakuman: Churenpouto & Daichisei
	// Commons: Chitoitsu + Menzenchintsumo
	private void yakuman1(Bunkai result) {
		if (sum(pool[fi]) == 14) {
			int k = pool[fi][2] * pool[fi][3] * pool[fi][4] * pool[fi][5] * pool[fi][6] * pool[fi][7] * pool[fi][8];
			if (k == 1) {
				result.addList((pool[fi][fj] == 4) ? Yaku.ChurenpoutoR : Yaku.Churenpouto);
			} else if ((k == 2) && (pool[fi][1] == pool[fi][9])) {
				result.addList((pool[fi][fj] == 2) ? Yaku.ChurenpoutoR : Yaku.Churenpouto);
			} else if (Game.calcLocal && (k == 128)) {
				result.addList((fi == 1) ? Yaku.Daisuurin : ((fi == 2) ? Yaku.Daichikurin : Yaku.Daisyarin));
			} else if (pool[0][1] * pool[0][2] * pool[0][3] * pool[0][4] * pool[0][5] * pool[0][6] * pool[0][7] == 128) {
				result.addList(Yaku.Daichisei);
			}
		}
		for (int i = 0; i < 4; ++i)
			for (int j = 1; j < 10; ++j)
				if ((pool[i][j] & 5) != 0)
					return;
		if (!Game.calcLocal) {
			result.addList(Yaku.Chitoitsu);
		} else if (pool[0][5] + pool[0][6] + pool[0][7] == 6) {
			result.addList(Yaku.Sangenchitoi);
		} else if (pool[0][1] + pool[0][2] + pool[0][3] + pool[0][4] == 8) {
			result.addList(Yaku.Sushichitoi);
		} else {
			result.addList(Yaku.Chitoitsu);
		}
		if (tsumo)	result.addList(Yaku.Menzenchintsumo);
		return;
	}
	
	// Yakuman: Suuanko & Suukantsu & Daisangen & Daisushi & Shousushi
	private void yakuman2(Bunkai result) {
		int k, i;
		for (k = i = 0; i < 4; ++i) {
			k += ((furo[i][1] == 0) && (furo[i][2] > 4)) ? 1 : 0;
			if (k == 3) {
				result.addList(Yaku.Daisangen);
				// if ((fixed == 4 && ((furo[3][1] == 0) && (furo[3][2] > 4))) ||
					// (fixed == 3 && ((furo[3][1] != 0) || (furo[3][2] < 5))))
					// result.sekinin = Yaku.Daisangen.getValue() + furo[i][3];
				break;
			}
		}
		for (k = i = 0; i < 4; ++i)
			k += ((furo[i][1] == 0) && (furo[i][2] < 5)) ? 1 : 0;
		if ((k == 3) && (jpair < 5)) {
			result.addList(Yaku.Shousushi);
		} else if (k == 4) {
			result.addList(Yaku.Daisushi);
			// if (fixed == 4)	result.sekinin = Yaku.Daisushi.getValue() + furo[3][3];
		}
		for (k = i = 0; i < 4; ++i)
			k += ((furo[i][0] == 4) || (furo[i][0] == 16)) ? 1 : 0;
		if (k == 4)
			result.addList((jpair == (fi << 4) + fj) ? Yaku.SuuankoR : Yaku.Suuanko);
		if (kans == 4) {
			result.addList(Yaku.Suukantsu);
			// if (fixed == 4)	result.sekinin = Yaku.Suukantsu.getValue() + furo[3][3];
		}
		return;
	}
	
	// Yakuman: Tsuiso & Ryuiso & Chinroto
	private void yakuman3(Bunkai result, int[] v) {
		if (c0 + c1 + c2 + c3 == hold[1][1] + hold[1][9] + hold[2][1] + hold[2][9] + hold[3][1] + hold[3][9]) {
			result.addList(Yaku.Chinroto);
		} else if (c1 + c2 + c3 == 0) {
			result.addList(Yaku.Tsuiso);
		} else if (c0 + c1 + c3 + hold[2][1] + hold[2][5] + hold[2][7] + hold[2][9] == hold[0][6]) {
			result.addList(Yaku.Ryuiso);
		} else if (Game.calcLocal) {
			if ((v[0] < -9) && (v[0] + 1 == v[1]) && (v[0] + 2 == v[2]) && (v[0] + 3 == v[3])) {
				result.addList(Yaku.Surenko);
				// if (fixed == 4)	result.sekinin = Yaku.Surenko.getValue() + furo[3][3];
			} else if ((v[0] > 9) && (v[0] == v[1]) && (v[0] == v[2]) && (v[0] == v[3])) {
				result.addList(Yaku.Issyokuyonjun);
				// if (fixed == 4)	result.sekinin = Yaku.Issyokuyonjun.getValue() + furo[3][3];
			}
		}
		return;
	}
	
	// Step1
	private void decomposeJyantou(final int currentFu) {
		for (int i = 1; i < 8; ++i) {
			if (pool[0][i] == 2) {
				int moreFu = 0;
				pool[0][i] = 0;
				jpair = i;
				if ((i > 4) || (i == Game.currentBakaze))
					moreFu += 2;
				if (i == menfonn)
					moreFu += 2;
				decomposeJihai(currentFu + moreFu, 1);
				pool[0][i] = 2;
			}
		}
		for (int i = 1; i < 4; ++i) {
			for (int j = 1; j < 10; ++j) {
				if (pool[i][j] >= 2) {
					pool[i][j] -= 2;
					jpair = (i << 4) + j;
					decomposeJihai(currentFu, 1);
					pool[i][j] += 2;
				}
			}
		}
		return;
	}
	
	// Step2: Tsupai - koutsu only
	private void decomposeJihai(final int currentFu, final int start) {
		if (sum(pool[0]) == 0)	decomposeSuupai1(currentFu, 1, 1);
		for (int i = start; i < 8; ++i) {
			if (pool[0][i] == 3) {
				pool[0][i] = 0;
				furo[fixed + unfixed][0] = 4;
				furo[fixed + unfixed][1] = 0;
				furo[fixed + unfixed][2] = i;
				++unfixed;
				decomposeJihai(currentFu + 8, i + 1);
				--unfixed;
				pool[0][i] = 3;
			}
		}
		return;
	}
	
	// Step357: Suupai - koutsu first
	private void decomposeSuupai1(final int currentFu, final int start, final int kind) {
		for (int i = start; i < 10; ++i) {
			if (pool[kind][i] >= 3) {
				pool[kind][i] -= 3;
				furo[fixed + unfixed][0] = 4;
				furo[fixed + unfixed][1] = kind;
				furo[fixed + unfixed][2] = i;
				++unfixed;
				decomposeSuupai1(currentFu + (((i > 1) && (i < 9)) ? 4 : 8), i + 1, kind);
				--unfixed;
				pool[kind][i] += 3;
			}
		}
		decomposeSuupai2(currentFu, 2, kind);
		return;
	}
	
	// Step468: Suupai - mentsu second
	private void decomposeSuupai2(final int currentFu, final int start, final int kind) {
		for (int i = start; i < 9; ++i) {
			if ((pool[kind][i - 1] > 0) && (pool[kind][i] > 0) && (pool[kind][i + 1] > 0)) {
				--pool[kind][i - 1];
				--pool[kind][i];
				--pool[kind][i + 1];
				furo[fixed + unfixed][0] = 0;
				furo[fixed + unfixed][1] = kind;
				furo[fixed + unfixed][2] = i;
				++unfixed;
				decomposeSuupai2(currentFu, i, kind);
				--unfixed;
				++pool[kind][i + 1];
				++pool[kind][i];
				++pool[kind][i - 1];
			}
		}
		if (sum(pool[kind]) == 0) {
			if (kind < 3)	decomposeSuupai1(currentFu, 1, kind + 1);
			else			decomposeAnalyze(currentFu);
		}
		return;
	}
	
	// Step9: finished
	private void decomposeAnalyze(int currentFu) {
		// base & furo
		currentFu += 20;
		for (int i = 0; i < fixed; ++i) {
			currentFu += furo[i][0] * (((furo[i][1] > 0) && (furo[i][2] > 1) && (furo[i][2] < 9)) ? 1 : 2);
		}
		Bunkai result = new Bunkai(currentFu);
		// Menzenchintsumo & Pinfu & fusuuAdjust
		if (menchin) {
			if (currentFu == 20)	result.addList(Yaku.Pinfu);
			else					result.fusuu += 2;
			if (tsumo)
				result.addList(Yaku.Menzenchintsumo);
			else
				result.fusuu += 10;
		} else if ((currentFu == 20) && !tsumo) {
			result.fusuu = 30;
		} else	;
		if (result.fusuu % 10 != 0)
			result.fusuu = (result.fusuu / 10 + 1) * 10;
		
		int[] v = new int[4];
		v[0] = ((furo[0][0] == 0) ? 1 : (-1)) * (furo[0][1] * 10 + furo[0][2]);
		v[1] = ((furo[1][0] == 0) ? 1 : (-1)) * (furo[1][1] * 10 + furo[1][2]);
		v[2] = ((furo[2][0] == 0) ? 1 : (-1)) * (furo[2][1] * 10 + furo[2][2]);
		v[3] = ((furo[3][0] == 0) ? 1 : (-1)) * (furo[3][1] * 10 + furo[3][2]);
		Arrays.sort(v);
		
		if (Game.restartable) {
			if (tsumo)	result.addList((menfonn == 1) ? Yaku.Tenhou : Yaku.Chihou);
			else if (Game.calcLocal)	result.addList(Yaku.Renhou);
		}
		
		yakuman2(result);
		yakuman3(result, v);
		yakuStackable(result);
		yakuCommon1(result);
		yakuCommon2(result, v);
		bunkaiList.add(result);
		return;
	}
	
	// Chinitsu & Honitsu & Tanyaochu & Honrouto (all of these are compatible with Chitoitsu)
	private void yakuStackable(Bunkai result) {
		if ((c1 + c2 == 0) || (c2 + c3 == 0) || (c3 + c1 == 0))
			result.addList((c0 == 0) ? (menchin ? Yaku.Chinitsu : Yaku.ChinitsuX) : (menchin ? Yaku.Honitsu : Yaku.HonitsuX));
		if (c0 + hold[1][1] + hold[1][9] + hold[2][1] + hold[2][9] + hold[3][1] + hold[3][9] == 0) {
			result.addList(Yaku.Tanyaochu);
		} else if (c1 + c2 + c3 == hold[1][1] + hold[1][9] + hold[2][1] + hold[2][9] + hold[3][1] + hold[3][9]) {
			result.addList(Yaku.Honrouto);
		}
		return;
	}
	
	// Shousangen & Junchantaiyao & Honchantaiyao & Toitoiho & Sananko & Sankantsu & Yakuhai
	private void yakuCommon1(Bunkai result) {
		int adj, kou;
		for (int i = adj = kou = 0; i < 4; ++i) {
			if (furo[i][0] > 0) {
				if (furo[i][1] == 0) {
					switch (furo[i][2]) {
						case 1:
							if (menfonn == 1)			result.addList(Yaku.Jikaze1);
							if (Game.currentBakaze == 1)	result.addList(Yaku.Bakaze1);
							break;
						case 2:
							if (menfonn == 2)			result.addList(Yaku.Jikaze2);
							if (Game.currentBakaze == 2)	result.addList(Yaku.Bakaze2);
							break;
						case 3:
							if (menfonn == 3)			result.addList(Yaku.Jikaze3);
							if (Game.currentBakaze == 3)	result.addList(Yaku.Bakaze3);
							break;
						case 4:
							if (menfonn == 4)			result.addList(Yaku.Jikaze4);
							if (Game.currentBakaze == 4)	result.addList(Yaku.Bakaze4);
							break;
						case 5:
							result.addList(Yaku.Yakuhai5);
							adj -= 4;	break;
						case 6:
							result.addList(Yaku.Yakuhai6);
							adj -= 3;	break;
						case 7:
							result.addList(Yaku.Yakuhai7);
							adj -= 2;	break;
					}
				}
				kou += (furo[i][1] > 0) ? (((furo[i][2] > 1) && (furo[i][2] < 9)) ? 1 : (64 + 8 + 1)) : (8 + 1);
			} else {	// 1-koutsu 8-yaochu 64-routo
				kou += ((furo[i][2] > 2) && (furo[i][2] < 8)) ? 0 : (64 + 8 + 0);
			}
		}
		
		if (jpair == -adj)	result.addList(Yaku.Shousangen);
		if (kans == 3)	result.addList(Yaku.Sankantsu);
		if ((kou & 7) == 4)	result.addList(Yaku.Toitoiho);
		if ((((jpair >> 4) == 0) || ((jpair & 15) == 1) || ((jpair & 15) == 9)) && (((kou >> 3) & 7) == 4)) {
			if (((jpair >> 4) > 0) && (((kou >> 6) & 7) == 4)) {
				result.addList(menchin ? Yaku.Junchantaiyao : Yaku.JunchantaiyaoX);
			} else {
				result.addList(menchin ? Yaku.Honchantaiyao : Yaku.HonchantaiyaoX);
			}
		}
		
		for (int i = adj = kou = 0; i < 4; ++i) {
			adj += ((furo[i][0] & 20) != 0) ? 1 : 0;
			kou += ((furo[i][1] == 0) && (furo[i][2] < 5) && (furo[i][2] != menfonn)) ? 1 : 0;
		}
		if (adj == 3)	result.addList(Yaku.Sananko);
		if (Game.calcLocal && (kou == 3) && (Game.currentBakaze == menfonn))	result.addList(Yaku.Kofonsanko);
		return;
	}
	
	// Ipeko & Ryanpeko & Sanshokudoujun & Sanshokudoko & Ikkitsuukan
	private void yakuCommon2(Bunkai result, int[] v) {
		if (menchin) {
			if ((v[0] == v[1]) && (v[1] > 0)) {
				result.addList(((v[2] == v[3]) && (v[3] > 0)) ? Yaku.Ryanpeko : Yaku.Ipeko);
			} else if ((v[1] == v[2]) && (v[2] > 0)) {
				result.addList(Yaku.Ipeko);
			} else if ((v[2] == v[3]) && (v[3] > 0)) {
				result.addList(Yaku.Ipeko);
			} else	;
		}
		
		if ((v[0] > 0) && ((v[0] + 3 == v[1]) && (v[1] + 3 == v[2]))) {
			result.addList(menchin ? Yaku.Ikkitsuukan : Yaku.IkkitsuukanX);	return;
		} else if ((v[1] > 0) && ((v[0] + 3 == v[1]) && (v[1] + 3 == v[3]))) {
			result.addList(menchin ? Yaku.Ikkitsuukan : Yaku.IkkitsuukanX);	return;
		} else if ((v[2] > 0) && ((v[0] + 3 == v[2]) && (v[2] + 3 == v[3]))) {
			result.addList(menchin ? Yaku.Ikkitsuukan : Yaku.IkkitsuukanX);	return;
		} else if ((v[3] > 0) && ((v[1] + 3 == v[2]) && (v[2] + 3 == v[3]))) {
			result.addList(menchin ? Yaku.Ikkitsuukan : Yaku.IkkitsuukanX);	return;
		} else	;
		
		if ((v[0] != v[1]) && (v[1] != v[2]) && (v[0] % 10 == v[1] % 10) && (v[1] % 10 == v[2] % 10)) {
			if (v[0] > 0) {
				result.addList(menchin ? Yaku.Sanshokudoujun : Yaku.SanshokudoujunX);
			} else if (v[2] < -9) {
				result.addList(Yaku.Sanshokudoko);
			} else	;
		} else if ((v[0] != v[1]) && (v[1] != v[3]) && (v[0] % 10 == v[1] % 10) && (v[1] % 10 == v[3] % 10)) {
			if (v[0] > 0) {
				result.addList(menchin ? Yaku.Sanshokudoujun : Yaku.SanshokudoujunX);
			} else if (v[3] < -9) {
				result.addList(Yaku.Sanshokudoko);
			} else	;
		} else if ((v[0] != v[2]) && (v[2] != v[3]) && (v[0] % 10 == v[2] % 10) && (v[2] % 10 == v[3] % 10)) {
			if (v[0] > 0) {
				result.addList(menchin ? Yaku.Sanshokudoujun : Yaku.SanshokudoujunX);
			} else if (v[3] < -9) {
				result.addList(Yaku.Sanshokudoko);
			} else	;
		} else if ((v[1] != v[2]) && (v[2] != v[3]) && (v[1] % 10 == v[2] % 10) && (v[2] % 10 == v[3] % 10)) {
			if (v[1] > 0) {
				result.addList(menchin ? Yaku.Sanshokudoujun : Yaku.SanshokudoujunX);
			} else if (v[3] < -9) {
				result.addList(Yaku.Sanshokudoko);
			} else	;
		} else	;
		
		if (!Game.calcLocal)	return;
		if (((v[0] == v[1]) && (v[1] == v[2]) && (v[1] > 0)) || ((v[1] == v[2]) && (v[2] == v[3]) && (v[2] > 0))) {
			result.addList(Yaku.Isshokusanjun);
		} else if ((v[2] < -9) && (v[0] + 1 == v[1]) && (v[1] + 1 == v[2])) {
			result.addList(Yaku.Sanreko);
		} else if ((v[3] < -9) && (v[0] + 1 == v[1]) && (v[1] + 1 == v[3])) {
			result.addList(Yaku.Sanreko);
		} else if ((v[3] < -9) && (v[0] + 1 == v[2]) && (v[2] + 1 == v[3])) {
			result.addList(Yaku.Sanreko);
		} else if ((v[3] < -9) && (v[1] + 1 == v[2]) && (v[2] + 1 == v[3])) {
			result.addList(Yaku.Sanreko);
		} else	;
		
		return;
	}
	
	// add some occasional case
	public Bunkai agari(boolean rch, boolean wrch, boolean ipp, boolean rsh, int[] doras) {
		Bunkai result = bunkaiList.get(0);
		// yakuman need not to show common and the following cases
		if (result.fansuu >= 512) {
			for (Iterator<Yaku> it = result.list.iterator(); it.hasNext(); )
				if (it.next().getValue() < 512)
					it.remove();
			return result;
		}
		
		if (rsh) {
			result.addList(Yaku.Rinsyankaihou);
		} else if (Game.remainCard == 0) {
			result.addList(tsumo ? Yaku.Haiteiraoyue : Yaku.Houteiraoyui);
		} else	;
		
		if (wrch) {
			result.addList(Yaku.RichiW);
			if (ipp)	result.addList(Yaku.Ippatsu);
		} else if (rch) {
			result.addList(Yaku.Richi);
			if (ipp)	result.addList(Yaku.Ippatsu);
		} else	;
		
		if (doras[0] > 0)	result.addDora(Yaku.Dora,    doras[0]);
		if (doras[2] > 0)	result.addDora(Yaku.AkaDora, doras[2]);
		if (rch)			result.addDora(Yaku.UraDora, doras[1]);
		
		return result;
	}
	
	private int sum(int[] block) {
		return (block[1] + block[2] + block[3] + block[4] + block[5] + block[6] + block[7] + block[8] + block[9]);
	}
	
}
