package omni.flowspot.callback;

/**
 * 进度回调接口
 */
public interface ProgressCallback {
    /**
     * 更新消息
     * @param message 消息内容
     */
    void updateMessage(String message);
    
    /**
     * 更新进度
     * @param progress 进度百分比 (0-100)
     */
    void updateProgress(int progress);
}
