package one.rewind.android.automator.test.util;

import com.google.common.collect.Lists;
import one.rewind.android.automator.model.Essays;
import org.json.JSONArray;
import org.json.JSONObject;
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
        queue.add("1");
        queue.add("1");
        queue.add("1");
        queue.add("1");
        queue.add("1");
        queue.add("1");
        queue.add("1");
        queue.add("1");
        queue.add("1");
        queue.add("1");
        System.out.println("size: " + queue.size());
        int i = 0;
        for (String var : queue) {
            i++;
            System.out.println("i: " + i);
        }
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

    /**
     * 微信公众号：Java技术栈
     **/
    public static void main(String[] args) {
        Map<String, String> hashtable = new Hashtable<>();
        hashtable.put("t1", "1");
        hashtable.put("t2", "2");
        hashtable.put("t3", "3");

        Enumeration<Map.Entry<String, String>> iterator1 = (Enumeration<Map.Entry<String, String>>) hashtable.entrySet().iterator();
        hashtable.remove(iterator1.nextElement().getKey());
        while (iterator1.hasMoreElements()) {
            System.out.println(iterator1.nextElement());
        }

        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("h1", "1");
        hashMap.put("h2", "2");
        hashMap.put("h3", "3");

        Iterator<Map.Entry<String, String>> iterator2 = hashMap.entrySet().iterator();
        hashMap.remove(iterator2.next().getKey());
        while (iterator2.hasNext()) {
            System.out.println(iterator2.next());
        }
    }

    @Test
    public void testList() {
        ArrayList<String> objects = Lists.newArrayList();
        objects.add("asdad");
        objects.add("asdad");
        objects.add("asdad");
        objects.add("asdad");
        objects.add("asdad");
        String rs = JSONObject.valueToString(objects);
        System.out.println(rs);
        JSONArray array = new JSONArray(rs);
        System.out.println(array);
    }
}
