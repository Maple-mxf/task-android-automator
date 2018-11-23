package one.rewind.android.automator.test.util;

import one.rewind.android.automator.model.Essays;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

    @Test
    public void testBlockQueue() throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.take();

        System.out.println("hello world");
    }

    @Test
    public void testConcurrentQueue() {
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        System.out.println(queue.poll());
    }

    @Test
    public void testStack() {
        Stack<String> stack = new Stack<>();

//        stack.push("")
//        System.out.println(stack.peek());

        System.out.println(stack.pop());
    }


    public void test2() {
        System.out.println("jjjjjj");
    }

    public void testMethod(Method method, Object o) throws InvocationTargetException, IllegalAccessException {
        method.invoke(o, "hjasdasjd");
    }


    public void test1() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method method = Essays.class.getMethod("parseContent", String.class);

        this.testMethod(method, new Essays());
    }
}
