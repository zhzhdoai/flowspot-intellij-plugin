# FlowSpot Plugin New Features

## 1. 规则选择对话框 (Rule Selection Dialog)

### 功能描述
点击 "Analyze Project Files" 或 "Analyze Selected Files" 时，会弹出规则选择对话框，允许用户自定义选择要扫描的 sources 和 sinks 规则。

### 主要特性
- **双标签页设计**：Sources 和 Sinks 分别在不同标签页中
- **预定义规则**：包含常见的安全扫描规则
- **批量操作**：支持全选/全不选功能
- **规则统计**：显示选中规则的数量统计

### 预定义规则列表

#### Sources 规则
- HttpServletRequest.getParameter
- HttpServletRequest.getHeader
- HttpServletRequest.getCookies
- System.getProperty
- System.getenv
- Scanner.nextLine
- BufferedReader.readLine
- File.getName
- URL.getQuery
- Properties.getProperty

#### Sinks 规则
- Statement.executeQuery
- Statement.execute
- PreparedStatement.executeQuery
- Connection.prepareStatement
- Runtime.exec
- ProcessBuilder.command
- File.delete
- FileWriter.write
- PrintWriter.write
- Response.getWriter
- LDAP.search
- XPath.evaluate

### 使用方法
1. 点击 "FlowSpot > Analyze Project Files" 菜单项
2. 在弹出的对话框中选择需要的 Sources 和 Sinks 规则
3. 点击 "OK" 开始分析
4. 如果没有选择任何规则，分析将被取消

## 2. 优化的UI布局 (Improved UI Layout)

### 功能描述
重新设计了 FlowSpot 工具窗口的布局，提供更好的用户体验和信息展示。

### 布局特点
- **上下分割**：主要内容区域采用垂直分割
- **上半部分**：左侧显示分析概览和统计信息，右侧显示 Data Flow 详情
- **下半部分**：显示漏洞树列表
- **动态统计**：实时显示分析结果统计信息

### UI组件

#### 分析概览面板 (Analysis Overview)
- **欢迎信息**：显示 FlowSpot 安全分析标题
- **使用指导**：提供操作指引
- **统计信息**：分析完成后显示：
  - 总漏洞数量
  - 按严重程度分类统计（高/中/低）
  - 彩色图标和文字提示

#### Data Flow 面板 (右侧)
- 显示选中漏洞的数据流信息
- 支持多标签页展示不同类型的详情
- 包含漏洞的完整描述和修复建议

#### FlowSpot Results 面板 (下方)
- 树形结构显示所有发现的漏洞
- 支持按分类、类型、文件等方式组织
- 点击漏洞项目可在右侧查看详情

### 统计信息展示
```
Analysis Results
Total Vulnerabilities: 5

🔴 High: 2
🟡 Medium: 2  
🔵 Low: 1

Select a vulnerability below to view details
```

## 3. 集成的分析流程 (Integrated Analysis Workflow)

### 功能描述
将规则选择完全集成到分析流程中，支持自定义规则的安全扫描。

### 工作流程
1. **规则选择** → 用户在对话框中选择规则
2. **参数传递** → 选中的规则传递给分析引擎
3. **自定义分析** → 分析引擎根据选中规则执行扫描
4. **结果展示** → 在优化的UI中展示分析结果
5. **统计更新** → 实时更新统计信息和进度

### 进度显示
- 主进度条：显示整体分析进度
- 副进度条：显示规则选择摘要
- 状态文本：显示当前分析阶段

### 日志记录
- 记录用户选择的规则信息
- 记录分析过程和结果统计
- 支持调试和问题排查

## 4. 技术实现细节

### 核心类
- `FlowSpotRuleSelectionDialog`: 规则选择对话框
- `FlowSpotToolWindowPanel`: 主工具窗口面板
- `FlowSpotAnalysisEngine`: 分析引擎（支持自定义规则）
- `AnalyzeProjectFilesAction`: 项目分析操作
- `AnalyzeSelectedFilesAction`: 文件分析操作

### 数据流
```
用户操作 → 规则选择对话框 → 分析操作 → 分析引擎 → 结果展示
```

### 兼容性
- 支持 IntelliJ IDEA 2023.3+
- 兼容 Java 21
- 支持 Gradle 8.12

## 使用建议

1. **首次使用**：建议选择所有规则进行全面扫描
2. **性能考虑**：对于大型项目，可以选择特定类型的规则
3. **增量分析**：针对特定安全问题，选择相关的 sinks 规则
4. **结果分析**：优先关注高严重程度的漏洞

## 故障排除

### 常见问题
1. **规则选择对话框不显示**：检查项目是否正确加载
2. **分析无结果**：确认选择了至少一个 source 和 sink 规则
3. **UI布局异常**：尝试重新打开 FlowSpot 工具窗口

### 日志位置
分析日志保存在项目的 `.omni` 目录中，文件名格式为 `flowspot-analysis-YYYYMMDD-HHMMSS.log`。
