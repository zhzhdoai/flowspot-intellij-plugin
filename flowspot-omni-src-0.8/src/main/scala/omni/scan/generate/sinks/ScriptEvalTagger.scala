package omni.scan.generate.sinks

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object ScriptEvalTagger extends QueryBundle{
  implicit val resolver: ICallResolver = NoResolve
  @q
  def reflectInvoke(): Query =
    Query.make(
      name = "CODE_INJECTION",
      author = Crew.osword,
      title = "CODE_INJECTION",
      description = """

                      |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        (cpg.call.methodFullName(".*org\\.mozilla\\.javascript\\.Context\\.compileString.*").argument.argumentIndex(1))
      }),
      sinkPattern="mozilla脚本执行",
      category="CODE_INJECTION",
      tags = List(QueryTags.taint, QueryTags.sink, QueryTags.default)
    )
}


