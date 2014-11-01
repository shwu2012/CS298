import java.util.ArrayList;

public class DataPoint {
	private String className;
	private ArrayList<Double> featureValues;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public ArrayList<Double> getFeatureValues() {
		return featureValues;
	}

	public void setFeatureValues(ArrayList<Double> featureValues) {
		this.featureValues = featureValues;
	}
}
