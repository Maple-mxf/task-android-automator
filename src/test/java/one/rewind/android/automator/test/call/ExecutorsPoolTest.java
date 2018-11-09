package one.rewind.android.automator.test.call;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ExecutorsPoolTest {


    public static void main(String[] args) {
        ReentrantReadWriteLock var = new ReentrantReadWriteLock();

        var.isWriteLocked();

        var.readLock().tryLock();

        var.writeLock().tryLock();
    }

}
