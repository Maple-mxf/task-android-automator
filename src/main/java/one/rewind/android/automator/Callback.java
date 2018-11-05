package one.rewind.android.automator;


import one.rewind.android.automator.exception.AndroidCollapseException;

import java.util.Queue;

public interface Callback {

    void onFailure(AndroidCollapseException e, Queue<String> queue);

    void onSuccess();

}
