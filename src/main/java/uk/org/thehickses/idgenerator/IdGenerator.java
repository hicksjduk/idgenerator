package uk.org.thehickses.idgenerator;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class IdGenerator
{
    private final int minId;
    private final int maxId;
    private final SortedSet<Range> freeRanges = new TreeSet<>();
    private int lastId;

    public IdGenerator(int bound1, int bound2)
    {
        this.minId = Math.min(bound1, bound2);
        this.maxId = Math.max(bound1, bound2);
        this.lastId = minId - 1;
        freeRanges.add(new Range(minId, maxId));
    }

    public int allocateId() throws IllegalStateException
    {
        int answer;
        synchronized (freeRanges)
        {
            if (freeRanges.isEmpty())
                throw new IllegalStateException("All possible IDs are allocated");
            Range range = Stream
                    .of(freeRanges.tailSet(new Range(lastId + 1)), freeRanges)
                    .filter(s -> !s.isEmpty())
                    .map(SortedSet::first)
                    .findFirst()
                    .get();
            answer = lastId = range.start;
            freeRanges.remove(range);
            if (range.start != range.end)
                freeRanges.add(new Range(range.start + 1, range.end));
        }
        return answer;
    }

    public void freeId(int id)
    {
        if (id < minId || id > maxId)
            return;
        Range freedRange = new Range(id);
        BiFunction<SortedSet<Range>, Function<SortedSet<Range>, Range>, Range> getIfNotEmpty = (set,
                getter) -> set.isEmpty() ? null : getter.apply(set);
        synchronized (freeRanges)
        {
            Range[] neighbours = Stream
                    .of(getIfNotEmpty.apply(freeRanges.headSet(freedRange), SortedSet::last),
                            getIfNotEmpty.apply(freeRanges.tailSet(freedRange), SortedSet::first))
                    .filter(Objects::nonNull)
                    .toArray(Range[]::new);
            if (Stream.of(neighbours).anyMatch(r -> r.contains(id)))
                return;
            Range newRange = Stream.of(neighbours).reduce(freedRange,
                    (res, range) -> range.adjoins(res) ? range.merge(res) : res);
            Stream.of(neighbours).filter(newRange::overlaps).forEach(freeRanges::remove);
            freeRanges.add(newRange);
        }
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

        public Range(int bound1, int bound2)
        {
            this.start = Math.min(bound1, bound2);
            this.end = Math.max(bound1, bound2);
        }

        public boolean contains(int number)
        {
            return start <= number && number <= end;
        }

        public boolean overlaps(Range other)
        {
            if (other == null)
                return false;
            return contains(other.start) || contains(other.end) || other.contains(start)
                    || other.contains(end);
        }

        public boolean adjoins(Range other)
        {
            if (other == null)
                return false;
            return start - other.end == 1 || other.start - end == 1;
        }

        public Range merge(Range other)
        {
            return other == null ? this
                    : new Range(Math.min(start, other.start), Math.max(end, other.end));
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
