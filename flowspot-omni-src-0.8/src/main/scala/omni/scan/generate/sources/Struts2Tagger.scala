package omni.scan.generate.sources

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object Struts2Tagger extends QueryBundle {
  implicit val resolver: ICallResolver = NoResolve
  
  @q
  def struts2Sources(): Query =
    Query.make(
      name = "struts2-sources",
      author = Crew.osword,
      title = "Struts2 Action Parameters",
      description = """
                      |Parameters from Struts2 Action methods and ValueStack operations
                      |are considered attacker-controlled.
                      |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        (cpg.method
          .filter(m => {
            // 检查类名是否以Action结尾
            val isActionClass = m.typeDecl.name(".*Action$").nonEmpty
            
            // 检查是否继承自ActionSupport或实现Action接口
            val extendsActionSupport = m.typeDecl.inheritsFromTypeFullName.exists(name => 
              name.matches(".*ActionSupport.*") || name.matches(".*com\\.opensymphony\\.xwork2\\.Action.*"))
            
            // 检查是否有Struts2特有的注解
            val hasStruts2Annotations = m.annotation.name.exists(name => 
              name.matches(".*Action.*") || name.matches(".*Namespace.*") || 
              name.matches(".*Result.*") || name.matches(".*Results.*") || 
              name.matches(".*InterceptorRef.*"))
            
            // 满足以下条件之一：1.类名以Action结尾且有execute方法 2.继承自ActionSupport 3.有Struts2注解
            (isActionClass && m.name.matches("execute")) || extendsActionSupport || hasStruts2Annotations
          })
          .parameter ++
          // Struts2 ValueStack 相关方法
          cpg.call
            .name("get|findValue|findString|peek|push")
            .where(_.typeFullName(".*ValueStack.*|.*ActionContext.*|.*com\\.opensymphony\\.xwork2.*"))
            .argument.filter(_.order > 0) ++
          // Struts2 参数拦截器相关方法
          cpg.call
            .name("getParameters|getParameter")
            .where(_.typeFullName(".*ParametersInterceptor.*|.*ServletActionContext.*|.*org\\.apache\\.struts2.*"))
            .argument.filter(_.order > 0)) ++
          // 特定类型的参数
          cpg.parameter
            .where(_.typeFullName(".*HttpServletRequest.*|.*ActionForm.*|.*org\\.apache\\.struts2\\.action\\..*|.*com\\.opensymphony\\.xwork2\\..*"))
      }),
      tags = List(QueryTags.taint, QueryTags.source, QueryTags.default)
    )
}
