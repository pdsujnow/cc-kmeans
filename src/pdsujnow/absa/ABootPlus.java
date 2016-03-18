package pdsujnow.absa;

import java.io.IOException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pdsu.xsf.algorithm.DocumentClusterer;
import pdsu.xsf.algorithm.Similarity;
import pdsu.xsf.algorithm.Word2VEC;
import pdsu.xsf.text.*;

import pdsu.xsf.utils.FileVsObject;
import pdsu.xsf.utils.MapUtil;
import pdsu.xsf.utils.SQLHelper;
import pdsu.xsf.utils.ScriptExecuter;

/***
 * 
 * 
 * @author xsf
 * 
 */
public class ABootPlus {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// String dataset = ABoot.dataset;

		ABootPlus aboot = new ABootPlus("data/dc1.ini", false);
		// **********************使用普通cosine 距离的DBSCAN开始处理***********************
		// HashMap<Integer, ArrayList<Integer>> clusterCount = new
		// HashMap<Integer, ArrayList<Integer>>();
		try {
			// aboot.computeDistance(true,1);
			// FeatureList = (ArrayList<String>) FileVsObject.readObject("data/"
			// + aboot.dataset + "/FeatureList");
			// int max = aboot.dist.length;// 最多的结点数目
			// int[] cluster = new int[max];
			// DBSCAN dbscan = new DBSCAN(aboot.dist);
			// // System.out.println(":       DBSCAN");
			// double epsilon = 0.9;
			//
			// while (max > aboot.dist.length / 4) {
			// epsilon -= 0.01;
			// clusterCount.clear();// 新循环，清零
			// cluster = dbscan.gogogo(epsilon, 4, clusterCount);
			//
			// // 统计最大的cluster中的结点数
			// max = 0;// 先将max重置为0
			// for (int j = 1; j <= clusterCount.size(); j++) {
			// if (max < clusterCount.get(j).size())
			// max = clusterCount.get(j).size();
			// }
			// }
			// //********************** 使用普通cosine 距离的DBSCAN
			// end***************************

			// boolean init = false;

			// if (init) {
			// // create some map ************************
			//
			// aboot.Evaluate(2, true);
			// // for (String s : aboot.FeatureMap.values()) {
			// // System.out.println(s);
			// // }
			// // end **************************************
			// } else {
			// System.setOut(System.out);// restore System.out
			// //
			// runing **************************

			// ArrayList<String> al = new
			// ArrayList<String>(aboot.gold.keySet());
			// // for(String s:oc.keySet()){
			// // System.out.println(s);
			// // }
			// ArrayList<String> CF = new ArrayList<String>(
			// aboot.FeatureMapfromRegx.keySet());
			// ArrayList<String> rndCF = (ArrayList<String>)
			// RundomSubsetToolkit
			// .randomSubset(CF, CF.size() * 1 / 5);
			// ArrayList<String> gold = (ArrayList<String>)
			// RundomSubsetToolkit
			// .randomSubset(al, al.size() * 99 / 100);
			// CF.clear();
			// CF.addAll(rndCF);
			// CF.addAll(gold);
			// FileVsObject
			// .writeObject("data/" + aboot.dataset + "/CF", CF);
			ArrayList<String> CF = (ArrayList<String>) FileVsObject
					.readObject("data/" + aboot.dataset + "/CF.best");
			ArrayList<String> OF = new ArrayList<String>(
					aboot.OpinionMap.keySet());
			for (int i = 0; i <= 10; i++) {
				aboot.lambda = 0;
				ArrayList<String> s = new ArrayList<String>();
				// // 获取种子
				// for (int j = 1; j < 11; j++) {
				// int index = (int) (Math.random() * al.size());
				// if (!s.contains(al.get(index))) {
				// s.add(al.get(index));
				// System.out.println(al.get(index));
				// }
				// }
				s = FileVsObject.readFileByLines("data/" + aboot.dataset
						+ "/seed.txt", 1);
				aboot.Feature.clear();
				aboot.ORFilter(CF, OF, s, 0.1*i);// non-gold历史最好值：0.4*7->4362,
				// 0.3*7->4394, 0.2*9->4362,
				// 0.5*5->4383;
				// gold历史最好值：0.3*7->6580,
				// 0.4*6->6564, 0.5*5->6572
				//
				System.out.print(CF.size() + "," + aboot.Feature.size());
				// 评估
				aboot.Evaluate(i, false);
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

	// private double ffth = 0.2;
	// private double foth = 0.2;
	// private double ooth = 0.2;
	private String dataset;// = "semeval/15";
	private String dburl;
	private String gold_sql;
	private String candidate_sql;
	private String Corpus_sql;
	private int total = 213;// 语料句子总数
	// 以上参数从配置中按行读取
	private double[][] similarity;
	private static ArrayList<String> FeatureList;// 所有候选列表
	// private static ArrayList<Integer> ClusterIDList;// 目标对应的clusterID list,
	// // non-feature id is -1
	// private HashMap<Integer, ArrayList<String>> Cluster;// save members for
	// each
	// // cluster
	private HashMap<String, String> FeatureMapfromRegx = new HashMap<String, String>();// 候选Feature
	private HashMap<String, String> OpinionMap = new HashMap<String, String>();// 候选Op
	private HashMap<String, ArrayList<String>> FeatureCount = new HashMap<String, ArrayList<String>>();//
	private HashMap<String, ArrayList<String>> OpinionCount = new HashMap<String, ArrayList<String>>();//
	private HashMap<String, ArrayList<String>> gold = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> ff_association = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> fo_association = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> of_association = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> oo_association = new HashMap<String, ArrayList<String>>();
	private HashMap<String, String> FMapcontainGold;
	private ArrayList<String> Feature = new ArrayList<String>();
	private ArrayList<String> Opinion = new ArrayList<String>();
	private double lambda = 0.98;// 0.95, .96 .97 .98 都可以
	private PhraseSimilarity ps;

	private Logger log;

	/***
	 * All data stored in sqlite, so reading configure from inifile firstly.
	 * Then, create gold from annotated data. Thirdly, create Candidate using
	 * POS tags and parsing tree information
	 * 
	 * @param init
	 *            是否重新初始化成员值
	 */
	@SuppressWarnings("unchecked")
	public ABootPlus(String inifile, boolean init) {
		log = Logger.getLogger("aboot");
		log.setLevel(Level.INFO);
		ArrayList<String> ini = FileVsObject.readFileByLines(inifile, 2);
		dataset = (String) ini.get(0);
		gold_sql = (String) ini.get(1);
		candidate_sql = (String) ini.get(2);
		Corpus_sql = (String) ini.get(3);
		total = Integer.parseInt(ini.get(4));

		this.createGold(init);
		this.createCandidate(init);
		this.computeSimilarity(init, 1);
		this.CountCorpus(init);
	}

	@SuppressWarnings("unchecked")
	private double association(String s1, String s2, String type) {
		// TODO Auto-generated method stub
		// s1="nomad explorer";
		// s2="nomad jukebox";
		double coo = 0;// 共现关系强度
		double sem = 0;// 语义关联强度
		int d1, d2;// 串s1s2在语义相似矩阵中的位置
		// System.out.println(s1+":"+s2);
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
		// 读取语义关联值
		if (type == "ff") {
			d1 = FeatureList.indexOf(s1);
			d2 = FeatureList.indexOf(s2);
			// precessing singular and plural
			if (d1 == -1) {//s1 may be a singular
				d1 = FeatureList.indexOf(Inflector.getInstance().pluralize(s1));
			}
			if (d2 == -1) {//s2 may be a singular
				d2 = FeatureList.indexOf(Inflector.getInstance().pluralize(s2));
			} else if (d1 == -1 || d2 == -1) {
				sem = 0;
				System.out.println(d1 + ":" + d2 + "->" + s1 + ":" + s2);
			}else{
				if (d1 < d2)
					sem = similarity[d1][d2];
				else
					sem = similarity[d2][d1];
				// sem = ps.getSimilarity(s1, s2);
			}
		}
		// System.out.println(s1+"/"+s2+":"+sem);
		if (a1 == null || a2 == null) {
			// if (a1 == null)
			// System.out.println("there is unmatched phrase  -> " + s1);
			// else
			// System.out.println("there is unmatched phrase  -> " + s2);
			return coo * lambda + (1 - lambda) * sem;
		}
		// 把a1的元素全加到a3里
		a3.addAll(a1);
		// 把a2,a3的共同元素保存到a3
		a3.retainAll(a2);
		int k1 = a3.size();
		int k2 = a1.size() - a3.size();
		int k3 = a2.size() - a3.size();
		int k4 = total - a1.size() - a2.size() + a3.size();
		// int k1 = 3;int k2=1;int k3=1;int k4 =4;
		int n1 = k1 + k3;
		int n2 = k2 + k4;
		double p1 = (double) k1 / n1;
		double p2 = (double) k2 / n2;
		double p = (double) (k1 + k2) / (n1 + n2);
		coo = 2 * (Math.log(computeL(p1, k1, n1))
				+ Math.log(computeL(p2, k2, n2))
				- Math.log(computeL(p, k1, n1)) - Math.log(computeL(p, k2, n2)));
		// System.out.println(coo);

		return coo * lambda + (1 - lambda) * sem;
		// return coo;
	}

//	public void augmentedEM() throws IOException, InterruptedException {
//		ArrayList<String> input = new ArrayList<String>();
//		for (String f : FMapcontainGold.keySet()) {
//			if (gold.containsKey(Inflector.getInstance().singularize(f)))
//				input.add(f);
//		}
//		// write gold file which need manual annotate the cluster ID
//		FileVsObject.writeText("data/" + dataset + "/gold.cluster", input);
//		ArrayList<Integer> ClusterIDList = this.connectPhrases(input);
//		HashMap<Integer, ArrayList<String>> Cluster = this.mergeComponents(
//				input, 10);
//		// this method will change the ClusterIDList
//		selectLabeledData(input, ClusterIDList, Cluster, 10);
//		contextExtract(ClusterIDList, input);
//		// contextExtract(ClusterIDList, input, 15);
//		// contextExtractInSentenceLevel(ClusterIDList, input);
//		// useWordVector(ClusterIDList,input);
//
//		String method = "Weighting & new Vector ->";// processing method which
//													// will print
//		// in result file
//		// load gold standard
//		ArrayList<String> instances = FileVsObject.readFileByLines("data/"
//				+ dataset + "/gold.cluster.txt", 1);
//		if (instances.size() == 0) {
//			log.info("There isn't gold standard file!");
//			System.exit(0);
//		}
//		HashMap<String, String> answer = new HashMap<String, String>();
//		for (String in : instances) {
//			String ins[] = in.split("\t");
//			answer.put(ins[0], ins[1]);
//		}
//		// end
//
//		String corpuspath = "data/" + dataset + "/InstanceList";
//		// runLDA rl = new runLDA();
//		// String wtpath = "data/" + dataset + "/word-topic";
//		// String dispath = "data/" + dataset + "/distribution";
//		// rl.run(corpuspath, wtpath, dispath);
//
//		// filtering noise words from context based on LDA topic assign
//
//		String regx = "((?:\\w+-* *)*)\\t+(-?[0-9]+)\\t+(.*)";
//		DocumentClusterer dc = new DocumentClusterer(answer, corpuspath,
//				"data/" + dataset + "/clusterResult.data", instances, regx);
//		// for (int ii = 19; ii < 20; ii++) {
//		// dc.clustering(DocumentClusterer.EM, 11);
//
//		Word2VEC vec = new Word2VEC();
//		// vec.loadModel("data/CRD/mp3/crd.bin");
//		vec.loadModelFromObject("data/CRD/crd_new");
//		dc.clusteringWithFeatureWeighting(vec, DocumentClusterer.KMEANS, 10);
//		// dc.clusteringFromFeatureVector(11);
//		method += "Kmeans->";
//		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		FileVsObject.writeText("data/" + dataset + "/evalResult",
//				method + df.format(new java.util.Date()), true);
//		// String command = "script/run.bat";
//		String command = "python script/eval.py " + "data/" + dataset
//				+ "/clusterResult.data";
//		String evalResult = ScriptExecuter.execute(command);
//		FileVsObject.writeText("data/" + dataset + "/evalResult", evalResult,
//				true);
//		// }
//	}

	public double AvgPhraseSim(PhraseSimilarity ps, ArrayList<String> al1,
			ArrayList<String> al2) {
		double sim = 0;
		for (String s1 : al1)
			for (String s2 : al2)
				sim += ps.getSimilarity(s1, s2);
		return sim;
	}

	/***
	 * option: 1: 采用word2vec 2: 采用 wordnet !!! this method is time comsuming
	 */
	@SuppressWarnings("unchecked")
	private void computeSimilarity(boolean init, int option) {
		similarity = (double[][]) FileVsObject.readObject("data/" + dataset
				+ "/similarity");
		FeatureList = (ArrayList<String>) FileVsObject.readObject("data/"
				+ dataset + "/FeatureList");
		if (!init && similarity != null && FeatureList != null) {
			return;
		}
		double[][] similarity = null;

		try {
			HashMap<String, double[]> FeatureVec = new HashMap<String, double[]>();
			// 先把所有目标表示成向量,包括FeatrueMaP gold合集
			Set<String> allFeature = FMapcontainGold.keySet();
			ArrayList<String> FeatureList = new ArrayList<String>(allFeature);
			similarity = new double[allFeature.size()][allFeature.size()];
			if (option == 1) {// use word2vec similarity
				// 如果是word2vec则需要循环一次计算一个目标
				for (String feature : allFeature) {
					Word2VEC vec = new Word2VEC();
					vec.loadModelFromObject("data/CRD/crd_new");
					String[] words = feature.split("\\s+");
					double[] featurevec = new double[200];
					// compute a target term vector sum
					// *******************************************
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
					// compute sum end ************************************
					// System.out.println(feature);
					FeatureVec.put(feature, featurevec);
				}
				// 将目标保存到arrylist，目的是对应二维数组中的顺序，因为map没有位置信息
				for (int i = 0; i < allFeature.size(); i++) {
					for (int j = i; j < allFeature.size(); j++) {
						double[] vector1 = FeatureVec.get(FeatureList.get(i));
						double[] vector2 = FeatureVec.get(FeatureList.get(j));
						// System.out.println(FeatureList.get(i)+":"+FeatureList.get(j));
						// for (int k = 0; k < 200; k++) {
						// // System.out.println(j);
						// dist[i][j] += vector1[k] * vector2[k];
						// }
						double d = new Similarity().cosineDistance(vector1,
								vector2);
						similarity[i][j] = 1 - d;
						// System.out.println();
					}
					// // 先把所有目标表示成向量
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
					// // 将目标保存到arrylist，目的是对应二维数组中的顺序，因为map没有位置信息
					// ArrayList<String> OpinionList = new
					// ArrayList<String>(
					// OpinionMap.keySet());
					// for (int i = 0; i < OpinionMap.size(); i++) {
					// for (int j = i; j < OpinionMap.size(); j++) {
					// float[] vector1 = FeatureVec.get(FeatureList.get(i));
					// float[] vector2 = FeatureVec.get(FeatureList.get(j));
					// for (int k = 0; k < 200; k++)
					// dist[i][j] += vector1[k] * vector2[k];
					// }
					// }
				}
			} else if (option == 2) {// use wordnet similarity
				ps = new PhraseSimilarity();
				for (int i = 0; i < allFeature.size(); i++) {
					log.info(i
							+ "   ***********************************************************");
					for (int j = i; j < allFeature.size(); j++) {
						String phrase1 = FeatureList.get(i);
						String phrase2 = FeatureList.get(j);
						// System.out.println(FeatureList.get(i)+":"+FeatureList.get(j));
						similarity[i][j] = ps.getSimilarity(phrase1, phrase2);
					}
				}
			}

			FileVsObject.writeObject("data/" + dataset + "/similarity",
					similarity);
			FileVsObject.writeObject("data/" + dataset + "/FeatureList",
					FeatureList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.similarity = similarity;
	}

	private double computeL(double p, int k, int n) {
		double t1 = Math.pow(p, k);
		double t2 = Math.pow(1 - p, n - k);
		return t1 * t2;
	}

	/***
	 * connecting any two phrase if they have sharing words in field
	 * 'FeatureList', save the result to field 'ClusterIDList' ClusterID use the
	 * smaller index between two phrase
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Integer> connectPhrases(ArrayList<String> FList)
			throws IOException {
		ArrayList<Integer> ClusterIDList = (ArrayList<Integer>) FileVsObject
				.readObject("data/" + dataset + "/ClusterIDList");
		if (ClusterIDList != null)
			return ClusterIDList;
		ClusterIDList = new ArrayList<Integer>(FList.size());
		for (int k = 0; k < FList.size(); k++)
			ClusterIDList.add(k, -1);// unlabeled data tag is -1
		for (int i = 0; i < FList.size() - 1; i++) {
			String[] p1 = FList.get(i).split("\\s+");
			for (int j = i + 1; j < FList.size(); j++) {
				String[] p2 = FList.get(j).split("\\s+");
				for (String w1 : p1)
					for (String w2 : p2)
						if (!new StopwordList().isStopWord(w1) && w1.equals(w2)) {
							if (ClusterIDList.get(i) == -1) {// if true that
																// means the ID
																// is
																// assigned ,not
																// modify
								ClusterIDList.set(i, i);
								ClusterIDList.set(j, i);
							} else
								ClusterIDList.set(j, ClusterIDList.get(i));
						}
			}
		}
		FileVsObject.writeObject("data/" + dataset + "/ClusterIDList",
				ClusterIDList);
		return ClusterIDList;
	}

	/***
	 * contain all words in sentences (including other features), so need to
	 * provide a post-process to filter the features could using
	 * cc.mallet.pipe.CharSequenceReplace in pipes. this method save the
	 * exclusive features list for each context, each features split with ":"
	 * 
	 * @param ClusterIDList
	 * @param FList
	 * @param window
	 */
	@SuppressWarnings("unchecked")
	public void contextExtract(ArrayList<Integer> ClusterIDList,
			ArrayList<String> FList) {
		// check Feature list
		ArrayList<Integer> container = (ArrayList<Integer>) FList.clone();
		// container.retainAll(FeatureCount.keySet());
		container.removeAll(FeatureCount.keySet());
		if (container.size() > 0) {
			log.info("input collection has some outer!");
			System.exit(0);
		}
		HashMap<String, String> sentences = getAllSentences();
		// save which Feature one sentence contains
		HashMap<String, ArrayList<String>> SentenceCount = new HashMap<String, ArrayList<String>>();
		for (String feature : FeatureCount.keySet()) {
			ArrayList<String> al = FeatureCount.get(feature);
			for (String rsid : al)
				MapUtil.AddToListMap(SentenceCount, rsid, feature);
		}
		// extract context for each featrue
		HashMap<String, ArrayList<String>> exclusives = new HashMap<String, ArrayList<String>>();
		ArrayList<String> InstanceList = new ArrayList<String>();
		for (String feature : FList) {
			ArrayList<String> al = FeatureCount.get(feature);
			StringBuffer sb2 = new StringBuffer();
			// instance name \t label \t context
			sb2.append(feature + "\t"
					+ ClusterIDList.get(FList.indexOf(feature)) + "\t");
			for (String rsid : al) {
				String text = sentences.get(rsid);
				// features list of the sentence which id is rsid
				ArrayList<String> list = SentenceCount.get(rsid);
				// save all features in sentence
				for (String f : list) {
					MapUtil.AddToListMap(exclusives, feature, f);
				}
				sb2.append(text + " ");
			}
			InstanceList.add(sb2.toString());
		}

		ArrayList<String> exclusiveList = new ArrayList<String>();
		// instance name, label, text content
		for (String name : exclusives.keySet()) {
			String tmp = exclusives.get(name).toString().replace("[", "")
					.replace("]", "");
			exclusiveList.add(name + "\t" + tmp);
		}
		try {
			FileVsObject.writeText("data/" + dataset + "/InstanceList",
					InstanceList);
			FileVsObject.writeText("data/" + dataset + "/exclusives",
					exclusiveList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void contextExtractInSentenceLevel(ArrayList<Integer> ClusterIDList,
			ArrayList<String> FList) {
		// check Feature list
		ArrayList<Integer> container = (ArrayList<Integer>) FList.clone();
		// container.retainAll(FeatureCount.keySet());
		container.removeAll(FeatureCount.keySet());
		if (container.size() > 0) {
			log.info("input collection has some outer!");
			System.exit(0);
		}
		HashMap<String, String> sentences = getAllSentences();
		// save which Feature one sentence contains
		HashMap<String, ArrayList<String>> SentenceCount = new HashMap<String, ArrayList<String>>();
		for (String feature : FeatureCount.keySet()) {
			ArrayList<String> al = FeatureCount.get(feature);
			for (String rsid : al)
				MapUtil.AddToListMap(SentenceCount, rsid, feature);
		}
		// extract context for each featrue
		HashMap<String, ArrayList<String>> exclusives = new HashMap<String, ArrayList<String>>();
		ArrayList<String> InstanceList = new ArrayList<String>();
		for (String feature : FList) {
			ArrayList<String> al = FeatureCount.get(feature);
			for (String rsid : al) {
				StringBuffer sb2 = new StringBuffer();
				String text = sentences.get(rsid);
				// instance name \t label \t context
				sb2.append(feature + "\t"
						+ ClusterIDList.get(FList.indexOf(feature)) + "\t"
						+ text);
				// features list of the sentence which id is rsid
				ArrayList<String> list = SentenceCount.get(rsid);
				// save all features in sentence
				for (String f : list) {
					MapUtil.AddToListMap(exclusives, feature, f);
				}
				InstanceList.add(sb2.toString());
			}
		}

		ArrayList<String> exclusiveList = new ArrayList<String>();
		// instance name, label, text content
		for (String name : exclusives.keySet()) {
			String tmp = exclusives.get(name).toString().replace("[", "")
					.replace("]", "");
			exclusiveList.add(name + "\t" + tmp);
		}
		try {
			FileVsObject.writeText("data/" + dataset + "/InstanceList",
					InstanceList);
			FileVsObject.writeText("data/" + dataset + "/exclusives",
					exclusiveList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void useWordVector(ArrayList<Integer> ClusterIDList,
			ArrayList<String> FList) throws IOException {
		// check Feature list
		ArrayList<Integer> container = (ArrayList<Integer>) FList.clone();
		// container.retainAll(FeatureCount.keySet());
		container.removeAll(FeatureCount.keySet());
		if (container.size() > 0) {
			log.info("input collection has some outer!");
			System.exit(0);
		}
		// extract context for each featrue
		ArrayList<String> InstanceList = new ArrayList<String>();
		Word2VEC vec = new Word2VEC();
		vec.loadModel("data/" + dataset + "/crd.bin");
		for (String feature : FList) {
			double[] aspectVec = new double[200];
			String[] aspects = feature.split("\\s+");
			if (aspects.length > 1) {
				aspectVec = vec.getWordVector(aspects[0]);
				if (aspectVec == null) {
					aspectVec = vec.getWordVector("</s>");
					// System.out.println("there is a out word: " +
					// words[0]);
				}
				for (int j = 1; j < aspects.length; j++) {
					double[] wv = vec.getWordVector(aspects[j]);
					if (wv == null) {
						wv = vec.getWordVector("</s>");
						// System.out.println("there is a out word: "
						// + words[j]);
					}
					for (int i = 0; i < 200; i++)
						aspectVec[i] = aspectVec[i] + wv[i];
				}
			} else {
				aspectVec = vec.getWordVector(feature);
				if (aspectVec == null) {
					aspectVec = vec.getWordVector("</s>");
					// System.out.println("there is a out word: " +
					// feature);
				}
			}
			StringBuffer sb2 = new StringBuffer();
			String text = "0:" + aspectVec[0];
			for (int i = 1; i < 200; i++)
				text += " " + i + ":" + aspectVec[i];
			// instance name \t label \t context
			sb2.append(feature + "\t"
					+ ClusterIDList.get(FList.indexOf(feature)) + "\t" + text);
			InstanceList.add(sb2.toString());
		}

		FileVsObject.writeText("data/" + dataset + "/InstanceList",
				InstanceList);
	}

	/***
	 * not contain other features and stopwords
	 * 
	 * @param ClusterIDList
	 * @param FList
	 * @param window
	 */
	@SuppressWarnings("unchecked")
	public void contextExtract(ArrayList<Integer> ClusterIDList,
			ArrayList<String> FList, int window) {
		// check Feature list
		ArrayList<Integer> container = (ArrayList<Integer>) FList.clone();
		// container.retainAll(FeatureCount.keySet());
		container.removeAll(FeatureCount.keySet());
		if (container.size() > 0) {
			log.info("input collection has some outer!");
			System.exit(0);
		}
		HashMap<String, ArrayList<String>> context = new HashMap<String, ArrayList<String>>();
		HashMap<String, String> sentences = getAllSentences();
		// save which Feature one sentence contains
		HashMap<String, ArrayList<String>> SentenceCount = new HashMap<String, ArrayList<String>>();
		for (String feature : FeatureCount.keySet()) {
			ArrayList<String> al = FeatureCount.get(feature);
			for (String rsid : al)
				MapUtil.AddToListMap(SentenceCount, rsid, feature);
		}
		// extract context for each featrue
		for (String feature : FList) {
			ArrayList<String> al = FeatureCount.get(feature);
			for (String rsid : al) {
				String text = sentences.get(rsid);
				text = text.replace(feature, "@@");// a placeholder for current
													// feature
				// features list of the sentence which id is rsid
				ArrayList<String> list = SentenceCount.get(rsid);
				// remove stopwords and all features in sentence
				for (String f : list) {// ensure not replace the inner chars of
										// word
					text.replace(f + " ", "");
					text.replace(" " + f, "");
				}
				ArrayList<String> tokens = tokenizer.tokenize(text);
				Iterator<String> it = tokens.iterator();
				while (it.hasNext()) {
					String w = it.next();
					if (new StopwordList().isStopWord(w))
						it.remove();
				}
				int index = tokens.indexOf("@@");
				tokens.set(index, feature);// restore the feature
				int start = index - window > 0 ? index - window : 0;
				int end = index + window < tokens.size() ? index + window
						: tokens.size();
				Pattern w = Pattern.compile("[a-zA-Z0-9]+");
				for (int i = start; i < end; i++) {
					String word = tokens.get(i);
					Matcher matcher = w.matcher(word);
					if (matcher.find())
						MapUtil.AddToListMap(context, feature, word);
				}
			}
		}

		ArrayList<String> InstanceList = new ArrayList<String>();
		// instance name, label, text content
		for (String name : context.keySet()) {
			String tmp = context.get(name).toString().replace("[", "")
					.replace("]", "");
			InstanceList.add(name + "\t"
					+ ClusterIDList.get(FList.indexOf(name)) + "\t" + tmp);
		}
		try {
			FileVsObject.writeText("data/" + dataset + "/InstanceList",
					InstanceList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void CountCorpus(boolean init) {
		FeatureCount = (HashMap<String, ArrayList<String>>) FileVsObject
				.readObject("data/" + dataset + "/FeatureCount");
		OpinionCount = (HashMap<String, ArrayList<String>>) FileVsObject
				.readObject("data/" + dataset + "/OpinionCount");

		if (!init && FeatureCount != null && OpinionCount != null)
			return;
		FeatureCount = new HashMap<String, ArrayList<String>>();
		OpinionCount = new HashMap<String, ArrayList<String>>();

		try {
			ResultSet rs = SQLHelper.getResultSet(Corpus_sql);
			String rsid, text;
			int i = 1;
			while (rs.next()) {
				rsid = rs.getString(1);
				text = rs.getString(2);
				// rsid = rsid.replaceAll(":[0-9]*", "");// 去掉句子号，只保留评论号
				statsForEachSentence(rsid, text);
				// System.out.println(i++);
				// if (i > 10)
				// break;
			}

			// check collections
			ArrayList<String> container = new ArrayList<String>(
					FMapcontainGold.keySet());
			container.removeAll(FeatureCount.keySet());
			if (container.size() > 0) {
				log.log(Level.WARNING,
						"there are some features could not find relative sentence!");
				for (String s : container) {
					log.info(s + ":" + FMapcontainGold.get(s));
				}
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

	@SuppressWarnings("unchecked")
	public void createCandidate(boolean init) {
		FeatureMapfromRegx = (HashMap<String, String>) FileVsObject
				.readObject("data/" + dataset + "/FeatureMapfromRegx");
		OpinionMap = (HashMap<String, String>) FileVsObject.readObject("data/"
				+ dataset + "/OpinionMap");
		FMapcontainGold = (HashMap<String, String>) FileVsObject
				.readObject("data/" + dataset + "/FMapcontainGold");

		if (!init && FMapcontainGold != null && FeatureMapfromRegx != null
				&& OpinionMap != null)
			return;
		FeatureMapfromRegx = new HashMap<String, String>();
		OpinionMap = new HashMap<String, String>();

		try {
			ResultSet rs = SQLHelper.getResultSet(candidate_sql);
			String rsid, penn, dependency;

			int i = 1;
			while (rs.next()) {
				rsid = rs.getString(1);
				penn = rs.getString(2);
				dependency = rs.getString(3);
				rsid = rsid.replaceAll(":[0-9]*", "");// 去掉句子号，只保留评论号
				extractCandidateFromSentence(rsid, penn, dependency);
				// System.out.println(i++);
				// if (i > 10)
				// break;
			}

			// clear words that contains only one char
			Iterator<String> it = FeatureMapfromRegx.keySet().iterator();
			while (it.hasNext()) {
				String str = (String) it.next();
				if (str.length() == 1)
					it.remove();
			}
			it = OpinionMap.keySet().iterator();
			while (it.hasNext()) {
				String str = (String) it.next();
				if (str.length() == 1)
					it.remove();
			}

			FileVsObject.writeObject("data/" + dataset + "/FeatureMapfromRegx",
					FeatureMapfromRegx);
			FileVsObject.writeObject("data/" + dataset + "/OpinionMap",
					OpinionMap);

			System.out.println("the size of FeatureMapfromRegx is "
					+ FeatureMapfromRegx.size());
			System.out
					.println("the size of OpinionMap is " + OpinionMap.size());

			// 将gold标注与FeatureMap合并，构成句子统计源
			FMapcontainGold = (HashMap<String, String>) FeatureMapfromRegx
					.clone();
			for (String obj : gold.keySet()) {
				// when plural and singular are not in, then add it
				if (!FMapcontainGold.containsKey(Inflector.getInstance()
						.pluralize(obj))
						&& !FMapcontainGold.containsKey(Inflector.getInstance()
								.singularize(obj)))
					FMapcontainGold.put(obj, obj);
			}
			FileVsObject.writeObject("data/" + dataset + "/FMapcontainGold",
					FMapcontainGold);
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void createGold(boolean init) {
		gold = (HashMap<String, ArrayList<String>>) FileVsObject
				.readObject("data/" + dataset + "/gold");
		if (gold != null && !init)
			return;
		gold = new HashMap<String, ArrayList<String>>();
		ResultSet rs = SQLHelper.getResultSet(gold_sql);
		String rsid, target, categray;
		try {
			while (rs.next()) {
				rsid = rs.getString(1).trim();
				target = rs.getString(2).trim();
				// rsid = rsid.replaceAll(":[0-9]*", "");
				MapUtil.AddToListMap(gold, target, rsid);
			}
			FileVsObject.writeObject("data/" + dataset + "/gold", gold);
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
			tmp.retainAll(this.FeatureMapfromRegx.keySet());
		// // 输出结果进行分析
		// for (String s : FeatureMap.keySet()) {
		// if (!gold_standard.contains(s))
		// System.out.println(s);
		// }
		double pre, rec, f1;
		if (!isTestCandidate)
			pre = (double) tmp.size() / Feature.size();
		else
			pre = (double) tmp.size() / FeatureMapfromRegx.size();
		rec = (double) tmp.size() / gold_standard.size();
		f1 = 2 * pre * rec / (pre + rec);
		System.out.print("," + th + "," + this.lambda + ",");
		System.out.print(pre + "," + rec + "," + f1);
		System.out.println();
	}

	/**
	 * 保存每一个由词组和其正则的map,如 ("buttons","") Result directly save to global Field
	 * 
	 * @param sid
	 *            句子号
	 * @param penn
	 *            句法树串
	 * @param dependency
	 *            依存关系列表串，逗号分割
	 */
	public void extractCandidateFromSentence(String sid, String penn,
			String dependency) {
		// 名词短语的正则
		// Pattern NPPattern = Pattern.compile("\\(NP\\s(?:\\(.+?\\))*\\)");
		// 从依存关系中直接找名词
		Pattern amod = Pattern
				.compile("(?:amod|dobj|nsubj[a-z]*|pobj)\\(([^-()]*)-[0-9]*,([^-()]*)-[0-9]*\\)");
		// Pattern amod = Pattern
		// .compile("(?:[a-z]*)\\(([^-()]*)-[0-9]*,([^-()]*)-[0-9]*\\)");
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
		// 从句法树找名词
		// Pattern NP = Pattern
		// .compile("\\(NP\\s\\(NP\\s\\([A-Z]*\\s[^()]*\\)\\s\\(NN[A-Z]*\\s[^()]*\\)\\)\\s\\(PP\\s\\(IN\\s[^()]*\\)\\s\\(NP\\s\\(NNP\\s[^()]*|(?:\\(NN[A-Z]*\\s[^()]*\\)\\s?|\\(CC\\s[^()]*\\)?)+");//
		// 多个NN加其他词组成的NP(只取连续NN部分)，或者其他单个词NN
		// Pattern NP = Pattern
		// .compile("\\(NN[A-Z]*\\s[^()]*\\)\\s?");//
		Pattern NP = Pattern
				.compile("((\\(NN[A-Z]*\\s[^()]*\\)\\s?|\\((JJ[A-Z]*|VBG)\\s[^()]*\\)\\s?)+((\\(VP\\s)|\\(NP\\s|\\)|\\s)*)*\\(NN[A-Z]*\\s[^()]*\\)\\s?");//
		// 多个NN加其他词组成的NP(只取连续NN部分)，或者其他单个词NN
		Matcher np_matcher = NP.matcher(penn);
		while (np_matcher.find()) {
			String tmp2, tmp1 = np_matcher.group(0);
			tmp2 = tmp1;
			tmp1 = tmp1
					.replaceAll(
							"\\(JJ[SR]\\s[^()]*\\)|\\(DT\\s[^()]*\\)|\\([A-Z]*\\s|[^A-Za-z0-9\\s-./']",
							"")
					// .replaceAll("\\(NN[A-Z]?\\s|\\(NP\\s|\\)|[^A-Za-z\\s]",
					// "")
					.trim();
			tmp1 = tmp1.replace("-RRB-", "").trim();
			// System.out.println("->"+tmp1);
			// 拆成词，从而判断是否有依存关系
			String[] tmp = tmp1.split("\\s+");
			for (String s : tmp) {
				if (hm.contains(s)) {
					FeatureMapfromRegx.put(tmp1, tmp2);
					break;
				}
			}
		}
		// 形容词和动词的正则
		Pattern VPADJPattern = Pattern
				.compile("\\((VB[A-Z]?|JJ[A-Z]?)\\s[^()]*\\)");
		Matcher VPmatcher = VPADJPattern.matcher(penn);
		while (VPmatcher.find()) {
			String tmp2, tmp = VPmatcher.group(0);
			tmp2 = tmp;
			tmp = tmp.replaceAll(
					"\\(VB[A-Z]?\\s|\\(JJ[A-Z]?\\s|\\)|[^A-Za-z\\s-./']", "")
					.trim();
			OpinionMap.put(tmp, tmp2);
		}
	}

	public HashMap<String, String> getAllSentences() {
		HashMap<String, String> sentences = new HashMap<String, String>();
		try {
			ResultSet rs = SQLHelper.getResultSet(Corpus_sql);
			String rsid, text;
			while (rs.next()) {
				rsid = rs.getString(1);
				text = rs.getString(2);
				sentences.put(rsid, text);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sentences;
	}

	/***
	 * merge components by their lexical similarity
	 * 
	 * @param k
	 *            the top k components which will be merged
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public HashMap<Integer, ArrayList<String>> mergeComponents(
			ArrayList<String> FList, int k) throws IOException {
		ArrayList<Integer> ClusterIDList = this.connectPhrases(FList);
		HashMap<Integer, ArrayList<String>> Cluster = (HashMap<Integer, ArrayList<String>>) FileVsObject
				.readObject("data/" + dataset + "/Cluster");
		if (Cluster != null) {
			return Cluster;
		}
		Cluster = new HashMap<Integer, ArrayList<String>>();
		for (int i = 0; i < FList.size(); i++) {
			if (ClusterIDList.get(i) != -1) {
				// log.info(ClusterIDList.get(i) + ":" +
				// FeatureList.get(i));
				MapUtil.AddToListMap(Cluster, ClusterIDList.get(i),
						FList.get(i));
			}
		}
		// double[][] similarity = new
		// double[Cluster.size()][Cluster.size()];
		HashMap<String, Double> similarity = new HashMap<String, Double>();
		ps = new PhraseSimilarity();
		ArrayList<Integer> key = new ArrayList<Integer>();
		key.addAll(Cluster.keySet());
		for (int i = 0; i < key.size() - 1; i++) {
			for (int j = i + 1; j < key.size(); j++) {
				similarity.put(
						key.get(i) + ":" + key.get(j),
						AvgPhraseSim(ps, Cluster.get(key.get(i)),
								Cluster.get(key.get(j))));
			}
		}
		List<Entry<?, Double>> order = MapUtil.MapSort(similarity);
		// merge top K,maybe there are more than two group should merge ,
		// 12:201
		// 17:207
		HashMap<Integer, Integer> mgroup = new HashMap<Integer, Integer>();// OLd_clusterID->New_ClusterID,
		// set j's clusterID with i's value in pair(i,j), because i<j and we
		// use the smaller index as clusterID
		for (int m = 0; m < k; m++) {
			String pair = (String) order.get(m).getKey();
			String[] pairs = pair.split(":");
			int i = Integer.parseInt(pairs[0]);
			int j = Integer.parseInt(pairs[1]);
			if (mgroup.containsKey(i))
				mgroup.put(j, mgroup.get(i));
			else if (mgroup.containsKey(j)) {// as the j cluster has deleted
												// and
												// member add to
												// cluster:mgroup.get(j)
				mgroup.put(i, mgroup.get(j));
			} else
				mgroup.put(j, i);
		}
		for (Integer in : mgroup.keySet()) {
			for (int c = 0; c < ClusterIDList.size(); c++) {
				if (ClusterIDList.get(c) == in)
					ClusterIDList.set(c, mgroup.get(in));
			}
			for (String s : Cluster.get(in))
				MapUtil.AddToListMap(Cluster, mgroup.get(in), s);
			Cluster.remove(in);
		}
		FileVsObject.writeObject("data/" + dataset + "/Cluster", Cluster);
		return Cluster;
	}

	/**
	 * 基于观点关系的过滤
	 * 
	 * @param CF
	 *            候选特征词集合
	 * @param CO
	 *            候选观点词集合
	 * @param S
	 *            已知特征种子集合
	 */
	@SuppressWarnings("unchecked")
	public void ORFilter(ArrayList<String> CF, ArrayList<String> CO,
			ArrayList<String> s, double th) {
		double ffth, foth, ooth;
		ffth = foth = ooth = th;
		ArrayList<String> F = new ArrayList<String>();// 特征词集合
		ArrayList<String> O = new ArrayList<String>();// 观点词集合
		ArrayList<String> CF_tmp = (ArrayList<String>) CF.clone();// 用于临时保存候选列表，方便遍历时直接删除
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
			double ass;// use to save association value in every iteration

			// 先处理特征词
			for (String f : F) {
				// 处理特征候选
				for (String cf : CF) {
					if ((ass = association(f, cf, "ff")) > ffth
							&& !F_tmp.contains(cf)) {
						MapUtil.AddToListMap(ff_association, f, cf + ":" + ass);
						F_tmp.add(cf);// 不能对原始的F操作
						CF_tmp.remove(cf);// 不能对原始的CF操作
						flag = true;
					}
				}
				// 处理观点候选
				for (String co : CO) {
					if ((ass = association(f, co, "fo")) > foth
							&& !O_tmp.contains(co)) {
						MapUtil.AddToListMap(fo_association, f, co + ":" + ass);
						O_tmp.add(co);
						CO_tmp.remove(co);
						flag = true;
					}
				}
			}
			// 再处理观点词
			O = (ArrayList<String>) O_tmp.clone();
			for (String o : O) {
				// 处理观点候选
				for (String co : CO) {
					if ((ass = association(o, co, "oo")) > ooth
							&& !O_tmp.contains(co)) {
						MapUtil.AddToListMap(oo_association, o, co + ":" + ass);
						O_tmp.add(co);
						CO_tmp.remove(co);
						flag = true;
					}
				}
				// 处理特征候选
				for (String cf : CF) {
					if ((ass = association(o, cf, "of")) > foth
							&& !F_tmp.contains(cf)) {
						MapUtil.AddToListMap(of_association, o, cf + ":" + ass);
						F_tmp.add(cf);
						CF_tmp.remove(cf);
						flag = true;
					}
				}
			}

			// // 监控迭代时集合的变化情况***************************
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
			// //监控结束******************************************
		} while (flag);

		F_tmp.clear();
		for (String f : F) {
			// System.out.println(f);
			String fmap = Inflector.getInstance().singularize(f);// 复数形式还原
			if (!F_tmp.contains(fmap))
				F_tmp.add(fmap);
		}
		O_tmp.clear();
		for (String o : O) {
			// System.out.println(o);
			// String omap = OpinionMap.get(o);
			O_tmp.add(o);
		}
		try {
			Feature = F_tmp;
			Opinion = O_tmp;
			FileVsObject.writeText("data/" + dataset + "/Feature.txt", Feature);
			FileVsObject.writeText("data/" + dataset + "/Opinion.txt", Opinion);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/***
	 * select top k components as leader of groups, and others as unlabeled data
	 * so the unlabeled cluster's ID should be set as -1 !!! and labeled
	 * cluster's ID should rerange from 1 to k
	 * 
	 * @param input
	 * @param ClusterIDList
	 * @param Cluster
	 * @param k
	 *            select k instance as labeled
	 */
	@SuppressWarnings("unchecked")
	public void selectLabeledData(ArrayList<String> input,
			ArrayList<Integer> ClusterIDList,
			HashMap<Integer, ArrayList<String>> Cluster, int k) {
		HashMap<Integer, Integer> leader = new HashMap<Integer, Integer>();
		for (Integer i : Cluster.keySet()) {
			leader.put(i, Cluster.get(i).size());
		}
		List<Entry<?, Integer>> order = MapUtil.MapSortInt(leader);
		HashMap<Integer, ArrayList<String>> Labeled = new HashMap<Integer, ArrayList<String>>();
		HashMap<Integer, ArrayList<String>> UnLabeled = new HashMap<Integer, ArrayList<String>>();
		UnLabeled = (HashMap<Integer, ArrayList<String>>) Cluster.clone();
		for (int i = 0; i < k; i++) {
			int key = (Integer) order.get(i).getKey();
			Labeled.put(key, UnLabeled.get(key));
			UnLabeled.remove(key);
		}
		// set all clusterid as -1, then set labeled id
		for (int ii = 0; ii < ClusterIDList.size(); ii++) {
			ClusterIDList.set(ii, -1);
		}
		// rerange labeled clusterid
		ArrayList<Integer> clusterids = new ArrayList<Integer>(Labeled.keySet());
		Collections.sort(clusterids);
		int n = 0;// new clusterid start with 0
		for (Integer id : clusterids) {
			for (String feature : Labeled.get(id))
				ClusterIDList.set(input.indexOf(feature), n);
			n++;
		}
		// Cluster = Labeled;
	}

	/**
	 * 基于语义相似度过滤
	 * 
	 * @param CF
	 *            候选特征词集合
	 * @param CO
	 *            候选观点词集合
	 * @param S
	 *            已知特征种子集合
	 */
	@SuppressWarnings("unchecked")
	public void SRFilter(ArrayList<String> CF, ArrayList<String> CO,
			ArrayList<String> s, double th) {
		double ffth, foth, ooth;
		ffth = foth = ooth = th;
		ArrayList<String> F = new ArrayList<String>();// 特征词集合
		ArrayList<String> O = new ArrayList<String>();// 观点词集合
		ArrayList<String> CF_tmp = (ArrayList<String>) CF.clone();// 用于临时保存候选列表，方便遍历时直接删除
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

			// 先处理特征词
			for (String f : F) {
				// 处理特征候选
				for (String cf : CF) {
					if (association(f, cf, "ff") > ffth && !F_tmp.contains(cf)) {
						F_tmp.add(cf);// 不能对原始的F操作
						CF_tmp.remove(cf);// 不能对原始的CF操作
						flag = true;
					}
				}
				// 处理观点候选
				for (String co : CO) {
					if (association(f, co, "fo") > foth && !O_tmp.contains(co)) {
						O_tmp.add(co);
						CO_tmp.remove(co);
						flag = true;
					}
				}
			}
			// 再处理观点词
			O = (ArrayList<String>) O_tmp.clone();
			for (String o : O) {
				// 处理观点候选
				for (String co : CO) {
					if (association(o, co, "oo") > ooth && !O_tmp.contains(co)) {
						O_tmp.add(co);
						CO_tmp.remove(co);
						flag = true;
					}
				}
				// 处理特征候选
				for (String cf : CF) {
					if (association(o, cf, "of") > foth && !F_tmp.contains(cf)) {
						F_tmp.add(cf);
						CF_tmp.remove(cf);
						flag = true;
					}
				}
			}

			// // 监控迭代时集合的变化情况***************************
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
			// //监控结束******************************************
		} while (flag);

		// 复数形式替换，用key对应的value
		F_tmp.clear();
		for (String f : F) {
			// System.out.println(f);
			String fmap = FMapcontainGold.get(f);
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
			FileVsObject.writeText("data/" + dataset + "/Feature.txt", Feature);
			FileVsObject.writeText("data/" + dataset + "/Opinion.txt", Opinion);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/***
	 * Statistic each feature located sentence rsid
	 * 
	 * @param rsid
	 * @param sents
	 */
	private void statsForEachSentence(String rsid, String sents) {
		// ArrayList<String> sentence =
		// tokenizer.tokenize(sents);//做分词到ArrayList后无法匹配短语
		String sentence = sents;
		ArrayList<String> al;
		// rsid = rsid.replaceAll(":[0-9]*", "");
		// 统计本句中包含的Feature词
		// FileHandler fileHandler = null;
		// try {
		// fileHandler = new FileHandler("data/myLogger.log");
		// } catch (SecurityException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// fileHandler.setFormatter(new SimpleFormatter());
		// log.addHandler(fileHandler);
		for (String f : FMapcontainGold.keySet()) {
			// log.info(f);
			if (sentence.contains(f)) {
				MapUtil.AddToListMap(FeatureCount, f, rsid);
			}
		}
		// 统计本句中包含的OPinion词
		for (String o : OpinionMap.keySet()) {
			if (sentence.contains(o)) {
				MapUtil.AddToListMap(OpinionCount, o, rsid);
			}
		}
	}
}
