package one.rewind.android.automator.exception;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class AndroidException extends Exception {
    public AndroidException(String message) {
        super(message);
    }

    public static class AndroidInitException extends AndroidException {
        public AndroidInitException(String message) {
            super(message);
        }
    }

    public static class AndroidCollapseException extends AndroidException {
        public AndroidCollapseException(String message) {
            super(message);
        }
    }

    public static class IllegalStatusException extends Exception {

    }
}
