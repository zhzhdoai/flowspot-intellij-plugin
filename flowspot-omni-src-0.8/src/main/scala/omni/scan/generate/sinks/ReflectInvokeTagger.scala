package omni.scan.generate.sinks

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle

object ReflectInvokeTagger extends QueryBundle{
  implicit val resolver: ICallResolver = NoResolve
  @q
  def reflectInvoke(): Query =
    Query.make(
      name = "Reflective_Code_Injection",
      author = Crew.osword,
      title = "Reflective Code Injection",
      description = """
                      |One can consider the cookies, configs, property-map of `javax.servlet.http.HttpServletRequest`s
                      |to be attacker-controlled.
                      |""".stripMargin,
      score = 5,
      withStrRep({ cpg =>
        (cpg.call.methodFullNameExact("<operator>.arrayInitializer").where(_.call.methodFullNameExact("java.lang.reflect.Method.invoke:java.lang.Object(java.lang.Object,java.lang.Object[])")).argument)
      }),
      sinkPattern="method.invoke",
      category="CODE_INJECTION",
      tags = List(QueryTags.taint, QueryTags.sink, QueryTags.default)
    )
}

