package one.rewind.android.automator.test.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Create By 2018/11/21
 * Description:
 */
public class JSONTest {

    @Test
    public void testGetObjFromJSON() {
        JSONObject jsonObject = new JSONObject("{\"log_id\": 3691883576612201525, \"words_result_num\": 18, \"words_result\": [{\"location\": {\"width\": 318, \"top\": 10, \"height\": 62, \"left\": 1100}, \"words\": \"∠B427\"}, {\"location\": {\"width\": 968, \"top\": 267, \"height\": 71, \"left\": 40}, \"words\": \"何子煜:3.15避险与加息多空因素交\"}, {\"location\": {\"width\": 617, \"top\": 346, \"height\": 80, \"left\": 40}, \"words\": \"织,日内黄金操作建议\"}, {\"location\": {\"width\": 511, \"top\": 458, \"height\": 55, \"left\": 43}, \"words\": \"2018年3月15日(原创)\"}, {\"location\": {\"width\": 875, \"top\": 630, \"height\": 85, \"left\": 40}, \"words\": \"何子煜:3.14黄金跌破楔形上轨,\"}, {\"location\": {\"width\": 430, \"top\": 720, \"height\": 70, \"left\": 46}, \"words\": \"内黄金操作建议\"}, {\"location\": {\"width\": 510, \"top\": 828, \"height\": 56, \"left\": 45}, \"words\": \"2018年3月14日(原创)\"}, {\"location\": {\"width\": 218, \"top\": 641, \"height\": 261, \"left\": 1143}, \"words\": \"划卡\"}, {\"location\": {\"width\": 1290, \"top\": 1000, \"height\": 98, \"left\": 37}, \"words\": \"黄金多头拐点要来了?2018312黄元\"}, {\"location\": {\"width\": 732, \"top\": 1091, \"height\": 70, \"left\": 42}, \"words\": \"金走势分析操作建议附解套\"}, {\"location\": {\"width\": 511, \"top\": 1200, \"height\": 56, \"left\": 43}, \"words\": \"2018年3月12日(原创)\"}, {\"location\": {\"width\": 1291, \"top\": 1371, \"height\": 100, \"left\": 33}, \"words\": \"何子煜:美指大涨,为何黄金未大元记\"}, {\"location\": {\"width\": 697, \"top\": 1461, \"height\": 71, \"left\": 42}, \"words\": \"跌?3.9日内黄金操作建议\"}, {\"location\": {\"width\": 463, \"top\": 1570, \"height\": 56, \"left\": 43}, \"words\": \"2018年3月9日(原创\"}, {\"location\": {\"width\": 1360, \"top\": 1752, \"height\": 70, \"left\": 40}, \"words\": \"何子煜:3.8今晚!欧央行利率决议家观点“行货\"}, {\"location\": {\"width\": 671, \"top\": 1833, \"height\": 68, \"left\": 43}, \"words\": \"来袭,日内黄金操作建议\"}, {\"location\": {\"width\": 463, \"top\": 1937, \"height\": 60, \"left\": 45}, \"words\": \"2018年3月8日(原创\"}, {\"location\": {\"width\": 207, \"top\": 2177, \"height\": 58, \"left\": 618}, \"words\": \"已无更多\"}]}");
        Object error_msg = jsonObject.opt("log_id");
        System.out.println(error_msg);
    }

    @Test
    public void testArrayToJSONArray() {
        JSONArray objects = new JSONArray("[{\"media\":\"芋道源码\"},{\"media\":\"淘米网\"}]");
        objects.forEach(System.out::println);
    }

    @Test
    public void testArrayToJSONArray2() {
        JSONArray objects = new JSONArray("[\"芋道源码\",\"淘米网\"]");
        objects.forEach(System.out::println);
    }
}
