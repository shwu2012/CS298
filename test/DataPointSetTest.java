import junit.framework.TestCase;

public class DataPointSetTest extends TestCase {

	private DataPointSet dataPointSet;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		dataPointSet = new DataPointSet(3);
	}

	public void testSizeAndCapacity() {
		assertFalse(dataPointSet.isFull());
		assertEquals(0, dataPointSet.getSize());
		assertEquals(3, dataPointSet.getCapacity());

		dataPointSet.add(MathUtil.makeInstance(1.0, 1.0), 0.1);
		assertFalse(dataPointSet.isFull());
		assertEquals(1, dataPointSet.getSize());
		assertEquals(3, dataPointSet.getCapacity());

		dataPointSet.add(MathUtil.makeInstance(2.0, 2.0), 0.2);
		assertFalse(dataPointSet.isFull());
		assertEquals(2, dataPointSet.getSize());
		assertEquals(3, dataPointSet.getCapacity());

		dataPointSet.add(MathUtil.makeInstance(3.0, 3.0), 0.3);
		assertTrue(dataPointSet.isFull());
		assertEquals(3, dataPointSet.getSize());
		assertEquals(3, dataPointSet.getCapacity());

		dataPointSet.add(MathUtil.makeInstance(4.0, 4.0), 0.4);
		assertTrue(dataPointSet.isFull());
		assertEquals(3, dataPointSet.getSize());
		assertEquals(3, dataPointSet.getCapacity());
	}

	public void testAddPointsAndGetMaxDistance() {
		assertNull(dataPointSet.add(MathUtil.makeInstance(1.0, 1.0), 0.1));
		assertEquals(0.1, dataPointSet.getMaxDistance());

		assertNull(dataPointSet.add(MathUtil.makeInstance(3.0, 3.0), 0.3));
		assertEquals(0.3, dataPointSet.getMaxDistance());

		assertNull(dataPointSet.add(MathUtil.makeInstance(2.0, 2.0), 0.2));
		assertEquals(0.3, dataPointSet.getMaxDistance());

		assertEquals("Reject a new point with larger distance.", MathUtil.makeInstance(4.0, 4.0),
				dataPointSet.add(MathUtil.makeInstance(4.0, 4.0), 0.4));
		assertEquals(0.3, dataPointSet.getMaxDistance());

		assertEquals("Add a new point with smaller distance.", MathUtil.makeInstance(3.0, 3.0),
				dataPointSet.add(MathUtil.makeInstance(4.0, 4.0), 0.25));
		assertEquals(0.25, dataPointSet.getMaxDistance());
	}
}
