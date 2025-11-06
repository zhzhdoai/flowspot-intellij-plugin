package omni.scan.generate.sources

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object ThriftHandlerTagger extends QueryBundle {
  implicit val resolver: ICallResolver = NoResolve
  
  @q
  def thriftHandlerSources(): Query =
    Query.make(
      name = "thrift-handler-sources",
      author = Crew.osword,
      title = "Thrift Handler Parameters",
      description = """
                      |Parameters from Thrift handlers and protocol methods
                      |are considered attacker-controlled.
                      |""".stripMargin,
      score = 5,
      withStrRep({ cpg =>
        (cpg.method
          .where(_.typeDecl.name(".*Processor.*|.*Handler.*|.*Service\\$Iface.*"))
          .where(_.typeDecl.name(".*TProcessor.*|.*TServiceIface.*"))
          .parameter ++
          // Thrift 自动生成的服务实现类方法参数
          cpg.method
            .where(_.name("process|process_.*"))
            .where(_.typeDecl.name(".*Processor.*"))
            .parameter ++
          // Thrift 协议相关方法
          cpg.call
            .name("readString|readBinary|readBool|readMap|readList|readSet")
            .where(_.typeFullName(".*TProtocol.*"))
            .argument.filter(_.order > 0))
      }),
      tags = List(QueryTags.taint, QueryTags.source, QueryTags.default)
    )
}
