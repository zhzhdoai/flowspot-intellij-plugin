package omni.scan.generate.sources

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object RequestBodyTagger extends QueryBundle {
  implicit val resolver: ICallResolver = NoResolve
  
  @q
  def requestBodySources(): Query =
    Query.make(
      name = "request-body-sources",
      author = Crew.osword,
      title = "Request Body Parameters",
      description = """
                      |Parameters annotated with @RequestBody in Spring MVC
                      |are considered attacker-controlled.
                      |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        cpg.parameter
          .where(_.annotation.name("RequestBody"))
          .filter(_.index > 0)
      }),
      tags = List(QueryTags.taint, QueryTags.source, QueryTags.default)
    )
}
