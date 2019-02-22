package one.rewind.android.automator.ocr.test;

import com.dw.ocr.client.OCRClient;
import com.dw.ocr.parser.OCRParser;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.json.JSON;
import one.rewind.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/10
 */
public class OCRTest {

	@Test
	public void testGetSearchResult1() throws IOException {

		List<OCRParser.TouchableTextArea> textAreas = OCRClient.getInstance().getTextBlockArea(FileUtil.readBytesFromFile("tmp/1.png"), 240, 255, 1400, 2380);
//		List<OCRParser.TouchableTextArea> textAreas = OCRClient.getInstance().getTextBlockArea(FileUtil.readBytesFromFile("tmp/1.png"));

		System.err.println(JSON.toPrettyJson(textAreas));

	}

	@Test
	public void testGetSearchResult2() throws IOException {

		List<OCRParser.TouchableTextArea> textAreas = OCRClient.getInstance().getTextBlockArea(FileUtil.readBytesFromFile("tmp/screenshots/media_search_result.png"), 250, 430, 1400, 2390);

		textAreas = Adapter.mergeForTitle(textAreas, 70);

		System.err.println(JSON.toPrettyJson(textAreas));
	}

	public static void main(String[] args) throws IOException {
	}

}
