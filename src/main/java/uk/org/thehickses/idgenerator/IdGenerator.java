package uk.org.thehickses.idgenerator;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdGenerator {
	private static Logger LOG = LoggerFactory.getLogger(IdGenerator.class);

	private final Range supportedValues;
	private final SortedSet<Range> freeRanges = new TreeSet<>();
	private int nextId;

	public IdGenerator(int bound1, int bound2) {
		this.supportedValues = new Range(Math.min(bound1, bound2), Math.max(bound1, bound2));
		this.nextId = supportedValues.start;
		freeRanges.add(supportedValues);
	}

	public int allocateId() throws IllegalStateException {
		int answer;
		synchronized (freeRanges) {
			LOG.debug("Allocating: nextId = {}, freeRanges = {}", nextId, freeRanges);
			if (freeRanges.isEmpty())
				throw new IllegalStateException("All possible IDs are allocated");
			Range nextRange = new Range(nextId);
			Range range = Stream
					.of(getIfNotEmpty(freeRanges.tailSet(nextRange), SortedSet::last),
							getIfNotEmpty(freeRanges.headSet(nextRange), SortedSet::first))
					.filter(Objects::nonNull).filter(r -> r.contains(nextId)).findFirst().orElse(freeRanges.first());
			nextId = (answer = range.contains(nextId) ? nextId : range.start) + 1;
			freeRanges.remove(range);
			range.splitAround(answer).forEach(freeRanges::add);
			LOG.debug("Allocated {}, freeRanges = {}", answer, freeRanges);
		}
		return answer;
	}

	public void freeId(int id) {
		if (!supportedValues.contains(id))
			return;
		Range freedRange = new Range(id);
		synchronized (freeRanges) {
			LOG.debug("Freeing {}, freeRanges = {}", id, freeRanges);
			Range[] neighbours = Stream
					.of(getIfNotEmpty(freeRanges.headSet(freedRange), SortedSet::last),
							getIfNotEmpty(freeRanges.tailSet(freedRange), SortedSet::first))
					.filter(Objects::nonNull).toArray(Range[]::new);
			if (Stream.of(neighbours).anyMatch(r -> r.contains(id)))
				return;
			Range newRange = Stream.of(neighbours).reduce(freedRange,
					(res, range) -> range.adjoins(res) ? range.merge(res) : res);
			Stream.of(neighbours).filter(newRange::overlaps).forEach(freeRanges::remove);
			freeRanges.add(newRange);
			LOG.debug("Freed {}, freeRanges = {}", id, freeRanges);
		}
	}

	private Range getIfNotEmpty(SortedSet<Range> set, Function<SortedSet<Range>, Range> getter) {
		return set.isEmpty() ? null : getter.apply(set);
	}

	private static class Range implements Comparable<Range> {
		private final static Comparator<Range> COMPARATOR = Comparator.comparingInt((Range r) -> r.start)
				.thenComparingInt(r -> r.end);

		public final int start;
		public final int end;

		public Range(int number) {
			this(number, number);
		}

		public Range(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public boolean contains(int number) {
			return start <= number && number <= end;
		}

		public boolean overlaps(Range other) {
			return contains(other.start) || contains(other.end) || other.contains(start) || other.contains(end);
		}

		public boolean adjoins(Range other) {
			return start - other.end == 1 || other.start - end == 1;
		}

		public Range merge(Range other) {
			return new Range(Math.min(start, other.start), Math.max(end, other.end));
		}

		public Stream<Range> splitAround(int number) {
			if (!contains(number))
				return Stream.empty();
			return Stream.of(new Range(start, number - 1), new Range(number + 1, end)).filter(r -> r.start <= r.end);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + end;
			result = prime * result + start;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Range other = (Range) obj;
			if (end != other.end)
				return false;
			if (start != other.start)
				return false;
			return true;
		}

		@Override
		public int compareTo(Range other) {
			return COMPARATOR.compare(this, other);
		}

		@Override
		public String toString() {
			return String.format("Range [start=%s, end=%s]", start, end);
		}
	}
}
