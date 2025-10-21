package omni.scan.generate.sources

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object WebServletTagger extends QueryBundle {
  implicit val resolver: ICallResolver = NoResolve
  
  @q
  def webServletSources(): Query =
    Query.make(
      name = "web-servlet-sources",
      author = Crew.osword,
      title = "Servlet API Parameters",
      description = """
                      |Parameters from Servlet API methods like doGet, doPost, etc.
                      |are considered attacker-controlled.
                      |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        cpg.call
          .name("doGet|doPost|doDelete|doPut|doHead|doOptions|doTrace")
          .argument
      }),
      tags = List(QueryTags.taint, QueryTags.source, QueryTags.default)
    )
}
