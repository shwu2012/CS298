import java.util.Stack;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

public class KdTree {
	private static final Logger log = Logger.getLogger(KdTree.class.getName());

	public enum Range {
		LEFT, RIGHT
	};

	private final Node rootNode;

	private final int numNodes;

	private KdTree(Node rootNode, int numNodes) {
		this.rootNode = rootNode;
		this.numNodes = numNodes;
	}

	public static KdTree build(DataSet dataSet) {
		return new KdTree(build(dataSet, null), dataSet.getSize());
	}

	private static Node build(DataSet dataSet, Range range) {
		Preconditions.checkNotNull(dataSet);
		int numInstances = dataSet.getSize();
		if (numInstances == 0) {
			return null;
		}

		// Select a dimension (i.e. feature index) and an instance to split.
		int splitFeatureIndex = selectSplitFeature(dataSet);
		IndexedValue<Double> splitFeatureValueAndInstanceIndex = MathUtil.median(dataSet
				.getSingleFeatureValues(splitFeatureIndex));
		double splitFeatureValue = splitFeatureValueAndInstanceIndex.getValue();
		int splitInstanceIndex = splitFeatureValueAndInstanceIndex.getIndex();
		DataPoint splitDataPoint = dataSet.getMutateInstance(splitInstanceIndex);

		// Split data-set (except the instance selected above as the split
		// point) to 2 sub-data-set.
		DataSet dataSetForLeftSubTree = new DataSet(dataSet.getDimension());
		DataSet dataSetForRightSubTree = new DataSet(dataSet.getDimension());
		for (int i = 0; i < numInstances; i++) {
			if (i != splitInstanceIndex) {
				DataPoint instance = dataSet.getInstance(i);
				if (instance.getFeatureValues().get(splitFeatureIndex) < splitFeatureValue) {
					dataSetForLeftSubTree.addInstance(instance);
				} else {
					dataSetForRightSubTree.addInstance(instance);
				}
			}
		}

		// Build the tree recursively.
		Node node = new Node();
		node.setRange(range);
		node.setDataPoint(splitDataPoint);
		node.setSplitFeatureIndex(splitFeatureIndex);
		node.setLeftChild(build(dataSetForLeftSubTree, Range.LEFT));
		node.setRightChild(build(dataSetForRightSubTree, Range.RIGHT));

		return node;
	}

	private static int selectSplitFeature(DataSet dataSet) {
		int splitFeatureIndex = -1;
		double maxVariance = -1.0;
		for (int i = 0; i < dataSet.getDimension(); i++) {
			double variance = MathUtil.variance(dataSet.getSingleFeatureValues(i));
			if (variance > maxVariance) {
				maxVariance = variance;
				splitFeatureIndex = i;
			}
		}
		return splitFeatureIndex;
	}

	/**
	 * Search the point in a (sub) KD-tree from root to leaf; early return if a
	 * node in the search path with exactly same data point in the search path.
	 * 
	 * @param startNode
	 *            the root node of the (sub) KD-tree
	 * @param searchPath
	 *            a stack to record all the searched nodes in the tree
	 * @param searchPoint
	 *            a data point to be searched
	 * @return if a node has exactly the same data point, return the node;
	 *         otherwise it returns {@code null}.
	 */
	private static Node followTreeToLeafOrEarlyReturn(final Node rootNode,
			final Stack<Node> searchPath, final DataPoint searchPoint) {
		Node node = rootNode;
		while (node != null) {
			log.info("push: " + node);
			searchPath.push(node);
			int splitFeatureIndex = node.getSplitFeatureIndex();
			if (node.getDataPoint().equalsIgnoringClassName(searchPoint)) {
				log.info("found the node with exactly same data point: " + node);
				return node;
			} else if (searchPoint.getFeatureValues().get(splitFeatureIndex) < node.getDataPoint()
					.getFeatureValues().get(splitFeatureIndex)) {
				node = node.getLeftChild();
			} else {
				node = node.getRightChild();
			}
		}
		return null;
	}

	/**
	 * Search the point in a (sub) KD-tree from root to leaf. Note that it is
	 * different from
	 * {@link KdTree#followTreeToLeafOrEarlyReturn(Node, Stack, DataPoint)};
	 * this method will not early return even if a node in the tree with the
	 * exact same data has been found -- instead, the search will not end until
	 * a leaf node was reached.
	 * 
	 * @param startNode
	 *            the root node of the (sub) KD-tree
	 * @param searchPath
	 *            a stack to record all the searched nodes in the tree
	 * @param searchPoint
	 *            a data point to be searched
	 */
	private static void followTreeToLeaf(final Node rootNode, final Stack<Node> searchPath,
			final DataPoint searchPoint) {
		Node node = rootNode;
		while (node != null) {
			log.info("push: " + node);
			searchPath.push(node);
			int splitFeatureIndex = node.getSplitFeatureIndex();
			if (searchPoint.getFeatureValues().get(splitFeatureIndex) < node.getDataPoint()
					.getFeatureValues().get(splitFeatureIndex)) {
				node = node.getLeftChild();
			} else {
				node = node.getRightChild();
			}
		}
	}

	public DataPoint findNearestNode(final DataPoint searchPoint) {
		log.info("findNearestNode: " + searchPoint);
		Stack<Node> searchPath = new Stack<>();
		Node nodeWithExactlySameData = followTreeToLeafOrEarlyReturn(rootNode, searchPath,
				searchPoint);
		if (nodeWithExactlySameData != null) {
			// Already found the nearest node.
			return nodeWithExactlySameData.getDataPoint();
		}

		// Back-track to find the nearest node with minimal distance.
		Node nearestNode = null;
		double distance = -1.0;
		double minDistance = Double.MAX_VALUE;

		while (!searchPath.empty()) {
			// Try previous split point in the search path.
			Node node = searchPath.pop();
			log.info("pop: " + node);
			// Need to search another half-space of the split node?
			int splitFeatureIndex = node.getSplitFeatureIndex();
			boolean searchAnotherHalfSpace = minDistance > Math.abs(searchPoint.getFeatureValues()
					.get(splitFeatureIndex)
					- node.getDataPoint().getFeatureValues().get(splitFeatureIndex));

			distance = MathUtil.euclideanDistance(node.getDataPoint().getFeatureValues(),
					searchPoint.getFeatureValues());
			if (distance < minDistance) {
				minDistance = distance;
				nearestNode = node;
			}

			if (searchAnotherHalfSpace) {
				Node splitDataNodeInAnotherHalf = null;
				Range rangeAlreadySearched = (searchPoint.getFeatureValues().get(splitFeatureIndex) < node
						.getDataPoint().getFeatureValues().get(splitFeatureIndex)) ? Range.LEFT
						: Range.RIGHT;
				if (rangeAlreadySearched == Range.LEFT) {
					// Go on searching on right-half space since left-half space
					// has been searched.
					log.info("try search right-half");
					splitDataNodeInAnotherHalf = node.getRightChild();
				} else {
					// Go on searching on left-half space since right-half space
					// has been searched.
					log.info("try search left-half");
					splitDataNodeInAnotherHalf = node.getLeftChild();
				}

				if (splitDataNodeInAnotherHalf != null) {
					// Follow from the new split node to a leaf node, and also
					// add those new split nodes into the search path.
					nodeWithExactlySameData = followTreeToLeafOrEarlyReturn(
							splitDataNodeInAnotherHalf, searchPath, searchPoint);
					if (nodeWithExactlySameData != null) {
						// Already found the nearest node.
						return nodeWithExactlySameData.getDataPoint();
					}
				}
			}
		}

		return nearestNode.getDataPoint();
	}

	public DataPointSet findKNearestNodes(final DataPoint searchPoint, final int numK) {
		log.info("findKNearestNodes: " + searchPoint + ", K=" + numK);
		Preconditions.checkArgument(numK <= numNodes);
		DataPointSet kNearestDataPoints = new DataPointSet(numK);

		Stack<Node> searchPath = new Stack<>();
		followTreeToLeaf(rootNode, searchPath, searchPoint);

		// Back-track to find K nearest nodes with minimal distance.
		double distance = -0.1;
		double minDistance = Double.MAX_VALUE;

		while (!searchPath.empty()) {
			// Try previous split point in the search path.
			Node node = searchPath.pop();
			// Need to search another half-space of the split node?
			int splitFeatureIndex = node.getSplitFeatureIndex();
			boolean searchAnotherHalfSpace = (!kNearestDataPoints.isFull())
					|| (minDistance > Math.abs(searchPoint.getFeatureValues()
							.get(splitFeatureIndex)
							- node.getDataPoint().getFeatureValues().get(splitFeatureIndex)));

			distance = MathUtil.euclideanDistance(node.getDataPoint().getFeatureValues(),
					searchPoint.getFeatureValues());
			kNearestDataPoints.add(node.getDataPoint(), distance);
			minDistance = kNearestDataPoints.getMaxDistance();

			if (searchAnotherHalfSpace) {
				Node splitDataNodeInAnotherHalf = null;
				Range rangeAlreadySearched = (searchPoint.getFeatureValues().get(splitFeatureIndex) < node
						.getDataPoint().getFeatureValues().get(splitFeatureIndex)) ? Range.LEFT
						: Range.RIGHT;
				if (rangeAlreadySearched == Range.LEFT) {
					// Go on searching on right-half space since left-half space
					// has been searched.
					log.info("try search right-half");
					splitDataNodeInAnotherHalf = node.getRightChild();
				} else {
					// Go on searching on left-half space since right-half space
					// has been searched.
					log.info("try search left-half");
					splitDataNodeInAnotherHalf = node.getLeftChild();
				}

				if (splitDataNodeInAnotherHalf != null) {
					// Follow from the new split node to a leaf node, and also
					// add those new split nodes into the search path.
					followTreeToLeaf(splitDataNodeInAnotherHalf, searchPath, searchPoint);
				}
			}
		}

		return kNearestDataPoints;
	}

	public Node getRootNode() {
		return rootNode;
	}

	public static class Node {
		private DataPoint dataPoint;
		private int splitFeatureIndex;
		private Node leftChild;
		private Node rightChild;
		private Range range;

		public DataPoint getDataPoint() {
			return dataPoint;
		}

		public void setDataPoint(DataPoint dataPoint) {
			this.dataPoint = dataPoint;
		}

		public int getSplitFeatureIndex() {
			return splitFeatureIndex;
		}

		public void setSplitFeatureIndex(int splitFeatureIndex) {
			this.splitFeatureIndex = splitFeatureIndex;
		}

		public Node getLeftChild() {
			return leftChild;
		}

		public void setLeftChild(Node leftChild) {
			this.leftChild = leftChild;
		}

		public Node getRightChild() {
			return rightChild;
		}

		public void setRightChild(Node rightChild) {
			this.rightChild = rightChild;
		}

		public Range getRange() {
			return range;
		}

		public void setRange(Range range) {
			this.range = range;
		}

		@Override
		public String toString() {
			return "Node [dataPoint=" + dataPoint + ", splitFeatureIndex=" + splitFeatureIndex
					+ ", hasLeftChild=" + (leftChild != null) + ", hasRightChild="
					+ (rightChild != null) + ", range=" + range + "]";
		}

	}
}
