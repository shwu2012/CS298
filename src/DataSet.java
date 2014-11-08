import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class DataSet {
	private final List<DataPoint> instances;

	private final int dimension;

	public DataSet(int dimension) {
		Preconditions.checkArgument(dimension > 0);
		this.instances = new ArrayList<>();
		this.dimension = dimension;
	}

	public int getDimension() {
		return dimension;
	}

	public int getSize() {
		return instances.size();
	}

	public void addInstance(DataPoint instance) {
		Preconditions.checkArgument(instance.getFeatureValues().size() == dimension);
		instances.add(instance);
	}

	public void addAllInstances(Collection<DataPoint> instances) {
		for (DataPoint instance : instances) {
			addInstance(instance);
		}
	}

	public DataPoint getInstance(int index) {
		DataPoint result = new DataPoint();
		result.setClassName(instances.get(index).getClassName());
		result.setFeatureValues(new ArrayList<>(instances.get(index).getFeatureValues()));
		// Return a deep copy of that instance.
		return result;
	}

	public DataPoint getMutateInstance(int index) {
		return instances.get(index);
	}

	public List<Double> getSingleFeatureValues(final int featureIndex) {
		return Lists.newArrayList(Iterables.transform(instances, new Function<DataPoint, Double>() {

			@Override
			public Double apply(DataPoint input) {
				return input.getFeatureValues().get(featureIndex);
			}
		}));
	}
}
