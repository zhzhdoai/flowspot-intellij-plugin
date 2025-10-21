package omni.scan.generate.sources

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object DubboArgTagger extends QueryBundle {
  implicit val resolver: ICallResolver = NoResolve

  @q
  def dubboArgSource(): Query =
    Query.make(
      name = "Dubbo-args",
      author = Crew.osword,
      title = "dubbo",
      description = """
                      dubbo.
                      |""".stripMargin,
      score = 4,
      withStrRep({ cpg =>
        (cpg.method
             .where(_.annotation.name("Service|DubboService"))
              .parameter ++
              cpg.method
                .where(_.typeDecl.annotation.name("Service|DubboService"))
                .parameter ++
              // Dubbo 接口实现类方法参数
              cpg.method
                .where(_.typeDecl.name(".*dubbo.*"))
                .parameter ++
              // Dubbo 上下文相关方法
              cpg.call
                .name("getAttachment|getAttachments|getArguments")
                .where(_.typeFullName(".*RpcContext.*|.*Invocation.*"))
                .argument.filter(_.order > 0))

      }),
      tags = List(QueryTags.taint, QueryTags.source, QueryTags.default)
    )
}

