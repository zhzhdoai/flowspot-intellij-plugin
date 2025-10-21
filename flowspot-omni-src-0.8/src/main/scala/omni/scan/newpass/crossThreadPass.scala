package omni.scan.newpass

import flatgraph.DiffGraphApplier
import io.joern.dataflowengineoss.queryengine.EngineContext
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes, Operators}
import io.shiftleft.passes.CpgPass
import io.shiftleft.semanticcpg.language.*
class crossThreadPass(cpg: Cpg, maxNumberOfValidators: Int = 1000)(implicit engineContext: EngineContext)  extends CpgPass(cpg) {

  override def run(builder: DiffGraphBuilder):  Unit = {
    val threadedRunnable =cpg.call.methodFullName(".*java\\.lang\\.Thread\\.\\<init\\>.*")
//    threadedRunnable.argument.foreach({arg =>
//      cpg.method.name("run").filter(_.typeDecl.inheritsFromTypeFullName.contains(arg.typ.fullName.head)).parameter.order(0).foreach({
//        runThis => builder.addEdge(arg,runThis,EdgeTypes.REACHING_DEF)
//      })
//    })
    if(!threadedRunnable.isEmpty){
      threadedRunnable.method.fullName.foreach({ mName =>
        cpg.call.methodFullNameExact(mName).foreach({ callThread =>
          callThread.method.typeDecl.fullName.foreach({ callType =>
            val runMethod = cpg.method.name("run").filter(_.typeDecl.fullName.contains(callType))
            runMethod.headOption.foreach { runMethodHead =>
              builder.addEdge(callThread, runMethodHead, EdgeTypes.CALL)
            }
          })
        })
      })
      DiffGraphApplier.applyDiff(cpg.graph, builder)
    }


    // 直接使用constructCall调用。没成功
//    val connections = cpg.call.name(Operators.alloc)
//      .filter { call =>
//        val typeDecl = call.typ.referencedTypeDecl
//        typeDecl.inheritsFromTypeFullName.exists { name =>
//          name == "java.lang.Thread" || name.endsWith(".Thread")
//        } ||
//          typeDecl.inheritsFromTypeFullName.exists { name =>
//            name == "java.lang.Runnable" || name.endsWith(".Runnable")
//          }
//      }.foreach({
//        constructorCall => constructorCall.typ.method.name("run").foreach({
//          runMethod =>
//            builder.addEdge(constructorCall,runMethod,EdgeTypes.CALL)
//            builder.addEdge(constructorCall,runMethod,EdgeTypes.CFG)
////            builder.addEdge(constructorCall.inCall.argument.order(1).head, runMethod.parameter.head, EdgeTypes.REACHING_DEF)
//            constructorCall.inCall.argument.foreach({
//              arg => runMethod.parameter.order(0).foreach({
//                runThisParam =>
//                  builder.addEdge(arg, runThisParam, EdgeTypes.REACHING_DEF)
//              })
//            })
//        })
//      })
//
    cpg.call.name("start").foreach({
      constructorCall =>
        constructorCall.argument.typ.method.name("run").foreach({
          runMethod =>
            builder.addEdge(constructorCall, runMethod, EdgeTypes.CALL)
        })
    })
    DiffGraphApplier.applyDiff(cpg.graph, builder)

  }

}
