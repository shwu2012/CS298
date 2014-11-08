import junit.framework.TestCase;

import com.google.common.collect.Lists;

public class KdTreeTest extends TestCase {

	private KdTree tree;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		DataSet dataSet = new DataSet(2);
		dataSet.addInstance(makeInstance(2, 3));
		dataSet.addInstance(makeInstance(5, 4));
		dataSet.addInstance(makeInstance(4, 7));
		dataSet.addInstance(makeInstance(9, 6));
		dataSet.addInstance(makeInstance(8, 1));
		dataSet.addInstance(makeInstance(7, 2));

		/* Expected tree:
		 * e.g. (7,2@0) means the point: x1=7, x2=2, feature index to split is 0.
		 * 
		 *           (7,2@0)
		 *        /           \
		 *     (5,4@1)      (9,6@1)
		 *    /      \      /
		 *  (2,3)   (4,7)  (8,1)
		 */

		tree = KdTree.build(dataSet);
	}

	public void testBuildTree() {
		KdTree.Node node = tree.getRootNode();
		assertEquals(makeInstance(7, 2), node.getDataPoint());
		assertEquals(0, node.getSplitFeatureIndex());
		assertNull(node.getRange());

		node = tree.getRootNode().getLeftChild();
		assertEquals(makeInstance(5, 4), node.getDataPoint());
		assertEquals(1, node.getSplitFeatureIndex());
		assertEquals(KdTree.Range.LEFT, node.getRange());

		node = tree.getRootNode().getRightChild();
		assertEquals(makeInstance(9, 6), node.getDataPoint());
		assertEquals(1, node.getSplitFeatureIndex());
		assertEquals(KdTree.Range.RIGHT, node.getRange());

		node = tree.getRootNode().getLeftChild().getLeftChild();
		assertEquals(makeInstance(2, 3), node.getDataPoint());
		assertEquals(KdTree.Range.LEFT, node.getRange());

		node = tree.getRootNode().getLeftChild().getRightChild();
		assertEquals(makeInstance(4, 7), node.getDataPoint());
		assertEquals(KdTree.Range.RIGHT, node.getRange());

		node = tree.getRootNode().getRightChild().getLeftChild();
		assertEquals(makeInstance(8, 1), node.getDataPoint());
		assertEquals(KdTree.Range.LEFT, node.getRange());
	}

	private static DataPoint makeInstance(double x1, double x2) {
		DataPoint instance = new DataPoint();
		instance.setFeatureValues(Lists.newArrayList(x1, x2));
		instance.setClassName(null);
		return instance;
	}

	public void testFindNearestNode() {
		assertEquals(makeInstance(2, 3), tree.findNearestNode(makeInstance(2.1, 3.1)));
		assertEquals(makeInstance(2, 3), tree.findNearestNode(makeInstance(2, 4.5)));
	}
}
