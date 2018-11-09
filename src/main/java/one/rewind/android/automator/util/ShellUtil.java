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
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
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

    //进入相应设备的shell
    private static void enterADBShell(String udid) throws IOException {
        String command = "abd -s " + udid + " shell";
        exeCmd(command);
    }


    // reboot
    @Deprecated
    public static void reboot(String udid) throws IOException, InterruptedException {

        enterADBShell(udid);

        exeCmd("reboot");

        Thread.sleep(120000);

        enterADBShell(udid);
        //滑动解锁
        exeCmd("adb shell input swipe 300 1000 300 500");
    }


    public static void shutdownProcess(String udid, String packageName) throws IOException, InterruptedException {

        enterADBShell(udid);

        exeCmd("am force-stop " + packageName);

        Thread.sleep(2000);

        exeCmd("exit");
    }
}
