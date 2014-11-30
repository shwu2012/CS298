import java.util.ArrayList;

public abstract class AbstractFeatureSelectionEvaluator {
	protected final int numInstances;
	protected final int numFeatures;
	protected final ArrayList<DataPoint> selectedDataSet;
	protected final int numSelectedFeatures;

	public AbstractFeatureSelectionEvaluator(ArrayList<Integer> featureSelectionResult,
			ArrayList<DataPoint> dataSet) {
		this.numInstances = dataSet.size();
		this.selectedDataSet = createSelectedDataSet(featureSelectionResult, dataSet);
		this.numFeatures = dataSet.get(0).getFeatureValues().size();
		this.numSelectedFeatures = this.selectedDataSet.get(0).getFeatureValues().size();
	}

	protected ArrayList<DataPoint> createSelectedDataSet(ArrayList<Integer> featureSelectionResult,
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

	public abstract double calcFitness(double alpha, double beta, int samplingFolders);
}
