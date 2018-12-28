package one.rewind.android.automator.test.db.biz;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class UserInterfaceImpl implements UserInterface {

	@Override
	public String say() {
		System.out.println("Hello world");
		return "N号码";
	}
}
