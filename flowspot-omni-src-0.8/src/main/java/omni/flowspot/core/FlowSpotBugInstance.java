package omni.flowspot.core;

import omni.flowspot.annotations.FlowSpotSourceLineAnnotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * FlowSpot 独立的漏洞实例类
 * 完全独立于 SpotBugs，不继承任何 SpotBugs 类
 */
public class FlowSpotBugInstance {
    private final String category;
    private final String type;
    private final int priority;
    private final List<FlowSpotSourceLineAnnotation> annotations;
    private String message;
    private String bugStack;  // HTML 格式的漏洞堆栈信息
    private final long timestamp;
    
    public FlowSpotBugInstance(String category,String type, int priority) {
        this.category = category;
        this.type = type;
        this.priority = priority;
        this.annotations = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取漏洞分类
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * 获取漏洞类型
     */
    public String getType() {
        return type;
    }
    
    /**
     * 获取优先级
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * 获取消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 设置消息
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * 获取漏洞堆栈信息
     */
    public String getBugStack() {
        return bugStack;
    }
    
    /**
     * 设置漏洞堆栈信息（HTML 格式）
     */
    public void setBugStack(String bugStack) {
        this.bugStack = bugStack;
    }
    
    /**
     * 获取 BugPattern 信息（简化版本）
     * 返回一个包含基本描述信息的对象
     */
    public FlowSpotBugPattern getBugPattern() {
        return new FlowSpotBugPattern(type);
    }
    
    /**
     * 获取时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 添加注解
     */
    public void add(FlowSpotSourceLineAnnotation annotation) {
        if (annotation != null) {
            annotations.add(annotation);
        }
    }
    
    /**
     * 获取所有注解
     */
    public Collection<FlowSpotSourceLineAnnotation> getAnnotations() {
        return new ArrayList<>(annotations);
    }
    
    /**
     * 获取主要源代码行注解
     */
    public FlowSpotSourceLineAnnotation getPrimarySourceLineAnnotation() {
        return annotations.isEmpty() ? null : annotations.get(0);
    }
    
    /**
     * 添加 FlowSpotSourceLineAnnotation 到此漏洞实例
     */
    public FlowSpotBugInstance addFlowSpotSourceLine(FlowSpotSourceLineAnnotation sourceLineAnnotation) {
        add(sourceLineAnnotation);
        return this;
    }
    
    /**
     * 获取注解数量
     */
    public int getAnnotationCount() {
        return annotations.size();
    }
    
    /**
     * 检查是否有注解
     */
    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }
    
    /**
     * 清除所有注解
     */
    public void clearAnnotations() {
        annotations.clear();
    }
    
    @Override
    public String toString() {
        return String.format("FlowSpotBugInstance[type=%s, priority=%d, annotations=%d, timestamp=%d]", 
                           type, priority, annotations.size(), timestamp);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FlowSpotBugInstance that = (FlowSpotBugInstance) obj;
        return priority == that.priority &&
               timestamp == that.timestamp &&
               type.equals(that.type) &&
               java.util.Objects.equals(message, that.message);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, priority, message, timestamp);
    }
}
