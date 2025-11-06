package omni.scan.generate.sources

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object WebSocketTagger extends QueryBundle {
  implicit val resolver: ICallResolver = NoResolve
  
  @q
  def webSocketSources(): Query =
    Query.make(
      name = "websocket-sources",
      author = Crew.osword,
      title = "WebSocket Parameters",
      description = """
                      |Parameters from WebSocket handlers and methods annotated with @MessageMapping
                      |are considered attacker-controlled.
                      |""".stripMargin,
      score = 5,
      withStrRep({ cpg =>
        (cpg.method
          .where(_.annotation.name("MessageMapping"))
          .parameter ++
          cpg.method
            .where(_.typeDecl.annotation.name("MessageMapping"))
            .parameter ++
          cpg.parameter
            .where(_.typeFullName(".*WebSocketSession.*|.*WebSocketMessage.*")))
          .filter(_.index > 0)
      }),
      tags = List(QueryTags.taint, QueryTags.source, QueryTags.default)
    )
}
