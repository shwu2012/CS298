import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;

public class DataPoint {
	private String className;
	private List<Double> featureValues;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public List<Double> getFeatureValues() {
		return Collections.unmodifiableList(featureValues);
	}

	public void setFeatureValues(List<Double> featureValues) {
		this.featureValues = new ArrayList<>(featureValues);
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
