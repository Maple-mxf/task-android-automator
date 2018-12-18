package one.rewind.android.automator.test.call;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Set;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class ThreadLocalTest {

    @Test
    public void testVar() {
        ThreadLocal<Boolean> var = new ThreadLocal<>();
        System.out.println(var.get());
    }

    @Test
    public void testCollectionThreadLocal() {
        ThreadLocal<Set> var = new ThreadLocal<>();
        var.set(Sets.newHashSet());
        System.out.println(var.get().size());
    }
}
