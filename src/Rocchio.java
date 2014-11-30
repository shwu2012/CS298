import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

public class Rocchio extends AbstractFeatureSelectionEvaluator {
	private static final Logger log = Logger.getLogger(Rocchio.class.getName());

	public Rocchio(ArrayList<Integer> featureSelectionResult, ArrayList<DataPoint> dataSet) {
		super(featureSelectionResult, dataSet);
	}

	@Override
	public double calcFitness(double alpha, double beta, int samplingFolders) {
		Preconditions.checkArgument(samplingFolders == -1, "Sampling is not supported.");
		double accuracy = calcAccuracy();
		double result = alpha * accuracy + beta
				* (((double) (numFeatures - numSelectedFeatures)) / numFeatures);
		log.info("Rocchio::calcFitness: accuracy=" + accuracy + ", numSelectedFeatures="
				+ numSelectedFeatures + ", fitness=" + result);
		return result;
	}

	private static String classify(DataPoint newPoint, Map<String, DataPoint> centroidsByClass) {
		double minDistance = 0.0;
		String className = null;
		for (DataPoint centroid : centroidsByClass.values()) {
			double distance = MathUtil.calculateEuclideanDistance(newPoint.getFeatureValues(),
					centroid.getFeatureValues());
			if (className == null) {
				className = centroid.getClassName();
				minDistance = distance;
			} else {
				if (distance < minDistance) {
					minDistance = distance;
					className = centroid.getClassName();
				}
			}
		}
		return className;
	}

	private double calcAccuracy() {
		final Map<String, DataPoint> centroidsByClass = getClassCentroids();
		int correctClassifiedCount = 0;
		for (int i = 0; i < numInstances; i++) {
			DataPoint point = selectedDataSet.get(i);
			String correctClassName = point.getClassName();
			if (correctClassName.equals(classify(point, centroidsByClass))) {
				correctClassifiedCount++;
			}
		}
		double accuracy = ((double) correctClassifiedCount) / numInstances;
		log.info("accuracy = " + accuracy + ", using Rocchio verified on " + numInstances
				+ " instances.");
		return accuracy;
	}

	private Map<String, DataPoint> getClassCentroids() {
		Map<String, List<DataPoint>> pointsByClass = new HashMap<>();
		Map<String, DataPoint> centroidsByClass = new HashMap<>();
		for (int i = 0; i < numInstances; i++) {
			DataPoint point = selectedDataSet.get(i);
			String className = point.getClassName();
			if (!pointsByClass.containsKey(className)) {
				pointsByClass.put(className, new ArrayList<DataPoint>());
			}
			pointsByClass.get(className).add(point);
		}
		for (Map.Entry<String, List<DataPoint>> entry : pointsByClass.entrySet()) {
			String className = entry.getKey();
			List<DataPoint> points = entry.getValue();
			centroidsByClass.put(className, getCentroid(points, className));
		}
		return centroidsByClass;
	}

	private DataPoint getCentroid(List<DataPoint> points, String className) {
		for (DataPoint point : points) {
			Preconditions.checkArgument(className.equals(point.getClassName()));
		}

		ArrayList<Double> featureValues = new ArrayList<>(numSelectedFeatures);
		for (int i = 0; i < numSelectedFeatures; i++) {
			featureValues.add(0.0);
		}
		for (DataPoint point : points) {
			for (int i = 0; i < numSelectedFeatures; i++) {
				featureValues.set(i, featureValues.get(i) + point.getFeatureValues().get(i));
			}
		}
		int numPoints = points.size();
		for (int i = 0; i < numSelectedFeatures; i++) {
			featureValues.set(i, featureValues.get(i) / numPoints);
		}
		return new DataPoint(className, featureValues);
	}
}
