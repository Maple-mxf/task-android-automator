package one.rewind.android.automator.model;

public enum TaskType {

    SUBSCRIBE(1),
    CRAWLER(2),
    FINAL(4),
    WAIT(3);


    private int type;

    TaskType(int type) {
        this.type = type;
    }
}
