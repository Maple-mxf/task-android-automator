package one.rewind.android.automator.test.util;

import com.dw.ocr.util.ImageUtil;
import org.junit.Test;

import java.io.IOException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class ImageutilTest {

    @Test
    public void testCompareImage() throws IOException {
        boolean compareResult = ImageUtil.compareImage("tmp/1.jpg", "tmp/2.jpg");

        System.out.println(compareResult);
    }
}
