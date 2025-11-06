package omni.scan.generate.sinks

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object HttpInvokerTagger extends QueryBundle{
  implicit val resolver: ICallResolver = NoResolve
  @q
  def invoke(): Query =
    Query.make(
      name = "DESERIALIZATION",
      author = Crew.osword,
      title = "DESERIALIZATION",
      description = """

                      |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        (cpg.call.methodFullName(".*(HttpInvokerServiceExporter).*").argument(0))
      }),
      sinkPattern="HttpInvokerServiceExporter",
      category="CODE_INJECTION",
      tags = List(QueryTags.taint, QueryTags.sink, QueryTags.default)
    )
}
