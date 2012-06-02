package rssminer.classfier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.junit.Before;
import org.junit.Test;

import rssminer.search.Searcher;
import clojure.lang.ArraySeq;

public class NaiveBayesTest {

	static final File TRAIN = new File(
			"/home/feng/workspace/rssminer/test/20news-bydate-train");
	static final File TEST = new File(
			"/home/feng/workspace/rssminer/test/20news-bydate-test");
	Searcher searcher;
	Map<String, Map<String, Double>> model;

	String[] likes = new String[] { "comp.os.ms-windows.misc",
			"comp.sys.ibm.pc.hardware", "comp.windows.x", "misc.forsale",
			"rec.autos", "rec.motorcycles", "rec.sport.baseball",
			"rec.sport.hockey", "sci.crypt", "sci.electronics", "sci.med",
			"sci.space", "soc.religion.christian", "talk.politics.guns",
			"talk.politics.mideast", "talk.politics.misc", "talk.religion.misc" };
	String[] disLikes = new String[] { "talk.politics.misc", "sci.electronics",
			"comp.sys.mac.hardware", "comp.graphics" };

	private List<Integer> trainLikeIds = new ArrayList<Integer>();

	private List<Integer> trainDisLikeIds = new ArrayList<Integer>();
	private List<Integer> testLikeIds = new ArrayList<Integer>();

	private List<Integer> testDisLikeIds = new ArrayList<Integer>();

	final int MAX_PERCATEGORY = 5000;

	public void indexFile(File f, long id) throws FileNotFoundException,
			IOException {
		List<String> lines = IOUtils.readLines(new FileInputStream(f));
		String subject = null;
		boolean isContent = false;
		StringBuilder c = new StringBuilder();
		for (String line : lines) {
			if (line.startsWith("Subject:")) {
				subject = line.substring("Subject:".length());
			} else if (isContent == false && line.isEmpty()) {
				isContent = true;
			}

			if (isContent == true) {
				if (!line.startsWith(">")) {
					c.append(line);
				}
			}
		}
		searcher.index((int) id, 1, null, subject, c.toString(), null);
		// System.out.println(f);
		// System.out.println(subject);
		// System.out.println(c);

	}

	private boolean like(String folderName) {
		for (String dislike : disLikes) {
			if (dislike.equals(folderName)) {
				return false;
			}
		}
		return true;
	}

	@Before
	public void setup() throws FileNotFoundException, IOException {
		searcher = Searcher.initGlobalSearcher("RAM");
		int id = 0;
		File[] train = TRAIN.listFiles();
		for (File folder : train) {
			File[] subs = folder.listFiles();
			int count = 0;
			for (File f : subs) {
				++id;
				if (like(folder.getName())) {
					trainLikeIds.add(id);
				} else {
					trainDisLikeIds.add(id);
				}
				indexFile(f, id);
				if (++count > MAX_PERCATEGORY) {
					break;
				}
			}
		}

		File[] test = TEST.listFiles();
		for (File folder : test) {
			File[] subs = folder.listFiles();
			int count = 0;
			for (File f : subs) {
				++id;
				if (like(folder.getName())) {
					testLikeIds.add(id);
				} else {
					testDisLikeIds.add(id);
				}
				indexFile(f, id);
				if (++count > MAX_PERCATEGORY) {
					break;
				}
			}
		}
		System.out.println("train like: " + trainLikeIds.size()
				+ "\t dislike: " + trainDisLikeIds.size());
		model = NaiveBayes.train(trainLikeIds, trainDisLikeIds);

		printModelDetail(model);
	}

	private void printModelDetail(Map<String, Map<String, Double>> model2) {
		for (Entry<String, Map<String, Double>> e : model.entrySet()) {
			int size = e.getValue().size();
			if (size > 0) {
				int count = 0;
				Map<String, Double> sub = e.getValue();
				for (Entry<String, Double> s : sub.entrySet()) {
					if (s.getValue() > 1.0D) {
						count++;
					}
					// System.out.println(s);
				}
				System.out.printf(e.getKey() + ": " + size + " ");
				System.out.println(count + "\t" + count / (double) size);
			}
		}
	}

	private boolean debug = false;

	@Test
	public void testBayes() throws CorruptIndexException, IOException {
		int count = 0;
		double guard = (double) trainLikeIds.size()
				/ (double) trainDisLikeIds.size();

		// guard = 1.0D;
		// testDisLikeIds = trainDisLikeIds;
		// testLikeIds = trainLikeIds;

		for (Integer like : testLikeIds) {
			double classify = NaiveBayes.classify(model, ArraySeq.create(like))[0];
			if (classify > guard) {
				count++;
			} else if (debug) {
				System.out.println(classify);
			}
		}

		if (debug) {
			System.out.println("===========================");
		}

		int discount = 0;
		for (Integer dislike : testDisLikeIds) {
			double classify = NaiveBayes.classify(model,
					ArraySeq.create(dislike))[0];
			// System.out.println(classify);
			if (classify < guard) {
				discount++;
			} else if (debug) {
				System.out.println(classify);
			}
		}

		System.out.println("like:     " + count + "/" + testLikeIds.size()
				+ "\t" + (double) count / testLikeIds.size());
		System.out.println("dislike:  " + discount + "/"
				+ testDisLikeIds.size() + "\t" + (double) discount
				/ testDisLikeIds.size());
		// System.out.println(discount);
	}

}
