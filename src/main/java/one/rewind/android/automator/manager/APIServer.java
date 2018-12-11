package one.rewind.android.automator.manager;

import com.google.common.base.Strings;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.Tab;
import one.rewind.android.automator.util.DateUtil;
import one.rewind.db.RedissonAdapter;
import one.rewind.io.server.Msg;
import org.json.JSONArray;
import org.json.JSONObject;
import org.redisson.api.RList;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;
import spark.Spark;

/*
 * 存储未完成请求ID集合 requests {n requestID}  完成之后删除此requestID
 * 存储完成任务公众号的集合   requestID_Finish
 * 存储未完成任务公众号集合   requestID_Not_Finish
 */

@Deprecated
public class APIServer {

    static Logger logger = LoggerFactory.getLogger(APIServer.class);

    public static RedissonClient redisClient = RedissonAdapter.redisson;


    private APIServer() {
    }

    public static void main(String[] args) {

        Spark.port(8090);

        // 一： 公众号任务保存
        Spark.post("/push", push);
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

        RQueue<Object> request = redisClient.getQueue(Tab.REQUESTS);

        // 添加请求集合
        request.add(requestID);

        // 创建未完成任务集合
        String noOkTaskQueue = requestID + "_Not_Finish";

        // 创建完成的任务集合
        String okTaskQueue = requestID + "_Finish";

        RList<String> noOKList = redisClient.getList(noOkTaskQueue);

        RList<String> okList = redisClient.getList(okTaskQueue);

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
}
































