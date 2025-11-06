package omni.scan.generate.sources

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object RabbitMqSourceTagger extends QueryBundle {
  implicit val resolver: ICallResolver = NoResolve

  @q
  def rabbitMqSources(): Query =
    Query.make(
      name = "rabbitmq-message-sources",
      author = Crew.osword, // 或者使用适当的作者标识
      title = "RabbitMQ Message Sources",
      description = """
                      |Methods that extract data from RabbitMQ messages are considered
                      |attacker-controlled as they could contain malicious payloads.
                      |""".stripMargin,
      score = 5,
      withStrRep({ cpg =>
        cpg.call.method.name("onMessage|handleMessage").parameter.filter(_.index > 0).dedup
      }),
      tags = List(QueryTags.taint, QueryTags.source, QueryTags.default)
    )
}