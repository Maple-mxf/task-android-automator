package one.rewind.android.automator.util;

import java.io.*;

/**
 *
 */
public class ShellUtil {

    public static void exeCall(String fileLac) {
        Runtime rt = Runtime.getRuntime();
        Process p = null;
        try {
            p = rt.exec(fileLac);
        } catch (Exception e) {
            System.out.println("open failure");
        }
    }

    public static void exeCmd(String commandStr) {

        BufferedReader br = null;
        try {
            Process p = Runtime.getRuntime().exec(commandStr);
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
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
        }
    }
}
