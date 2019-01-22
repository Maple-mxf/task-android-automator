package one.rewind.android.automator.ocr;

import jodd.io.FileUtil;
import one.rewind.json.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author maxuefeng [m17793873123@163.com]
 * tesseract弊端:不可以识别出颜色比较暗的文字
 * tesseract  <image path> <out name> -l chi_sim hocr(附带坐标的command)
 * github wiki:https://github.com/tesseract-ocr/tesseract/wiki/Command-Line-Usage
 */
public class TesseractOCRParser implements OCRParser {

    public static TesseractOCRParser instance;

    /**
     * 单例模式
     *
     * @return
     */
    public static TesseractOCRParser getInstance() {

        if (instance == null) {
            synchronized (TesseractOCRParser.class) {
                if (instance == null) {
                    instance = new TesseractOCRParser();
                }
            }
        }
        return instance;
    }


    /**
     * @param filePath 文件原始路径
     * @return 返回图片文字坐标
     * @throws Exception IOException cropException
     */
    @Override
    public List<TouchableTextArea> getTextBlockArea(String filePath, boolean crop) throws IOException, InterruptedException {

        // 1 裁剪图片
        File inImage = new File(filePath);

        if (crop) {
            BufferedImage bufferedImage = OCRParser.cropImage(ImageIO.read(inImage));

            // 覆盖原有图片  TODO 第二个参数formatName设置为png文件是否会变名字
            ImageIO.write(bufferedImage, "png", new File(inImage.getAbsolutePath()));

        }

        // 2 灰度化图片
//		ImageUtil.grayImage(filePath, filePath, "png");

        // 3 tesseract图像识别

        // 4 解析tesseract out 得到Java Object
        List<TouchableTextArea> textAreas = parseHtml(parse2Html(inImage));

        logger.info(JSON.toJson(textAreas));

        // 5 删除.hocr文件
        FileUtil.deleteFile(filePath.replace(".png", ".hocr"));

        return textAreas;
    }

    /**
     * 具体调用命令
     * tesseract /usr/local/java-workplace/wechat-android-automator/data/wx.jpeg wx -l chi_sim hocr
     *
     * @param file
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static String parse2Html(File file) throws IOException, InterruptedException {

        String fileName = file.getName();

        int index = fileName.lastIndexOf(".");

        String filePrefix = fileName.substring(0, index);

        String pathPrefix = file.getAbsolutePath().replace(fileName, "");

        // tesseract command
        // tesseract <img_dir> <output_name> <options>
        // 附带文字坐标command
        String command = "tesseract " + file.getAbsolutePath() + " " + pathPrefix + filePrefix + "  -l chi_sim hocr"; // chi_sim 是字体参数
        /*String command = "tesseract " + file.getAbsolutePath() + " " + pathPrefix + filePrefix + "  -l chi hocr";*/

        Process process = Runtime.getRuntime().exec(command);

        // 确认命令执行完毕
        process.waitFor();

        // 识别得出文字集合

        return one.rewind.util.FileUtil.readFileByLines(file.getAbsolutePath().replace(fileName, "") + filePrefix + ".hocr");
    }

    /**
     * @param title 每一行文字
     * @return 当前行文字内容及其坐标
     */
    private Rectangle parseRectangle(String title) {

        // 利用封号进行分解
        String[] result = title.split(";");
        if (result.length < 2) return null;

        String prefix = result[0];

        // 利用空格分割所有的坐标
        String[] nodes = prefix.split(" ");

        int[] point = new int[4];

        int index = 0;

        for (String node : nodes) {
            if (NumberUtils.isDigits(node.trim())) {
                point[index++] = Integer.parseInt(node);
            }
        }

        return new Rectangle(point[0], point[1], point[2], point[3]);
    }


    /**
     * <p>
     * TODO:目前为止解析出来的数据位置信息没有任何特征(除了日期标记之外)  原生HTML文件中可以利用p标签将图片中的文字进行分段落
     * </p>
     *
     * @param source html源码
     */
    public List<TouchableTextArea> parseHtml(String source) {

        List<TouchableTextArea> textAreas = new LinkedList<>();

        Document document = Jsoup.parse(source);
        Elements spanLine = document.getElementsByClass("ocr_line");

        for (Element el : spanLine) {

            // 当前行
            StringBuffer line = new StringBuffer();
            Elements words = el.getElementsByClass("ocrx_word");

            int length = words.size();
            // 文字坐标位置
            Rectangle rectangle = null;

            for (int i = 0; i < length; i++) {
                Element wordEl = words.get(i);
                if (i == 0) {
                    // 存储坐标
                    // 获取当前span的title属性
                    rectangle = parseRectangle(wordEl.attr("title"));
                }

                if (StringUtils.isNotBlank(wordEl.text())) {
                    line.append(wordEl.text());
                }
            }

            if (StringUtils.isNotBlank(line.toString())) {

                try {
                    textAreas.add(new TouchableTextArea(line.toString(), rectangle));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return textAreas;
    }

    /**
     * 由于默认的解析方法会把两行标题解析成两个文本框，此时需要根据顺序关系和坐标关系，对文本框进行合并
     *
     * @param originalTextAreas 初始解析的文本框列表
     * @return 合并后的文本框列表
     */
    private List<TouchableTextArea> mergeForTitle(List<TouchableTextArea> originalTextAreas, int gap) throws ParseException {

        List<TouchableTextArea> newTextAreas = new LinkedList<>();

        TouchableTextArea lastArea = null;

        // 遍历初始获得的TextArea
        for (TouchableTextArea area : originalTextAreas) {

            if (lastArea != null) {

                // 判断是否与之前的TextArea合并
                if (area.left == lastArea.left && (area.top - (lastArea.top + lastArea.height)) < gap) {
                    lastArea = lastArea.add(area);
                } else {
                    newTextAreas.add(area);
                    lastArea = area;
                }

            } else {
                newTextAreas.add(area);
                lastArea = area;
            }
        }

        return newTextAreas;
    }
}
