package one.rewind.android.automator.task;

import java.util.concurrent.Callable;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public abstract class Task implements Callable<Boolean> {



	public abstract Boolean call() throws Exception;

}
