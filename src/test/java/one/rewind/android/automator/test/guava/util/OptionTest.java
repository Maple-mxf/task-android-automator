package one.rewind.android.automator.test.guava.util;

import org.junit.Test;

import java.util.Optional;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class OptionTest {

    @Test
    public void testNullPoint() {
        Optional<Integer> var = Optional.of(5);
        System.out.println(var.isPresent());
    }
}
