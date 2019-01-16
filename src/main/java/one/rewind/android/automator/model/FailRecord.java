package one.rewind.android.automator.model;


import one.rewind.db.model.ModelL;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
@Deprecated
public class FailRecord extends ModelL {

    public FailRecord() {
    }

    /**
     * 已经抓取文章的数量
     */
    public int finishNum;

    /**
     * 下一次抓取需要滑动多少页
     */
    public int slideNumByPage;

    /**
     * 当前微信公众号的名称
     */
    public String mediaName;
}
