package info.bagen.rust.plaoc.system.camera

import com.google.gson.GsonBuilder

enum class CameraResultType(val type: String) {
    BASE64("base64"), URI("uri"), DATAURL("dataUrl"),
}

enum class CameraSource(val source: String) {
    PROMPT("PROMPT"), CAMERA("CAMERA"), PHOTOS("PHOTOS"),
}

internal val DEFAULT_QUALITY: Int = 90
internal val DEFAULT_SAVE_IMAGE_TO_GALLERY: Boolean = false
internal val DEFAULT_CORRECT_ORIENTATION: Boolean = true

data class CameraSettings(
    var resultType: CameraResultType = CameraResultType.BASE64,
    var quality: Int = DEFAULT_QUALITY,
    var shouldResize: Boolean = false,
    var shouldCorrectOrientation: Boolean = DEFAULT_CORRECT_ORIENTATION,
    var saveToGallery: Boolean = DEFAULT_SAVE_IMAGE_TO_GALLERY,
    var allowEditing: Boolean = false,
    var width: Int = 0,
    var height: Int = 0,
    var source: CameraSource = CameraSource.PROMPT,
)

data class CameraImageOption(
    var resultType: String = "base64",
    var quality: Int? = 100,
    var shouldResize: Boolean? = false,
    var correctOrientation: Boolean? = DEFAULT_CORRECT_ORIENTATION,
    var saveToGallery: Boolean? = DEFAULT_SAVE_IMAGE_TO_GALLERY,
    var allowEditing: Boolean? = false,
    var width: Int? = 0,
    var height: Int? = 0,
    var source: String? = "PROMPT",
)

data class CameraGalleryImageOption(
    var quality: Int? = DEFAULT_QUALITY,
    var width: Int? = 0,
    var height: Int? = 0,
    var correctOrientation: Boolean? = false
)

// data class convert to another data class
inline fun <reified T : Any> Any.mapTo(): T =
    GsonBuilder().create().run {
        toJson(this@mapTo).let { fromJson(it, T::class.java) }
    }

fun CameraImageOption.toCameraSettings(): CameraSettings =
    mapTo<CameraSettings>().copy(
        resultType = CameraResultType.valueOf(resultType),
        source = CameraSource.valueOf(source ?: CameraSource.PROMPT.toString()),
        shouldCorrectOrientation = correctOrientation ?: DEFAULT_CORRECT_ORIENTATION
    )

fun CameraGalleryImageOption.toCameraSettings(): CameraSettings =
    mapTo<CameraSettings>().copy(
        shouldCorrectOrientation = correctOrientation ?: DEFAULT_CORRECT_ORIENTATION
    )
