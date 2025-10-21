package omni.semantic

import flatgraph.help.Table.AvailableWidthProvider
import flatgraph.traversal.RepeatBehaviour.Traversal
import io.joern.dataflowengineoss.DefaultSemantics
import io.joern.dataflowengineoss.language.*
import io.joern.dataflowengineoss.layers.dataflows.{OssDataFlow, OssDataFlowOptions}
import io.joern.dataflowengineoss.queryengine.EngineContext
import io.joern.dataflowengineoss.semanticsloader.Semantics
import io.joern.x2cpg.X2Cpg
import io.shiftleft.codepropertygraph.cpgloading.CpgLoader
import io.shiftleft.codepropertygraph.generated.nodes.*
import io.shiftleft.semanticcpg.codedumper.CodeDumper
import io.shiftleft.semanticcpg.defaultAvailableWidthProvider
import io.shiftleft.semanticcpg.language.*
import io.shiftleft.semanticcpg.layers.LayerCreatorContext
import io.shiftleft.utils.IOUtils

import java.nio.file.Path
import scala.collection.mutable

trait SemanticGenerator {

  implicit val resolver: ICallResolver = NoResolve

  /** Utility to get the default semantics for dataflow queries
   *
   * @return
   */
  def getDefaultSemantics: Semantics = {
    DefaultSemantics()
  }

  case class Semantic(
                       signature: String,
                       flow: String,
                       file: String,
                       language: "JAVA",
                       categoryTree: Array[String]
                     )
  /** Generate semantics for tainting passed argument based on the number of parameter in method signature
   *
   * @param method
   *   or call node \- complete signature of method
   * @return
   *   \- semantic string
   */
  def generateSemanticForTaint(methodNode: AstNode, toTaint: Int = -2, extraParameter: Int = 0) = {
    val (parameterSize, fullName) = {
      methodNode match {
        case method: Method => (method.parameter.size + extraParameter, method.fullName)
        case call: Call     => (call.argument.size + extraParameter, call.methodFullName)
        case _              => (0, "NA")
      }
    }
    val parameterSemantic = mutable.HashSet[String]()
    for (i <- 0 until parameterSize) {
      if (toTaint != -2)
        parameterSemantic.add(s"$i->$toTaint")
      parameterSemantic.add(s"$i->$i")
    }
    Semantic(fullName, parameterSemantic.toList.sorted.mkString(" ").trim, "", "JAVA", Array())
  }

  /** Generate Semantic string based on input Semantic
   *
   * @param semantic
   *   \- semantic object containing semantic information
   * @return
   */
  def generateSemantic(semantic: Semantic): Option[String] = {
    if (semantic.signature.nonEmpty) {
      val generatedSemantic = "\"" + semantic.signature.trim + "\" " + semantic.flow
      Some(generatedSemantic.trim)
    } else None
  }

  /** Takes sequence of semantic as input and returns the unique semantic by signature which have the longest flow
   *
   * ex - If we have 2 semantics with the same signature, we would want the maximum flow one
   *   1. "logging.py:<module>.getLogger.<returnValue>.info" 0->-1 0->0 1->-1 1->1 2->-1 2->2 3->-1 3->3 4->-1 4->4
   *      5->-1 5->5
   *
   * 2. "logging.py:<module>.getLogger.<returnValue>.info" 0->-1 0->0 1->-1 1->1 2->-1 2->2
   *
   * We want the output to be 1st one as it has the longer flow
   * @param semantics
   * @return
   */
  def getMaximumFlowSemantic(semantics: Traversal[Semantic]): Seq[String] = {
    semantics.l
      .groupBy(_.signature)
      .map(item => (item._1, item._2.flatMap(sem => sem.flow.split(" ")).toSet.sorted.mkString(" ")))
      .map(item => { "\"" + item._1 + "\" " + item._2.replace("_A_", " ") })
      .sorted
  }
}
