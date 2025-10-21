package omni.semantic

import io.joern.dataflowengineoss.semanticsloader.{FlowSemantic, FullNameSemanticsParser}
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.semanticcpg.language.*
import omni.model.Constants

import java.io.PrintWriter

object JavaSemanticGenerator extends SemanticGenerator {


  /** Utility to get the semantics (default + custom) using cpg for dataflow queries
   *
   * @param cpg
   * \- cpg for adding customSemantics
   * @return
   */
  def getSemantics(
                    cpg: Cpg,
                    exportRuntimeSemantics: Boolean = false
                  ): List[FlowSemantic] = {
    val customSinkSemantics = getMaximumFlowSemantic(
      cpg.call
        .where(_.tag.nameExact(Constants.SINKSNAME).valueExact("sinks"))
        .map(generateSemanticForTaint(_, -1))
    )

    var customNonTaintDefaultSemantics   = Seq[String]()
    var specialNonTaintDefaultSemantics  = Seq[String]()
    var customStringSemantics            = Seq[String]()
    var customNonPersonalMemberSemantics = Seq[String]()
    customNonPersonalMemberSemantics= getMaximumFlowSemantic(cpg.method.fullName(".*(redis\\.clients|redis\\.core\\.DefaultValueOperations).*").map(generateSemanticForTaint(_)))
    //没有进行赋值会影响数据流，排除未赋值语句
    //boolean和void不影响数据流，去除了
    val nonTaintingMethods = cpg.method.where(_.callIn).fullName(".*:(long|int)\\(.*").l
      customNonTaintDefaultSemantics = getMaximumFlowSemantic(
        nonTaintingMethods
          .fullNameNot(".*\\.(add|put|<init>|set|get|append|store|insert|update|merge).*")
          .map(generateSemanticForTaint(_))
      )
    val dateTimeBreakers = getMaximumFlowSemantic(
      cpg.method.where(_.callIn).fullName(
          ".*(" +
            "java\\.text\\.SimpleDateFormat\\.parse|" + // 日期解析
            "java\\.time\\.LocalDate\\.parse|" + // 本地日期解析
            "java\\.time\\.LocalDateTime\\.parse|" + // 本地日期时间解析
            "java\\.time\\.ZonedDateTime\\.parse|" + // 带时区日期时间解析
            "java\\.time\\.Instant\\.parse|" + // 时间戳解析
            "java\\.time\\.format\\.DateTimeFormatter\\.parse" + // 日期时间格式化器解析
            ").*"
        )
        .map(generateSemanticForTaint(_)) // 不传播污点
    )
    val hashingBreakers = getMaximumFlowSemantic(
      cpg.method.where(_.callIn).fullName(
          ".*(" +
            "java\\.security\\.MessageDigest\\.digest|" + // 消息摘要
            "javax\\.crypto\\.Mac\\.doFinal|" + // HMAC
            "java\\.security\\.Signature\\.sign|" + // 签名
            "javax\\.crypto\\.Cipher\\.doFinal|" + // 加密
            "org\\.springframework\\.security\\.crypto\\.bcrypt\\.BCrypt|" + // BCrypt
            "org\\.springframework\\.security\\.crypto\\.password\\.Pbkdf2PasswordEncoder|" + // PBKDF2
            "org\\.mindrot\\.jbcrypt\\.BCrypt|" + // jBCrypt
            "com\\.google\\.common\\.hash\\.Hashing" + // Guava哈希
            ").*"
        )
        .map(generateSemanticForTaint(_)) // 不传播污点
    )
    val numericBreakers = getMaximumFlowSemantic(
      cpg.method.where(_.callIn).fullName(
          ".*(" +
            "java\\.lang\\.Math\\.|" + // 数学运算
            "java\\.util\\.Random\\.|" + // 随机数
            "java\\.math\\.BigDecimal\\.(add|subtract|multiply|divide)|" + // BigDecimal运算
            "java\\.math\\.BigInteger\\.(add|subtract|multiply|divide)" + // BigInteger运算
            ").*"
        )
        .map(generateSemanticForTaint(_)) // 不传播污点
    )
      specialNonTaintDefaultSemantics = getMaximumFlowSemantic(
        nonTaintingMethods
          .fullName(".*\\.(add|put|set|get|append|store|insert|update|merge).*")
          .map(generateSemanticForTaint(_, 0))
      )

      customStringSemantics = getMaximumFlowSemantic(
        cpg.method
          .filter(_.isExternal)
          .where(_.callIn)
          .fullName(".*:java.lang.String\\(.*")
          .fullNameNot(".*\\.set[A-Za-z_]*:.*")
          .map(generateSemanticForTaint(_, -1))
      )

//      customNonPersonalMemberSemantics = generateNonPersonalMemberSemantics(cpg)

//    val semanticFromConfig = ruleCache.getRule.semantics.flatMap(generateSemantic).sorted

    val headerAndSemanticPairs: Map[String, Seq[String]] = Map(
      "Custom Non taint default semantics" -> customNonTaintDefaultSemantics,
      "Custom specialNonTaintDefaultSemantics semantics" -> specialNonTaintDefaultSemantics,
      "Custom customStringSemantics semantics" -> customStringSemantics,
      "Custom customNonPersonalMemberSemantics semantics" -> customNonPersonalMemberSemantics,
      "Custom customSinkSemantics semantics" -> customSinkSemantics,
      "Custom hashingBreakers semantics" -> hashingBreakers,
      "Custom numericBreakers semantics" -> numericBreakers,
      "Custom dateTimeBreakers semantics" -> numericBreakers
    )
    semanticFileExporter(
      sourceRepoLocation = "/tmp",
      headerAndSemanticPairs
    )
    val list =
      customSinkSemantics++customNonTaintDefaultSemantics ++ specialNonTaintDefaultSemantics ++ customStringSemantics ++ customNonPersonalMemberSemantics ++
        dateTimeBreakers ++hashingBreakers ++numericBreakers
//    val testList = getMaximumFlowSemantic(cpg.method.where(_.callIn).fullName(".*getSuiteId.*").map(generateSemanticForTaint(_)))

    val parsed         = new FullNameSemanticsParser().parse(list.mkString("\n"))
    val finalSemantics = parsed
    finalSemantics
  }

  /** Generates Semantics for non Personal member
   * @param cpg
   * @return
   *   non-tainting semantic rule
   */
//  def generateNonPersonalMemberSemantics(cpg: Cpg): List[String] = {
//
//    val nonPersonalGetterSemantics = getMaximumFlowSemantic(
//      cpg.tag
//        .where(_.nameExact(InternalTag.INSENSITIVE_METHOD_RETURN.toString))
//        .call
//        .whereNot(_.tag.nameExact(InternalTag.SENSITIVE_METHOD_RETURN.toString))
//        .map(generateSemanticForTaint(_))
//    ).l
//
//    val nonPersonalSetterMethodFullNames = getMaximumFlowSemantic(
//      cpg.tag
//        .where(_.nameExact(InternalTag.INSENSITIVE_SETTER.toString))
//        .call
//        .whereNot(_.nameExact(InternalTag.SENSITIVE_SETTER.toString))
//        .map(generateSemanticForTaint(_))
//    ).l
//
//    val personalSetterMethodFullNames =
//      getMaximumFlowSemantic(
//        cpg.tag
//          .where(_.nameExact(InternalTag.SENSITIVE_SETTER.toString))
//          .call
//          .map(methodName => generateSemanticForTaint(methodName, 0))
//      ).l
//    (nonPersonalGetterSemantics ::: nonPersonalSetterMethodFullNames ::: personalSetterMethodFullNames).sorted
//  }
  def semanticFileExporter(sourceRepoLocation: String, headerAndSemanticPairs: Map[String, Seq[String]]): Unit = {
    if (headerAndSemanticPairs.keys.toList.length != headerAndSemanticPairs.values.toList.length) {

      return;
    }

    var runTimeSemanticsString: String = ""
    for ((header, semantics) <- headerAndSemanticPairs) {
      runTimeSemanticsString += header + "\n"
      for (semantic <- semantics) {
        runTimeSemanticsString += semantic + "\n"
      }
      runTimeSemanticsString += "------------------------------------------\n"
    }

    try {
      new PrintWriter(s"${sourceRepoLocation}/.flowspot/semantic.txt") {
        write(runTimeSemanticsString)
        close()
      }
    } catch {
      case e: Throwable => e.getMessage
    }

  }
}
