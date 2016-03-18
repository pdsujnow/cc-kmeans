package pdsujnow.absa;

import java.io.IOException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.types.InstanceList;
import cc.mallet.types.NormalizedDotProductMetric;
import cc.mallet.types.SparseVector;
import pdsu.xsf.algorithm.DocumentClusterer;
import pdsu.xsf.algorithm.Word2VEC;
import pdsu.xsf.text.*;

import pdsu.xsf.utils.FileVsObject;
import pdsu.xsf.utils.MapUtil;
import pdsu.xsf.utils.SQLHelper;
import pdsu.xsf.utils.ScriptExecuter;

/***
 * 
 * assumption2: using capacity constrains to clustering target
 * @author xsf
 * 
 */
public class targetClustering2 {

	public static final String[] contextMethods = { "All Sentences", "Window",
			"Sentence" };
	public static final String[] trainSets = { "TC-W2V", "+W2V", "+GLO" };
	public static final String[] clusteringMethods = { "L_EM", "WKmeans",
			"Kmeans", "AKmeans", "AWKmeans", "VEC", "TLC-KMeans" };
	// private double ffth = 0.2;
	// private double foth = 0.2;
	// private double ooth = 0.2;
	private String dataset;// = "semeval/15";
	private String gold_sql;
	private String candidate_sql;
	private String Corpus_sql;
	private int total;// 语料句子总数
	// 以上参数从配置中按行读取
	private double[][] dist;
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
	private HashMap<String, String> FMapcontainGold;
	private ArrayList<String> Feature = new ArrayList<String>();
	private ArrayList<String> Opinion = new ArrayList<String>();
	private double lambda = 0.95;// 0.95, .96 .97 .98 都可以
	private PhraseSimilarity ps;

	private Logger log;
	private int clusterNumber;
	private double propotion;
	private double h;

	public static final int CONTEXT_ALL_SENTENCES = 0;
	public static final int CONTEXT_WINDOW = 1;
	public static final int CONTEXT_SENTENCE = 2;
	public static final int TRAINSET_TEST = 0;
	public static final int TRAINSET_TEST_WORD2VEC = 1;
	public static final int TRAINSET_TEST_GLOVE = 2;
	public static final int CLUSTERING_L_EM = 0;
	public static final int CLUSTERING_W_KMEANS = 1;
	public static final int CLUSTERING_KMEANS = 2;
	public static final int CLUSTERING_AKMEANS = 3;
	public static final int CLUSTERING_AWKMEANS = 4;
	public static final int CLUSTERING_VEC = 5;
	public static final int CLUSTERING_CCKMeans = 6;
	public static final int DATA_DC1 = 0;
	public static final int DATA_DC1_MF = 1;
	public static final int DATA_DC2 = 2;
	public static final int DATA_DC2_MF = 3;
	public static final int DATA_DVD = 4;
	public static final int DATA_DVD_MF = 5;
	public static final int DATA_MP3 = 6;
	public static final int DATA_MP3_MF = 7;
	public static final int DATA_PHONE = 8;
	public static final int DATA_PHONE_MF = 9;
	public static final int DATA_SEMEVAL = 10;

	/***
	 * 
	 * @param init
	 *            是否重新初始化成员值
	 */
	@SuppressWarnings("unchecked")
	public targetClustering2(int dataID, boolean init) {
		log = Logger.getLogger("aboot");
		log.setLevel(Level.INFO);
		ArrayList<List<String>> configList = CsvProcesser
				.readConfig("data/data.ini");
		ArrayList<String> ini = (ArrayList<String>) configList.get(dataID);
		dataset = (String) ini.get(0);
		gold_sql = (String) ini.get(1);
		candidate_sql = (String) ini.get(2);
		Corpus_sql = (String) ini.get(3);
		total = Integer.parseInt(ini.get(4));
		clusterNumber = Integer.parseInt(ini.get(5));
		propotion = Double.parseDouble(ini.get(6));
		h = Double.parseDouble(ini.get(7));
		// propotion = 0.1 * propotion;

		this.createGold(init);
		// this.computeDistance(init, 1);
	}

	public static void main(String[] args) {
		// String dataset = ABoot.dataset;

		// **********************使用普通cosine 距离的DBSCAN开始处理***********************
		HashMap<Integer, ArrayList<Integer>> clusterCount = new HashMap<Integer, ArrayList<Integer>>();
		try {
			// aboot.computeDistance(1);
			// FeatureList = (ArrayList<String>) FileVsObject.readObject("data/"
			// + aboot.dataset + "/FeatureList");
			// for (int i = 0; i < 3; i++)
			// for (int j = 0; j < 2; j++)
			// for (int k = 0; k < 3; k++)
			// aboot.clustering(i, j, k, 11);

			for (int i = 4; i <= 10; i = i + 2) {
				targetClustering2 aboot = new targetClustering2(i, true);
				// for (int i = 5; i <= 100; i++) {
				// aboot.clusterNumber = i;
//				aboot.clustering(CONTEXT_WINDOW, TRAINSET_TEST,
//						CLUSTERING_L_EM, true, 0, aboot.clusterNumber);
//				aboot.clustering(CONTEXT_WINDOW, TRAINSET_TEST,
//						CLUSTERING_L_EM, true, 2, aboot.clusterNumber);
//				aboot.clustering(CONTEXT_WINDOW, TRAINSET_TEST,
//						CLUSTERING_L_EM, true, 10, aboot.clusterNumber);
//				aboot.clustering(CONTEXT_ALL_SENTENCES, TRAINSET_TEST,
//						CLUSTERING_KMEANS, true, 0, aboot.clusterNumber);
//				aboot.clustering(CONTEXT_ALL_SENTENCES, TRAINSET_TEST_GLOVE,
//						CLUSTERING_AWKMEANS, true, 0, aboot.clusterNumber);
				// aboot.clustering(CONTEXT_ALL_SENTENCES, TRAINSET_TEST_GLOVE,
				// CLUSTERING_VEC, true, 0, aboot.clusterNumber);
				aboot.clustering(CONTEXT_ALL_SENTENCES, TRAINSET_TEST_GLOVE,
						CLUSTERING_CCKMeans, true, 0, aboot.clusterNumber);

				// aboot.clustering(CONTEXT_ALL_SENTENCES, TRAINSET_TEST,
				// CLUSTERING_AWKMEANS, true, 0, aboot.clusterNumber);
				// aboot.clustering(CONTEXT_ALL_SENTENCES, TRAINSET_TEST,
				// CLUSTERING_W_KMEANS, true, 0, aboot.clusterNumber);
				// aboot.clustering(CONTEXT_ALL_SENTENCES, TRAINSET_TEST_GLOVE,
				// CLUSTERING_AWKMEANS, true, 0, aboot.clusterNumber);
				// aboot.clustering(CONTEXT_ALL_SENTENCES, TRAINSET_TEST_GLOVE,
				// CLUSTERING_W_KMEANS, true, 0, aboot.clusterNumber,capacity);
				// aboot.clustering(CONTEXT_ALL_SENTENCES,
				// TRAINSET_TEST_WORD2VEC,
				// CLUSTERING_AWKMEANS, true, 0, aboot.clusterNumber);
				// aboot.clustering(CONTEXT_ALL_SENTENCES,
				// TRAINSET_TEST_WORD2VEC,
				// CLUSTERING_W_KMEANS, true, 0, aboot.clusterNumber);

				// aboot.clustering(CONTEXT_ALL_SENTENCES, TRAINSET_TEST,
				// CLUSTERING_VEC, true, 0, aboot.clusterNumber);

				// aboot.Eval("spe");
			}

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
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/***
	 * 
	 * @param contextMethod
	 * @param trainSet
	 * @param clusteringMethod
	 * @param init
	 * @param mergeNum
	 * @param labeledNum
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void clustering(int contextMethod, int trainSet,
			int clusteringMethod, boolean init, int mergeNum, int labeledNum)
			throws IOException, InterruptedException {

		// loadVector("data/vocab.txt","data/wordVectors.txt");
		// get substring after "/" as data prefix, such as "dvd" for "CRD/dvd"
		String prefix = dataset.substring(dataset.indexOf("/") + 1,
				dataset.length());
		String method = // contextMethods[contextMethod] + "->" +
		trainSets[trainSet] + "->" + clusteringMethods[clusteringMethod];
		ArrayList<String> input = new ArrayList<String>();
		ArrayList<Integer> sentCountOfInstance = new ArrayList<Integer>();// 记录实例的体积
		int MaxInstance = 0;
		for (String f : gold.keySet()) {
			input.add(f);
			int size = gold.get(f).size();
			sentCountOfInstance.add(size);
			if (size > MaxInstance)
				MaxInstance = size;
		}
		// write gold file which need manual annotate the cluster ID
		FileVsObject.writeText("data/" + dataset + "/gold.cluster", input);
		HashMap<String, Double> MLink = new HashMap<String, Double>();// 必须连接约束
		ArrayList<Integer> ClusterIDList = this.connectPhrases(input, init,
				MLink);
		// if (clusteringMethod == CLUSTERING_L_EM) {
		HashMap<Integer, ArrayList<String>> Cluster = this.mergeComponents(
				input, mergeNum, init, ClusterIDList);
		// this method will change the ClusterIDList
		selectLabeledData(input, ClusterIDList, Cluster, labeledNum);
		// }

		Word2VEC vec = new Word2VEC();

		switch (trainSet) {
		case TRAINSET_TEST:
			vec.loadModel("data/CRD/crd.bin");
			break;
		case TRAINSET_TEST_WORD2VEC:
			vec.loadModel("data/CRD/" + prefix + "_ext_w2v");
			break;
		case TRAINSET_TEST_GLOVE:
			// vec.loadModelFromObject("data/CRD/" + prefix + "_ext_glove");
			vec.loadModelFromObject("data/CRD/comm_ext_glove");
			break;
		}

		switch (contextMethod) {
		case CONTEXT_ALL_SENTENCES:
			contextExtract(vec, ClusterIDList, input);
			break;
		case targetClustering2.CONTEXT_WINDOW:
			contextExtract(ClusterIDList, input, 15);
			break;
		case targetClustering2.CONTEXT_SENTENCE:
			contextExtractInSentenceLevel(ClusterIDList, input);
			break;
		// useWordVector(ClusterIDList,input);
		}
		// load gold standard
		ArrayList<String> instances = FileVsObject.readFileByLines("data/"
				+ dataset + "/gold.cluster.txt", 1);
		if (instances.size() == 0) {
			log.info("There isn't gold standard file!");
			System.exit(0);
		}
		HashMap<String, String> answer = new HashMap<String, String>();
		for (String in : instances) {
			String ins[] = in.split("\t");
			answer.put(ins[0], ins[1]);
		}
		// end

		String corpuspath = "data/" + dataset + "/InstanceList";
		// runLDA rl = new runLDA();
		// String wtpath = "data/" + dataset + "/word-topic";
		// String dispath = "data/" + dataset + "/distribution";
		// rl.run(corpuspath, wtpath, dispath);

		// filtering noise words from context based on LDA topic assign

		String regx = "((?:\\w+-* */*\\.*)*)\\t+(-?[0-9]+)\\t+(.*)";
		DocumentClusterer dc = new DocumentClusterer(answer, corpuspath,
				"data/" + dataset + "/clusterResult.data", sentCountOfInstance,
				regx);
		@SuppressWarnings("unchecked")
		HashMap<String, HashMap<String, Double>> contextWeight = (HashMap<String, HashMap<String, Double>>) FileVsObject
				.readObject("data/" + dataset + "/contextWeight");
		// for (int ii = 19; ii < 20; ii++) {

		// double propotion =0;
		int Capacity = 0;// 占位变量，在非TLCKMeans聚类方法中不起作用
		switch (clusteringMethod) {
		case CLUSTERING_KMEANS:
			dc.clustering(DocumentClusterer.KMEANS, clusterNumber, 1);
			Eval(method);
			break;
		case CLUSTERING_L_EM:
			dc.clustering(DocumentClusterer.EM, clusterNumber, 0);
			Eval(method);
			break;
		case CLUSTERING_AKMEANS:
			for (int i = 0; i <= 10; i++) {
				propotion = 0.1 * i;
				dc.clustering(DocumentClusterer.AUGEMENTEDKMEANS,
						clusterNumber, propotion);
				Eval(method + " /*" + i + "*/ ");
			}
			break;
		case CLUSTERING_W_KMEANS:
			dc.clusteringWithFeatureWeighting(vec, DocumentClusterer.KMEANS,
					clusterNumber, 1);
			Eval(method);
			break;
		case CLUSTERING_AWKMEANS:
			for (int i = 10; i <= 10; i++) {
				propotion = 0.1 * i;
				dc.clusteringWithFeatureWeighting(vec,
						DocumentClusterer.AUGEMENTEDKMEANS, clusterNumber,
						propotion);
				Eval(method + " /*" + i + "*/ ");
			}
			break;
		case CLUSTERING_VEC:
			for (int i = 10; i <= 10; i++) {
				propotion = 0.1 * i;
				String vectorFile = "data/" + dataset + "/FVector";
				createFeatureVectorFile(vec, vectorFile);
				dc.clusteringFromFeatureVector(vectorFile,
						DocumentClusterer.AUGEMENTEDKMEANS, clusterNumber,
						propotion);
				Eval(method + " /*" + i + "*/ ");
			}
			break;
		case CLUSTERING_CCKMeans:
			// 计算两个实例的连接强度,目前采用词相似度
			for (String pair : MLink.keySet()) {
				String[] inst = pair.split(",");
				double[] vec1 = vec.getPhraseVector(input.get(Integer
						.parseInt(inst[0])));
				double[] vec2 = vec.getPhraseVector(input.get(Integer
						.parseInt(inst[1])));
				// vec1 = vec.getPhraseVector("sound");
				// vec2 = vec.getPhraseVector("bright");
				double sim = 1 - new NormalizedDotProductMetric().distance(
						new SparseVector(vec1), new SparseVector(vec2));
				MLink.put(pair, sim);
			}

			MLink.clear();// 清空，不使用约束
			for (int j = 0; j <= 10; j++) {
				 h=j;
				for (int i = 1; i <= 20; i++) {
					Capacity = (int) (MaxInstance + MaxInstance * i * 0.2);
					// Capacity = (int) (MaxInstance + MaxInstance * propotion);
					dc.clusteringWithFeatureWeighting(vec, 5, clusterNumber,
							h * 0.1, Capacity, MLink, 0);
					Eval(method + "_W /* c:" + i + " t:" + h + " */ ");
				}
			}

			break;
		default:
			String vectorFile = "data/" + dataset + "/TargetSim";
			createTargetVecFile(vec, vectorFile, answer);
		}
	}

	private void createTargetVecFile(Word2VEC vec, String vectorFile,
			HashMap<String, String> answer) throws IOException {
		// TODO Auto-generated method stub
		String corpuspath = "data/" + dataset + "/InstanceList";
		String exclusives = "data/" + dataset + "/exclusives";
		String clusterResult = "data/" + dataset + "/clusterResult.data";
		ArrayList<String> InstanceList = FileVsObject.readFileByLines(
				corpuspath, 1);
		ArrayList<String> exclusiveList = FileVsObject.readFileByLines(
				exclusives, 1);
		HashMap<String, String> exclusiveMap = new HashMap<String, String>();
		for (String s : exclusiveList) {
			String[] e = s.split("\\t");
			exclusiveMap.put(e[0], e[1]);
		}
		ArrayList<String> FVector = new ArrayList<String>();
		ArrayList<String> result = new ArrayList<String>();
		int i = 0;
		for (String s : InstanceList) {
			i++;
			String[] info = s.split("\\t");
			result.add(info[0] + "\t" + answer.get(info[0]));
			double[] v = vec.getPhraseVector(info[0]);
			int j = 0;
			for (String t : InstanceList) {
				j++;
				String[] info2 = t.split("\\t");
				double[] v2 = vec.getPhraseVector(info2[0]);
				double sim = 1 - new NormalizedDotProductMetric().distance(
						new SparseVector(v), new SparseVector(v2));
				if (i != j && sim > 0) {
					StringBuffer sb = new StringBuffer();
					sb.append(i + "," + j + "," + sim);
					FVector.add(sb.toString());
				}
			}
		}
		FileVsObject.writeText(vectorFile, FVector);
		FileVsObject.writeText(clusterResult, result);
	}

	private void createFeatureVectorFile(Word2VEC vec, String vectorFile)
			throws IOException {
		// TODO Auto-generated method stub
		String corpuspath = "data/" + dataset + "/InstanceList";
		String exclusives = "data/" + dataset + "/exclusives";
		ArrayList<String> InstanceList = FileVsObject.readFileByLines(
				corpuspath, 1);
		ArrayList<String> exclusiveList = FileVsObject.readFileByLines(
				exclusives, 1);
		HashMap<String, String> exclusiveMap = new HashMap<String, String>();
		for (String s : exclusiveList) {
			String[] e = s.split("\\t");
			exclusiveMap.put(e[0], e[1]);
		}
		ArrayList<String> FVector = new ArrayList<String>();
		for (String s : InstanceList) {
			String[] info = s.split("\\t");
			double[] v = vec.getPhraseVector(info[0]);
			// String exclusive = exclusiveMap.get(info[0]);
			// if (exclusive != null) {
			// String[] excl = exclusive.split(",");
			// for (String e : excl) {
			// double[] wv1 = getPhraseVector(e, vec);
			// for (int i = 0; i < wv1.length; i++)
			// v[i] = v[i] - wv1[i];
			// }
			// }
			StringBuffer sb = new StringBuffer();
			sb.append(info[0] + "\t" + info[1] + "\t" + 0 + ":" + v[0]);
			for (int i = 1; i < 200; i++)
				sb.append(" " + i + ":" + v[i]);
			FVector.add(sb.toString());
		}
		FileVsObject.writeText(vectorFile, FVector);
	}

	public void Eval(String method) throws IOException, InterruptedException {
		String command = "C:\\Python27\\python.exe script/eval.py " + "data/"
				+ dataset + "/clusterResult.data";
		String evalResult = ScriptExecuter.execute(command);
		SimpleDateFormat df = new SimpleDateFormat("yy-M-d HH:mm:ss");
		FileVsObject.writeText("data/" + dataset + "/" + "evalResult.csv",
				method + " /* " + df.format(new java.util.Date()) + " */ ,"
						+ evalResult, true);
	}

	public double AvgPhraseSim(PhraseSimilarity ps, ArrayList<String> al1,
			ArrayList<String> al2) {
		double sim = 0;
		for (String s1 : al1)
			for (String s2 : al2)
				sim += ps.getSimilarity(s1, s2);
		return sim;
	}

	/***
	 * connecting any two phrase if they have sharing words in field
	 * 'FeatureList', save the result to field 'ClusterIDList' ClusterID use the
	 * smaller index between two phrase
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Integer> connectPhrases(ArrayList<String> FList,
			boolean init, HashMap<String, Double> MLink) throws IOException {
		ArrayList<Integer> ClusterIDList = (ArrayList<Integer>) FileVsObject
				.readObject("data/" + dataset + "/ClusterIDList");
		if (!init && ClusterIDList != null)
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
																// has not
																// assigned
								ClusterIDList.set(i, i);
								ClusterIDList.set(j, i);
							} else
								ClusterIDList.set(j, ClusterIDList.get(i));
							MLink.put(i + "," + j, 0d);
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
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void contextExtract(Word2VEC vec, ArrayList<Integer> ClusterIDList,
			ArrayList<String> FList) throws IOException {
		HashMap<String, String> sentences = getAllSentences();
		// save which Feature one sentence contains
		HashMap<String, ArrayList<String>> SentenceCount = new HashMap<String, ArrayList<String>>();
		for (String feature : FList) {
			ArrayList<String> al = gold.get(feature);
			for (String rsid : al)
				MapUtil.AddToListMap(SentenceCount, rsid, feature);
		}
		// extract context for each featrue
		HashMap<String, ArrayList<String>> exclusives = new HashMap<String, ArrayList<String>>();
		ArrayList<String> InstanceList = new ArrayList<String>();
		HashMap<String, HashMap<String, Double>> contextWeight = new HashMap<String, HashMap<String, Double>>();
		for (String feature : FList) {
			HashMap<String, Double> Weight = new HashMap<String, Double>();
			ArrayList<String> al = gold.get(feature);
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
					if (!feature.equals(f))
						MapUtil.AddToListMap(exclusives, feature, f);
				}
				sb2.append(text + " ");
				text = text.replace(" " + feature + " ",
						" " + feature.replace(" ", "_") + " ");
				contextWeighting(vec, feature, list, text, Weight);
			}
			InstanceList.add(sb2.toString());
			contextWeight.put(feature, Weight);
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
			FileVsObject.writeObject("data/" + dataset + "/contextWeight",
					contextWeight);
			FileVsObject.writeText("data/" + dataset + "/exclusives",
					exclusiveList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/***
	 * give vectors, a feature, other features in the same sentence and
	 * sentence(context) return each word's weight about the feature
	 * (feature'similarity - other feature'similarity)
	 * 
	 * @param vec
	 *            word presentation
	 * @param feature
	 *            target (aspect) words
	 * @param featureList
	 *            all features in the same sentence
	 * @param context
	 *            sentence string
	 * @param weight
	 *            word -> weight pairs map
	 * @throws IOException
	 */
	private void contextWeighting(Word2VEC vec, String feature,
			ArrayList<String> featureList, String context,
			HashMap<String, Double> weight) throws IOException {
		// TODO Auto-generated method stub
		double[] vec1 = vec.getPhraseVector(feature);
		ArrayList<double[]> exclusive = new ArrayList<double[]>();
		for (String s : featureList) {
			if (!s.equals(feature))
				exclusive.add(vec.getPhraseVector(s));
		}
		String[] instances = new String[1];
		instances[0] = context;

		// parsing context using mallet tools
		Pattern pa = Pattern.compile("(?:\\w+-*/*\\.*)*");
		Pipe instancePipe = new SerialPipes(new Pipe[] {
				new CharSequence2TokenSequence(pa),
				new TokenSequenceLowercase(),
				new TokenSequenceRemoveStopwords(),
				new TokenSequence2FeatureSequence(), });

		// Create an empty list of the training instances
		InstanceList ilist = new InstanceList(instancePipe);
		ilist.addThruPipe(new StringArrayIterator(instances));
		// ilist.addThruPipe(new CsvIterator(new StringReader(context), "(.*)",
		// 1, 0,
		// 0));
		String tmp = (String) ilist.getAlphabet().toString();
		// tmp =tmp + "\n" + feature;

		String[] contexts = tmp.split("\\s+");

		// end
		ArrayList<String> keywords = new ArrayList<String>();
		for (String s : feature.split("\\s+"))
			keywords.add(s);
		for (String c : contexts) {
			if (c.equals(feature.replace(" ", "_"))) {
				for (String w : c.split("_"))
					weight.put(w, (double) 1);
			} else {
				double[] tmp_vec = vec.getPhraseVector(c);
				double sim1 = 1 - new NormalizedDotProductMetric().distance(
						new SparseVector(vec1), new SparseVector(tmp_vec));
				double sim = sim1;
				for (double[] d : exclusive) {
					double tmp_sim = 1 - new NormalizedDotProductMetric()
							.distance(new SparseVector(d), new SparseVector(
									tmp_vec));
					sim += tmp_sim;
				}
				sim1 = sim1 / sim;
				if (weight.containsKey(c))
					weight.put(c, (sim1 + weight.get(c)) / 2);
				else
					weight.put(c, sim1);
			}
		}
	}

	public void contextReWeighting() {

	}

	public void contextExtractInSentenceLevel(ArrayList<Integer> ClusterIDList,
			ArrayList<String> FList) {
		HashMap<String, String> sentences = getAllSentences();
		// save which Feature one sentence contains
		HashMap<String, ArrayList<String>> SentenceCount = new HashMap<String, ArrayList<String>>();
		for (String feature : gold.keySet()) {
			ArrayList<String> al = gold.get(feature);
			for (String rsid : al)
				MapUtil.AddToListMap(SentenceCount, rsid, feature);
		}
		// extract context for each featrue
		HashMap<String, ArrayList<String>> exclusives = new HashMap<String, ArrayList<String>>();
		ArrayList<String> InstanceList = new ArrayList<String>();
		for (String feature : FList) {
			ArrayList<String> al = gold.get(feature);
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
		HashMap<String, ArrayList<String>> context = new HashMap<String, ArrayList<String>>();
		HashMap<String, String> sentences = getAllSentences();
		// save which Feature one sentence contains
		HashMap<String, ArrayList<String>> SentenceCount = new HashMap<String, ArrayList<String>>();
		for (String feature : gold.keySet()) {
			ArrayList<String> al = gold.get(feature);
			for (String rsid : al)
				MapUtil.AddToListMap(SentenceCount, rsid, feature);
		}
		// extract context for each featrue
		for (String feature : FList) {
			ArrayList<String> al = gold.get(feature);
			// System.err.println(feature);
			for (String rsid : al) {
				String text = sentences.get(rsid);
				text = text.replace(feature, " @@ ");// a placeholder for
														// current
														// feature
				// features list of the sentence which id is rsid
				ArrayList<String> list = SentenceCount.get(rsid);
				// remove stopwords and all features in sentence
				for (String f : list) {// ensure not replace the inner chars of
										// word
					text.replace(f + " ", "");
					text.replace(" " + f, "");
				}
				// if (feature.equals("support")){
				// System.err.println(text);
				// }
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
	public void createGold(boolean init) {
		gold = (HashMap<String, ArrayList<String>>) FileVsObject
				.readObject("data/" + dataset + "/gold");
		if (gold != null && !init)
			return;
		gold = new HashMap<String, ArrayList<String>>();
		ResultSet rs = SQLHelper.getResultSet(gold_sql);
		String rsid, target;
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
			ArrayList<String> FList, int k, boolean init,
			ArrayList<Integer> ClusterIDList) throws IOException {
		HashMap<Integer, ArrayList<String>> Cluster = (HashMap<Integer, ArrayList<String>>) FileVsObject
				.readObject("data/" + dataset + "/Cluster");
		if (!init && Cluster != null) {
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
		if (order.size() < k)
			k = order.size();// 如果合并后得到的群组数小于设定值，则以实际群组数为准
		for (int m = 0; m < k; m++) {
			String pair = (String) order.get(m).getKey();
			String[] pairs = pair.split(":");
			int i = Integer.parseInt(pairs[0]);
			int j = Integer.parseInt(pairs[1]);
			if (mgroup.containsKey(i)) {
				if (mgroup.containsKey(j))
					mgroup.put(mgroup.get(j), mgroup.get(i));
				mgroup.put(j, mgroup.get(i));
			} else if (mgroup.containsKey(j)) {// as the j cluster has deleted
												// and
												// member add to
												// cluster:mgroup.get(j)
				mgroup.put(i, mgroup.get(j));
			} else
				mgroup.put(j, i);
		}
		for (Integer in : mgroup.keySet()) {
			if (in == mgroup.get(in))
				continue;
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
			if (i < order.size()) {
				int key = (Integer) order.get(i).getKey();
				Labeled.put(key, UnLabeled.get(key));
				UnLabeled.remove(key);
			}
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
			for (String feature : Labeled.get(id)) {
				// System.out.println(feature);
				ClusterIDList.set(input.indexOf(feature), n);
			}
			n++;
		}
		// Cluster = Labeled;
	}

}
