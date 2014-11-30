import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

public class KNN extends AbstractFeatureSelectionEvaluator {
	private static final Logger log = Logger.getLogger(KNN.class.getName());

	private final Stopwatch stopwatch = Stopwatch.createUnstarted();

	private final int numK;

	public KNN(int numK, ArrayList<Integer> featureSelectionResult, ArrayList<DataPoint> dataSet) {
		super(featureSelectionResult, dataSet);
		this.numK = numK;
	}

	private ArrayList<ArrayList<Double>> createDistanceMatrix(int numSampleInstances) {
		stopwatch.reset();
		ArrayList<ArrayList<Double>> distanceMatrix = new ArrayList<ArrayList<Double>>();

		// Get sample instances; those indexes are sorted.
		List<Integer> sampleIndices = MathUtil.randomlyPickNumbers(0, numInstances,
				numSampleInstances);

		for (int i = 0; i < numSampleInstances; i++) {
			stopwatch.start();
			int sampleInstanceIndex = sampleIndices.get(i);
			// pick a sample instance
			DataPoint sampleDataPoint = this.selectedDataSet.get(sampleInstanceIndex);
			// the distance between a sample instance to every other instances.
			ArrayList<Double> distances = new ArrayList<>(numInstances);
			for (int j = 0; j < numInstances; j++) {
				if (j < i) {
					distances.add(distanceMatrix.get(j).get(i));
				} else if (j == i) {
					distances.add(0.0);
				} else {
					distances.add(MathUtil.calculateCosineSimilarity(
							sampleDataPoint.getFeatureValues(),
							this.selectedDataSet.get(j).getFeatureValues(),
							true /* already normalized */));
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

	private static String findDominantClass(List<DataPoint> points, List<Double> distances) {
		Preconditions.checkArgument(points.size() == distances.size());
		// Use distance of each data point as tie-breaker.
		HashMap<String, Double> classNameWeights = new HashMap<>();
		// Count each class name.
		for (int i = 0; i < points.size(); i++) {
			DataPoint point = points.get(i);
			double distance = distances.get(i);

			String className = point.getClassName();
			if (classNameWeights.containsKey(className)) {
				classNameWeights.put(className, classNameWeights.get(className) + 1 / distance);
			} else {
				classNameWeights.put(className, 1 / distance);
			}
		}
		return Collections.max(classNameWeights.entrySet(), new HashMapComparator()).getKey();
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
			List<DataPoint> nearestPoints = new ArrayList<>();
			List<Double> distancesOfNearestPoints = new ArrayList<>();
			for (int j = 0; j < numK; j++) {
				nearestPoints.add(selectedDataSet.get(distancesWithIndex.get(j + 1).getIndex()));
				distancesOfNearestPoints.add(distancesWithIndex.get(j + 1).getValue());
			}
			String dominantClass = findDominantClass(nearestPoints, distancesOfNearestPoints);
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

	@Override
	public double calcFitness(double alpha, double beta, int samplingFolders) {
		int numSamples = -1;
		if (samplingFolders > 1) {
			numSamples = numInstances / samplingFolders;
		}
		double accuracy = calcAccuracy(numSamples);
		double result = alpha * accuracy + beta
				* (((double) (numFeatures - numSelectedFeatures)) / numFeatures);
		log.info("KNN::calcFitness: accuracy=" + accuracy + ", numSelectedFeatures="
				+ numSelectedFeatures + ", fitness=" + result);
		return result;
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

	private static class HashMapComparator implements Comparator<Map.Entry<String, Double>> {

		@Override
		public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
			return Double.compare(o1.getValue(), o2.getValue());
		}

	}

}
