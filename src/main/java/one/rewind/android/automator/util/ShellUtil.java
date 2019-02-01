package one.rewind.android.automator.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ShellUtil {

    /**
     * @param fileLac
     */
    public static void exeCall(String fileLac) {
        Runtime rt = Runtime.getRuntime();
        Process p = null;
        try {
            p = rt.exec(fileLac);
        } catch (Exception e) {
            System.out.println("open failure");
        }
    }

    /**
     * @param commandStr
     */
    public static String exeCmd(String commandStr) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(commandStr);
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            p.waitFor(2000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }

    }
}
