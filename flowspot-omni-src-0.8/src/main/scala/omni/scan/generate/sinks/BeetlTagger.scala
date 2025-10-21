package omni.scan.generate.sinks

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object BeetlTagger extends QueryBundle{
  implicit val resolver: ICallResolver = NoResolve
  @q
  def invoke(): Query =
    Query.make(
      name = "CODE_INJECTION",
      author = Crew.osword,
      title = "Reflective Code Injection",
      description = """
                      |One can consider the cookies, configs, property-map of `javax.servlet.http.HttpServletRequest`s
                      |to be attacker-controlled.
                      |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        (cpg.call.methodFullName(".*GroupTemplate\\.getTemplate.*").argument(1))
      }),
      sinkPattern=".*GroupTemplate\\.getTemplate.*",
      category="CODE_INJECTION",
      tags = List(QueryTags.taint, QueryTags.sink, QueryTags.default)
    )
}

