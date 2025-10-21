package omni.flowspot.core;

/**
 * FlowSpot 简化的 BugPattern 类
 * 提供漏洞类型的基本描述信息
 */
public class FlowSpotBugPattern {
    
    private final String type;
    private final String shortDescription;
    private final String longDescription;
    
    public FlowSpotBugPattern(String type) {
        this.type = type;
        this.shortDescription = generateShortDescription(type);
        this.longDescription = generateLongDescription(type);
    }
    
    /**
     * 获取漏洞类型
     */
    public String getType() {
        return type;
    }
    
    /**
     * 获取简短描述
     */
    public String getShortDescription() {
        return shortDescription;
    }
    
    /**
     * 获取详细描述
     */
    public String getLongDescription() {
        return longDescription;
    }
    
    /**
     * 根据类型生成简短描述
     */
    private String generateShortDescription(String type) {
        if (type == null) {
            return "Unknown Vulnerability";
        }
        
        // 移除 FLOWSPOT_ 前缀并格式化
        String cleanType = type.replaceFirst("^FLOWSPOT_", "");
        
        // 将下划线替换为空格并转换为标题格式
        String[] words = cleanType.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1));
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 根据类型生成详细描述
     */
    private String generateLongDescription(String type) {
        if (type == null) {
            return "Unknown security vulnerability detected by FlowSpot.";
        }
        
        String lowerType = type.toLowerCase();
        
        // 根据类型提供具体的描述
        if (lowerType.contains("sql") || lowerType.contains("injection")) {
            return "Potential SQL injection vulnerability where user input may be used to construct SQL queries without proper sanitization.";
        } else if (lowerType.contains("xss") || lowerType.contains("cross_site")) {
            return "Potential cross-site scripting (XSS) vulnerability where user input may be reflected in web pages without proper encoding.";
        } else if (lowerType.contains("command") || lowerType.contains("exec")) {
            return "Potential command injection vulnerability where user input may be used to execute system commands.";
        } else if (lowerType.contains("path") || lowerType.contains("traversal")) {
            return "Potential path traversal vulnerability where user input may be used to access files outside the intended directory.";
        } else if (lowerType.contains("deserial")) {
            return "Potential unsafe deserialization vulnerability where untrusted data may be deserialized.";
        } else if (lowerType.contains("ldap")) {
            return "Potential LDAP injection vulnerability where user input may be used in LDAP queries without proper sanitization.";
        } else {
            return String.format("Security vulnerability of type '%s' detected by FlowSpot static analysis.", 
                               generateShortDescription(type));
        }
    }
    
    @Override
    public String toString() {
        return String.format("FlowSpotBugPattern{type='%s', shortDescription='%s'}", 
                           type, shortDescription);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FlowSpotBugPattern that = (FlowSpotBugPattern) obj;
        return type != null ? type.equals(that.type) : that.type == null;
    }
    
    @Override
    public int hashCode() {
        return type != null ? type.hashCode() : 0;
    }
}
