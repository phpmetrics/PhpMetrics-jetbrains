package hal.phpmetrics.idea.runner;

public interface ResultListener {
    void onSuccess(String output);
    void onError(String error, String output, int exitCode);
}
