package omni.scan

trait IAnalyzer {
  def getSourcesQueryName(): List[String] =
    throw new UnsupportedOperationException()
    
  def getSinksQueryName(): List[String] =
    throw new UnsupportedOperationException()
}
