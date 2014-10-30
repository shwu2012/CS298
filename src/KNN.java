import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class KNN {
	private int k;
	private ArrayList<DataSet> dataSets;
	private ArrayList<Integer> featureSelectionResult;
	private int NumInstance;
	private ArrayList<DataSet> selectedDataSets;
	
	public KNN(int k, ArrayList<Integer> featureSelectionResult, ArrayList<DataSet> dataSets){
		this.k = k;
		this.featureSelectionResult = featureSelectionResult;
		this.dataSets = dataSets;
		this.NumInstance = dataSets.size();
		this.selectedDataSets = createSelectedDataSets();
	}
	
	private ArrayList<ArrayList<Double>> createDistanceMatrix(){
		ArrayList<ArrayList<Double>> distanceMatrix = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> featureValues = null;
		ArrayList<Double> featureValuesInnerLoop = null;
		// the distance between a certain instance to every other instances.
		ArrayList<Double> distances = null;
		double distance;
		for(int i = 0; i < NumInstance; i++){
			featureValues = this.selectedDataSets.get(i).getFeatureValues();
			distances = new ArrayList<Double>();
			for(int j = 0; j < NumInstance; j++){
				if(i < j){
					featureValuesInnerLoop = this.selectedDataSets.get(j).getFeatureValues();
					distance = calcEuclideanDistance(featureValues, featureValuesInnerLoop);
					distances.add(distance);
//					System.out.printf("%.6f\t", distance);
				}else if (i == j){
					distances.add(0.0);
//					System.out.printf("%.6f\t", 0.0f);
				}else{
					// i > j, can do this since the matrix is diagonally symmetric
					distance = distanceMatrix.get(j).get(i);
					distances.add(distance);
//					System.out.printf("%.6f\t", distance);
				}
			}
			distanceMatrix.add(distances);
//			System.out.print("\n");
		}
		return distanceMatrix;
	}
	
	private double calcEuclideanDistance(ArrayList<Double> instance1, ArrayList<Double> instance2){
		assert instance1.size() == instance2.size();
		double sum = 0.0;
		for(int i = 0; i < instance1.size(); i++){
			sum += Math.pow(instance1.get(i) - instance2.get(i), 2);
		}
		return Math.sqrt(sum);
	}
	
	private ArrayList<DataSet> createSelectedDataSets(){
		ArrayList<DataSet> selectedDataSets = new ArrayList<>();
		int numFeatures = this.featureSelectionResult.size();
		for(int i = 0; i < NumInstance; i++){
			DataSet dataSet = new DataSet();
			dataSet.setClassName(this.dataSets.get(i).getClassName());
			ArrayList<Double> selectedFeatureValues = new ArrayList<>();
			for(int j = 0; j < numFeatures; j++){			
				if(this.featureSelectionResult.get(j) == 1){
					selectedFeatureValues.add(this.dataSets.get(i).getFeatureValues().get(j));
				}
			}
			dataSet.setFeatureValues(selectedFeatureValues);
			selectedDataSets.add(dataSet);
		}
		return selectedDataSets;
	}
	
	private double calcAccuracy(){
		int numCorrectClassification = 0;
		ArrayList<ArrayList<Double>> distanceMatrix = createDistanceMatrix();
		ArrayList<IndexedValue<Double>> indexedDoubles = null;
		for(int i = 0; i < NumInstance; i++){
			// find the top k nearest neighbor of the i-th instance
			indexedDoubles = makeIndexedValue(distanceMatrix.get(i));
			Collections.sort(indexedDoubles, new DistanceComparator());
			// get the classes of top k nearest neighbors and put in a hashmap
			HashMap<String, Integer> hm = new HashMap<>();
			for(int j = 0; j < k; j++){
				String className = dataSets.get(indexedDoubles.get(j).getIndex()).getClassName();
				if(hm.containsKey(className)){
					hm.put(className, hm.get(className)+1);
				}else{
					hm.put(className, 1);
				}
			}
			// find out the most dominant class
			String dominantClass = Collections.max(hm.entrySet(), new HashMapComparator()).getKey();
//			System.out.printf("The dominant Class of the %d round is %s.\n", i, dominantClass);
			// check whether the classified class is the same as the true class
			if(dataSets.get(i).getClassName().equals(dominantClass)){
				numCorrectClassification++;
//				System.out.println("numCorrectClassification = " + numCorrectClassification);
			}
		}
//		System.out.println("NumInstance = " + NumInstance);
		return ((double)numCorrectClassification)/NumInstance;
	}
	
	public double calcFitness(double alpha, double beta){
		int numFeatures = this.dataSets.get(0).getFeatureValues().size();
		int numSelectedFeatures = this.selectedDataSets.get(0).getFeatureValues().size();
		return alpha*calcAccuracy() + beta*(((double)(numFeatures - numSelectedFeatures))/numFeatures);
	}
	
	private ArrayList<IndexedValue<Double>> makeIndexedValue(ArrayList<Double> featureValues){
		ArrayList<IndexedValue<Double>> indexedDoubles = new ArrayList<>();
		IndexedValue<Double> iv = null;
		for(int i = 0; i < featureValues.size(); i++){
			iv = new IndexedValue<>();
			iv.setIndex(i);
			iv.setValue(featureValues.get(i));
			indexedDoubles.add(iv);
		}
		return indexedDoubles;
	}
	
	private static class DistanceComparator implements Comparator<IndexedValue<Double>>{

		@Override
		public int compare(IndexedValue<Double> arg0, IndexedValue<Double> arg1) {
			return Double.compare(arg0.getValue(), arg1.getValue());
		}
		
	}
	
	private static class HashMapComparator implements Comparator<Map.Entry<String, Integer>>{

		@Override
		public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
			return Integer.compare(o1.getValue(), o2.getValue());
		}
		
	}
	
}
	
class IndexedValue<T>{
	private int index;
	private T value;
	
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public T getValue() {
		return value;
	}
	public void setValue(T value) {
		this.value = value;
	}	
}