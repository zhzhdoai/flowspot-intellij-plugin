package omni.scan.generate.sources

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object NettyHandlerTagger extends QueryBundle {
  implicit val resolver: ICallResolver = NoResolve
  
  @q
  def nettyHandlerSources(): Query =
    Query.make(
      name = "netty-handler-sources",
      author = Crew.osword,
      title = "Netty Handler Parameters",
      description = """
                      |Parameters from Netty ChannelHandler methods and HTTP request operations
                      |are considered attacker-controlled.
                      |""".stripMargin,
      score = 5,
      withStrRep({ cpg =>
        (cpg.method
          .where(_.typeDecl.name(".*ChannelHandler.*|.*ChannelInboundHandler.*"))
          .parameter ++
          // Netty 特定方法参数
          cpg.method
            .name("channelRead|channelRead0|channelActive|userEventTriggered|messageReceived")
            .parameter ++
          // Netty 请求相关方法
          cpg.call
            .name("content|headers|uri|path|parameters|param|queryParam")
            .where(_.typeFullName(".*HttpRequest.*|.*FullHttpRequest.*|.*HttpContent.*"))
            .argument.filter(_.order > 0) ++
          // Netty ByteBuf 相关方法
          cpg.call
            .name("readBytes|toString|getBytes")
            .where(_.typeFullName(".*ByteBuf.*"))
            .argument.filter(_.order > 0))
      }),
      tags = List(QueryTags.taint, QueryTags.source, QueryTags.default)
    )
}
