package one.rewind.android.automator.test.util;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CollectionsTest {

    @Test
    public void testSync(){
        Set<Object> objects = Collections.synchronizedSet(new HashSet<>());
        System.out.println("hhh");
    }
}
