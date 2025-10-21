# 保留SinkRule类及其字段
-keep class omni.rule.SinkRule { *; }
-keep class omni.rule.SinkPattern { *; }

# 保留JSON序列化相关的类
-keep class org.json4s.** { *; }

# 保留Scala相关的类
-keep class scala.collection.immutable.List { *; }
-keep class scala.Predef$ { *; }
-keep class scala.collection.** { *; }

# 保留反射相关的信息
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# 不混淆JSON序列化相关的类名和字段名
-keepclassmembers class omni.rule.** {
    <fields>;
    <init>(...);
}

# 保留SinkRuleParser类
-keep class omni.rule.SinkRuleParser { *; }

# 保留用于JSON解析的case class
-keepclassmembers class ** {
    public static ** MODULE$;
}
-keepclassmembers class * extends scala.Product {
    <fields>;
    <methods>;
}
