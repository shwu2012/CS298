import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
	// this would be the same as the number of CPUs
	private static final int POOL_SIZE = 8;

	private final Stopwatch stopwatch = Stopwatch.createUnstarted();
	private final ExecutorService pool;

	private final int k;
	private final ArrayList<DataPoint> dataSet;
	private final int numInstances;
	private final ArrayList<DataPoint> selectedDataSet;

	public KNN(int k, ArrayList<Integer> featureSelectionResult, ArrayList<DataPoint> dataSet) {
		this.k = k;
		this.dataSet = dataSet;
		this.numInstances = dataSet.size();
		this.selectedDataSet = createSelectedDataSet(featureSelectionResult);
		this.pool = Executors.newFixedThreadPool(POOL_SIZE);
	}

	private static List<Integer> randomlyPickNumbers(int start, int length, int n) {
		List<Integer> list = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			list.add(start + i);
		}
		Collections.shuffle(list);
		return list.subList(0, n);
	}

	private ArrayList<ArrayList<Double>> createDistanceMatrix(int numSampleInstances) {
		stopwatch.reset();
		ArrayList<ArrayList<Double>> distanceMatrix = new ArrayList<ArrayList<Double>>();

		// get sample instances
		List<Integer> sampleIndices = randomlyPickNumbers(0, numInstances, numSampleInstances);
		log.fine("sampleIndices: " + sampleIndices);

		for (int i = 0; i < numSampleInstances; i++) {
			stopwatch.start();
			int sampleInstanceIndex = sampleIndices.get(i);
			// pick a sample instance
			DataPoint sampleDataPoint = this.selectedDataSet.get(sampleInstanceIndex);
			// the distance between a sample instance to every other instances.
			ArrayList<Double> distances = new ArrayList<>(numInstances);
			Collection<Callable<Void>> distanceCalculationTasks = new LinkedList<Callable<Void>>();

			final int instancesGroupSize = 1000;
			for (int j = 0; j < numInstances; j += instancesGroupSize) {
				int groupSize = instancesGroupSize;
				if (j + instancesGroupSize > numInstances) {
					groupSize = numInstances - j;
				}
				for (int m = 0; m < groupSize; m++) {
					distances.add(-0.1);
				}
				distanceCalculationTasks.add(new CalcEuclideanDistanceTask(sampleDataPoint,
						selectedDataSet.subList(j, j + groupSize), distances, j, groupSize));
			}

			try {
				for (Future<Void> f : pool.invokeAll(distanceCalculationTasks)) {
					// do real distance calculation for each pair of points
					f.get();
				}
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}

			distanceMatrix.add(distances);
			stopwatch.stop();
			log.fine("filled distance for data point " + i + "/" + numSampleInstances
					+ " (to points) " + stopwatch);
		}
		log.info("createDistanceMatrix done." + stopwatch);
		return distanceMatrix;
	}

	private ArrayList<DataPoint> createSelectedDataSet(ArrayList<Integer> featureSelectionResult) {
		ArrayList<DataPoint> result = new ArrayList<>();
		int numFeatures = featureSelectionResult.size();
		for (int i = 0; i < numInstances; i++) {
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

	private double calcAccuracy(int numSampleInstances) {
		Preconditions.checkArgument(numSampleInstances <= numInstances,
				"Invalid number of samples: %s of out %s", numSampleInstances, numInstances);
		int numCorrectClassification = 0;
		// Calculate the distance matrix of numSampleInstances rows by
		// numInstance columns.
		ArrayList<ArrayList<Double>> distanceMatrix = createDistanceMatrix(numSampleInstances);
		Preconditions.checkArgument(distanceMatrix.size() == numSampleInstances);
		Preconditions.checkArgument(distanceMatrix.get(0).size() == numInstances);
		stopwatch.reset().start();
		ArrayList<IndexedValue<Double>> distancesWithIndex = null;
		for (int i = 0; i < numSampleInstances; i++) {
			// find the top k nearest neighbor of the i-th sample instance
			distancesWithIndex = makeIndexedValue(distanceMatrix.get(i));
			Collections.sort(distancesWithIndex, new DistanceComparator());
			// get the classes of top k nearest neighbors and put in a hashmap
			HashMap<String, Integer> hm = new HashMap<>();
			for (int j = 0; j < k; j++) {
				String className = dataSet.get(distancesWithIndex.get(j).getIndex()).getClassName();
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
		double accuracy = ((double) numCorrectClassification) / numSampleInstances;
		log.fine("accuracy = " + accuracy + ", using KNN on " + numSampleInstances
				+ " sample instances. " + stopwatch);
		return accuracy;
	}

	public double calcFitness(double alpha, double beta) {
		int numFeatures = this.dataSet.get(0).getFeatureValues().size();
		int numSelectedFeatures = this.selectedDataSet.get(0).getFeatureValues().size();
		int numSamples = numInstances / 10;
		return alpha * calcAccuracy(numSamples) + beta
				* (((double) (numFeatures - numSelectedFeatures)) / numFeatures);
	}

	private static ArrayList<IndexedValue<Double>> makeIndexedValue(ArrayList<Double> list) {
		ArrayList<IndexedValue<Double>> indexedDoubles = new ArrayList<>();
		IndexedValue<Double> iv = null;
		for (int i = 0; i < list.size(); i++) {
			iv = new IndexedValue<>();
			iv.setIndex(i);
			iv.setValue(list.get(i));
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
		private final DataPoint sampleInstance;
		private final List<DataPoint> instances;
		private final ArrayList<Double> result;
		private final int resultStartIndex;
		private final int resultLength;
		private final int numFeatures;

		public CalcEuclideanDistanceTask(DataPoint sampleInstance, List<DataPoint> instances,
				ArrayList<Double> result, int resultStartIndex, int resultLength) {
			Preconditions.checkArgument(!instances.isEmpty());
			Preconditions.checkArgument(sampleInstance.getFeatureValues().size() == instances
					.get(0).getFeatureValues().size(),
					"Euclidean distance must be applied on 2 points with same dimension.");
			Preconditions.checkArgument(resultStartIndex + resultLength <= result.size(),
					"Result is too small to have the index. start: %s, length: %s",
					resultStartIndex, resultLength);
			this.sampleInstance = sampleInstance;
			this.instances = instances;
			this.numFeatures = sampleInstance.getFeatureValues().size();
			this.result = result;
			this.resultStartIndex = resultStartIndex;
			this.resultLength = resultLength;
		}

		@Override
		public Void call() throws Exception {
			for (int i = 0; i < resultLength; i++) {
				result.set(
						resultStartIndex + i,
						calculateDistance(sampleInstance.getFeatureValues(), instances.get(i)
								.getFeatureValues()));
			}
			return null;
		}

		// actually it returns the square of the distance
		private double calculateDistance(ArrayList<Double> instance1, ArrayList<Double> instance2) {
			double sum = 0.0;
			for (int i = 0; i < numFeatures; i++) {
				sum += Math.pow(instance1.get(i) - instance2.get(i), 2);
			}
			return sum;
		}
	}
}
