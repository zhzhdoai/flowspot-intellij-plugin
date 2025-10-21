package omni.scan.newpass

import flatgraph.DiffGraphApplier
import io.joern.dataflowengineoss.language.*
import io.joern.dataflowengineoss.queryengine.{EngineContext}
import omni.scan.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes}
import io.shiftleft.passes.CpgPass
import io.shiftleft.semanticcpg.language.*
import io.shiftleft.semanticcpg.language.operatorextension.OpNodes

class BeanValidationDataFlowPass(cpg: Cpg, maxNumberOfValidators: Int = 1000)(implicit engineContext: EngineContext)  extends CpgPass(cpg) {

  // 从泛型签名中提取约束注解类型和被验证类型的正则表达式
  private val constraintValidatorPattern = "LConstraintValidator<L([^;]+);L([^;]+);>".r
  private val validParaTypeNameMap = cpg.sources.isParameter
    .filter(_.annotation.name("Valid|Validated").nonEmpty)
    .map(parameter => (parameter, parameter.typeFullName))
    .toList
  def beanValidField(annotationField: String): List[(MethodParameterIn, OpNodes.Assignment)] = {

    validParaTypeNameMap.flatMap {
      case (p, pType) =>
        // 获取匹配类型的声明
        val typeDecls = cpg.typeDecl.fullNameExact(pType).l

        // 查找这些类型的赋值操作
        val assignments = typeDecls.flatMap { t =>
          t.assignment
            .where(_.fieldAccess.where(_.member.annotation.name(annotationField)))
            .l
        }
        // 将参数与每个匹配的assignment配对

        assignments.map(assignment => (p, assignment))
    }
  }
  def beanValidType(annotationType:String):List[(MethodParameterIn, OpNodes.Assignment)]={
    validParaTypeNameMap.flatMap{
      case(p,pType) =>{
        val validTypeDecls = cpg.typeDecl.fullNameExact(pType).where(_.annotation.name(annotationType)).l
        val assignments = validTypeDecls.flatMap{
          t =>t.assignment.l
        }
        assignments.map(assignment => (p, assignment))
      }
    }
  }

  override def run(diffGraph: DiffGraphBuilder): Unit = {
    println("开始运行 BeanValidationDataFlowPass...")

    // 1. 找到所有 ConstraintValidator 实现类
    val validators = cpg.typeDecl
      .filter(_.inheritsFromTypeFullName.contains("javax.validation.ConstraintValidator"))
      .l
    println(s"找到 ${validators.size} 个 ConstraintValidator 实现类")
    val callSink = cpg.call.methodFullName(".*buildConstraintViolationWithTemplate.*").isEmpty
    if (validators.isEmpty&&callSink) {
      println("未找到任何 ConstraintValidator 实现类，BeanValidationDataFlowPass分析终止。")
      return
    }
    if (validators.size > maxNumberOfValidators) {
      println(s"验证器数量 (${validators.size}) 超过阈值 ($maxNumberOfValidators)，可能会导致性能问题")
    }

    // 2. 从泛型签名中提取约束注解类型和被验证类型
    val validatorTypesMap = validators.flatMap { validator =>
      extractTypesFromGenericSignature(validator).map { case (annotationType, validatedType) =>

        (validator.method.name("isValid").toList(0), beanValidField(annotationType)++beanValidType(annotationType))
      }
    }
    validatorTypesMap.foreach {
      case(vMethod,vFields)=>{
        vFields.foreach{
          v=>
          diffGraph.addEdge(v._2, vMethod, EdgeTypes.CALL)
        }
      }
    }

    DiffGraphApplier.applyDiff(cpg.graph, diffGraph)
    val paths = validatorTypesMap.flatMap {
      case (vMethod, vFields) =>
        val sinks = cpg.call.methodFullName(".*buildConstraintViolationWithTemplate.*").argument.l
        vFields.flatMap { v =>
          sinks.reachableByFlows(v._2.fieldAccess)(engineContext)
            .distinctBy { p =>
              (p.elements.headOption.map(_.asInstanceOf[CfgNode].method.fullName),
                p.elements.lastOption.map(_.asInstanceOf[CfgNode].method.fullName))
            }
            .sortBy(p => -p.elements.size)
            .map(flow => (v._1, flow))
        }
    }
    val findingGraph = Cpg.newDiffGraphBuilder

    paths.zipWithIndex.map{
      case(pairPaths,index) =>
        QueryWrapper.finding(pairPaths._1::pairPaths._2.elements, "CODE_INJECTION", "osword", "el validate", "el validate", 10, "el validate","CODE_INJECTION")
    }.foreach(findingGraph.addNode)
    DiffGraphApplier.applyDiff(cpg.graph, findingGraph)

    println("BeanValidationDataFlowPass 运行完成")
  }

  // 从泛型签名中提取约束注解类型和被验证类型
  private def extractTypesFromGenericSignature(validator: TypeDecl): Option[(String, String)] = {
    // 获取泛型签名
    val genericSignature = validator.genericSignature

    // 如果没有泛型签名，返回空
    if (genericSignature.isEmpty) {
      return None
    }

    // 使用正则表达式提取 ConstraintValidator<A, B> 中的 A 和 B
    val matches = constraintValidatorPattern.findAllMatchIn(genericSignature).toList
    if (matches.isEmpty) {
      return None
    }

    val match1 = matches.head
    if (match1.groupCount >= 2) {
      val annotationType = match1.group(1) // 约束注解类型
      val validatedType = match1.group(2) // 被验证的字段类型
      Some((annotationType, validatedType))
    } else {
      None
    }
  }

//  private def addEdge(fromNode: StoredNode, toNode: StoredNode, variable: String = "")(implicit
//                                                                                       dstGraph: DiffGraphBuilder
//  ): Unit = {
//    if (fromNode.isInstanceOf[Unknown] || toNode.isInstanceOf[Unknown])
//      return
//
//    (fromNode, toNode) match {
//      case (parentNode: CfgNode, childNode: CfgNode) if EdgeValidator.isValidEdge(childNode, parentNode) =>
//        dstGraph.addEdge(fromNode, toNode, EdgeTypes.REACHING_DEF, variable)
//      case _ =>
//
//    }
//  }
}