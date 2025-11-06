package omni.scan.generate.sinks

import io.joern.console.q
import io.joern.dataflowengineoss.queryengine.EngineContext
import omni.scan.QueryMacros.withStrRep
import omni.scan.{Crew, Query, QueryTags}
import io.shiftleft.semanticcpg.language.*
import omni.scan.generate.QueryBundle


//DiagnosticCollector < JavaFileObject > diagnostics = new DiagnosticCollector();
//StandardJavaFileManager standardManager = this.compiler.getStandardFileManager(diagnostics, null, null);
//ClassFileManager classFileManager = new ClassFileManager(standardManager);
//Iterable <?
//extends JavaFileObject > compilationUnits = standardManager.getJavaFileObjects(sourceFile);

object CompileJavaTagger extends QueryBundle{
  implicit val resolver: ICallResolver = NoResolve
  @q
  def invoke(): Query =
    Query.make(
      name = "CODE_INJECTION",
      author = Crew.osword,
      title = "CODE_INJECTION",
      description = """

                      |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        (cpg.call.methodFullName(".*(ClassFileManager).*").argument(0))
      }),
      sinkPattern="ClassFileManager",
      category="CODE_INJECTION",
      tags = List(QueryTags.taint, QueryTags.sink, QueryTags.default)
    )
}
