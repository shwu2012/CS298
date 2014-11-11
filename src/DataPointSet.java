import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * A data point set with limited capacity that prefers holding smaller
 * distances. If the capacity is reached, points with larger distances will be
 * thrown away.
 */
public class DataPointSet {
	private static final Logger log = Logger.getLogger(DataPointSet.class.getName());
	private final Map<DataPoint, Double> points;

	private final int capacity;

	private double maxDistance;

	private DataPoint pointWithMaxDistance;

	public DataPointSet(int capacity) {
		Preconditions.checkArgument(capacity > 0);
		this.capacity = capacity;
		this.points = new HashMap<>();
		this.maxDistance = -1.0;
		this.pointWithMaxDistance = null;
	}

	public Set<DataPoint> getDataPoints() {
		return ImmutableSet.copyOf(points.keySet());
	}

	public int getSize() {
		return points.size();
	}

	public int getCapacity() {
		return capacity;
	}

	public double getMaxDistance() {
		if (points.isEmpty()) {
			throw new UnsupportedOperationException("Cannot return max distanec from an empty set.");
		}
		return maxDistance;
	}

	public boolean isFull() {
		return points.size() >= capacity;
	}

	public DataPoint add(DataPoint newPoint, double distance) {
		Preconditions.checkArgument(newPoint != null);
		Preconditions.checkArgument(distance >= 0.0);
		if (points.containsKey(newPoint)) {
			log.fine("ignore duplicated data point: " + newPoint);
			return newPoint;
		}

		// If the set doesn't reach the capacity, we simply add the new point.
		if (!isFull()) {
			points.put(newPoint, distance);
			if (distance > maxDistance) {
				maxDistance = distance;
				pointWithMaxDistance = newPoint;
			}
			return null;
		}

		// If the set is already full, but the new point has a larger distance,
		// then the new point will not be added because the set prefers keeping
		// points with smaller distances.
		if (distance > maxDistance) {
			return newPoint;
		}

		// If the set is already full, but the new point has a smaller distance,
		// then the new point will be added, and the point with largest distance
		// will be thrown away.
		DataPoint pointToBeThrown = pointWithMaxDistance;
		points.remove(pointToBeThrown);
		points.put(newPoint, distance);
		Map.Entry<DataPoint, Double> newPointWithMaxDistance = Collections.max(points.entrySet(),
				new Comparator<Map.Entry<DataPoint, Double>>() {

					@Override
					public int compare(Entry<DataPoint, Double> o1, Entry<DataPoint, Double> o2) {
						return Double.compare(o1.getValue(), o2.getValue());
					}
				});
		maxDistance = newPointWithMaxDistance.getValue();
		pointWithMaxDistance = newPointWithMaxDistance.getKey();
		return pointToBeThrown;
	}
}
