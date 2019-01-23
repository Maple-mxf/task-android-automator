package one.rewind.android.automator.task.wechat;

import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.data.raw.model.Platform;
import one.rewind.txt.StringUtil;

import java.io.IOException;

/**
 * 订阅公众号
 *
 * @author scisaga@gmail.com
 */
public class SubscribeMediaTask extends Task {

    // 点击无响应重试上限
    public static final int MAX_ATTEMPTS = 5;

    public static Platform platform;

    static {
        try {
            platform = new Platform("微信公众号平台", "WX");
            platform.id = 1;
            platform.insert();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public SubscribeMediaTask(TaskHolder holder, String... params) {

		super(holder, params);
	}

    @Override
    public Boolean call() throws AdapterException.NoResponseException, AdapterException.OperationException,
			AdapterException.IllegalStateException, AccountException.NoAvailableAccount, InterruptedException, IOException {
        return null;
    }

    public static String genId(String nick) {
        return StringUtil.MD5(platform.short_name + "-" + nick);
    }

    /**
     * save subscribe record
     *
     * @param mediaName media
     * @param topic     redis topic
     */
//	private void saveSubscribeRecord(String mediaName, String topic) {
//		try {
//			long tempCount = Tab.subscribeDao.queryBuilder().where()
//					.eq("media_name", mediaName)
//					.countOf();
//			if (tempCount == 0) {
//				AccountMediaSubscribe e = new AccountMediaSubscribe();
//				e.udid = adapter.device.udid;
//				e.media_name = mediaName;
//				e.number = 100;
//				e.retry_count = 0;
//				e.status = AccountMediaSubscribe.State.NOT_FINISH.status;
//				e.topic = topic;
//				e.relative = 1;
//				e.insert();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

}
