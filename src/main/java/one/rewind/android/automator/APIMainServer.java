package one.rewind.android.automator;

import one.rewind.android.automator.route.PublicAccountsHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static spark.Spark.*;

/**
 * Create By  2018/10/18
 * Description: route
 */

public class APIMainServer {

	public static final Logger logger = LogManager.getLogger(APIMainServer.class.getName());

	private static APIMainServer Instance;

	public static APIMainServer getInstance() {

		if (Instance == null) {

			synchronized (APIMainServer.class) {
				if (Instance == null) {
					Instance = new APIMainServer();
				}
			}

		}

		return Instance;
	}


	/**
	 *
	 */
	private APIMainServer() {

		port(8080);

	}

	/**
	 * @return
	 */
	@SuppressWarnings("JavaDoc")
	public APIMainServer initRoutes() {

		path("/api", () -> {
			post("/accounts", PublicAccountsHandler.postAccounts);

			post("/recovery", PublicAccountsHandler.recovery);
		});

		return this;
	}

	public static void main(String[] args) {
		APIMainServer.getInstance().initRoutes();
	}

}
