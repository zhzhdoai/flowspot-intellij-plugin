package omni.scan.generate.sources

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object SpringMappingTagger extends QueryBundle{
  implicit val resolver: ICallResolver = NoResolve
  @q
  def springMapping(): Query =
    Query.make(
    name = "spring-mapping-sources",
    author = Crew.osword,
    title = "Spring Mvc Api",
    description = """
                    |One can consider the cookies, configs, property-map of `javax.servlet.http.HttpServletRequest`s
                    |to be attacker-controlled.
                    |""".stripMargin,
    score = 8,
    withStrRep({ cpg =>
      (cpg.method
        .where(_.annotation.name(".*ActionMethod")).parameter++cpg.method
        .where(_.annotation.name(".*Mapping")).parameter ++ cpg.method
        .where(_.typeDecl.name(".*ActionClass.*"))
        .parameter ++ cpg.method
        .where(_.annotation.name(".*ThriftActioner.*"))
        .parameter ++
        // Spring注解参数
        cpg.parameter
          .where(_.annotation.name("RequestParam|PathVariable|RequestHeader|CookieValue|ModelAttribute"))).filter(_.index > 0).filterNot(_.typeFullName.matches(".*(java\\.lang\\.Long|java\\.lang\\.Integer).*"))
    }),
    tags = List(QueryTags.taint, QueryTags.source, QueryTags.default)
  )
}
