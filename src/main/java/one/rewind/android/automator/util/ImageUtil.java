package one.rewind.android.automator.util;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class ImageUtil {
    /**
     * 裁剪图片方法
     *
     * @param bufferedImage 图像源
     * @param startX        裁剪开始x坐标  0-1065
     * @param startY        裁剪开始y坐标  56-1918
     * @param endX          裁剪结束x坐标
     * @param endY          裁剪结束y坐标
     * @return BufferedImage
     */
    public static BufferedImage cropImage(BufferedImage bufferedImage, int startX, int startY, int endX, int endY) {

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        if (startX == -1) {
            startX = 0;
        }

        if (startY == -1) {
            startY = 0;
        }

        if (endX == -1) {
            endX = width - 1;
        }

        if (endY == -1) {
            endY = height - 1;
        }

        BufferedImage result = new BufferedImage(endX - startX, endY - startY, 4);

        for (int x = startX; x < endX; ++x) {
            for (int y = startY; y < endY; ++y) {
                int rgb = bufferedImage.getRGB(x, y);
                result.setRGB(x - startX, y - startY, rgb);
            }
        }

        return result;
    }

    /**
     * 将图片进行灰度化 为了方便tesseract识别
     *
     * @param inPath     输入文件
     * @param outPath    输出文件
     * @param formatName 图片文件格式  截图必须为png 其他一般为jpg或者jpeg
     * @throws IOException 读取文件异常
     */
    public static void grayImage(String inPath, String outPath, String formatName) throws IOException {

        File file = new File(inPath);

        BufferedImage image = ImageIO.read(file);

        int width = image.getWidth();

        int height = image.getHeight();

        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);//重点，技巧在这个参数BufferedImage.TYPE_BYTE_GRAY

        for (int i = 0; i < width; i++) {

            for (int j = 0; j < height; j++) {

                int rgb = image.getRGB(i, j);

                grayImage.setRGB(i, j, rgb);
            }
        }

        File newFile = new File(outPath);

        ImageIO.write(grayImage, formatName, newFile);
    }

    /**
     * 图片到byte数组
     *
     * @param path file relative or absolute path; path must contain file suffix;
     * @return byte array
     */
    public static byte[] image2Byte(String path) {

        byte[] data = null;

        FileImageInputStream input;

        try {

            input = new FileImageInputStream(new File(path));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buf = new byte[1024];

            int numBytesRead;

            while ((numBytesRead = input.read(buf)) != -1) {
                output.write(buf, 0, numBytesRead);
            }

            data = output.toByteArray();

            output.close();
            input.close();

        } catch (IOException ex1) {
            ex1.printStackTrace();
        }
        return data;
    }

    /**
     * byte数组到图片
     *
     * @param data string to byte
     * @param path file will be apply path; path must contain file suffix(file type)
     */
    public static void byte2Image(byte[] data, String path) {

        if (data.length < 3 || path.equals("")) return;

        try {

            FileImageOutputStream imageOutput = new FileImageOutputStream(new File(path));

            imageOutput.write(data, 0, data.length);

            imageOutput.close();

            System.out.println("Make Picture success,Please find image in " + path);

        } catch (Exception ex) {

            System.out.println("Exception: " + ex);
            ex.printStackTrace();

        }
    }

    public static String[][] getPX(BufferedImage bi) throws IOException {
        int[] rgb = new int[3];
        int width = bi.getWidth();
        int height = bi.getHeight();
        int minx = bi.getMinX();
        int miny = bi.getMinY();

        String[][] list = new String[width][height];

        for (int i = minx; i < width; i++) {

            for (int j = miny; j < height; j++) {

                int pixel = bi.getRGB(i, j);

                rgb[0] = (pixel & 0xff0000) >> 16;
                rgb[1] = (pixel & 0xff00) >> 8;
                rgb[2] = (pixel & 0xff);

                list[i][j] = rgb[0] + "," + rgb[1] + "," + rgb[2];
            }
        }
        return list;
    }

    public static String[][] getPX(String args) throws IOException {
        File file = new File(args);
        return getPX(ImageIO.read(file));
    }

    /**
     * 比较;两个图片的相似性
     *
     * @param imgPath1
     * @param imgPath2
     * @throws IOException
     */
    public static void compareImage(String imgPath1, String imgPath2) throws IOException {
        String[] images = {imgPath1, imgPath2};
        // 分析图片相似度 begin
        String[][] list1 = getPX(images[0]);
        String[][] list2 = getPX(images[1]);

        int score = 0;
        int busi = 0;

        int i = 0, j = 0;
        for (String[] strings : list1) {

            if ((i + 1) == list1.length) {
                continue;
            }

            for (int m = 0; m < strings.length; m++) {
                try {
                    String[] value1 = list1[i][j].toString().split(",");
                    String[] value2 = list2[i][j].toString().split(",");
                    int k = 0;
                    for (int n = 0; n < value2.length; n++) {
                        if (Math.abs(Integer.parseInt(value1[k]) - Integer.parseInt(value2[k])) < 5) {
                            score++;
                        } else {
                            busi++;
                        }
                    }
                } catch (RuntimeException e) {
                    continue;
                }
                j++;
            }
            i++;
        }

        list1 = getPX(images[1]);
        list2 = getPX(images[0]);
        i = 0;
        j = 0;
        for (String[] strings : list1) {
            if ((i + 1) == list1.length) {
                continue;
            }
            for (int m = 0; m < strings.length; m++) {
                try {
                    String[] value1 = list1[i][j].split(",");
                    String[] value2 = list2[i][j].split(",");

                    int k = 0;

                    for (int n = 0; n < value2.length; n++) {

                        if (Math.abs(Integer.parseInt(value1[k]) - Integer.parseInt(value2[k])) < 5) {
                            score++;
                        } else {
                            busi++;
                        }
                    }
                } catch (RuntimeException e) {
                    continue;
                }
                j++;
            }
            i++;
        }
        String baifen;

        try {

            baifen = ((Double.parseDouble(score + "") / Double.parseDouble((busi + score) + "")) + "");
            baifen = baifen.substring(baifen.indexOf(".") + 1, baifen.indexOf(".") + 3);
        } catch (Exception e) {

            baifen = "0";
        }
        if (baifen.length() <= 0) {

            baifen = "0";
        }
        if (busi == 0) {

            baifen = "100";
        }
    }
}
