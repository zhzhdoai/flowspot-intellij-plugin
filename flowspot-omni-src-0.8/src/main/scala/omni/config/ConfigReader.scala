package omni.config

import com.typesafe.config.{Config, ConfigFactory}
import java.io.File
import java.nio.file.{Files, Paths}

/**
 * Configuration reader for the Omni application.
 * This class is responsible for loading and parsing the configuration file.
 */
object ConfigReader {
  // Base directory for external configuration files - using relative path
  private val configBaseDir = "config"
  
  // Configuration file names
  private val configFileName = "config.conf"
  private val sinksFileName = "sinks.json"
  
  // Full paths to configuration files
  private val externalConfigPath = s"$configBaseDir/$configFileName"
  private val externalSinksPath = s"$configBaseDir/$sinksFileName"
  
  // Default classpath resources
  private val defaultConfigPath = "/config.conf"
  private val defaultSinksPath = "/sinks.json"
  
  // Loaded configuration
  private lazy val config: Config = loadConfig()
  
  /**
   * Load the configuration from the external directory or fall back to classpath resources.
   *
   * @return The loaded configuration
   */
  private def loadConfig(): Config = {
    val externalFile = new File(externalConfigPath)
    
    if (externalFile.exists() && externalFile.canRead()) {
      println(s"Loading configuration from external file: $externalConfigPath")
      ConfigFactory.parseFile(externalFile).resolve()
    } else {
      println(s"Loading configuration from classpath resource: $defaultConfigPath")
      ConfigFactory.load(defaultConfigPath)
    }
  }
  
  /**
   * Get the path to the sinks.json file.
   * First checks the external directory, then falls back to classpath resource.
   *
   * @return The path to the sinks.json file
   */
  def getSinksFilePath(): String = {
    val externalFile = new File(externalSinksPath)
    
    if (externalFile.exists() && externalFile.canRead()) {
      println(s"Using sinks configuration from: $externalSinksPath")
      externalSinksPath
    } else {
      println(s"Using sinks configuration from classpath: $defaultSinksPath")
      getClass.getResource(defaultSinksPath).getPath
    }
  }
  
  /**
   * Get the entire configuration object.
   *
   * @return The configuration object
   */
  def getConfig: Config = config
  
  /**
   * Get a boolean value from the configuration.
   *
   * @param path The path to the configuration value
   * @param default The default value to return if the path is not found
   * @return The boolean value
   */
  def getBoolean(path: String, default: Boolean = false): Boolean = {
    if (config.hasPath(path)) config.getBoolean(path) else default
  }
  
  /**
   * Get a string value from the configuration.
   *
   * @param path The path to the configuration value
   * @param default The default value to return if the path is not found
   * @return The string value
   */
  def getString(path: String, default: String = ""): String = {
    if (config.hasPath(path)) config.getString(path) else default
  }
  
  /**
   * Get an integer value from the configuration.
   *
   * @param path The path to the configuration value
   * @param default The default value to return if the path is not found
   * @return The integer value
   */
  def getInt(path: String, default: Int = 0): Int = {
    if (config.hasPath(path)) config.getInt(path) else default
  }
  
  /**
   * Get a double value from the configuration.
   *
   * @param path The path to the configuration value
   * @param default The default value to return if the path is not found
   * @return The double value
   */
  def getDouble(path: String, default: Double = 0.0): Double = {
    if (config.hasPath(path)) config.getDouble(path) else default
  }
  
  /**
   * Check if a path exists in the configuration.
   *
   * @param path The path to check
   * @return True if the path exists, false otherwise
   */
  def hasPath(path: String): Boolean = config.hasPath(path)
  
  /**
   * Get a string list from the configuration.
   *
   * @param path The path to the configuration value
   * @return The string list, or an empty list if the path is not found
   */
  def getStringList(path: String): List[String] = {
    if (config.hasPath(path)) {
      import scala.jdk.CollectionConverters._
      config.getStringList(path).asScala.toList
    } else {
      List.empty[String]
    }
  }
}
