package omni.flowspot.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * FlowSpot 源文件查找器
 * 替代 edu.umd.cs.findbugs.ba.SourceFinder，完全独立于 SpotBugs
 */
public class FlowSpotSourceFinder implements AutoCloseable {
    
    private final List<String> sourceDirs;
    private final List<String> sourceFiles;
    
    public FlowSpotSourceFinder() {
        this.sourceDirs = new ArrayList<>();
        this.sourceFiles = new ArrayList<>();
    }
    
    /**
     * 添加源代码目录
     */
    public void addSourceDir(String sourceDir) {
        if (sourceDir != null && !sourceDirs.contains(sourceDir)) {
            sourceDirs.add(sourceDir);
        }
    }
    
    /**
     * 添加源代码文件
     */
    public void addSourceFile(String sourceFile) {
        if (sourceFile != null && !sourceFiles.contains(sourceFile)) {
            sourceFiles.add(sourceFile);
        }
    }
    
    /**
     * 获取所有源代码目录
     */
    public List<String> getSourceDirs() {
        return new ArrayList<>(sourceDirs);
    }
    
    /**
     * 获取所有源代码文件
     */
    public List<String> getSourceFiles() {
        return new ArrayList<>(sourceFiles);
    }
    
    /**
     * 查找指定类的源文件
     */
    public String findSourceFile(String className) {
        if (className == null) {
            return null;
        }
        
        // 将类名转换为文件路径
        String relativePath = className.replace('.', '/') + ".java";
        
        // 在源代码目录中查找
        for (String sourceDir : sourceDirs) {
            File sourceFile = new File(sourceDir, relativePath);
            if (sourceFile.exists() && sourceFile.isFile()) {
                try {
                    return sourceFile.getCanonicalPath();
                } catch (IOException e) {
                    // 继续查找其他目录
                }
            }
        }
        
        // 在源文件列表中查找
        for (String sourceFile : sourceFiles) {
            if (sourceFile.endsWith(relativePath)) {
                return sourceFile;
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否有源代码信息
     */
    public boolean hasSourceInfo() {
        return !sourceDirs.isEmpty() || !sourceFiles.isEmpty();
    }
    
    /**
     * 获取源代码目录数量
     */
    public int getSourceDirCount() {
        return sourceDirs.size();
    }
    
    /**
     * 获取源代码文件数量
     */
    public int getSourceFileCount() {
        return sourceFiles.size();
    }
    
    /**
     * 清空所有源代码信息
     */
    public void clear() {
        sourceDirs.clear();
        sourceFiles.clear();
    }
    
    /**
     * 检查指定目录是否存在
     */
    public boolean isValidSourceDir(String sourceDir) {
        if (sourceDir == null) {
            return false;
        }
        
        File dir = new File(sourceDir);
        return dir.exists() && dir.isDirectory();
    }
    
    /**
     * 检查指定文件是否存在
     */
    public boolean isValidSourceFile(String sourceFile) {
        if (sourceFile == null) {
            return false;
        }
        
        File file = new File(sourceFile);
        return file.exists() && file.isFile() && file.getName().endsWith(".java");
    }
    
    @Override
    public void close() throws Exception {
        // 清理资源
        clear();
    }
    
    @Override
    public String toString() {
        return String.format("FlowSpotSourceFinder[dirs=%d, files=%d]", 
                           sourceDirs.size(), sourceFiles.size());
    }
}
