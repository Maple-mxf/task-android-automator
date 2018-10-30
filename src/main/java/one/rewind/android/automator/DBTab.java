package one.rewind.android.automator;

import one.rewind.android.automator.route.PublicAccountsHandler;

/**
 * Create By  2018/10/24
 * Description:
 * <p>
 * Because of the program of accounts from java client to send ,The program can not
 * Know a few account,so must Tag the current data flag
 *
 * @see PublicAccountsHandler#postAccounts
 */

public class DBTab {

	/**
	 * The each page size
	 */
	public static final int SIZE = 20;

	/**
	 * The current page,The start is zero
	 */
	public static int current = 0;


}
