package one.rewind.android.automator.test.guava.util;

import org.junit.Test;

import java.util.Optional;

/**
 * Create By 2018/11/21
 * Description:
 */
public class OptionTest {

    @Test
    public void testNullPoint() {
        Optional<Integer> var = Optional.of(5);
        System.out.println(var.isPresent());
    }
}
