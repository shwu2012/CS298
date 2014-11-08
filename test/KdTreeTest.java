import junit.framework.TestCase;

import com.google.common.collect.Lists;

public class KdTreeTest extends TestCase {

	public void testBuildTree() {
		DataSet dataSet = new DataSet(2);

		dataSet.addInstance(makeInstance(2, 3)); // 0
		dataSet.addInstance(makeInstance(5, 4)); // 1
		dataSet.addInstance(makeInstance(4, 7)); // 2
		dataSet.addInstance(makeInstance(9, 6)); // 3
		dataSet.addInstance(makeInstance(8, 1)); // 4
		dataSet.addInstance(makeInstance(7, 2)); // 5

		/* Expected tree:
		 * e.g. (7,2@0) means the point: x1=7, x2=2, feature index to split is 0.
		 * 
		 *           (7,2@0)
		 *        /           \
		 *     (5,4@1)      (9,6@1)
		 *    /      \      /
		 *  (2,3)   (4,7)  (8,1)
		 */

		KdTree tree = KdTree.build(dataSet);
		assertEquals(dataSet.getMutateInstance(5), tree.getRootNode().getDataPoint());
		assertEquals(0, tree.getRootNode().getSplitFeatureIndex());

		assertEquals(dataSet.getMutateInstance(1), tree.getRootNode().getLeftChild().getDataPoint());
		assertEquals(1, tree.getRootNode().getLeftChild().getSplitFeatureIndex());

		assertEquals(dataSet.getMutateInstance(3), tree.getRootNode().getRightChild()
				.getDataPoint());
		assertEquals(1, tree.getRootNode().getRightChild().getSplitFeatureIndex());

		assertEquals(dataSet.getMutateInstance(0), tree.getRootNode().getLeftChild().getLeftChild()
				.getDataPoint());

		assertEquals(dataSet.getMutateInstance(2), tree.getRootNode().getLeftChild()
				.getRightChild().getDataPoint());

		assertEquals(dataSet.getMutateInstance(4), tree.getRootNode().getRightChild()
				.getLeftChild().getDataPoint());
	}

	private static DataPoint makeInstance(double x1, double x2) {
		DataPoint instance = new DataPoint();
		instance.setFeatureValues(Lists.newArrayList(x1, x2));
		instance.setClassName(null);
		return instance;
	}

}
