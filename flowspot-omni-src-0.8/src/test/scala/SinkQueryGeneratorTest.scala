//package omni.scan.generate
//
//import omni.rule.SinkRuleParser
//
//object SinkQueryGeneratorTest extends App {
//  
//  def testSinkJsonParsing(): Unit = {
//    println("=== Testing SinkQueryGenerator ===")
//    
//    val sinkJsonPath = "config/sinks.json"
//    println(s"Testing with path: $sinkJsonPath")
//    
//    try {
//      // 1. 测试SinkRuleParser
//      println("\n1. Testing SinkRuleParser.loadRules...")
//      val rules = SinkRuleParser.loadRules(sinkJsonPath)
//      println(s"Loaded ${rules.length} rules")
//      
//      rules.take(3).foreach { rule =>
//        println(s"  Rule: ${rule.name}")
//        println(s"    Category: ${rule.category}")
//        println(s"    Priority: ${rule.priority}")
//        println(s"    Sinks: ${rule.sinks.length}")
//        rule.sinks.foreach { sink =>
//          println(s"      Sink type: ${sink.typeName}")
//          println(s"      Patterns: ${sink.patterns.length}")
//        }
//      }
//      
//      // 2. 测试SinkQueryGenerator
//      println("\n2. Testing SinkQueryGenerator.generateSinkQueries...")
//      val queries = SinkQueryGenerator.generateSinkQueries(sinkJsonPath)
//      println(s"Generated ${queries.length} queries")
//      
//      queries.take(5).foreach { query =>
//        println(s"  Query: ${query.name()}")
//        println(s"    Title: ${query.title()}")
//        println(s"    Description: ${query.description().take(100)}...")
//      }
//      
//      // 3. 测试allSinkQueries方法
//      println("\n3. Testing SinkQueryGenerator.allSinkQueries...")
//      val allQueries = SinkQueryGenerator.allSinkQueries(sinkJsonPath)
//      println(s"All queries: ${allQueries.length}")
//      
//    } catch {
//      case e: Exception =>
//        println(s"Error: ${e.getMessage}")
//        e.printStackTrace()
//    }
//  }
//  
//  testSinkJsonParsing()
//}
