import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.math.DoubleMath;

public final class MathUtil {
	private MathUtil() {
	}

	public static double variance(Collection<Double> values) {
		Preconditions.checkArgument(!values.isEmpty());
		double avg = DoubleMath.mean(values);
		double sum = 0.0;
		for (double value : values) {
			sum += Math.pow(value - avg, 2);
		}
		return sum / values.size();
	}

	public static IndexedValue<Double> median(List<Double> unsortedValues) {
		// TODO: use quick-selection to avoid sorting all elements. See
		// http://en.wikipedia.org/wiki/Quickselect
		Preconditions.checkArgument(!unsortedValues.isEmpty());
		int count = unsortedValues.size();
		List<IndexedValue<Double>> unsortedValuesWithIndex = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			IndexedValue<Double> indexedValue = new IndexedValue<>();
			indexedValue.setIndex(i);
			indexedValue.setValue(unsortedValues.get(i));
			unsortedValuesWithIndex.add(indexedValue);
		}
		Collections.sort(unsortedValuesWithIndex, new Comparator<IndexedValue<Double>>() {

			@Override
			public int compare(IndexedValue<Double> o1, IndexedValue<Double> o2) {
				return Double.compare(o1.getValue(), o2.getValue());
			}
		});
		return unsortedValuesWithIndex.get(count / 2);
	}
}
