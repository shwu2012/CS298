import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.google.common.base.Preconditions;

public class FeatureSelectedDocGenerator {
	public FeatureSelectedDocGenerator(String inputFilePath, String outputFilePath, ArrayList<Integer> selectedFeatures){
		BufferedReader in = null;
		PrintWriter out = null;
		
		try {
			in = new BufferedReader(new FileReader(inputFilePath));
			out = new PrintWriter(outputFilePath);
			StringBuilder sb = null;	
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] terms = line.split(",");
				Preconditions.checkState(terms.length == selectedFeatures.size() + 1);
				sb = new StringBuilder();
				sb.append(terms[0]);				
				for(int i = 0; i < selectedFeatures.size(); i++){
					if(selectedFeatures.get(i) == 1){
						sb.append(",");
						sb.append(terms[i+1]);	
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
		
	public static void main(String[] args){
		ArrayList<Integer> al = new ArrayList<>();
		al.add(0);
		al.add(1);
		al.add(0);
		al.add(1);
		al.add(0);
		al.add(1);
		al.add(1);
		al.add(1);
		new FeatureSelectedDocGenerator(args[0], args[1], al);
	}
}
