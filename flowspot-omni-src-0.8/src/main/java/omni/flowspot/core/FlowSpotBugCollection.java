package omni.flowspot.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * FlowSpot 独立的漏洞集合类
 * 替代 edu.umd.cs.findbugs.SortedBugCollection
 */
public class FlowSpotBugCollection implements Iterable<FlowSpotBugInstance> {
    
    private final List<FlowSpotBugInstance> bugInstances;
    private final String projectName;
    private final long timestamp;
    
    public FlowSpotBugCollection(String projectName) {
        this.projectName = projectName;
        this.bugInstances = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 添加漏洞实例
     */
    public void add(FlowSpotBugInstance bugInstance) {
        if (bugInstance != null) {
            bugInstances.add(bugInstance);
        }
    }
    
    /**
     * 添加多个漏洞实例
     */
    public void addAll(Collection<FlowSpotBugInstance> instances) {
        if (instances != null) {
            bugInstances.addAll(instances);
        }
    }
    
    /**
     * 获取所有漏洞实例
     */
    public Collection<FlowSpotBugInstance> getCollection() {
        return new ArrayList<>(bugInstances);
    }
    
    /**
     * 获取漏洞数量
     */
    public int size() {
        return bugInstances.size();
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return bugInstances.isEmpty();
    }
    
    /**
     * 清空所有漏洞
     */
    public void clear() {
        bugInstances.clear();
    }
    
    /**
     * 获取项目名称
     */
    public String getProjectName() {
        return projectName;
    }
    
    /**
     * 获取创建时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取按类型分组的统计信息
     */
    public java.util.Map<String, Integer> getTypeStatistics() {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        for (FlowSpotBugInstance bug : bugInstances) {
            String type = bug.getType();
            stats.put(type, stats.getOrDefault(type, 0) + 1);
        }
        return stats;
    }
    
    /**
     * 获取按优先级分组的统计信息
     */
    public java.util.Map<Integer, Integer> getPriorityStatistics() {
        java.util.Map<Integer, Integer> stats = new java.util.HashMap<>();
        for (FlowSpotBugInstance bug : bugInstances) {
            int priority = bug.getPriority();
            stats.put(priority, stats.getOrDefault(priority, 0) + 1);
        }
        return stats;
    }
    
    @Override
    public Iterator<FlowSpotBugInstance> iterator() {
        return bugInstances.iterator();
    }
    
    @Override
    public String toString() {
        return String.format("FlowSpotBugCollection[project=%s, size=%d, timestamp=%d]", 
                           projectName, size(), timestamp);
    }
}
