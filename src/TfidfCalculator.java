import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.common.base.Preconditions;

public class TfidfCalculator {
	// the set of terms in each document
	private ArrayList<HashSet<String>> termSets = new ArrayList<HashSet<String>>();
	// the number of each terms in each document
	private ArrayList<HashMap<String, Integer>> termAndCounts = new ArrayList<HashMap<String, Integer>>();
	// the class of each document
	private ArrayList<String> classes = new ArrayList<String>();
	// the total numbers of terms in each document
	private ArrayList<Integer> termCounts = new ArrayList<Integer>();
	// the numbers of appearance in the corpus of each term
	private HashMap<String, Integer> numAppearance = new HashMap<String, Integer>();
	// the data structure for idf (inverse document frequency) of each term
	private HashMap<String, Double> invDocFreq = new HashMap<String, Double>();
	// the data structure for tfidf (term frequency * inverse document
	// frequency) of each document
	private ArrayList<HashMap<String, Double>> tfidfs = new ArrayList<HashMap<String, Double>>();
	// the term and index mapping
	private ArrayList<String> termIndexMapping = new ArrayList<String>();

	public void calculate(final String inputFilePath, final String outputFilePath,
			final boolean isNormalizeVector) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(inputFilePath));
			String line = null;
			HashSet<String> termSet = null;
			HashMap<String, Integer> termAndCount = null;
			while ((line = in.readLine()) != null) {
				String[] terms = line.split("\\s");
				termSet = new HashSet<String>();
				termAndCount = new HashMap<String, Integer>();
				classes.add(terms[0]);
				// get rid of the first word because it is the "class"
				for (int i = 1; i < terms.length; i++) {
					String term = terms[i];
					// do corpus statistics
					termSet.add(term);
					// do document statistics
					if (termAndCount.containsKey(term)) {
						termAndCount.put(term, termAndCount.get(term) + 1);
					} else {
						termAndCount.put(term, 1);
					}
				}
				termSets.add(termSet);
				termAndCounts.add(termAndCount);
				termCounts.add(terms.length - 1);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		System.out.println("file reading finished");

		// calculate numAppearance and wordIndexMapping
		for (HashSet<String> ts : termSets) {
			for (String term : ts) {
				if (numAppearance.containsKey(term)) {
					numAppearance.put(term, numAppearance.get(term) + 1);
				} else {
					numAppearance.put(term, 1);
					termIndexMapping.add(term);
				}
			}
		}
		System.out.println("numAppearance: " + numAppearance);

		Preconditions.checkState(termAndCounts.size() == termCounts.size());
		Preconditions.checkState(termAndCounts.size() == classes.size());
		int numDoc = termAndCounts.size();

		// Calculate idf of each term
		for (Map.Entry<String, Integer> entry : numAppearance.entrySet()) {
			invDocFreq.put(entry.getKey(), Math.log10(1.0 * numDoc / entry.getValue()));
		}

		// Calculate tfidf of each term in each document
		HashMap<String, Double> tfidf = null;
		for (int i = 0; i < numDoc; i++) {
			int wc = termCounts.get(i);
			tfidf = new HashMap<String, Double>();
			String term = null;
			for (Map.Entry<String, Integer> entry : termAndCounts.get(i).entrySet()) {
				term = entry.getKey();
				tfidf.put(term, (1.0 * entry.getValue() / wc) * invDocFreq.get(term));
			}
			tfidfs.add(tfidf);
		}

		Preconditions.checkState(tfidfs.size() == termAndCounts.size());
		System.out.println("numDoc: " + numDoc);

		// generate the vector space model file
		PrintWriter out = null;
		try {
			out = new PrintWriter(outputFilePath);

			// print out the vector space model
			for (int i = 0; i < numDoc; i++) {
				ArrayList<Double> featureValues = new ArrayList<Double>();
				for (int j = 0; j < termIndexMapping.size(); j++) {
					String term = termIndexMapping.get(j);
					HashMap<String, Double> temp = tfidfs.get(i);
					if (temp.containsKey(term)) {
						featureValues.add(temp.get(term).doubleValue());
					} else {
						featureValues.add(0.0);
					}
				}

				if (isNormalizeVector) {
					normalizeVector(featureValues);
				}

				out.println(toTextLine(new DataPoint(classes.get(i), featureValues)));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			out.flush();
			out.close();
		}
	}

	private static String toTextLine(DataPoint instance) {
		StringBuilder sb = new StringBuilder();
		sb.append(instance.getClassName());
		for (double value : instance.getFeatureValues()) {
			sb.append(",");
			sb.append(value);
		}
		return sb.toString();
	}

	private static void normalizeVector(ArrayList<Double> values) {
		double length = 0.0;
		for (double value : values) {
			length += value * value;
		}
		length = Math.sqrt(length);
		for (int i = 0; i < values.size(); i++) {
			values.set(i, values.get(i) / length);
		}
	}

	public static void main(String[] args) {
		TfidfCalculator tc = new TfidfCalculator();
		boolean isNormalizeVector = false;
		if ((args.length == 2) || (args.length == 3 && args[2].equals("-n"))) {
			isNormalizeVector = args.length == 3;
		} else {
			System.err.printf("usage: %s inputFilePath, outputFilePath [-n]\n",
					TfidfCalculator.class.getName());
			System.err.printf("options:\n");
			System.err
					.printf("\t-n: normalize vectors to unit-vectors (i.e. the length is 1.0).\n");
			System.exit(1);
		}
		String inputFilePath = args[0];
		String outputFilePath = args[1];
		if (isNormalizeVector) {
			outputFilePath += "_normalized";
		}
		System.out.printf("inputFilePath=%s, outputFilePath=%s, isNormalizeVector=%s\n",
				inputFilePath, outputFilePath, isNormalizeVector);
		tc.calculate(inputFilePath, outputFilePath, isNormalizeVector);
	}
}
