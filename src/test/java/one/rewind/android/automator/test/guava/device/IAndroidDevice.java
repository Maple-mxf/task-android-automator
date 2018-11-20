package one.rewind.android.automator.test.guava.device;

import com.google.common.util.concurrent.AbstractIdleService;

/**
 * Create By 2018/11/20
 * Description:
 */
public class IAndroidDevice extends AbstractIdleService {

    @Override
    protected void startUp() throws Exception {
        int a = 1;

        boolean flag = true;

        while (flag) {
            System.out.println("i: " + a);
            a++;
            if (a == 1000000) {
                flag = false;
            }
        }
    }

    @Override
    protected void shutDown() throws Exception {
    }


    public static void main(String[] args) {
        new IAndroidDevice().startAsync();
    }
}
