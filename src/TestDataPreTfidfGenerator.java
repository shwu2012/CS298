import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

/*This generator keeps only the selected features in the raw test data by doing the following two steps:
 * 1. read the feature names of the training data with only selected features. (The file which is ready to put into weka to run the model)
 * 2. read the raw test file to keep only the selected features. (after step 2, this test file will be used in the tfidf generator to generate the test file to put into weka )*/
public class TestDataPreTfidfGenerator {
	public TestDataPreTfidfGenerator(String inputTrainingDataFilePath, String inputTestDataFilePath, String outputFilePath){
		BufferedReader in = null;
		PrintWriter out = null;
		
		HashMap<String, Integer> featureHashmap = new HashMap<>();
		
		try {
			StringBuilder sb = null;
			
			in = new BufferedReader(new FileReader(inputTrainingDataFilePath));
			out = new PrintWriter(outputFilePath);			
			// read the first line of training data with only selected features to get the feature names into the hashmap
			String featureNameLine = in.readLine();
			String[] featureNames = featureNameLine.split(",");
			// start from i = 1 because the first item is "class_unique"
			for(int i = 1; i < featureNames.length; i++){
				String feature = featureNames[i];
				if(featureHashmap.containsKey(feature)){
					System.out.println("duplicate feature name:" + feature);
				}else{
					featureHashmap.put(feature, 1);
				}			
			}
			
			in = new BufferedReader(new FileReader(inputTestDataFilePath));
			out = new PrintWriter(outputFilePath);
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] terms = line.split("\\s");
				sb = new StringBuilder();
				sb.append(terms[0]);
				sb.append("\t");
				for(int i = 1; i < terms.length; i++){
					if(featureHashmap.containsKey(terms[i])){
						sb.append(terms[i]);
						sb.append(" ");
					}
				}
				sb.setLength(sb.length()-1);
				out.println(sb.toString());
			}			
			
		} catch (IOException x) {
			x.printStackTrace();
			System.err.format("IOException: %s%n", x);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if(out != null){
					out.flush();
					out.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args){		
		TestDataPreTfidfGenerator generator = new TestDataPreTfidfGenerator(args[0], args[1], args[2]);		
	}
}
