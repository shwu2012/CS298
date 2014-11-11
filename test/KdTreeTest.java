import junit.framework.TestCase;

import com.google.common.collect.Sets;

public class KdTreeTest extends TestCase {

	private KdTree tree;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		DataSet dataSet = new DataSet(2);
		dataSet.addInstance(MathUtil.makeInstance(2, 3));
		dataSet.addInstance(MathUtil.makeInstance(5, 4));
		dataSet.addInstance(MathUtil.makeInstance(4, 7));
		dataSet.addInstance(MathUtil.makeInstance(9, 6));
		dataSet.addInstance(MathUtil.makeInstance(8, 1));
		dataSet.addInstance(MathUtil.makeInstance(7, 2));

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
		assertEquals(MathUtil.makeInstance(7, 2), node.getDataPoint());
		assertEquals(0, node.getSplitFeatureIndex());
		assertNull(node.getRange());

		node = tree.getRootNode().getLeftChild();
		assertEquals(MathUtil.makeInstance(5, 4), node.getDataPoint());
		assertEquals(1, node.getSplitFeatureIndex());
		assertEquals(KdTree.Range.LEFT, node.getRange());

		node = tree.getRootNode().getRightChild();
		assertEquals(MathUtil.makeInstance(9, 6), node.getDataPoint());
		assertEquals(1, node.getSplitFeatureIndex());
		assertEquals(KdTree.Range.RIGHT, node.getRange());

		node = tree.getRootNode().getLeftChild().getLeftChild();
		assertEquals(MathUtil.makeInstance(2, 3), node.getDataPoint());
		assertEquals(KdTree.Range.LEFT, node.getRange());

		node = tree.getRootNode().getLeftChild().getRightChild();
		assertEquals(MathUtil.makeInstance(4, 7), node.getDataPoint());
		assertEquals(KdTree.Range.RIGHT, node.getRange());

		node = tree.getRootNode().getRightChild().getLeftChild();
		assertEquals(MathUtil.makeInstance(8, 1), node.getDataPoint());
		assertEquals(KdTree.Range.LEFT, node.getRange());
	}

	public void testFindNearestNode() {
		assertEquals(MathUtil.makeInstance(2, 3),
				tree.findNearestNode(MathUtil.makeInstance(2.1, 3.1)));
		assertEquals(MathUtil.makeInstance(2, 3),
				tree.findNearestNode(MathUtil.makeInstance(2, 4.5)));
		assertEquals(MathUtil.makeInstance(9, 6), tree.findNearestNode(MathUtil.makeInstance(9, 6)));
		assertEquals(MathUtil.makeInstance(8, 1),
				tree.findNearestNode(MathUtil.makeInstance(6.99, 0.01)));
	}

	public void testFindKNearestNodes() {
		assertEquals(Sets.newHashSet(MathUtil.makeInstance(2, 3), MathUtil.makeInstance(5, 4)),
				tree.findKNearestNodes(MathUtil.makeInstance(3, 4.1), 2).getDataPoints());
		assertEquals(
				Sets.newHashSet(MathUtil.makeInstance(5, 4), MathUtil.makeInstance(7, 2),
						MathUtil.makeInstance(8, 1)),
				tree.findKNearestNodes(MathUtil.makeInstance(6, 2), 3).getDataPoints());
		assertEquals(
				Sets.newHashSet(MathUtil.makeInstance(5, 4), MathUtil.makeInstance(7, 2),
						MathUtil.makeInstance(9, 6)),
				tree.findKNearestNodes(MathUtil.makeInstance(6.99, 3.99), 3).getDataPoints());
	}
}
