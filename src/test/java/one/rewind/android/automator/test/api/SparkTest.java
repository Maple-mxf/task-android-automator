package one.rewind.android.automator.test.api;

import org.junit.Test;

import static spark.Spark.get;

/**
 * Create By 2018/11/21
 * Description:
 */
public class SparkTest {



    @Test
    public void testStart(){
        get("/hello", (req, res) -> "Hello World");
    }
}
