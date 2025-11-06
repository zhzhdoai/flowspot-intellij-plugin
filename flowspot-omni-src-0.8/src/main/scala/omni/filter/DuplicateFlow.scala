package omni.filter

import io.joern.dataflowengineoss.language.Path
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.{AstNode, CfgNode, Expression, Identifier}
import io.shiftleft.semanticcpg.language._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}
import scala.util.control.Breaks.{break, breakable}
object DuplicateFlow {
  private val NODE_PATH_SEPARATOR = "-"
  def getUniquePathsAfterDedup(dataflowPaths: List[Path]): List[Path] = {
    val flows = dataflowPaths
      .map(path => {
        (calculatePathId(path).getOrElse(""), path)
      })
      .toMap
    val uniquePathIds = pathIdsPerSourceIdAfterDedup(flows.keySet)
    uniquePathIds.map(pathId => flows(pathId)).toList
  }

  def calculatePathId(flow: Path) = Try {
    flow.elements.map(node => node.id()).mkString(NODE_PATH_SEPARATOR)
  }

  def pathIdsPerSourceIdAfterDedup(pathIds: Set[String]) = {
    val visitedFlows = mutable.Set[String]()
    pathIds.foreach(pathId => {
      if (!visitedFlows.contains(pathId)) {
        val pathSubIds = getSubPathIds(pathId)
        if (pathSubIds.nonEmpty)
          visitedFlows.addAll(pathSubIds)
      }
    })
    pathIds.diff(visitedFlows) // This will give us all the Path ids which are super set of overlapping paths
  }

  private def getSubPathIds(pathId: String) = {
    val subIdList = ListBuffer[String]()
    val pathIds = pathId.split(NODE_PATH_SEPARATOR)
    for (i <- 1 until (pathIds.size - 1)) {
      subIdList.append(pathIds.slice(i, pathIds.size).mkString(NODE_PATH_SEPARATOR))
    }
    subIdList.toList
  }

  def filterFlowsByContext(flow: Path): Boolean = {
    val reversedPath = flow.elements.reverse
    val observedThisTypeFullNameAndIdentifier = mutable.HashMap[String, String]()
    var isFlowCorrect = true
    breakable {
      for (i <- reversedPath.indices) {
        val node = reversedPath(i)
        if (node.isCall) {
          val traversalNode = Iterator(node).isCall.l
          var thisNode = List[Expression]()

          if (traversalNode.name.headOption.getOrElse("") == Operators.fieldAccess)
            thisNode = traversalNode.argument.where(_.argumentIndex(1)).code("this").l
          else
            thisNode = traversalNode.argument.where(_.argumentIndex(0)).code("this").l

          if (thisNode.nonEmpty) {
            val currentThisTypeFullName = thisNode.isIdentifier.typeFullName.headOption.getOrElse("")
            val currentThisCode = traversalNode.code.headOption.getOrElse("")
            observedThisTypeFullNameAndIdentifier.get(currentThisTypeFullName) match {
              case Some(prevThisCode) =>
                if (prevThisCode != currentThisCode) {
//                  print(s"Removed Flow due to 'this' tainting: ${flow.elements.code.mkString("||")}")
                  isFlowCorrect = false
                  break()
                }
              case _ => observedThisTypeFullNameAndIdentifier(currentThisTypeFullName) = currentThisCode
            }
          }
        }
      }
    }
    isFlowCorrect
  }

  def flowNotTaintedByThis(flow: Path): Boolean = {
    val flowSize = flow.elements.size
    !flow.elements.head.code.equals("this")
  }
}
