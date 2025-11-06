package omni.flowspot.project;

/**
 * FlowSpot 项目统计信息类
 * 替代 edu.umd.cs.findbugs.ProjectStats，完全独立于 SpotBugs
 */
public class FlowSpotProjectStats {
    
    private int totalClasses = 0;
    private int analyzedClasses = 0;
    private int totalMethods = 0;
    private int analyzedMethods = 0;
    private long analysisTime = 0;
    private int bugCount = 0;
    private String projectName;
    private final long startTime;
    
    public FlowSpotProjectStats() {
        this.startTime = System.currentTimeMillis();
    }
    
    public FlowSpotProjectStats(String projectName) {
        this();
        this.projectName = projectName;
    }
    
    /**
     * 获取项目名称
     */
    public String getProjectName() {
        return projectName;
    }
    
    /**
     * 设置项目名称
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    /**
     * 获取总类数
     */
    public int getTotalClasses() {
        return totalClasses;
    }
    
    /**
     * 设置总类数
     */
    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }
    
    /**
     * 获取已分析类数
     */
    public int getAnalyzedClasses() {
        return analyzedClasses;
    }
    
    /**
     * 设置已分析类数
     */
    public void setAnalyzedClasses(int analyzedClasses) {
        this.analyzedClasses = analyzedClasses;
    }
    
    /**
     * 增加已分析类数
     */
    public void incrementAnalyzedClasses() {
        this.analyzedClasses++;
    }
    
    /**
     * 获取总方法数
     */
    public int getTotalMethods() {
        return totalMethods;
    }
    
    /**
     * 设置总方法数
     */
    public void setTotalMethods(int totalMethods) {
        this.totalMethods = totalMethods;
    }
    
    /**
     * 获取已分析方法数
     */
    public int getAnalyzedMethods() {
        return analyzedMethods;
    }
    
    /**
     * 设置已分析方法数
     */
    public void setAnalyzedMethods(int analyzedMethods) {
        this.analyzedMethods = analyzedMethods;
    }
    
    /**
     * 增加已分析方法数
     */
    public void incrementAnalyzedMethods() {
        this.analyzedMethods++;
    }
    
    /**
     * 获取分析时间（毫秒）
     */
    public long getAnalysisTime() {
        return analysisTime;
    }
    
    /**
     * 设置分析时间（毫秒）
     */
    public void setAnalysisTime(long analysisTime) {
        this.analysisTime = analysisTime;
    }
    
    /**
     * 完成分析，计算总时间
     */
    public void finishAnalysis() {
        this.analysisTime = System.currentTimeMillis() - startTime;
    }
    
    /**
     * 获取漏洞数量
     */
    public int getBugCount() {
        return bugCount;
    }
    
    /**
     * 设置漏洞数量
     */
    public void setBugCount(int bugCount) {
        this.bugCount = bugCount;
    }
    
    /**
     * 增加漏洞数量
     */
    public void incrementBugCount() {
        this.bugCount++;
    }
    
    /**
     * 获取开始时间
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * 获取分析进度百分比
     */
    public double getProgress() {
        if (totalClasses == 0) {
            return 0.0;
        }
        return (double) analyzedClasses / totalClasses * 100.0;
    }
    
    /**
     * 检查分析是否完成
     */
    public boolean isAnalysisComplete() {
        return totalClasses > 0 && analyzedClasses >= totalClasses;
    }
    
    /**
     * 获取统计摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("FlowSpot Analysis Statistics:\n");
        if (projectName != null) {
            sb.append("  Project: ").append(projectName).append("\n");
        }
        sb.append("  Classes: ").append(analyzedClasses).append("/").append(totalClasses).append("\n");
        sb.append("  Methods: ").append(analyzedMethods).append("/").append(totalMethods).append("\n");
        sb.append("  Bugs Found: ").append(bugCount).append("\n");
        sb.append("  Analysis Time: ").append(analysisTime).append(" ms");
        if (analysisTime > 0) {
            sb.append(" (").append(String.format("%.2f", analysisTime / 1000.0)).append(" seconds)");
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("FlowSpotProjectStats[project=%s, classes=%d/%d, methods=%d/%d, bugs=%d, time=%dms]",
                           projectName, analyzedClasses, totalClasses, analyzedMethods, totalMethods, bugCount, analysisTime);
    }
}
