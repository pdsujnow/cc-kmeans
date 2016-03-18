package pdsujnow.absa;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pdsu.xsf.algorithm.DBSCAN;
import pdsu.xsf.algorithm.Word2VEC;
import pdsu.xsf.text.*;

import pdsu.xsf.utils.FileVsObject;
import pdsu.xsf.utils.ReadFileUtil;

public class PUExtracting {

	// private double ffth = 0.2;
	// private double foth = 0.2;
	// private double ooth = 0.2;
	private String dataset;// = "semeval/15";
	private String dburl;
	private String gold_sql;
	private String candidate_sql;
	private String Corpus_sql;
	private int total = 213;// ���Ͼ�������
	// ���ϲ����������а��ж�ȡ
	private double[][] dist;
	private static ArrayList<String> FeatureList;
	private HashMap<String, String> FeatureMap = new HashMap<String, String>();// ��ѡFeature
	private HashMap<String, String> OpinionMap = new HashMap<String, String>();// ��ѡOp
	private HashMap<String, ArrayList<String>> FeatureCount = new HashMap<String, ArrayList<String>>();//
	private HashMap<String, ArrayList<String>> OpinionCount = new HashMap<String, ArrayList<String>>();//
	private HashMap<String, ArrayList<String>> gold = new HashMap<String, ArrayList<String>>();
	private HashMap<String, String> FMap;
	private ArrayList<String> Feature = new ArrayList<String>();
	private ArrayList<String> Opinion = new ArrayList<String>();
	private double lambda = 0.95;// 0.95, .96 .97 .98 ������

	/***
	 * 
	 * @param init
	 *            �Ƿ����³�ʼ����Աֵ
	 */
	@SuppressWarnings("unchecked")
	public PUExtracting(String inifile, boolean init) {
		// read config file
		ArrayList<String> ini = ReadFileUtil.readFileByLines(inifile, 2);
		dataset = (String) ini.get(0);
		dburl = (String) ini.get(1);
		gold_sql = (String) ini.get(2);
		candidate_sql = (String) ini.get(3);
		Corpus_sql = (String) ini.get(4);
		total = Integer.parseInt(ini.get(5));

		dist = (double[][]) FileVsObject.readObject("data/" + dataset
				+ "/dist");
		gold = (HashMap<String, ArrayList<String>>) FileVsObject
				.readObject("data/" + dataset + "/gold");
		FeatureMap = (HashMap<String, String>) FileVsObject
				.readObject("data/" + dataset + "/FeatureMap");
		OpinionMap = (HashMap<String, String>) FileVsObject
				.readObject("data/" + dataset + "/OpinionMap");
		FMap = (HashMap<String, String>) FileVsObject.readObject("data/"
				+ dataset + "/FMap");
		FeatureCount = (HashMap<String, ArrayList<String>>) FileVsObject
				.readObject("data/" + dataset + "/FeatureCount");
		OpinionCount = (HashMap<String, ArrayList<String>>) FileVsObject
				.readObject("data/" + dataset + "/OpinionCount");
		if (!init) {
			if (gold == null)// �Ƿ��ȡ�ɹ�
				this.createGold();
			if (FeatureMap == null || OpinionMap == null)// �Ƿ��ȡ�ɹ�
				this.createCandidate();
			if (FMap == null)// �Ƿ��ȡ�ɹ�
				this.createFeatureVector();
			if (FeatureCount == null || OpinionCount == null)// �Ƿ��ȡ�ɹ�
				this.CountCorpus();
			if (dist == null)// �Ƿ��ȡ�ɹ�
				this.createFeatureVector();
		} else {// ����ǳ�ʼ������Ҫ��������м������Ա
			gold.clear();
			FeatureMap.clear();
			OpinionMap.clear();
			FMap.clear();
			FeatureCount.clear();
			OpinionCount.clear();
			this.createGold();
			this.createCandidate();
			this.createFeatureVector();
			this.CountCorpus();
		}
	}

	/**
	 * 
	 * @param CF
	 *            ��ѡ�����ʼ���
	 * @param CO
	 *            ��ѡ�۵�ʼ���
	 * @param S
	 *            ��֪�������Ӽ���
	 */
	@SuppressWarnings("unchecked")
	public void run(ArrayList<String> CF, ArrayList<String> CO,
			ArrayList<String> s, double th) {
		double ffth, foth, ooth;
		ffth = foth = ooth = th;
		ArrayList<String> F = new ArrayList<String>();// �����ʼ���
		ArrayList<String> O = new ArrayList<String>();// �۵�ʼ���
		ArrayList<String> CF_tmp = (ArrayList<String>) CF.clone();// ������ʱ�����ѡ�б��������ʱֱ��ɾ��
		ArrayList<String> CO_tmp = (ArrayList<String>) CO.clone();
		ArrayList<String> F_tmp = s;
		ArrayList<String> O_tmp = new ArrayList<String>();
		boolean flag = false;
		int i = 1;
		do {
			flag = false;
			// System.out.println("Iter:" + i++);
			CF = (ArrayList<String>) CF_tmp.clone();
			CO = (ArrayList<String>) CO_tmp.clone();
			F = (ArrayList<String>) F_tmp.clone();

			// �ȴ���������
			for (String f : F) {
				// ����������ѡ
				for (String cf : CF) {
					if (association(f, cf, "ff") > ffth && !F_tmp.contains(cf)) {
						F_tmp.add(cf);// ���ܶ�ԭʼ��F����
						CF_tmp.remove(cf);// ���ܶ�ԭʼ��CF����
						flag = true;
					}
				}
				// ����۵��ѡ
				for (String co : CO) {
					if (association(f, co, "fo") > foth && !O_tmp.contains(co)) {
						O_tmp.add(co);
						CO_tmp.remove(co);
						flag = true;
					}
				}
			}
			// �ٴ���۵��
			O = (ArrayList<String>) O_tmp.clone();
			for (String o : O) {
				// ����۵��ѡ
				for (String co : CO) {
					if (association(o, co, "oo") > ooth && !O_tmp.contains(co)) {
						O_tmp.add(co);
						CO_tmp.remove(co);
						flag = true;
					}
				}
				// ����������ѡ
				for (String cf : CF) {
					if (association(o, cf, "of") > foth && !F_tmp.contains(cf)) {
						F_tmp.add(cf);
						CF_tmp.remove(cf);
						flag = true;
					}
				}
			}

			// // ��ص���ʱ���ϵı仯���***************************
			// System.out.println("F size:" + F_tmp.size());
			// System.out.print("Feature:{");
			// for (String f : F_tmp) {
			// System.out.print(f + ",");
			// }
			// System.out.println("}");
			// System.out.print("OPinioin:{");
			// for (String o : O_tmp)
			// System.out.print(o + ",");
			// System.out.println("}");
			// //��ؽ���******************************************
		} while (flag);

		// ������ʽ�滻����key��Ӧ��value
		F_tmp.clear();
		for (String f : F) {
			// System.out.println(f);
			String fmap = FMap.get(f);
			F_tmp.add(fmap);
		}
		O_tmp.clear();
		for (String o : O) {
			// System.out.println(o);
			String omap = OpinionMap.get(o);
			O_tmp.add(omap);
		}
		try {
			Feature = F_tmp;
			Opinion = O_tmp;
			FileVsObject.writeText("data/" + dataset + "/Feature.txt",
					Feature);
			FileVsObject.writeText("data/" + dataset + "/Opinion.txt",
					Opinion);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private double association(String s1, String s2, String type) {
		// TODO Auto-generated method stub
		double coo = 0;// ���ֹ�ϵǿ��
		double sem = 0;// �������ǿ��
		int d1, d2;// ��s1s2���������ƾ����е�λ��
//		 System.out.println(s1+":"+s2);
		ArrayList<String> a1 = new ArrayList<String>();
		ArrayList<String> a2 = new ArrayList<String>();
		ArrayList<String> a3 = new ArrayList<String>();
		switch (type) {
		case "ff":
			a1 = FeatureCount.get(s1);
			a2 = FeatureCount.get(s2);
			break;
		case "fo":
			a1 = FeatureCount.get(s1);
			a2 = OpinionCount.get(s2);
			break;
		case "oo":
			a1 = OpinionCount.get(s1);
			a2 = OpinionCount.get(s2);
			break;
		case "of":
			a1 = OpinionCount.get(s1);
			a2 = FeatureCount.get(s2);
			break;
		}
		// ��ȡ�������ֵ
		if (type == "ff") {
			d1 = FeatureList.indexOf(s1);
			d2 = FeatureList.indexOf(s2);
			// System.out.println(d1+":"+d2+"->"+s1+":"+s2);
			if (d1 < d2)
				sem = dist[d1][d2];
			else
				sem = dist[d2][d1];
		}
		if (a1 == null || a2 == null) {
//			 if (a1 == null)
//			 System.out.println("there is unmatched phrase  -> " + s1);
//			 else
//			 System.out.println("there is unmatched phrase  -> " + s2);
			return coo * lambda + (1 - lambda) * sem;
		}
		// ��a1��Ԫ��ȫ�ӵ�a3��
		a3.addAll(a1);
		// ��a2,a3�Ĺ�ͬԪ�ر��浽a3
		a3.retainAll(a2);
		int k1 = a3.size();
		int k2 = a1.size() - a3.size();
		int k3 = a2.size() - a3.size();
		int k4 = total - a1.size() - a2.size() + a3.size();
//		 int k1 = 3;int k2=1;int k3=1;int k4 =4;
		int n1 = k1 + k3;
		int n2 = k2 + k4;
		double p1 = (double) k1 / n1;
		double p2 = (double) k2 / n2;
		double p = (double) (k1 + k2) / (n1 + n2);
		coo = 2 * (Math.log(computeL(p1, k1, n1))
				+ Math.log(computeL(p2, k2, n2))
				- Math.log(computeL(p, k1, n1)) - Math.log(computeL(p, k2, n2)));
//		 System.out.println(coo);

		return coo * lambda + (1 - lambda) * sem;
	}

	private double computeL(double p, int k, int n) {
		double t1 = Math.pow(p, k);
		double t2 = Math.pow(1 - p, n - k);
		return t1 * t2;
	}

	/**
	 * ����һ���ɴ���������ھ��ӵ�map,�� ("CD four",{1004293:1,xx,...}) ������ͳ�ƴʹ���ʱ����ֱ�Ӳ�ѯ�����ھ����б�
	 * 
	 * @param sid
	 *            ���Ӻ�
	 * @param penn
	 *            �䷨����
	 * @param dependency
	 *            �����ϵ�б������ŷָ�
	 */
	public void createMap(String sid, String penn, String dependency) {
		// ���ʶ��������
		// Pattern NPPattern = Pattern.compile("\\(NP\\s(?:\\(.+?\\))*\\)");
		// �������ϵ��ֱ��������
		 Pattern amod = Pattern
		 .compile("(?:amod|dobj|nsubj[a-z]*|pobj)\\(([^-()]*)-[0-9]*,([^-()]*)-[0-9]*\\)");
//		Pattern amod = Pattern
//				.compile("(?:[a-z]*)\\(([^-()]*)-[0-9]*,([^-()]*)-[0-9]*\\)");
		// Pattern dobj = Pattern.compile("dobj\\([^()]*,([^-]*)-[0-9]*\\)");
		// Pattern nsubj =
		// Pattern.compile("nsubj[a-z]*\\(([^-()]*)-[0-9]*,([^-]*)-[0-9]*\\)");
		HashSet<String> hm = new HashSet<String>();
		Matcher amod_matcher = amod.matcher(dependency);
		// Matcher dobj_matcher = dobj.matcher(dependency);
		// Matcher nsubj_matcher = nsubj.matcher(dependency);
		while (amod_matcher.find()) {
			hm.add(amod_matcher.group(1).trim());
			hm.add(amod_matcher.group(2).trim());
		}
		// while (dobj_matcher.find()) {
		// hm.add(dobj_matcher.group(1).trim());
		// }
		// while (nsubj_matcher.find()) {
		// hm.add(nsubj_matcher.group(1).trim());
		// hm.add(nsubj_matcher.group(2).trim());
		// }
		// �Ӿ䷨��������
		// Pattern NP = Pattern
		// .compile("\\(NP\\s\\(NP\\s\\([A-Z]*\\s[^()]*\\)\\s\\(NN[A-Z]*\\s[^()]*\\)\\)\\s\\(PP\\s\\(IN\\s[^()]*\\)\\s\\(NP\\s\\(NNP\\s[^()]*|(?:\\(NN[A-Z]*\\s[^()]*\\)\\s?|\\(CC\\s[^()]*\\)?)+");//
		// ���NN����������ɵ�NP(ֻȡ����NN����)����������������NN
		// Pattern NP = Pattern
		// .compile("\\(NN[A-Z]*\\s[^()]*\\)\\s?");//
		Pattern NP = Pattern
				.compile("((\\(NN[A-Z]*\\s[^()]*\\)\\s?|\\((JJ[A-Z]*|VBG)\\s[^()]*\\)\\s?)+((\\(VP\\s)|\\(NP\\s|\\)|\\s)*)*\\(NN[A-Z]*\\s[^()]*\\)\\s?");//
		// ���NN����������ɵ�NP(ֻȡ����NN����)����������������NN
		Matcher np_matcher = NP.matcher(penn);
		while (np_matcher.find()) {
			// ��ɴʣ��Ӷ��ж��Ƿ��������ϵ
			String tmp1 = np_matcher.group(0);
			// ���ʸ����䵥��
			Pattern nns = Pattern.compile("\\(NNS\\s(([^()]*)s)\\)");
			Matcher nns_matcher = nns.matcher(tmp1);
			String t1 = "", t2 = "", tmp2 = tmp1;
			while (nns_matcher.find()) {
				t1 = nns_matcher.group(1);
				t2 = nns_matcher.group(2);
				tmp2 = tmp1.replaceAll(t1, t2);
				// System.out.println(t1+"->"+t2);
			}
			tmp1 = tmp1
					.replaceAll(
							"\\(JJ[SR]\\s[^()]*\\)|\\(DT\\s[^()]*\\)|\\([A-Z]*\\s|[^A-Za-z0-9\\s-./']",
							"")
					// .replaceAll("\\(NN[A-Z]?\\s|\\(NP\\s|\\)|[^A-Za-z\\s]",
					// "")
					.trim();
			tmp2 = tmp2
					.replaceAll(
							"\\(JJ[SR]\\s[^()]*\\)|\\(DT\\s[^()]*\\)|\\([A-Z]*\\s|[^A-Za-z0-9\\s-./']",
							"")
					// .replaceAll("\\(NN[A-Z]?\\s|\\(NP\\s|\\)|[^A-Za-z\\s]",
					// "")
					.trim();
			// System.out.println("->"+tmp1);
			String[] tmp = tmp1.split("\\s");
			for (String s : tmp) {
				if (hm.contains(s)) {
					// ArrayList<String> al;
					// if (FeatureMap.containsKey(tmp1)) {
					// al = FeatureMap.get(tmp1);
					// } else
					// al = new ArrayList<String>();
					// if (!al.contains(sid))
					// al.add(sid);
					// tmp1 = tmp1.replace(t1, t2);// д��֮ǰ�Ѹ����滻�ɵ���
					// System.out.println(tmp1);
					FeatureMap.put(tmp1, tmp2);
					break;
				}
			}
		}
		// ���ݴʺͶ��ʵ�����
		Pattern VPADJPattern = Pattern
				.compile("\\((VB[A-Z]?|JJ[A-Z]?)\\s[^()]*\\)");
		Matcher VPmatcher = VPADJPattern.matcher(penn);
		while (VPmatcher.find()) {
			String tmp = VPmatcher
					.group(0)
					.replaceAll(
							"\\(VB[A-Z]?\\s|\\(JJ[A-Z]?\\s|\\)|[^A-Za-z\\s-./']",
							"").trim();
			// ArrayList<String> al;
			// if (OpinionMap.containsKey(tmp)) {
			// al = OpinionMap.get(tmp);
			// } else
			// al = new ArrayList<String>();
			// if (!al.contains(sid))
			// al.add(sid);
			OpinionMap.put(tmp, tmp);
		}
	}

	public void createGold() {
		String driver = "com.mysql.jdbc.Driver";// ��������
		String url = dburl;// odbc����Դ
		String username = "root";// �û���
		String password = "root";// ����
		String command = null;
		java.sql.Statement sm = null;
		Connection con = null;

		try {
			Class.forName(driver);
			con = DriverManager.getConnection(url, username, password);
			PreparedStatement preparedStatement = con
					.prepareStatement(gold_sql);
			ResultSet rs = preparedStatement.executeQuery();
			String rsid, target, categray;
			while (rs.next()) {
				rsid = rs.getString(1).trim();
				target = rs.getString(2).trim();
				ArrayList<String> al;
				// rsid = rsid.replaceAll(":[0-9]*", "");
				if (gold.containsKey(target)) {
					al = gold.get(target);
				} else {
					// System.out.println(target);
					al = new ArrayList<String>();
				}
				if (!al.contains(rsid))
					al.add(rsid);
				gold.put(target, al);
			}
			FileVsObject.writeObject("data/" + dataset + "/gold", gold);
			System.out.println("the size of gold is " + gold.size());
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void createCandidate() {
		String driver = "com.mysql.jdbc.Driver";// ��������
		String url = dburl;// odbc����Դ
		String username = "root";// �û���
		String password = "root";// ����
		String command = null;
		java.sql.Statement sm = null;
		Connection con = null;

		try {
			Class.forName(driver);
			con = DriverManager.getConnection(url, username, password);
			PreparedStatement preparedStatement = con
					.prepareStatement(candidate_sql);
			ResultSet rs = preparedStatement.executeQuery();
			String rsid, penn, dependency;

			int i = 1;
			while (rs.next()) {
				rsid = rs.getString(1);
				penn = rs.getString(2);
				dependency = rs.getString(3);
				 rsid = rsid.replaceAll(":[0-9]*", "");// ȥ�����Ӻţ�ֻ�������ۺ�
				createMap(rsid, penn, dependency);
				// System.out.println(i++);
				// if (i > 10)
				// break;
			}

			FileVsObject.writeObject("data/" + dataset + "/FeatureMap",
					FeatureMap);
			FileVsObject.writeObject("data/" + dataset + "/OpinionMap",
					OpinionMap);

			System.out
					.println("the size of FeatureMap is " + FeatureMap.size());
			System.out
					.println("the size of OpinionMap is " + OpinionMap.size());

			// ��gold��ע��FeatureMap�ϲ������ɾ���ͳ��Դ
			FMap = (HashMap<String, String>) FeatureMap.clone();
			for (Object obj : gold.keySet()) {
				FMap.put((String) obj, (String) obj);
			}
			FileVsObject.writeObject("data/" + dataset + "/FMap", FMap);
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	public void Evaluate(double th, boolean isTestCandidate) {
		ArrayList<String> gold_standard = new ArrayList<String>();
		gold_standard = new ArrayList<String>(gold.keySet());
		ArrayList<String> tmp = new ArrayList<String>();
		tmp.addAll(gold_standard);
		if (!isTestCandidate)
			tmp.retainAll(this.Feature);
		else
			tmp.retainAll(this.FeatureMap.keySet());
		// // ���������з���
		// for (String s : FeatureMap.keySet()) {
		// if (!gold_standard.contains(s))
		// System.out.println(s);
		// }
		double pre, rec, f1;
		if (!isTestCandidate)
			pre = (double) tmp.size() / Feature.size();
		else
			pre = (double) tmp.size() / FeatureMap.size();
		rec = (double) tmp.size() / gold_standard.size();
		f1 = 2 * pre * rec / (pre + rec);
		System.out.print("," + th + "," + this.lambda + ",");
		System.out.print(pre + "," + rec + "," + f1);
		System.out.println();
	}

	private void statsForEachSentence(String rsid, String sents) {
//		ArrayList<String> sentence = tokenizer.tokenize(sents);//���ִʵ�ArrayList���޷�ƥ�����
		String sentence = sents;
		ArrayList<String> al;
		 rsid = rsid.replaceAll(":[0-9]*", "");
		// ͳ�Ʊ����а�����Feature��
		for (String f : FMap.keySet()) {
			if (sentence.contains(" "+f) || sentence.contains(f+" ")) {
				if (FeatureCount.containsKey(f)) {
					al = FeatureCount.get(f);
				} else {
					// System.out.println(target);
					al = new ArrayList<String>();
				}
				if (!al.contains(rsid))
					al.add(rsid);
				FeatureCount.put(f, al);
			}
		}
		// ͳ�Ʊ����а�����OPinion��
		for (String f : OpinionMap.keySet()) {
			if (sentence.contains(" "+f+" ")) {
				if (OpinionCount.containsKey(f)) {
					al = OpinionCount.get(f);
				} else {
					// System.out.println(target);
					al = new ArrayList<String>();
				}
				if (!al.contains(rsid))
					al.add(rsid);
				OpinionCount.put(f, al);
			}
		}
	}

	private void CountCorpus() {
		FeatureCount = new HashMap<String, ArrayList<String>>();
		OpinionCount = new HashMap<String, ArrayList<String>>();
		String driver = "com.mysql.jdbc.Driver";// ��������
		String url = dburl;// odbc����Դ
		String username = "root";// �û���
		String password = "root";// ����
		String command = null;
		java.sql.Statement sm = null;
		Connection con = null;

		try {
			Class.forName(driver);
			con = DriverManager.getConnection(url, username, password);
			PreparedStatement preparedStatement = con
					.prepareStatement(Corpus_sql);
			ResultSet rs = preparedStatement.executeQuery();
			String rsid, text;

			int i = 1;
			while (rs.next()) {
				rsid = rs.getString(1);
				text = rs.getString(2);
//				 rsid = rsid.replaceAll(":[0-9]*", "");// ȥ�����Ӻţ�ֻ�������ۺ�
				statsForEachSentence(rsid, text);
				// System.out.println(i++);
				// if (i > 10)
				// break;
			}

			FileVsObject.writeObject("data/" + dataset + "/FeatureCount",
					FeatureCount);
			FileVsObject.writeObject("data/" + dataset + "/OpinionCount",
					OpinionCount);

			System.out.println("the size of FeatureCount is "
					+ FeatureCount.size());
			System.out.println("the size of OpinionCount is "
					+ OpinionCount.size());
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	private void createFeatureVector() {
		double[][] dist = null;

		Word2VEC vec = new Word2VEC();
		try {
			vec.loadModel("data/" + dataset + "/dc1.bin");
			HashMap<String, double[]> wordmap = vec.getWordMap();
			HashMap<String, double[]> FeatureVec = new HashMap<String, double[]>();
			// �Ȱ�����Ŀ���ʾ������,����FeatrueMaP gold�ϼ�
			Map<String,String> allFeature = new HashMap<String,String>();
			for (String key:gold.keySet()){
				allFeature.put(key, "1");
			}
			for (String key:FeatureMap.keySet()){
				allFeature.put(key, "-1");
			}
			dist = new double[allFeature.size()][allFeature.size()];
			for (String feature : allFeature.keySet()) {
				String[] words = feature.split("\\s+");
				double[] featurevec = new double[200];
				if (words.length > 1) {
					featurevec = vec.getWordVector(words[0]);
					if (featurevec == null) {
						featurevec = vec.getWordVector("</s>");
						// System.out.println("there is a out word: " +
						// words[0]);
					}
					for (int j = 1; j < words.length; j++) {
						double[] wv = vec.getWordVector(words[j]);
						if (wv == null) {
							wv = vec.getWordVector("</s>");
							// System.out.println("there is a out word: "
							// + words[j]);
						}
						for (int i = 0; i < 200; i++)
							featurevec[i] = featurevec[i] + wv[i];
					}
				} else {
					featurevec = vec.getWordVector(feature);
					if (featurevec == null) {
						featurevec = vec.getWordVector("</s>");
						// System.out.println("there is a out word: " +
						// feature);
					}
				}
//				System.out.println(featurevec);
				FeatureVec.put(feature, featurevec);
			}
			// ��Ŀ�걣�浽arrylist��Ŀ���Ƕ�Ӧ��ά�����е�˳����Ϊmapû��λ����Ϣ
	            
			ArrayList<String> FeatureList = new ArrayList<String>(allFeature.keySet());
			for (int i = 0; i < allFeature.size(); i++) {
				for (int j = i; j < allFeature.size(); j++) {
					double[] vector1 = FeatureVec.get(FeatureList.get(i));
					double[] vector2 = FeatureVec.get(FeatureList.get(j));
					// System.out.println(FeatureList.get(i)+":"+FeatureList.get(j));
					for (int k = 0; k < 200; k++) {
						// System.out.println(j);
						dist[i][j] += vector1[k] * vector2[k];
					}
				}
			}
			// // �Ȱ�����Ŀ���ʾ������
			// for (String feature : FeatureMap.keySet()) {
			// String[] words = feature.split("\\s+");
			// float[] featurevec = new float[200];
			// if (words.length > 1) {
			// featurevec = vec.getWordVector(words[0]);
			// for (int j = 1; j < words.length; j++) {
			// float[] wv = vec.getWordVector(words[j]);
			// for (int i = 0; i < 200; i++)
			// featurevec[i] = featurevec[i] + wv[i];
			// }
			// } else
			// featurevec = vec.getWordVector(feature);
			// FeatureVec.put(feature, featurevec);
			// }
			// // ��Ŀ�걣�浽arrylist��Ŀ���Ƕ�Ӧ��ά�����е�˳����Ϊmapû��λ����Ϣ
			// ArrayList<String> OpinionList = new ArrayList<String>(
			// OpinionMap.keySet());
			// for (int i = 0; i < OpinionMap.size(); i++) {
			// for (int j = i; j < OpinionMap.size(); j++) {
			// float[] vector1 = FeatureVec.get(FeatureList.get(i));
			// float[] vector2 = FeatureVec.get(FeatureList.get(j));
			// for (int k = 0; k < 200; k++)
			// dist[i][j] += vector1[k] * vector2[k];
			// }
			// }
			FileVsObject.writeObject("data/" + dataset + "/dist", dist);
			FileVsObject.writeObject("data/" + dataset + "/FeatureList",
					FeatureList);
			
			// ���FeatureVector���ı�
			// ͬʱ����������ת����SVM��ʶ����ʽ������ı�
//			ArrayList<String> FVector = new ArrayList<String>();
	        FileOutputStream fos = new FileOutputStream("data/" + dataset + "/FVector");
			for (int i = 0; i < allFeature.size(); i++) {
				fos.write(allFeature.get(FeatureList.get(i)).getBytes());
				fos.write(" ".getBytes());
					double[] vector = FeatureVec.get(FeatureList.get(i));					
					for (int k = 1; k <= 200; k++) {
						// System.out.println(j);
						fos.write((k+":"+vector[k-1]).getBytes());
						fos.write(" ".getBytes());
					}
				fos.write("\r\n".getBytes()); //����windowsϵͳ�Ļ��С�
			}			 
	        fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.dist = dist;
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// String dataset = ABoot.dataset;

		PUExtracting aboot = new PUExtracting("data/dc1.ini", true);
		// **********************ʹ����ͨcosine �����DBSCAN��ʼ����***********************
		HashMap<Integer, ArrayList<Integer>> clusterCount = new HashMap<Integer, ArrayList<Integer>>();
		try {
			aboot.createFeatureVector();
			FeatureList = (ArrayList<String>) FileVsObject
					.readObject("data/" + aboot.dataset + "/FeatureList");

			// int max = dist.length;// ���Ľ����Ŀ
			// int[] cluster = new int[max];
			// DBSCAN dbscan = new DBSCAN(dist);
			// // System.out.println(":       DBSCAN");
			// double epsilon = 0.9;
			//
			// while (max > dist.length / 4) {
			// epsilon -= 0.01;
			// clusterCount.clear();// ��ѭ��������
			// cluster = dbscan.gogogo(epsilon, 4, clusterCount);
			//
			// // ͳ������cluster�еĽ����
			// max = 0;// �Ƚ�max����Ϊ0
			// for (int j = 1; j <= clusterCount.size(); j++) {
			// if (max < clusterCount.get(j).size())
			// max = clusterCount.get(j).size();
			// }
			// }
			// ********************** ʹ����ͨcosine �����DBSCAN
			// end***************************

			boolean init = false;

			if (init) {
				// create some map ************************
				// ABoot aboot = new ABoot();
				// aboot.gold.clear();
				// aboot.createGold();
				//
				// aboot.FeatureMap.clear();
				// aboot.OpinionMap.clear();
				// aboot.createCandidate();
				//
				// aboot.FeatureCount.clear();
				// aboot.OpinionCount.clear();
				// aboot.CountCorpus();

				aboot.Evaluate(2, true);
				// for (String s : aboot.FeatureMap.values()) {
				// System.out.println(s);
				// }
				// end **************************************
			} else {
				//
				// runing **************************

				ArrayList<String> al = new ArrayList<String>(
						aboot.gold.keySet());
				// for(String s:oc.keySet()){
				// System.out.println(s);
				// }
//				 ArrayList<String> CF = new ArrayList<String>(
//				 aboot.FeatureMap.keySet());
//				 ArrayList<String> rndCF = (ArrayList<String>)
//				 RundomSubsetToolkit
//				 .randomSubset(CF, CF.size() * 1 / 4);
//				 ArrayList<String> gold = (ArrayList<String>)
//				 RundomSubsetToolkit
//				 .randomSubset(al, al.size() * 99 / 100);
//				 CF.clear();
//				 CF.addAll(rndCF);
//				 CF.addAll(gold);
//				 StoreFileObject
//				 .writeObject("data/" + aboot.dataset + "/CF", CF);
				ArrayList<String> CF = (ArrayList<String>) FileVsObject
						.readObject("data/" + aboot.dataset + "/CF.best");
				ArrayList<String> OF = new ArrayList<String>(
						aboot.OpinionMap.keySet());
				for (int i = 0; i <= 50; i++) { 
//					 aboot.lambda = i*0.1;
					ArrayList<String> s = new ArrayList<String>();
					// // ��ȡ����
//					 for (int j = 1; j < 11; j++) {
//					 int index = (int) (Math.random() * al.size());
//					 if (!s.contains(al.get(index))) {
//					 s.add(al.get(index));
//					 System.out.println(al.get(index));
//					 }
//					 }
					 // for CRD dc1 �ֹ����
					 s.add("memory card");
					 s.add("white offset");
					 s.add("viewfinder");
					 s.add("weight");
					 s.add("lens cover");
					 s.add("depth");
					 s.add("stitch picture");
					 s.add("grain");
					 s.add("finish");
					 s.add("lcd");

//					// for absa15, manual
//					s.add("ambience");
//					s.add("dessert");
//					s.add("decor");
//					s.add("atmosphere");
//					s.add("bagel");
//					s.add("menu");
//					s.add("dishes");
//					s.add("pizza");
//					s.add("fish");
//					s.add("drinks");

					aboot.Feature.clear();
					aboot.run(CF, OF, s, i);// non-gold��ʷ���ֵ��0.4*7->4362,
					// 0.3*7->4394, 0.2*9->4362,
					// 0.5*5->4383;
					// gold��ʷ���ֵ��0.3*7->6580,
					// 0.4*6->6564, 0.5*5->6572
					//
					System.out.print(CF.size() + ","
							+ aboot.Feature.size());
					// ����
					aboot.Evaluate(i, false);
				}
			}
			// end **********************************
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// for(String s:ABoot.FeatureMap.keySet()){
		// System.out.println(s);
		// }
		// for(String s:ABoot.OpinionMap.keySet()){
		// System.out.println(s);
		// }

	}
}
