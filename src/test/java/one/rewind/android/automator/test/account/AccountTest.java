package one.rewind.android.automator.test.account;

import one.rewind.android.automator.account.Account;
import org.junit.Test;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class AccountTest {

    @Test
    public void testInsertAccount() throws Exception {
        Account account = new Account();
        account.username = "无名小姐";
        account.src_id = "wxid_ix9ftbi4tub522";
        account.mobile = "17063880226";
        account.password = "123456abc";
        account.udid = "ZX1G426B3V";
        account.adapter_class_name = "one.rewind.android.automator.adapter.wechat.WeChatAdapter";
        account.status = Account.Status.Normal;
        account.occupied = false;
        account.insert();
    }
}
