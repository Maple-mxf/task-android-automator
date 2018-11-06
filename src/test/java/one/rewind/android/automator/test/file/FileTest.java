package one.rewind.android.automator.test.file;

import org.junit.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
public class FileTest {


    @Test
    public void testWorkspacePath() {
        String property = System.getProperty("user.dir");
        System.out.println(property);
        File file = new File(property);
        boolean b = file.canRead();
        System.out.println(b);
    }

    @Test
    public void testImageCount() {
        File file = new File("");
        Date tmp = new Date();

        SimpleDateFormat df = new SimpleDateFormat("yyyy--MM-dd");

        String format = df.format(tmp);

        if (file.isDirectory()) {
            int count = 0;
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {

                long lastModified = files[i].lastModified();

                Date date = new Date(lastModified);

                if (df.format(date).equals(format)) {
                    count++;
                }

            }
            System.out.println(count);
        }
    }
}
