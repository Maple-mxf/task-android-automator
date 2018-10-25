package one.rewind.android.automator.test.model;

import one.rewind.android.automator.model.WechatFriend;
import org.junit.Test;

public class ModelTest {

    @Test
    public void testAddWeixinFriend() {

        try {

            WechatFriend wf = new WechatFriend(
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
