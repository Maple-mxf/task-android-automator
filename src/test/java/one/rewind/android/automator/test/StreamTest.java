package one.rewind.android.automator.test;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class StreamTest {

    @Test
    public void testOptional() {
        List<String> var0 = ImmutableList.of("yellow", "green", "red");

        Optional<String> op = var0.stream().filter(v -> v.equals("red")).findFirst();

        op.ifPresent(System.out::println);
    }
}
