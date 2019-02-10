package one.rewind.android.automator.task;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.callback.TaskCallback;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.db.RedissonAdapter;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.exception.ModelException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RTopic;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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

    private Adapter adapter;

    public TaskHolder h;

    private int logCount = 0;

    //
    public List<Account.Status> accountPermitStatuses = new ArrayList<>();

    // 任务完成回调
    public List<TaskCallback> doneCallbacks = new ArrayList<>();

    // 任务成功回调
    public List<TaskCallback> successCallbacks = new ArrayList<>();

    // 任务失败回调
    public List<TaskCallback> failureCallbacks = new ArrayList<>();

	/**
	 * 构造方法
	 * @param h
	 * @param params
	 * @throws IllegalParamsException
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

	/**
	 *
	 * @param adapter
	 * @return
	 */
	public abstract Task setAdapter(Adapter adapter);

	/**
	 *
	 * @return
	 */
	public abstract Adapter getAdapter();

	/**
	 * 任务的生命周期
	 *
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws AccountException.NoAvailableAccount
	 * @throws AccountException.Broken
	 * @throws AdapterException.LoginScriptError
	 * @throws AdapterException.IllegalStateException
	 * @throws AdapterException.NoResponseException
	 * @throws DBInitException
	 * @throws SQLException
	 */
    public abstract Boolean call()
            throws InterruptedException, // 任务中断
            IOException, //
            AccountException.NoAvailableAccount, // 没有可用账号
            AccountException.Broken, // 账号不可用
            AdapterException.LoginScriptError, // Adapter 逻辑出错
            AdapterException.IllegalStateException, // Adapter 状态有问题 多数情况下是 逻辑出错
            AdapterException.NoResponseException, // App 没有响应
            DBInitException, // 数据库初始化问题
			SQLException;

	/**
	 *
	 * @param tc
	 * @return
	 */
	public Task addDoneCallback(TaskCallback tc) {
        this.doneCallbacks.add(tc);
        return this;
    }

	/**
	 *
	 * @param tc
	 * @return
	 */
	public Task addSuccessCallback(TaskCallback tc) {
        this.successCallbacks.add(tc);
        return this;
    }

	/**
	 *
	 * @param tc
	 * @return
	 */
	public Task addFailureCallback(TaskCallback tc) {
        this.failureCallbacks.add(tc);
        return this;
    }

    /**
     * @throws AdapterException.OperationException
     * @throws AccountException.NoAvailableAccount
     */
    public void checkAccountStatus(Adapter adapter) throws
			AccountException.NoAvailableAccount,
			AdapterException.LoginScriptError,
			InterruptedException,
			SQLException,
			DBInitException {

        // TODO 判断 adapter != null
        if (!accountPermitStatuses.contains(adapter.account.status)) {
            adapter.switchAccount(accountPermitStatuses.toArray(new Account.Status[accountPermitStatuses.size()]));
        }
    }

	/**
	 *
	 * @return
	 */
	public String getInfo() {
    	return this.getClass().getSimpleName() + "-" + h.id.substring(0,8);
	}

	/**
	 *
	 * @return
	 */
	public int getRetryCount() {
		return h.retry_count;
	}

	/**
	 *
	 */
	public void addRetryCount() {
		h.retry_count ++;
	}

	/**
	 *
	 */
	public void success() {
		h.success = true;
		try {
			h.upsert();
		} catch (DBInitException | SQLException | ModelException.ClassNotEqual | IllegalAccessException ex) {
			logger.error("Error insert task holder, ", ex);
		}
	}

	/**
	 *
	 * @param e
	 */
	public void failure(Throwable e) {

		failure(e, null);
	}

	public void failure(Throwable e, String msg) {

		logger.error("[{}] [{}]" + (msg != null? " " + msg : "") + ",", getAdapter().getInfo(), getInfo(), msg, e);

		try {

			h.success = false;
			h.error = Arrays.stream(e.getStackTrace()).map(ste -> ste.toString()).collect(Collectors.joining("\n"));
			h.upsert();

			TaskRecord record = new TaskRecord(this);
			record.content = e.getClass().getName();
			record.insert();

		} catch (DBInitException | SQLException | ModelException.ClassNotEqual | IllegalAccessException ex) {
			logger.error("Error insert record, ", ex);
		}
	}


	/**
	 * @param c
	 */
	public void RC(String c) {
		try {
			TaskRecord record = new TaskRecord(this);
			logger.info("[{}] [{}] {}", getAdapter().getInfo(), getInfo(), (logCount ++) + " " + c);
			record.content = c;
			record.insert();
		} catch (DBInitException | SQLException e) {
			logger.error("Error insert record, ", e);
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

    // TODO 此处应该在线程池中执行
    public void runCallbacks(List<Runnable> callbacks) {
        callbacks.forEach(c -> new Thread(c).start());
    }

}
