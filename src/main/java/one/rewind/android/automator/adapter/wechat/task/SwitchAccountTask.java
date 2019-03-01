package one.rewind.android.automator.adapter.wechat.task;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.db.Daos;
import one.rewind.db.exception.DBInitException;

import java.sql.SQLException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class SwitchAccountTask extends Task {


    public WeChatAdapter adapter;

    public Account account;

    /**
     * 构造方法
     *
     * @param h
     * @param params
     * @throws IllegalParamsException
     */
    public SwitchAccountTask(TaskHolder h, String... params) throws IllegalParamsException {
        super(h, params);

        accountPermitStatuses.add(Account.Status.Get_Public_Account_Essay_List_Frozen);
        accountPermitStatuses.add(Account.Status.Search_Public_Account_Frozen);

        int accountId = Integer.valueOf(params[0]);
        try {
            Dao<Account, String> accountDao = Daos.get(Account.class);
            account = accountDao.queryBuilder().where().eq("id", accountId).queryForFirst();
            if (account == null) throw new IllegalParamsException("Error Account is null");

        } catch (DBInitException | SQLException e) {
            logger.error("Error DB Init failure, e", e);
        }
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
    public Boolean call() throws InterruptedException {

        try {
            RC("启动微信");
            adapter.restart();

            RC("检查加载的账号是否与登录的账号是否一致");
            adapter.checkAccount();

            RC("启动微信");
            adapter.restart();

            RC("退出微信");
            adapter.logout();

//            RC("为Adapter.Account赋值");  // logout
            adapter.account = this.account;

            RC("登录微信");
            this.adapter.login();

            RC("任务完成");
            success();
        } catch (AccountException.Broken broken) {

            logger.error("Error Account[{}] state is broken, ", this.adapter.account.id, broken);

            RC("任务失败!");
            failure(broken);
            return false;
        } catch (AdapterException.LoginScriptError loginScriptError) {
            logger.error("Error Account[{}] state is broken, ", this.adapter.account.id, loginScriptError);

            RC("任务失败!");
            failure(loginScriptError);
            return false;
        }

        return true;
    }
}
