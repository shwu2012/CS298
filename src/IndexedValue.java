import java.util.Objects;

public class IndexedValue<T> {
	private int index;
	private T value;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(index, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexedValue<?> other = (IndexedValue<?>) obj;
		return Objects.equals(index, other.getIndex()) && Objects.equals(value, other.getValue());
	}

	@Override
	public String toString() {
		return "IndexedValue [index=" + index + ", value=" + value + "]";
	}
}