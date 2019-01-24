package one.rewind.android.automator.test.model;

import one.rewind.android.automator.adapter.wechat.model.WechatContact;
import org.junit.Test;


/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class ModelTest {

    @Test
    public void testAddWeixinFriend() {

        try {

            WechatContact wf = new WechatContact(
                    "123", "1", "", "2", "21");
            wf.insert();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() {
        System.err.println((int) ((100D / 1000D) * 1600));
    }


}
