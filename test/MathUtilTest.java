import java.util.List;

import junit.framework.TestCase;

import com.google.common.collect.Lists;

public class MathUtilTest extends TestCase {

	public void testMedian() {
		List<Double> values = Lists.newArrayList(1.0, 3.0, 2.0);
		IndexedValue<Double> expectedValue = new IndexedValue<>();
		expectedValue.setIndex(2);
		expectedValue.setValue(2.0);
		assertEquals(expectedValue, MathUtil.median(values));

		values.add(4.0);
		expectedValue.setIndex(1);
		expectedValue.setValue(3.0);
		assertEquals(expectedValue, MathUtil.median(values));
	}

}
