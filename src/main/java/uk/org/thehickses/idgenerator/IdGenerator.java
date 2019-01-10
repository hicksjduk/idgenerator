package uk.org.thehickses.idgenerator;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generator of unique integer IDs.
 * 
 * When requested to allocate an ID, this object allocates the smallest free ID which is larger than the last one
 * allocated, or failing that the smallest free ID. It also frees IDs on request.
 * 
 * @author Jeremy Hicks
 */
public class IdGenerator
{
    private static Logger LOG = LoggerFactory.getLogger(IdGenerator.class);

    private final Range supportedValues;
    private final SortedSet<Range> freeRanges = new TreeSet<>();
    private int nextId;

    /**
     * Initialises the generator with a range of supported IDs that is between the specified bounds (inclusive). The
     * bounds need not be specified in any particular order. The generator will never generate an ID which is smaller
     * than the smaller bound, or greater than the greater bound.
     * 
     * @param bound1
     *            the first bound.
     * @param bound2
     *            the second bound.
     */
    public IdGenerator(int bound1, int bound2)
    {
        this.supportedValues = new Range(Math.min(bound1, bound2), Math.max(bound1, bound2));
        this.nextId = supportedValues.start;
        freeRanges.add(supportedValues);
    }

    /**
     * Allocates an ID, if one is available.
     * 
     * @return the allocated ID.
     * @throws IllegalStateException
     *             if all IDs are currently allocated.
     */
    public int allocateId() throws IllegalStateException
    {
        int answer;
        synchronized (freeRanges)
        {
            LOG.debug("Allocating: nextId = {}, freeRanges = {}", nextId, freeRanges);
            if (freeRanges.isEmpty())
                throw new IllegalStateException("All possible IDs are allocated");
            Range range = neighbouringFreeRanges(nextId)
                    .filter(r -> r.contains(nextId) || r.start > nextId)
                    .findFirst()
                    .orElse(freeRanges.first());
            nextId = (answer = range.contains(nextId) ? nextId : range.start) + 1;
            freeRanges.remove(range);
            range.splitAround(answer).forEach(freeRanges::add);
            LOG.debug("Allocated {}, freeRanges = {}", answer, freeRanges);
        }
        return answer;
    }

    /**
     * Frees the specified ID, if it is within the range of supported values and is currently allocated.
     * 
     * @param id
     *            the ID.
     */
    public void freeId(int id)
    {
        if (!supportedValues.contains(id))
            return;
        synchronized (freeRanges)
        {
            LOG.debug("Freeing {}, freeRanges = {}", id, freeRanges);
            Range[] neighbours = neighbouringFreeRanges(id).toArray(Range[]::new);
            if (Stream.of(neighbours).anyMatch(r -> r.contains(id)))
                return;
            Range newRange = Stream.of(neighbours).reduce(new Range(id), Range::mergeIfAdjacent);
            Stream.of(neighbours).filter(newRange::overlaps).forEach(freeRanges::remove);
            freeRanges.add(newRange);
            LOG.debug("Freed {}, freeRanges = {}", id, freeRanges);
        }
    }

    private Stream<Range> neighbouringFreeRanges(int id)
    {
        Range searchRange = new Range(id);
        return Stream
                .of(getIfNotEmpty(freeRanges.headSet(searchRange), SortedSet::last),
                        getIfNotEmpty(freeRanges.tailSet(searchRange), SortedSet::first))
                .filter(Objects::nonNull);
    }

    private Range getIfNotEmpty(SortedSet<Range> set, Function<SortedSet<Range>, Range> getter)
    {
        return set.isEmpty() ? null : getter.apply(set);
    }

    private static class Range implements Comparable<Range>
    {
        private final static Comparator<Range> COMPARATOR = Comparator
                .comparingInt((Range r) -> r.start)
                .thenComparingInt(r -> r.end);

        public final int start;
        public final int end;

        public Range(int number)
        {
            this(number, number);
        }

        public Range(int start, int end)
        {
            this.start = start;
            this.end = end;
        }

        public boolean contains(int number)
        {
            return start <= number && number <= end;
        }

        public boolean overlaps(Range other)
        {
            return contains(other.start) || contains(other.end) || other.contains(start)
                    || other.contains(end);
        }

        public boolean adjoins(Range other)
        {
            return start - other.end == 1 || other.start - end == 1;
        }

        public Range mergeIfAdjacent(Range other)
        {
            return !adjoins(other) ? this
                    : new Range(Math.min(start, other.start), Math.max(end, other.end));
        }

        public Stream<Range> splitAround(int number)
        {
            if (!contains(number))
                return Stream.empty();
            return Stream.of(new Range(start, number - 1), new Range(number + 1, end)).filter(
                    r -> r.start <= r.end);
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + end;
            result = prime * result + start;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
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
        public int compareTo(Range other)
        {
            return COMPARATOR.compare(this, other);
        }

        @Override
        public String toString()
        {
            return String.format("Range [start=%s, end=%s]", start, end);
        }
    }
}
