package omni.scan.generate.sinks

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object DerSerTagger extends QueryBundle{
  implicit val resolver: ICallResolver = NoResolve
  @q
  def derTagger(): Query =
    Query.make(
      name = "DESERIALIZATION",
      author = Crew.osword,
      title = "DESERIALIZATION",
      description = """

                      |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        (cpg.call.methodFullName(".*(readObject|readExternal).*").argument(0)++cpg.call.methodFullName(".*SerializationUtils\\.deserialize.*").argument(1))
      }),
      sinkPattern=".*(readObject|readExternal)|.*SerializationUtils\\.deserialize.*",
      category="CODE_INJECTION",
      tags = List(QueryTags.taint, QueryTags.sink, QueryTags.default)
    )
}
