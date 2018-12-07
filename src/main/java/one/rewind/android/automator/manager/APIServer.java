package one.rewind.android.automator.manager;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.Tab;
import one.rewind.android.automator.util.DateUtil;
import one.rewind.db.RedissonAdapter;
import one.rewind.io.server.Msg;
import org.json.JSONArray;
import org.json.JSONObject;
import org.redisson.api.RList;
import org.redisson.api.RQueue;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;
import spark.Spark;

import java.util.List;
import java.util.Set;

/*
 * 存储未完成请求ID集合 requests {n requestID}  完成之后删除此requestID
 * 存储完成任务公众号的集合   requestID_Finish
 * 存储未完成任务公众号集合   requestID_Not_Finish
 *
 *
 *
 */


public class APIServer {

    static Logger logger = LoggerFactory.getLogger(APIServer.class);

    public static RedissonClient redissonClient = RedissonAdapter.redisson;


    private APIServer() {
    }

    public static void main(String[] args) {

        Spark.port(8090);

        // 一： 公众号任务保存
        Spark.post("/push", push);

        // 二：根据req_id拿到已完成的任务

        Spark.post("/medias", medias);


    }


    // 1 生成请求ID
    // 2 操作redis 记录数据
    // 3 响应用户数据

    private static Route push = (req, resp) -> {
        String body = req.body();

        if (Strings.isNullOrEmpty(body)) return new Msg<>(0, "请检查您的参数！");

        JSONObject result = new JSONObject(body);

        JSONArray mediasArray = result.getJSONArray("medias");

        if (mediasArray == null || mediasArray.length() == 0) return new Msg<>(0, "请检查您的参数！");

        String requestID = Tab.REQUEST_ID_PREFIX + DateUtil.timestamp();

        RQueue<Object> request = redissonClient.getQueue(Tab.REQUESTS);

        // 添加请求集合
        request.add(requestID);

        // 创建未完成任务集合
        String noOkTaskQueue = requestID + "_Not_Finish";

        // 创建完成的任务集合
        String okTaskQueue = requestID + "_Finish";

        RList<String> noOKList = redissonClient.getList(noOkTaskQueue);

        RList<String> okList = redissonClient.getList(okTaskQueue);

        // 公众号添加到redis集合中
        for (Object tmpVar : mediasArray) {

            String tmp = (String) tmpVar;

            SubscribeMedia media = Tab.subscribeDao.queryBuilder().where().eq("media_name", tmp).queryForFirst();
            if (media != null) {

                // media可能是历史任务  也可能当前media的任务已经完成了

                // 使用requestID作为redis的key   value存放一个有序集合

                // 已经完成了任务，将当前的公众号名称存储到redis中

                // media的状态可能是Finish(任务在DB中已经存在且完成) 也可能是NOT_EXIST(不存在)

                if (media.status == SubscribeMedia.State.FINISH.status || media.status == SubscribeMedia.State.NOT_EXIST.status) {

                    logger.info("公众号{}加入okSet,状态为:{}", media.media_name, media.status);

                    okList.add(media.media_name);
                } else {

                    // status: 0 未完成   但是已经订阅
                    logger.info("公众号{}已经订阅！任务尚未完成，状态为{}", media.media_name, media.status);
                    media.request_id = requestID;
                    media.update();
                }
            } else {
                // noOKSet   media_name + requestID   阿里巴巴+req_idasdsadas
                noOKList.add(tmp + requestID);
                logger.info("公众号{}加入notOkSet", tmp);
            }
        }

        return new Msg<>(1, requestID);
    };


    // 1 任务完成 删除requests中的requestID
    // 2 删除未完成任务的集合
    // 3 完成任务的集合不需要动


    /**
     * 根据requestID获取已经完成的任务
     * <p>
     * template {"request_id":"req_id_20181207161440726"}
     */
    public static Route medias = (req, resp) -> {
        JSONObject requestJSON = new JSONObject(req.body());
        String request_id = requestJSON.getString("request_id");

        String setName = request_id + "_Finish";
        RSet<String> result = redissonClient.getSet(setName);

        if (!result.isExists()) return new Msg<>(0, "请求不存在，请检查您的参数！");

        if (result.size() == 0) return new Msg<>(1, "任务还未完成，请耐心等待！");

        Set<String> var = result.readAll();

        return new Msg<>(0, var);
    };


    /**
     * 根据media获取到文章数据
     * <p>
     * template: {"medias":["阿里巴巴","阿里妈妈","支付宝","蚂蚁金服"]}
     */
    public static Route essays = (req, resp) -> {

        JSONObject jsonObject = new JSONObject(req.body());

        JSONArray medias = jsonObject.getJSONArray("medias");

        List<Essays> rs = Lists.newArrayList();

        for (Object media : medias) {
            String mediaName = (String) media;
            List<Essays> tmpEssays = Tab.essayDao.queryBuilder().where().eq("media_nick", mediaName).query();
            rs.addAll(tmpEssays);
        }
        return new Msg<>(1, rs);
    };

}
































