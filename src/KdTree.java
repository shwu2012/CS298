import com.google.common.base.Preconditions;

public class KdTree {
	public enum Range {
		LEFT, RIGHT
	};

	private final Node rootNode;

	private KdTree(Node rootNode) {
		this.rootNode = rootNode;
	}

	public static KdTree build(DataSet dataSet) {
		Node node = build(dataSet, null);
		return new KdTree(node);
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
