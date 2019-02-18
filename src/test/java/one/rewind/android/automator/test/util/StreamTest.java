package one.rewind.android.automator.test.util;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Set;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class StreamTest {

    @Test
    public void testSetMap() {
        Set<ITmp> allAdapters = Sets.newConcurrentHashSet();

        ITmp tmp = new ITmp();

        allAdapters.add(tmp);

        for (ITmp adapter : allAdapters) {
            adapter.APITask = true;
        }
        allAdapters.forEach(v -> System.out.println(v.APITask));
    }


    class ITmp {
        public boolean APITask = false;
    }
}
