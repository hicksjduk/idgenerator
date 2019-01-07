package uk.org.thehickses.idgenerator;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.IntStream;

import org.junit.Test;

public class IdGeneratorTest
{

    @Test
    public void test()
    {
        IdGenerator generator = new IdGenerator(5, 1);
        expectAllocations(generator, IntStream.rangeClosed(1, 5).toArray());
        expectExceptionOnAllocate(generator);
        free(generator, 3, 5, 4, 0);
        expectAllocations(generator, 3);
        free(generator, 4, 6, 1);
        expectAllocations(generator, 4, 5, 1);
        expectExceptionOnAllocate(generator);
    }

    private void expectAllocations(IdGenerator generator, int... expectedResults)
    {
        IntStream.of(expectedResults).forEach(i -> assertThat(generator.allocateId()).isEqualTo(i));
    }

    private void expectExceptionOnAllocate(IdGenerator generator)
    {
        try
        {
            generator.allocateId();
            fail("Should have thrown an exception");
        }
        catch (IllegalStateException ex)
        {
        }
    }

    private void free(IdGenerator generator, int... ids)
    {
        IntStream.of(ids).forEach(generator::freeId);
    }
}
