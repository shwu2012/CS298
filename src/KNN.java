import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

public class KNN {
	private static final Logger log = Logger.getLogger(KNN.class.getName());
	private static final int POOL_SIZE = 4;

	private final Stopwatch stopwatch = Stopwatch.createUnstarted();
	private final ExecutorService pool;

	private final int k;
	private final ArrayList<DataPoint> dataSet;
	private final int numInstance;
	private final ArrayList<DataPoint> selectedDataSet;

	public KNN(int k, ArrayList<Integer> featureSelectionResult, ArrayList<DataPoint> dataSet) {
		this.k = k;
		this.dataSet = dataSet;
		this.numInstance = dataSet.size();
		this.selectedDataSet = createSelectedDataSet(featureSelectionResult);
		this.pool = Executors.newFixedThreadPool(POOL_SIZE);
	}

	private ArrayList<ArrayList<Double>> createDistanceMatrix() {
		stopwatch.reset();
		ArrayList<ArrayList<Double>> distanceMatrix = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> featureValues = null;
		ArrayList<Double> featureValuesInnerLoop = null;
		// the distance between a certain instance to every other instances.
		ArrayList<Double> distances = null;
		double distance = -1.0;
		for (int i = 0; i < numInstance; i++) {
			stopwatch.start();
			featureValues = this.selectedDataSet.get(i).getFeatureValues();
			distances = new ArrayList<Double>(numInstance);
			Collection<Callable<Void>> distanceCalculationTasks = new LinkedList<Callable<Void>>();
			for (int j = 0; j < numInstance; j++) {
				distances.add(-1.0);
				if (i < j) {
					featureValuesInnerLoop = this.selectedDataSet.get(j).getFeatureValues();
					distanceCalculationTasks.add(new CalcEuclideanDistanceTask(featureValues,
							featureValuesInnerLoop, distances, j));
				}
			}

			try {
				for (Future<Void> f : pool.invokeAll(distanceCalculationTasks)) {
					// do real distance calculation for each pair of points
					// (where i < j)
					f.get();
				}
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}

			for (int j = 0; j < numInstance; j++) {
				if (i < j) {
					Preconditions.checkState(distances.get(j) >= 0.0,
							"Those distances should be calculated if i < j");
				} else if (i == j) {
					distances.set(j, 0.0);
				} else {
					// i > j, can do this since the matrix is diagonally
					// symmetric
					distance = distanceMatrix.get(j).get(i);
					distances.set(j, distance);
				}
			}
			distanceMatrix.add(distances);
			stopwatch.stop();
			log.fine("filled distance for data point " + i + "/" + numInstance
					+ " (to all other data points) " + stopwatch);
		}
		log.info("createDistanceMatrix done." + stopwatch);
		return distanceMatrix;
	}

	private ArrayList<DataPoint> createSelectedDataSet(ArrayList<Integer> featureSelectionResult) {
		ArrayList<DataPoint> result = new ArrayList<>();
		int numFeatures = featureSelectionResult.size();
		for (int i = 0; i < numInstance; i++) {
			DataPoint dataSet = new DataPoint();
			dataSet.setClassName(this.dataSet.get(i).getClassName());
			ArrayList<Double> selectedFeatureValues = new ArrayList<>();
			for (int j = 0; j < numFeatures; j++) {
				if (featureSelectionResult.get(j) == 1) {
					selectedFeatureValues.add(this.dataSet.get(i).getFeatureValues().get(j));
				}
			}
			dataSet.setFeatureValues(selectedFeatureValues);
			result.add(dataSet);
		}
		return result;
	}

	private double calcAccuracy() {
		int numCorrectClassification = 0;
		ArrayList<ArrayList<Double>> distanceMatrix = createDistanceMatrix();
		stopwatch.reset().start();
		ArrayList<IndexedValue<Double>> indexedDoubles = null;
		for (int i = 0; i < numInstance; i++) {
			// find the top k nearest neighbor of the i-th instance
			indexedDoubles = makeIndexedValue(distanceMatrix.get(i));
			Collections.sort(indexedDoubles, new DistanceComparator());
			// get the classes of top k nearest neighbors and put in a hashmap
			HashMap<String, Integer> hm = new HashMap<>();
			for (int j = 0; j < k; j++) {
				String className = dataSet.get(indexedDoubles.get(j).getIndex()).getClassName();
				if (hm.containsKey(className)) {
					hm.put(className, hm.get(className) + 1);
				} else {
					hm.put(className, 1);
				}
			}
			// find out the most dominant class
			String dominantClass = Collections.max(hm.entrySet(), new HashMapComparator()).getKey();
			// check whether the classified class is the same as the true class
			if (dataSet.get(i).getClassName().equals(dominantClass)) {
				numCorrectClassification++;
			}
		}
		stopwatch.stop();
		log.fine("calcAccuracy done using KNN on " + numInstance + " instances. " + stopwatch);
		return ((double) numCorrectClassification) / numInstance;
	}

	public double calcFitness(double alpha, double beta) {
		int numFeatures = this.dataSet.get(0).getFeatureValues().size();
		int numSelectedFeatures = this.selectedDataSet.get(0).getFeatureValues().size();
		return alpha * calcAccuracy() + beta
				* (((double) (numFeatures - numSelectedFeatures)) / numFeatures);
	}

	private ArrayList<IndexedValue<Double>> makeIndexedValue(ArrayList<Double> featureValues) {
		ArrayList<IndexedValue<Double>> indexedDoubles = new ArrayList<>();
		IndexedValue<Double> iv = null;
		for (int i = 0; i < featureValues.size(); i++) {
			iv = new IndexedValue<>();
			iv.setIndex(i);
			iv.setValue(featureValues.get(i));
			indexedDoubles.add(iv);
		}
		return indexedDoubles;
	}

	private static class DistanceComparator implements Comparator<IndexedValue<Double>> {

		@Override
		public int compare(IndexedValue<Double> arg0, IndexedValue<Double> arg1) {
			return Double.compare(arg0.getValue(), arg1.getValue());
		}

	}

	private static class HashMapComparator implements Comparator<Map.Entry<String, Integer>> {

		@Override
		public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
			return Integer.compare(o1.getValue(), o2.getValue());
		}

	}

	private static class CalcEuclideanDistanceTask implements Callable<Void> {
		private final ArrayList<Double> instance1;
		private final ArrayList<Double> instance2;
		private final ArrayList<Double> result;
		private final int resultIndex;
		private final int numFeatures;

		public CalcEuclideanDistanceTask(ArrayList<Double> instance1, ArrayList<Double> instance2,
				ArrayList<Double> result, int resultIndex) {
			this.instance1 = instance1;
			this.instance2 = instance2;
			Preconditions.checkArgument(instance1.size() == instance2.size(),
					"Euclidean distance must be applied on 2 points with same dimension.");
			this.numFeatures = instance1.size();
			Preconditions.checkArgument(resultIndex < result.size(),
					"Result is too small to have the index: " + resultIndex);
			this.result = result;
			this.resultIndex = resultIndex;
		}

		@Override
		public Void call() throws Exception {
			double sum = 0.0;
			for (int i = 0; i < numFeatures; i++) {
				sum += Math.pow(instance1.get(i) - instance2.get(i), 2);
			}
			result.set(resultIndex, Math.sqrt(sum));
			return null;
		}
	}
}

class IndexedValue<T> {
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
