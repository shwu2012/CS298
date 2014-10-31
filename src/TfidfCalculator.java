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

	public void calculate(String inputFilePath, String outputFilePath) {
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
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
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
			StringBuilder sb = null;
			String line = null;

			// print out the terms
			/*
			sb = new StringBuilder();
			sb.append("class_unique");
			sb.append(",");
			for (int i = 0; i < termIndexMapping.size(); i++) {
				sb.append(termIndexMapping.get(i));
				sb.append(",");
			}
			sb.setLength(sb.length() - 1);
			line = sb.toString();
			out.println(line);
			*/


			// print out the vector space model
			for (int i = 0; i < numDoc; i++) {
				sb = new StringBuilder();
				sb.append(classes.get(i));
				sb.append(",");
				String term = null;
				for (int j = 0; j < termIndexMapping.size(); j++) {
					term = termIndexMapping.get(j);
					HashMap<String, Double> temp = tfidfs.get(i);
					if (temp.containsKey(term)) {
						sb.append(temp.get(term));
					} else {
						sb.append(0);
					}
					sb.append(",");
				}
				sb.setLength(sb.length() - 1);
				line = sb.toString();
				out.println(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			out.flush();
			out.close();
		}
	}

	public static void main(String[] args) {
		TfidfCalculator tc = new TfidfCalculator();
		tc.calculate(args[0], args[1]);
	}

}
