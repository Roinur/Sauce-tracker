# Keep release shrinking stable with GeckoView transitive YAML code paths
# that reference java.beans types not available on Android runtime.
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
