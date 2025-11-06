package omni.flowspot.annotations;

/**
 * 扩展的FlowSpotSourceLineAnnotation类，添加了pattern属性
 * 用于存储匹配的模式信息
 * 完全独立于 SpotBugs
 */
public class FlowSpotEnhancedSourceLineAnnotation extends FlowSpotSourceLineAnnotation {
    
    private String pattern;
    private String methodName;
    

    public FlowSpotEnhancedSourceLineAnnotation(String className, String sourceFile, int startLine, int endLine, int startBytecode, int endBytecode) {
        super(className, sourceFile, startLine, endLine, startBytecode, endBytecode);
    }
    
    /**
     * 从FlowSpotSourceLineAnnotation创建增强版本
     */
    public static FlowSpotEnhancedSourceLineAnnotation fromFlowSpotSourceLineAnnotation(FlowSpotSourceLineAnnotation original, String pattern) {
        FlowSpotEnhancedSourceLineAnnotation enhanced = new FlowSpotEnhancedSourceLineAnnotation(
            original.getClassName(),
            original.getSourceFile(),
            original.getStartLine(),
            original.getEndLine(),
            original.getStartBytecode(),
            original.getEndBytecode()
        );
        
        // 复制FlowSpot特定属性
        enhanced.setIdentifierName(original.getIdentifierName());
        enhanced.setPattern(pattern);
        
        return enhanced;
    }
    
    /**
     * 获取匹配的模式
     */
    public String getPattern() {
        return pattern;
    }
    
    /**
     * 设置匹配的模式
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    /**
     * 获取方法名
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * 设置方法名
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
