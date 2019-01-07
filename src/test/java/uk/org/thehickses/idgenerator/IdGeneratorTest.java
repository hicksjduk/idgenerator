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
        try
        {
            generator.allocateId();
            fail("Should have thrown an exception");
        }
        catch (IllegalStateException ex)
        {
        }
        IntStream.of(3, 4, 0).forEach(generator::freeId);
        assertThat(generator.allocateId()).isEqualTo(3);
        IntStream.of(6, 1).forEach(generator::freeId);
        assertThat(generator.allocateId()).isEqualTo(4);
        assertThat(generator.allocateId()).isEqualTo(1);
    }

}
