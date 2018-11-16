package one.rewind.android.automator.test.util;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CollectionsTest {

    @Test
    public void testSync() {
        Set<Object> objects = Collections.synchronizedSet(new HashSet<>());
        System.out.println("hhh");
    }

    @Test
    public void testQueue() {
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        queue.add("one");
        queue.add("two");
        queue.add("three");
        queue.add("four");
        int length = queue.size();
        for (int i = 0; i < length; i++) {
            System.out.println(queue.peek());
        }
    }
}
