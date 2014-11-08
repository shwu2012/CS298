import java.util.ArrayList;

import com.google.common.base.Objects;

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

	@Override
	public int hashCode() {
		return Objects.hashCode(className, featureValues);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataPoint other = (DataPoint) obj;
		return Objects.equal(className, other.getClassName())
				&& Objects.equal(featureValues, other.getFeatureValues());
	}

	@Override
	public String toString() {
		return "DataPoint [className=" + className + ", featureValues=" + featureValues + "]";
	}

}
