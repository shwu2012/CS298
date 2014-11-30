import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;

public class KNN {
	public enum SearchMethod {
		BRUTE_FORCE, KD_TREE
	}

	private static final Logger log = Logger.getLogger(KNN.class.getName());

	private final Stopwatch stopwatch = Stopwatch.createUnstarted();

	private final int numK;
	private final int numInstances;
	private final int numFeatures;
	private final ArrayList<DataPoint> selectedDataSet;
	private final int numSelectedFeatures;

	public KNN(int numK, ArrayList<Integer> featureSelectionResult, ArrayList<DataPoint> dataSet) {
		this.numK = numK;
		this.numInstances = dataSet.size();
		this.selectedDataSet = createSelectedDataSet(featureSelectionResult, dataSet);
		this.numFeatures = dataSet.get(0).getFeatureValues().size();
		this.numSelectedFeatures = this.selectedDataSet.get(0).getFeatureValues().size();
	}

	private static List<Integer> randomlyPickNumbers(int start, int length, int n) {
		List<Integer> list = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			list.add(start + i);
		}
		if (length == n) {
			return list;
		}
		Collections.shuffle(list);
		List<Integer> result = list.subList(0, n);
		Collections.sort(result);
		return result;
	}

	private ArrayList<ArrayList<Double>> createDistanceMatrix(int numSampleInstances) {
		stopwatch.reset();
		ArrayList<ArrayList<Double>> distanceMatrix = new ArrayList<ArrayList<Double>>();

		// Get sample instances; those indexes are sorted.
		List<Integer> sampleIndices = randomlyPickNumbers(0, numInstances, numSampleInstances);

		for (int i = 0; i < numSampleInstances; i++) {
			stopwatch.start();
			int sampleInstanceIndex = sampleIndices.get(i);
			// pick a sample instance
			DataPoint sampleDataPoint = this.selectedDataSet.get(sampleInstanceIndex);
			// the distance between a sample instance to every other instances.
			ArrayList<Double> distances = new ArrayList<>(numInstances);
			for (int j = 0; j < numInstances; j ++) {
				if (j < i) {
					distances.add(distanceMatrix.get(j).get(i));
				} else if (j == i) {
					distances.add(0.0);
				} else {
					distances.add(MathUtil.calculateDistance(sampleDataPoint.getFeatureValues(),
							this.selectedDataSet.get(j).getFeatureValues()));
				}
			}
			Preconditions.checkState(distances.size() == numInstances, "Invaild distances size: "
					+ distances.size());
			distanceMatrix.add(distances);
			stopwatch.stop();
			log.fine("filled distance for data point " + i + "/" + numSampleInstances
					+ " (to points) " + stopwatch);
		}
		log.info("createDistanceMatrix done." + stopwatch);
		return distanceMatrix;
	}

	private ArrayList<DataPoint> createSelectedDataSet(ArrayList<Integer> featureSelectionResult,
			ArrayList<DataPoint> originalDataSet) {
		ArrayList<DataPoint> result = new ArrayList<>();
		int numFeatures = featureSelectionResult.size();
		for (int i = 0; i < numInstances; i++) {
			DataPoint dataPoint = new DataPoint();
			dataPoint.setClassName(originalDataSet.get(i).getClassName());
			ArrayList<Double> selectedFeatureValues = new ArrayList<>();
			for (int j = 0; j < numFeatures; j++) {
				if (featureSelectionResult.get(j) == 1) {
					selectedFeatureValues.add(originalDataSet.get(i).getFeatureValues().get(j));
				}
			}
			dataPoint.setFeatureValues(selectedFeatureValues);
			result.add(dataPoint);
		}
		return result;
	}

	private double calcAccuracyWithKdTree() {
		DataSet dataSet = new DataSet(numSelectedFeatures);
		dataSet.addAllInstances(selectedDataSet);
		stopwatch.reset().start();
		KdTree tree = KdTree.build(dataSet, true);
		stopwatch.stop();
		log.info("KdTree was built. " + stopwatch);

		// Use leave-one-out method to verify.
		stopwatch.reset().start();
		int numCorrectClassification = 0;
		for (int i = 0; i < numInstances; i++) {
			final DataPoint searchPoint = dataSet.getInstance(i);
			DataPointSet nearestPoints = tree.findKNearestNodes(searchPoint, numK + 1);
			String dominantClass = findDominantClass(Sets.filter(nearestPoints.getDataPoints(),
					new Predicate<DataPoint>() {

						@Override
						public boolean apply(DataPoint input) {
							return !input.equalsIgnoringClassName(searchPoint);
						}
					}));
			// check whether the classified class is the same as the true class
			if (selectedDataSet.get(i).getClassName().equals(dominantClass)) {
				numCorrectClassification++;
			}
		}
		stopwatch.stop();
		log.info("Searched with KdTree. " + stopwatch);

		double accuracy = ((double) numCorrectClassification) / numInstances;
		log.info("accuracy = " + accuracy + ", using KNN on " + numInstances + " instances. "
				+ stopwatch);
		return accuracy;
	}

	private static String findDominantClass(Collection<DataPoint> points) {
		// TODO: use distance of each data point as tie-breaker.
		HashMap<String, Integer> classNameCounts = new HashMap<>();
		// Count each class name.
		for (DataPoint point : points) {
			String className = point.getClassName();
			if (classNameCounts.containsKey(className)) {
				classNameCounts.put(className, classNameCounts.get(className) + 1);
			} else {
				classNameCounts.put(className, 1);
			}
		}
		return Collections.max(classNameCounts.entrySet(), new HashMapComparator()).getKey();
	}

	private double calcAccuracy(int numSampleInstances) {
		if (numSampleInstances < 0) {
			numSampleInstances = numInstances;
		}
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
			// Pick first K data points; those data points have shortest
			// distances. Note that we should exclude the testing point itself!
			Set<DataPoint> nearestPoints = new HashSet<>();
			for (int j = 0; j < numK; j++) {
				nearestPoints.add(selectedDataSet.get(distancesWithIndex.get(j + 1).getIndex()));
			}
			String dominantClass = findDominantClass(nearestPoints);
			// check whether the classified class is the same as the true class
			if (selectedDataSet.get(i).getClassName().equals(dominantClass)) {
				numCorrectClassification++;
			}
		}
		stopwatch.stop();
		double accuracy = ((double) numCorrectClassification) / numSampleInstances;
		log.info("accuracy = " + accuracy + ", using KNN verified on " + numSampleInstances
				+ " sample instances. " + stopwatch);
		return accuracy;
	}

	public double calcFitness(double alpha, double beta, SearchMethod searchMethod,
			int samplingFolders) {
		int numSamples = -1;
		if (samplingFolders > 1) {
			numSamples = numInstances / samplingFolders;
		}
		double accuracy = (searchMethod == SearchMethod.KD_TREE) ? calcAccuracyWithKdTree()
				: calcAccuracy(numSamples);
		return alpha * accuracy + beta
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

}
