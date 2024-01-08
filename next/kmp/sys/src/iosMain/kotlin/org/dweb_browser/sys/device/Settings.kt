package org.dweb_browser.sys.device


public interface Settings {

  public companion object;

  /**
   * A factory that can produce [Settings] instances or derivations thereof.
   */
  public interface Factory {
    /**
     * Creates a [Settings] object associated with the provided [name].
     *
     * Multiple `Settings` instances created with the same `name` parameter will be backed by the same persistent
     * data, while distinct `name`s will use different data.
     */
    public fun create(name: String? = null): Settings
  }

  /**
   * Returns a `Set` containing all the keys present in this [Settings].
   */
  public val keys: Set<String>

  /**
   * Clears all values stored in this [Settings] instance.
   */
  public fun clear()

  /**
   * Removes the value stored at [key].
   */
  public fun remove(key: String)

  /**
   * Returns `true` if there is a value stored at [key], or `false` otherwise.
   */
  public fun hasKey(key: String): Boolean

  public fun putString(key: String, value: String)

  /**
   * Returns the `String` value stored at [key], or [defaultValue] if no value was stored. If a value of a different
   * type was stored at `key`, the behavior is not defined.
   */
  public fun getString(key: String, defaultValue: String): String

  /**
   * Returns the `String` value stored at [key], or `null` if no value was stored. If a value of a different type was
   * stored at `key`, the behavior is not defined.
   */
  public fun getStringOrNull(key: String): String?

}

