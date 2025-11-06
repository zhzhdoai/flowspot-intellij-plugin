package omni.scan.generate.sources

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object HttpRequestTagger extends QueryBundle {
  implicit val resolver: ICallResolver = NoResolve
  
  @q
  def httpRequestSources(): Query =
    Query.make(
      name = "http-request-sources",
      author = Crew.osword,
      title = "HTTP Request Parameters",
      description = """
                      |Methods that extract data from HTTP requests like getParameter, getHeader, etc.
                      |are considered attacker-controlled.
                      |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        cpg.call
          .name(
            "getParameter|getParameterValues|getParameterMap|" +
              "getHeader|getHeaders|getHeaderNames|" +
              "getCookie|getCookies|" +
              "getQueryString|getRequestURI|getRequestURL|" +
              "getRemoteUser|getRemoteAddr|getRemoteHost|" +
              "getReader"
//              "getSession|getAttribute"
          )
          .argument.filter(_.order > 0)
      }),
      tags = List(QueryTags.taint, QueryTags.source, QueryTags.default)
    )
}
