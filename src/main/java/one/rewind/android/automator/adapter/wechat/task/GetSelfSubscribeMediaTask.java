package one.rewind.android.automator.adapter.wechat.task;

import com.dw.ocr.client.OCRClient;
import com.dw.ocr.parser.OCRParser;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.exception.MediaException;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.adapter.wechat.util.PublicAccountInfo;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.data.raw.model.Media;
import one.rewind.db.exception.DBInitException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 获取当前订阅的公众号列表
 *
 * @author scisaga@gmail.com
 * @date 2019/1/19
 */
public class GetSelfSubscribeMediaTask extends Task {

    public WeChatAdapter adapter;

    private List<String> accountSubscribedMediaNicks = new ArrayList<>();

    /**
     * @param holder
     * @param params
     */
    public GetSelfSubscribeMediaTask(TaskHolder holder, String... params) throws IllegalParamsException {

        super(holder, params);

		accountPermitStatuses.add(Account.Status.Get_Public_Account_Essay_List_Frozen);
		accountPermitStatuses.add(Account.Status.Search_Public_Account_Frozen);
    }

	@Override
	public Task setAdapter(Adapter adapter) {
    	this.adapter = (WeChatAdapter) adapter;
		return this;
	}

	@Override
	public Adapter getAdapter() {
		return this.adapter;
	}

    @Override
    public Boolean call() throws
			InterruptedException, // 任务中断
			IOException, //
			AccountException.NoAvailableAccount, // 没有可用账号
			AccountException.Broken, // 账号不可用
			AdapterException.LoginScriptError, // Adapter 逻辑出错
			AdapterException.IllegalStateException, // Adapter 状态有问题 多数情况下是 逻辑出错
			AdapterException.NoResponseException, // App 没有响应
			SQLException,
			DBInitException
    {



		RC("读取已经保存的数据库记录");
		try {
			accountSubscribedMediaNicks = WechatAccountMediaSubscribe.getSubscribeMediaIds(adapter.account.id);
		} catch (Exception e) {
			logger.error("Error get Account[{}] subscribed Media, ", adapter.account.id, e);
		}

		RC("判断帐号状态");
		checkAccountStatus(adapter);

		RC("启动APP");
		adapter.restart();

		RC("检查加载的当前账号是否与登录的一直");
		adapter.checkAccount();

		RC("启动APP");
		adapter.restart();

		RC("进入已订阅公众号的列表页面");
		adapter.goToSubscribePublicAccountList();

		// 最后一页
		boolean atBottom = false;

		while (!atBottom) {

			RC("- 获取当前页截图");
			List<OCRParser.TouchableTextArea> publicAccountTitles = OCRClient.getInstance()
					.getTextBlockArea(adapter.device.screenshot(), 240, 332, 1356, 2392);

			RC("- 解析公众号名称所在位置坐标");
			for (OCRParser.TouchableTextArea area : publicAccountTitles) {

				RC("-- 当前文字坐标 --> " + area.toJSON());
				if (area.content.matches("\\d+个公众号")) {
					RC("-- 已经到公众号列表底部");
					atBottom = true;
					break;
				}

				/*if(area.content.matches("该帐号已冻结|该帐号已注销")) {
					// 取消关注
					RC("公众号状态异常 取消订阅");
					adapter.goToSubscribedPublicAccountHome(area.left + 10, area.top + 10);
					adapter.unsubscribePublicAccount();
					continue;
				}*/

				// 当前任务去重 + 已经保存记录去重 + 不完整文本区域 过滤
				if (h.findings.contains(area.content)
						|| accountSubscribedMediaNicks.contains(area.content)
						|| area.top + area.height > 2392
						|| area.height < 40) continue;

				RC("--- 进入公众号Home页");
				adapter.goToSubscribedPublicAccountHome(area.left + 10, area.top + 10);

				RC("--- 查看公众号更多资料 获取PublicAccountInfo");
				PublicAccountInfo pai;
				try {
					pai = this.adapter.getPublicAccountInfo(false, true);
				}
				catch (MediaException.Illegal illegal) {

					RC("--- 公众号状态异常 取消订阅");

					try {
						adapter.unsubscribePublicAccount(area.content);
					} catch (MediaException.NotSubscribe notSubscribe) {
						logger.warn("[{}] not subscribe {}, ", adapter.getInfo(), area.content, notSubscribe);
					}

					h.findings.add(area.content);
					continue;
				}

				if (
					// 由于图像识别结果 和 实际结果有可能不一致 此处需要再次去重
					!h.findings.contains(pai.nick) && !accountSubscribedMediaNicks.contains(pai.nick)
				) {

					h.findings.add(area.content);

					// 公众号名称图像识别结果 与 实际获取结果 不相同
					if(!pai.nick.equals(area.content)) {
						h.findings.add(pai.nick);
					}

					try {
						RC("--- 生成Media");
						Media media = GetMediaEssaysTask.parseMedia(pai);
						media.update();

						RC("--- 记录Account订阅的Media信息");
						WechatAccountMediaSubscribe wams = new WechatAccountMediaSubscribe(this.adapter.account.id, media.id, media.name, media.nick);
						wams.insert();
					} catch (SQLException e) {
						logger.warn("Insert PublicAccount records, ", e);
					}
				}

				RC("--- 返回公众号列表页面");
				adapter.device.goBack();
				Thread.sleep(1000);
				adapter.device.goBack();
				Thread.sleep(1000);

				adapter.status = WeChatAdapter.Status.Subscribe_PublicAccount_List;
			}

			RC("- Slide down");
			this.adapter.device.slideToPoint(1000, 1600, 1000, 400, 2000);
		}

		RC("任务完成");

		success();

        return false;
    }
}
