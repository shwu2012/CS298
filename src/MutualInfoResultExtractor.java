import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class MutualInfoResultExtractor {
	private ArrayList<Integer> selectedFeatureIndex = null;
	
	private MutualInfoResultExtractor(String inputFilePath){
		BufferedReader in = null;
		selectedFeatureIndex = new ArrayList<>();
		try {
			in = new BufferedReader(new FileReader(inputFilePath));
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] terms = line.trim().split("\\s+");
				selectedFeatureIndex.add(Integer.parseInt(terms[1])-1);
			}
			// Sort the Feature Index
			Collections.sort(selectedFeatureIndex);
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
	}
	
	public ArrayList<Integer> getSelectedFeatureIndex(){
		return selectedFeatureIndex;
	}
	
	private void outputPSODoc(String inputFilePath, String outputFilePath, boolean isNormalizeVector){
		BufferedReader in = null;
		PrintWriter out = null;		
		
		try {
			in = new BufferedReader(new FileReader(inputFilePath));
			out = new PrintWriter(outputFilePath);
			StringBuilder sb = null;
			String line = null;
			ArrayList<Double> featureValues = new ArrayList<>();
			// print out the names of selected features
			String[] featureNames = in.readLine().split(",");
			sb = new StringBuilder();
			sb.append(featureNames[0]);
			for(int i = 0; i < selectedFeatureIndex.size(); i++){
				sb.append(",");
				sb.append(featureNames[selectedFeatureIndex.get(i)]);
			}
			out.println(sb.toString());
			// print out the feature values considering whether they need to be normalized
			while ((line = in.readLine()) != null) {
				String[] terms = line.split(",");
				sb = new StringBuilder();			
				sb.append(terms[0]);
				int size = selectedFeatureIndex.size();				
				if(isNormalizeVector){
					for(int i = 0; i < size; i++){	
						featureValues.add(Double.parseDouble(terms[selectedFeatureIndex.get(i)]));		
					}
					normalizeVector(featureValues);
					for(int i = 0; i < size; i++){
						sb.append(",");
						sb.append(featureValues.get(i).doubleValue());
					}			
				}else{
					for(int i = 0; i < size; i++){
						sb.append(",");
						sb.append(terms[selectedFeatureIndex.get(i)]);
					}							
				}
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
	
	// args[0] is the doc excerpted from Weka's Information Gain feature selection output, see example doc mir.txt
	// args[1] is the doc generated from TfidfCalculator without normalization
	// args[2] is the output file name
	// args[4] is the argument indicated whether to do normalization or not
	public static void main(String[] args){
		MutualInfoResultExtractor mir = new MutualInfoResultExtractor(args[0]);
		boolean isNormalizeVector = args.length == 4;
		System.out.println(mir.getSelectedFeatureIndex());
		mir.outputPSODoc(args[1], args[2], isNormalizeVector);
	}
}
