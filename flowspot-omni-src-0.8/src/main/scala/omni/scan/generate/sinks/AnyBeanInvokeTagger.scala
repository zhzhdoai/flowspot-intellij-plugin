package omni.scan.generate.sinks

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object AnyBeanInvokeTagger extends QueryBundle{
  implicit val resolver: ICallResolver = NoResolve
  @q
  def reflectInvoke(): Query =
    Query.make(
      name = "SPRING_FRAMEWORK_RCE",
      author = Crew.osword,
      title = "SPRING_FRAMEWORK_RCE",
      description = """

                      |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        (cpg.call.methodFullName(".*\\.getBean\\(.*").argument.order(1))
      }),
      sinkPattern="任意bean 调用",
      category="CODE_INJECTION",
      tags = List(QueryTags.taint, QueryTags.sink, QueryTags.default)
    )
}


