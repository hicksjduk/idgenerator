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
        IntStream.rangeClosed(1, 5).forEach(i -> assertThat(generator.allocateId()).isEqualTo(i));
        expectExceptionOnAllocate(generator);
        IntStream.of(3, 5, 4, 0).forEach(generator::freeId);
        assertThat(generator.allocateId()).isEqualTo(3);
        IntStream.of(4, 6, 1).forEach(generator::freeId);
        IntStream.of(4, 5, 1).forEach(i -> assertThat(generator.allocateId()).isEqualTo(i));
        expectExceptionOnAllocate(generator);
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
}
