package info.bagen.libappmgr.utils

/**
 * 通过SystemProperties获取值
 */
class SystemPropertiesUtil {
    companion object {
        private const val CLASS_NAME = "android.os.SystemProperties"

        fun getProperties(key: String, defaultValue: String): String {
            var value = defaultValue
            try {
                var c = Class.forName(CLASS_NAME)
                var method = c.getMethod("get", String::class.java, String::class.java)
                value = method.invoke(c, key, defaultValue) as String
            } catch (e: Exception) {
                e.printStackTrace();
            } finally {
                return value;
            }
        }

        fun setProperties(key: String, value: String) {
            try {
                var c = Class.forName(CLASS_NAME)
                var method = c.getMethod("set", String::class.java, String::class.java)
                method.invoke(c, key, value)
            } catch (e: Exception) {
                e.printStackTrace();
            }
        }
    }
}
