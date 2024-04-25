export enum BiometricsTypes {
  /**指纹识别 */
  fingerprint = "fingerprint",
  /**人脸识别 */
  faceRecognition = "faceRecognition",
}

export enum BioetricsCheckResult {
  /**用户无法进行身份验证，因为没有注册生物识别或设备凭据。 */
  BIOMETRIC_ERROR_NONE_ENROLLED = 11,
  /**用户可以成功进行身份验证。 */
  BIOMETRIC_SUCCESS = 0,
  /**无法确定用户是否可以进行身份验证。 */
  BIOMETRIC_STATUS_UNKNOWN = -1,
  /**用户无法进行身份验证，因为指定的选项与当前的 Android 版本不兼容。 */
  BIOMETRIC_ERROR_UNSUPPORTED = -2,
  /**由于硬件不可用，用户无法进行身份验证。 稍后再试。 */
  BIOMETRIC_ERROR_HW_UNAVAILABLE = 1,
  /**用户无法进行身份验证，因为没有合适的硬件（例如没有生物识别传感器或没有键盘保护装置）。 */
  BIOMETRIC_ERROR_NO_HARDWARE = 12,
  /**用户无法进行身份验证，因为发现一个或多个硬件传感器存在安全漏洞。 在安全更新解决该问题之前，受影响的传感器将不可用。 */
  BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED = 15,
}
