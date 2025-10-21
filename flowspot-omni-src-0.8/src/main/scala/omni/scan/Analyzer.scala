package omni.scan

import omni.scan.generate.SinkQueryGenerator
import omni.scan.generate.sinks.{AnyBeanInvokeTagger, ApachePoiTagger, AviatorScriptTagger, BeetlTagger, BshTagger, CompileJavaTagger, DerSerTagger, HttpInvokerTagger, HutoolTagger, JexlTagger, JxpathTagger, ReflectInvokeTagger, ScriptEvalTagger, WorkFlowTagger, XMLDecoderTagger, YamlBeansTagger, messageResultTagger}
import omni.scan.generate.sources.*
// 定义规则信息类，包含名称和分数

class Analyzer() extends IAnalyzer {
  // 修改返回类型为RuleInfo列表
  override def getSourcesQueryName(): List[String] = {
    getSourcesQuery().map(_.name)
  }
  
  // 新增方法，返回包含名称和分数的RuleInfo列表
  def getSourcesQueryInfo(): List[(String,Double)] = {
    getSourcesQuery().map(q => (q.name, q.score))
  }

  def getSourcesQuery():List[Query]={
    List(
      HttpRequestTagger.httpRequestSources(),
      WebServletTagger.webServletSources(),
      SpringMappingTagger.springMapping(),
      WebSocketTagger.webSocketSources(),
      RabbitMqSourceTagger.rabbitMqSources(),
      RequestBodyTagger.requestBodySources(),
      Struts2Tagger.struts2Sources(),
      ThriftHandlerTagger.thriftHandlerSources(),
      NettyHandlerTagger.nettyHandlerSources(),
      DubboArgTagger.dubboArgSource()
    )
  }

  override def getSinksQueryName(): List[String] = {
    getSinksQuery().map(_.name)
  }
  
  // 新增方法，返回包含名称和分数的RuleInfo列表
  def getSinksQueryInfo(): List[(String,Double)] = {
    getSinksQuery().map(q => (q.name, q.score))
  }
  def getSinksQuery():List[Query] = {
    SinkQueryGenerator.allSinkQueries()++
      List(ReflectInvokeTagger.reflectInvoke(),ScriptEvalTagger.reflectInvoke(),
        AnyBeanInvokeTagger.reflectInvoke(),DerSerTagger.derTagger(),
        ApachePoiTagger.invoke(),AviatorScriptTagger.invoke(),
        BeetlTagger.invoke(),BshTagger.invoke(),CompileJavaTagger.invoke(),
        HttpInvokerTagger.invoke(),HutoolTagger.invoke(),JexlTagger.invoke(),JxpathTagger.invoke()
      ,WorkFlowTagger.invoke(),XMLDecoderTagger.invoke(),YamlBeansTagger.invoke())
  }
  
  /**
   * 使用指定的sinks.json路径获取sink查询
   * @param sinkJsonPath sinks.json文件路径
   * @return Query列表
   */
  def getSinksQuery(sinkJsonPath: String):List[Query] = {
    SinkQueryGenerator.allSinkQueries(sinkJsonPath)++
      List(ReflectInvokeTagger.reflectInvoke(),ScriptEvalTagger.reflectInvoke(),
        AnyBeanInvokeTagger.reflectInvoke(),DerSerTagger.derTagger(),
        ApachePoiTagger.invoke(),AviatorScriptTagger.invoke(),
        BeetlTagger.invoke(),BshTagger.invoke(),CompileJavaTagger.invoke(),
        HttpInvokerTagger.invoke(),HutoolTagger.invoke(),JexlTagger.invoke(),JxpathTagger.invoke()
      ,WorkFlowTagger.invoke(),XMLDecoderTagger.invoke(),YamlBeansTagger.invoke())
  }
}
