# FlowSpot IntelliJ Plugin

FlowSpot IntelliJ Plugin 是一个专门为 FlowSpot 安全漏洞检测工具设计的 IntelliJ IDEA 插件。该插件提供了完整的安全漏洞分析功能，包括实时代码分析、漏洞展示和源代码导航。

## 项目概述

这是一个完全独立的 FlowSpot 插件项目，不依赖于 SpotBugs 框架，专注于提供最佳的 FlowSpot 安全分析体验。

### 主要特性

- 🔍 **安全漏洞检测**: 使用 FlowSpot 引擎进行深度安全分析
- 🎯 **精确定位**: 准确定位漏洞在源代码中的位置
- 📊 **分类展示**: 按分类、类型、文件和严重程度组织漏洞
- 🚀 **实时分析**: 支持项目级和文件级的实时分析
- 📝 **详细报告**: 提供完整的漏洞描述和修复建议
- 🔗 **代码导航**: 一键跳转到漏洞源代码位置

## 架构设计

### 核心组件

```
flowspot-plugin/
├── actions/          # 分析操作
│   ├── AnalyzeProjectFilesAction.java
│   ├── AnalyzeSelectedFilesAction.java
│   ├── FlowSpotStopAction.java
│   └── FlowSpotClearAction.java
├── core/             # 核心分析引擎
│   ├── FlowSpotAnalysisEngine.java
│   ├── FlowSpotVulnerabilityConverter.java
│   ├── FlowSpotLogManager.java
│   └── FlowSpotToolWindowFactory.java
├── gui/              # UI组件
│   └── FlowSpotToolWindowPanel.java
├── model/            # 数据模型
│   ├── FlowSpotVulnerability.java
│   ├── FlowSpotLocation.java
│   ├── FlowSpotAnnotation.java
│   └── FlowSpotVulnerabilityCollection.java
└── service/          # 服务层
    └── FlowSpotResultsPublisher.java
```

### 数据流程

```
TestScan.doAnalysis() (FlowSpot 核心引擎)
    ↓
SortedBugCollection
    ↓
FlowSpotBugInstance (原始数据)
    ↓
FlowSpotVulnerabilityConverter (转换层)
    ↓
FlowSpotVulnerabilityCollection (UI 数据模型)
    ↓
MessageBus (异步发布)
    ↓
FlowSpotToolWindowPanel (UI 展示)
```

## 技术栈

- **开发语言**: Java 21
- **构建工具**: Gradle 8.12
- **IDE 平台**: IntelliJ Platform 2023.3+
- **分析引擎**: FlowSpot Omni 0.8 (通过 TestScan.doAnalysis)
- **UI 框架**: Swing (IntelliJ Platform)
- **核心集成**: TestScan.scala 提供完整的漏洞分析功能

## 快速开始

### 环境要求

- JDK 21+
- IntelliJ IDEA 2023.3+
- Gradle 8.12+

### 构建项目

```bash
# 克隆项目
cd flowspot-plugin

# 构建插件
./gradlew build

# 运行插件
./gradlew runIde
```

### 安装插件

1. 构建完成后，在 `build/distributions/` 目录下找到插件 ZIP 文件
2. 在 IntelliJ IDEA 中，打开 `Settings > Plugins`
3. 点击齿轮图标，选择 `Install Plugin from Disk...`
4. 选择构建的 ZIP 文件并安装
5. 重启 IntelliJ IDEA

## 使用指南

### 基本操作

1. **分析项目文件**
   - 菜单: `FlowSpot > Analyze Project Files`
   - 快捷键: `Shift+Ctrl+Alt+P`

2. **分析选中文件**
   - 菜单: `FlowSpot > Analyze Selected Files`
   - 快捷键: `Shift+Ctrl+Alt+S`

3. **查看分析结果**
   - 分析完成后，FlowSpot 工具窗口会自动显示
   - 漏洞按分类组织显示
   - 点击漏洞可查看详细信息

4. **停止分析**
   - 使用工具栏中的停止按钮
   - 或使用 `FlowSpot > Stop Analysis` 菜单

### 工具窗口

FlowSpot 工具窗口提供以下功能：

- **状态显示**: 显示当前分析状态
- **结果展示**: 按分类显示检测到的漏洞
- **统计信息**: 显示漏洞总数、分类数等统计信息
- **详细信息**: 每个漏洞的详细描述和位置信息

## 配置选项

### Gradle 配置

在 `gradle.properties` 中配置：

```properties
# IntelliJ IDEA 安装路径
idea.home.path=/Applications/IntelliJ IDEA CE.app/Contents

# 平台版本
platformType=IC
platformVersion=2023.3.8
```

### 插件配置

插件支持以下配置选项：

- **分析范围**: 项目级或文件级分析
- **规则选择**: 支持动态选择 source 和 sink 规则
- **日志级别**: 可配置日志详细程度

## 开发指南

### 项目结构

```
flowspot-plugin/
├── build.gradle              # 构建配置
├── gradle.properties         # Gradle 属性
├── settings.gradle          # 项目设置
├── src/main/
│   ├── java/                # Java 源代码
│   └── resources/           # 资源文件
│       ├── META-INF/
│       │   └── plugin.xml   # 插件配置
│       └── icons/           # 图标资源
└── src/test/               # 测试代码
```

### 扩展开发

1. **添加新的分析操作**
   - 继承 `AnAction` 类
   - 在 `plugin.xml` 中注册操作

2. **自定义 UI 组件**
   - 扩展现有的 GUI 组件
   - 实现 `FlowSpotResultsPublisher` 接口

3. **集成新的分析引擎**
   - 实现 `FlowSpotAnalysisEngine` 接口
   - 更新 `FlowSpotVulnerabilityConverter`

### 测试

```bash
# 运行单元测试
./gradlew test

# 运行集成测试
./gradlew integrationTest

# 代码覆盖率
./gradlew jacocoTestReport
```

## 故障排除

### 常见问题

1. **编译错误**
   - 确保 JDK 21+ 已正确安装
   - 检查 `gradle.properties` 中的路径配置

2. **插件无法加载**
   - 检查 IntelliJ IDEA 版本兼容性
   - 查看 IDE 日志文件中的错误信息

3. **分析失败**
   - 检查项目是否已正确编译
   - 确保 FlowSpot 引擎 JAR 文件存在

### 日志文件

插件会在项目根目录的 `.flowspot` 文件夹中生成详细的分析日志：

```
项目根目录/
├── .flowspot/
│   ├── flowspot_analysis_2025-09-19_10-30-15.log
│   └── flowspot_analysis_2025-09-19_09-15-42.log
```

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

### 代码规范

- 使用 Java 21 语法特性
- 遵循 IntelliJ Platform 开发规范
- 添加适当的注释和文档
- 编写单元测试

## 许可证

本项目采用 GNU General Public License v3.0 许可证。详见 [LICENSE](LICENSE) 文件。

## 联系方式

- **项目主页**: [FlowSpot Plugin](https://github.com/flowspot/intellij-plugin)
- **问题报告**: [Issues](https://github.com/flowspot/intellij-plugin/issues)
- **邮箱**: support@flowspot.com

## 更新日志

### v1.0.1 (2024-09-19)

- 🔧 **重要更新**: 集成 TestScan.doAnalysis 真实分析引擎
- ✨ 替换了占位符实现，使用完整的 FlowSpot 核心分析功能
- 🎯 支持动态规则选择和项目配置
- 📈 增强的进度回调和错误处理机制
- 🔄 完整的 SortedBugCollection 到 FlowSpotBugInstance 转换流程

### v1.0.0 (2024-09-19)

- 🎉 初始版本发布
- ✨ 完整的 FlowSpot 安全分析功能
- 🔧 独立的插件架构，不依赖 SpotBugs
- 📊 漏洞分类和统计展示
- 🚀 实时分析和结果展示
- 📝 详细的分析日志记录

---

**FlowSpot IntelliJ Plugin** - 让安全分析更简单、更高效！
