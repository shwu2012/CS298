package temp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ArffTranslator {

	public ArffTranslator(String inputFilePath, String outputFilePath){
		BufferedReader in = null;
		PrintWriter out = null;
		
		try {
			in = new BufferedReader(new FileReader(inputFilePath));
			out = new PrintWriter(outputFilePath);
			StringBuilder sb = null;	
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] terms = line.split(",");
				int length = terms.length - 1;
				sb = new StringBuilder();
				sb.append(terms[length]);				
				for(int i = 0; i < length; i++){
					sb.append(",");
					sb.append(terms[i]);
					
				}
				out.println(sb.toString());
			}
		} catch (IOException x) {
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
		ArffTranslator at = new ArffTranslator(args[0], args[1]);
	}
}
