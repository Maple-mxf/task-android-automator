package one.rewind.android.automator.adapter;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/25
 */
public interface Action<A extends Adapter, V> {

	abstract V run(A adapter, String... params) throws Exception;

}
