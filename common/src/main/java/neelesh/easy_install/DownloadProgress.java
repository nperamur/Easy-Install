package neelesh.easy_install;

public class DownloadProgress {
    private volatile float progress = 0;

    private boolean showProgress = false;

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public boolean isShowingProgress() {
        return showProgress;
    }

    public void setShowingProgress(boolean showProgress) {
        this.showProgress = showProgress;
    }
}