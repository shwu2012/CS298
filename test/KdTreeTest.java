import junit.framework.TestCase;

import com.google.common.collect.Lists;

public class KdTreeTest extends TestCase {

	public void testBuildTree() {
		DataSet dataSet = new DataSet(2);

		dataSet.addInstance(makeInstance(2, 3));
		dataSet.addInstance(makeInstance(5, 4));
		dataSet.addInstance(makeInstance(4, 7));
		dataSet.addInstance(makeInstance(9, 6));
		dataSet.addInstance(makeInstance(8, 1));
		dataSet.addInstance(makeInstance(7, 2));

		// (7, 2) should be on the root node.
		DataPoint expectedInstanceOnRoot = dataSet.getMutateInstance(5);
		KdTree tree = KdTree.build(dataSet);
		assertEquals(expectedInstanceOnRoot, tree.getRootNode().getDataPoint());
	}

	private static DataPoint makeInstance(double x1, double x2) {
		DataPoint instance = new DataPoint();
		instance.setFeatureValues(Lists.newArrayList(x1, x2));
		instance.setClassName(null);
		return instance;
	}

}
