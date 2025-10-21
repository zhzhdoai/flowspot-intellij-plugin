/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * FlowSpotProject.java
 *
 * Created on March 30, 2003, 2:22 PM
 * Modified for FlowSpot integration
 */

package omni.flowspot.project;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

// 移除 SpotBugs SourceFinder 依赖

/**
 * FlowSpot Project class - extends the functionality of SpotBugs Project
 * for FlowSpot-specific features.
 *
 * A FlowSpotProject represents all of the source/class files, aux classpath entries,
 * source directories, etc. for a project to be analyzed for bugs.
 *
 * <p>
 * This class also supports the plugin.xml file format written by the
 * FindBugs Eclipse plugin.
 * </p>
 *
 * @author David Hovemeyer
 * @author FlowSpot Team
 */
public class FlowSpotProject implements  AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowSpotProject.class);

    private final List<File> currentWorkingDirectoryList;

    private String projectName;

    private final List<String> fileList;

    private final List<String> auxClasspathEntryList;

    private final List<String> sourceDirList;

    private boolean isGuiAvailable;

    private FlowSpotSourceFinder sourceFinder;

    // FlowSpot specific fields
    private boolean decompile = false;
    private String scanMode = "balanced";
    private Set<String> selectedSourceRules;
    private Set<String> selectedSinkRules;
    private omni.scan.OptimizationConfig optimizationConfig;
    
    // 路径配置字段
    private String analysisTargetPath;  // 被分析的目录路径
    private String baseProjectPath;     // 项目根目录路径（用于配置管理）

    /**
     * Constructor. Creates an empty project.
     */
    public FlowSpotProject() {
        fileList = new LinkedList<>();
        auxClasspathEntryList = new LinkedList<>();
        sourceDirList = new LinkedList<>();
        currentWorkingDirectoryList = new LinkedList<>();
        isGuiAvailable = true;
        selectedSourceRules = new java.util.HashSet<>();
        selectedSinkRules = new java.util.HashSet<>();
        optimizationConfig = new omni.scan.OptimizationConfig(true, true, true);
    }

    /**
     * Set the project name.
     *
     * @param projectName the project name
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Get the project name.
     *
     * @return the project name, or null if no project name has been set
     */
    @CheckForNull
    public String getProjectName() {
        return projectName;
    }

    /**
     * Add a file or directory to the project.
     *
     * @param fileName name of file or directory to add
     */
    public void addFile(String fileName) {
        fileList.add(fileName);
    }

    /**
     * Add an auxiliary classpath entry to the project.
     *
     * @param auxClasspathEntry the auxiliary classpath entry to add
     */
    public void addAuxClasspathEntry(String auxClasspathEntry) {
        auxClasspathEntryList.add(auxClasspathEntry);
    }

    /**
     * Add a source directory to the project.
     *
     * @param sourceDirName name of source directory to add
     */
    public void addSourceDir(String sourceDirName) {
        sourceDirList.add(sourceDirName);
    }

    /**
     * Get the list of files and directories in the project.
     *
     * @return list of files and directories in the project
     */
    public List<String> getFileList() {
        return Collections.unmodifiableList(fileList);
    }

    /**
     * Get a file or directory from the project.
     *
     * @param index index of the file or directory
     * @return the file or directory name
     */
    public String getFile(int index) {
        return fileList.get(index);
    }

    /**
     * Get the number of files and directories in the project.
     *
     * @return number of files and directories in the project
     */
    public int getFileCount() {
        return fileList.size();
    }

    /**
     * Get the list of auxiliary classpath entries.
     *
     * @return list of auxiliary classpath entries
     */
    public List<String> getAuxClasspathEntryList() {
        return Collections.unmodifiableList(auxClasspathEntryList);
    }

    /**
     * Get the list of source directories.
     *
     * @return list of source directories
     */
    public List<String> getSourceDirList() {
        return Collections.unmodifiableList(sourceDirList);
    }

    // FlowSpot specific methods

    /**
     * Set whether decompilation is enabled.
     *
     * @param decompile true if decompilation should be enabled
     */
    public void setDecompile(boolean decompile) {
        this.decompile = decompile;
    }

    /**
     * Get whether decompilation is enabled.
     *
     * @return true if decompilation is enabled
     */
    public boolean getDecompile() {
        return decompile;
    }

    /**
     * Set the scan mode.
     *
     * @param scanMode the scan mode (e.g., "fast", "balanced", "thorough")
     */
    public void setScanMode(String scanMode) {
        this.scanMode = scanMode;
    }

    /**
     * Get the scan mode.
     *
     * @return the scan mode
     */
    public String getScanMode() {
        return scanMode;
    }

    /**
     * Set the selected source rules.
     *
     * @param selectedSourceRules set of selected source rule names
     */
    public void setSelectedSourceRules(Set<String> selectedSourceRules) {
        this.selectedSourceRules = new java.util.HashSet<>(selectedSourceRules);
    }

    /**
     * Get the selected source rules.
     *
     * @return set of selected source rule names
     */
    public Set<String> getSelectedSourceRules() {
        return Collections.unmodifiableSet(selectedSourceRules);
    }

    /**
     * Set the selected sink rules.
     *
     * @param selectedSinkRules set of selected sink rule names
     */
    public void setSelectedSinkRules(Set<String> selectedSinkRules) {
        this.selectedSinkRules = new java.util.HashSet<>(selectedSinkRules);
    }

    /**
     * Get the selected sink rules.
     *
     * @return set of selected sink rule names
     */
    public Set<String> getSelectedSinkRules() {
        return Collections.unmodifiableSet(selectedSinkRules);
    }

    /**
     * Set the optimization configuration.
     *
     * @param optimizationConfig the optimization configuration
     */
    public void setOptimizationConfig(omni.scan.OptimizationConfig optimizationConfig) {
        this.optimizationConfig = optimizationConfig;
    }

    /**
     * Get the optimization configuration.
     *
     * @return the optimization configuration
     */
    public omni.scan.OptimizationConfig getOptimizationConfig() {
        return optimizationConfig;
    }

    /**
     * Get the analysis target path (directory to be analyzed).
     *
     * @return the analysis target path
     */
    public String getAnalysisTargetPath() {
        return analysisTargetPath;
    }

    /**
     * Set the analysis target path (directory to be analyzed).
     *
     * @param analysisTargetPath the analysis target path
     */
    public void setAnalysisTargetPath(String analysisTargetPath) {
        this.analysisTargetPath = analysisTargetPath;
    }

    /**
     * Get the base project path (project root directory for configuration management).
     *
     * @return the base project path
     */
    public String getBaseProjectPath() {
        return baseProjectPath;
    }

    /**
     * Set the base project path (project root directory for configuration management).
     *
     * @param baseProjectPath the base project path
     */
    public void setBaseProjectPath(String baseProjectPath) {
        this.baseProjectPath = baseProjectPath;
    }

    // toSpotBugsProject 方法已移除 - FlowSpot 现在完全独立于 SpotBugs


    @Override
    public void close() throws Exception {
        // Cleanup resources if needed
        if (sourceFinder != null) {
            sourceFinder.close();
        }
        // FlowSpot 项目资源清理完成
    }

    @Override
    public String toString() {
        return "FlowSpotProject{" +
                "projectName='" + projectName + '\'' +
                ", fileCount=" + fileList.size() +
                ", auxClasspathCount=" + auxClasspathEntryList.size() +
                ", sourceDirCount=" + sourceDirList.size() +
                ", decompile=" + decompile +
                ", scanMode='" + scanMode + '\'' +
                ", sourceRules=" + selectedSourceRules.size() +
                ", sinkRules=" + selectedSinkRules.size() +
                ", analysisTargetPath='" + analysisTargetPath + '\'' +
                ", baseProjectPath='" + baseProjectPath + '\'' +
                '}';
    }
}
