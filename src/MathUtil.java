import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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

	public static double calculateEuclideanDistance(List<Double> instance1, List<Double> instance2) {
		Preconditions.checkArgument(instance1.size() == instance2.size());
		int dimension = instance1.size();
		double sum = 0.0;
		for (int i = 0; i < dimension; i++) {
			sum += Math.pow(instance1.get(i) - instance2.get(i), 2);
		}
		return Math.sqrt(sum);
	}

	public static double calculateCosineSimilarity(List<Double> instance1, List<Double> instance2,
			boolean alreadyNormalized) {
		Preconditions.checkArgument(instance1.size() == instance2.size());
		int dimension = instance1.size();
		double sum = 0.0;
		for (int i = 0; i < dimension; i++) {
			sum += (instance1.get(i) * instance2.get(i));
		}
		return alreadyNormalized ? sum : sum / (vectorLength(instance1) * vectorLength(instance2));
	}

	private static double vectorLength(List<Double> instance) {
		double length = 0.0;
		for (double value : instance) {
			length += value * value;
		}
		return Math.sqrt(length);
	}

	public static DataPoint makeInstance(double x1, double x2) {
		return new DataPoint(null, Lists.newArrayList(x1, x2));
	}

	public static BitSet randomBits(int numBits) {
		BitSet result = new BitSet(numBits);
		String binaryString = Strings.padStart(new BigInteger(numBits, new Random()).toString(2),
				numBits, '0');
		for (int i = 0; i < binaryString.length(); i++) {
			result.set(i, binaryString.charAt(i) == '1');
		}
		return result;
	}
}
