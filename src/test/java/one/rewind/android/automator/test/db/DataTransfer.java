package one.rewind.android.automator.test.db;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class DataTransfer {

    private Connection oldConn;

    private Connection newConn;

    public DataTransfer() throws SQLException {
        oldConn = DriverManager.getConnection("jdbc:mysql://10.0.0.171:3306/raw", "root", "root");
        newConn = DriverManager.getConnection("jdbc:mysql://10.0.0.171:3306/raw", "root", "root");
    }

    @Test
    public void testTransferData() {

    }

    class TransferTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            oldConn.prepareStatement("select * from essays limit ?,?");
            return null;
        }
    }

}
