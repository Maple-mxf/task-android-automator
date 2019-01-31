package one.rewind.android.automator.task;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.callback.TaskCallback;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.db.RedissonAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RTopic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 任务重试
 * adapter异常处理
 * 任务执行
 *
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public abstract class Task implements Callable<Boolean> {

    public static final Logger logger = LogManager.getLogger(Task.class.getName());

    public Adapter adapter;

    public TaskHolder h;

    // Flag 在call()和调用方法中，显式调用，判断任务是否继续执行
    public volatile boolean stop = false;

    public List<Account.Status> accountPermitStatuses = new ArrayList<>();

    // 任务完成回调
    public List<TaskCallback> doneCallbacks = new ArrayList<>();

    // 任务成功回调
    public List<TaskCallback> successCallbacks = new ArrayList<>();

    // 任务失败回调
    public List<TaskCallback> failureCallbacks = new ArrayList<>();

    /**
     * @param h
     */
    public Task(TaskHolder h, String... params) throws IllegalParamsException {

        this.h = h;
        accountPermitStatuses.add(Account.Status.Normal);

        // 设定任务成功回调
        addSuccessCallback((t) -> {
            t.h.success = true;

        });

        // 设定任务失败回调
        addFailureCallback((t) -> {
            t.h.success = false;
        });

        // 设定任务完成回调
        addDoneCallback((t) -> {

            if (t.h.topic_name == null) return;

            // 发布消息
            RTopic<Object> topic = RedissonAdapter.redisson.getTopic(t.h.topic_name);
            topic.publish(t.h);

            // 记录Holder
            try {
                t.h.update_time = new Date();
                t.h.insert();
            } catch (Exception e) {
                logger.error("Error insert h, ", e);
            }
        });
    }

    // 任务的生命周期
    public abstract Boolean call()
            throws InterruptedException, // 任务中断
            IOException, //
            AccountException.NoAvailableAccount, // 没有可用账号
            AccountException.Broken, // 账号不可用
            AdapterException.OperationException, // Adapter 逻辑出错
            AdapterException.IllegalStateException, // Adapter 状态有问题 多数情况下是 逻辑出错
            AdapterException.NoResponseException // App 没有响应
    ;

    public Task addDoneCallback(TaskCallback tc) {
        this.doneCallbacks.add(tc);
        return this;
    }

    public Task addSuccessCallback(TaskCallback tc) {
        this.successCallbacks.add(tc);
        return this;
    }

    public Task addFailureCallback(TaskCallback tc) {
        this.failureCallbacks.add(tc);
        return this;
    }

    /**
     * @throws AdapterException.OperationException
     * @throws AccountException.NoAvailableAccount
     */
    public void checkAccountStatus() throws AccountException.NoAvailableAccount, AdapterException.OperationException {

        // TODO 判断 adapter 不能为null
        if (!accountPermitStatuses.contains(adapter.account.status)) {

            boolean switchAccount = false;
            int retryCount = 0;

            while (retryCount < 2 && !switchAccount) {

                Account account = Account.getAccount(adapter.device.udid, adapter.getClass().getName(), accountPermitStatuses);

                if (account != null) {

                    try {
                        adapter.switchAccount(account);
                        switchAccount = true;
                    } catch (AccountException.Broken broken) {
                        logger.warn("Account[{}] broken, ", account.id, broken);
                        try {
                            broken.account.update();
                        } catch (Exception e1) {
                            logger.error("Account[{}] update failure, ", account.id, e1);
                        }
                    }
                    // TODO  InterruptedException
                    catch (InterruptedException e) {

                    }

                } else {
                    // 找不到可用账号
                    throw new AccountException.NoAvailableAccount();
                }

                retryCount++;
            }
        }
    }

    /**
     * 参数异常
     */
    public static class IllegalParamsException extends Exception {
        public IllegalParamsException(String params) {
            super(params);
        }
    }

    public void runCallbacks(List<Runnable> callbacks) {
        callbacks.forEach(c -> new Thread(c).start());
    }
}
