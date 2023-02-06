package info.bagen.rust.plaoc.system.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.FileProvider
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.system.device.Device
import io.ktor.client.engine.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class CameraPlugin {
    // Permission alias constants
    val CAMERA = "camera"
    val PHOTOS = "photos"

    private val ProcessCameraImage: Int = 91
    private val ProcessPickedImage: Int = 92
    private val ProcessPickedImages: Int = 93
    private val ProcessEditedImage: Int = 94

    // Message constants
    private val INVALID_RESULT_TYPE_ERROR = "Invalid resultType option"
    private val PERMISSION_DENIED_ERROR_CAMERA = "User denied access to camera"
    private val PERMISSION_DENIED_ERROR_PHOTOS = "User denied access to photos"
    private val NO_CAMERA_ERROR = "Device doesn't have a camera available"
    private val NO_CAMERA_ACTIVITY_ERROR = "Unable to resolve camera activity"
    private val NO_PHOTO_ACTIVITY_ERROR = "Unable to resolve photo activity"
    private val IMAGE_FILE_SAVE_ERROR = "Unable to create photo on disk"
    private val IMAGE_PROCESS_NO_FILE_ERROR = "Unable to process image, file not found on disk"
    private val UNABLE_TO_PROCESS_IMAGE = "Unable to process image"
    private val IMAGE_EDIT_ERROR = "Unable to edit image"
    private val IMAGE_GALLERY_SAVE_ERROR = "Unable to save the image in the gallery"

    private var imageFileSavePath: String? = null
    private var imageEditedFileSavePath: String? = null
    private var imageFileUri: Uri? = null
    private var imagePickedContentUri: Uri? = null
    private var isEdited = false
    private var isFirstRequest = true
    private var isSaved = false

    private var settings = CameraSettings()

    fun getPhoto(
        settings: CameraSettings = CameraSettings(), onCallback: ((String) -> Unit)? = null
    ) {
        isEdited = false
        this.settings = settings
        doShow(onCallback)
    }

    fun pickImages(
        settings: CameraSettings = CameraSettings(), onCallback: ((String) -> Unit)? = null
    ) {
        this.settings = settings
        openPhotos(true, false, onCallback)
    }

    fun pickLimitedLibraryPhotos(onCallback: ((String) -> Unit)? = null) {
        onCallback?.let { it("not supported on android") }
    }

    fun getLimitedLibraryPhotos(onCallback: ((String) -> Unit)? = null) {
        onCallback?.let { it("not supported on android") }
    }

    private fun doShow(onCallback: ((String) -> Unit)?) {
        when (settings.source) {
            CameraSource.CAMERA -> showCamera(onCallback)
            CameraSource.PHOTOS -> showPhotos(onCallback)
            else -> showPrompt(onCallback = onCallback)
        }
    }

    private fun showPrompt(
        promptHeader: String = "Photo",
        promptPhoto: String = "From Photos",
        promptPicture: String = "Take Picture",
        onCallback: ((String) -> Unit)?
    ) {
        // We have all necessary permissions, open the camera
        val options: MutableList<String> = ArrayList()
        options.add(promptPhoto)
        options.add(promptPicture)
        val fragment = CameraBottomSheetDialogFragment()
        fragment.setTitle(promptHeader)
        fragment.setOptions(options = options,
            listener = object : CameraBottomSheetDialogFragment.BottomSheetListener {
                override fun onSelected(index: Int) {
                    when (index) {
                        0 -> {
                            settings.source = CameraSource.PHOTOS
                            openPhotos(onCallback)
                        }
                        1 -> {
                            settings.source = CameraSource.CAMERA
                            openCamera(onCallback)
                        }
                        else -> {}
                    }
                }

                override fun onCanceled() {
                    onCallback?.let { it("User cancelled photos app") }
                }
            })
        App.dwebViewActivity?.let { activity ->
            fragment.show(activity.supportFragmentManager, "capacitorModalsActionSheet")
        }
    }

    private fun showCamera(onCallback: ((String) -> Unit)?) {
        if (!App.appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            onCallback?.let { it(NO_CAMERA_ERROR) }
            return
        }
        openCamera(onCallback)
    }

    private fun showPhotos(onCallback: ((String) -> Unit)?) {
        openPhotos(onCallback)
    }

    private fun checkCameraPermissions(onCallback: ((String) -> Unit)?): Boolean {
        // if the manifest does not contain the camera permissions key, we don't need to ask the user
        /*val needCameraPerms: Boolean = isPermissionDeclared(CAMERA)
        val hasCameraPerms = !needCameraPerms || getPermissionState(CAMERA) === PermissionState.GRANTED
        val hasPhotoPerms = getPermissionState(PHOTOS) === PermissionState.GRANTED

        // If we want to save to the gallery, we need two permissions
        if (settings.saveToGallery && !(hasCameraPerms && hasPhotoPerms) && isFirstRequest) {
          isFirstRequest = false
          val aliases = if (needCameraPerms) {
            arrayOf(CAMERA, PHOTOS)
          } else {
            arrayOf(PHOTOS)
          }
          requestPermissionForAliases(aliases, call, "cameraPermissionsCallback")
          return false
        } else if (!hasCameraPerms) {
          requestPermissionForAlias(CAMERA, call, "cameraPermissionsCallback")
          return false
        }*/
        return true
    }

    private fun checkPhotosPermissions(onCallback: ((String) -> Unit)?): Boolean {
        /*if (getPermissionState(PHOTOS) !== PermissionState.GRANTED) {
          requestPermissionForAlias(PHOTOS, call, "cameraPermissionsCallback")
          return false
        }*/
        return true
    }

    private fun cameraPermissionsCallback(methodName: String, onCallback: ((String) -> Unit)?) {
        if (methodName.equals("pickImages")) {
            openPhotos(true, true, onCallback)
        } else {
            if (settings.source === CameraSource.CAMERA && getPermissionState(CAMERA) !== PackageManager.PERMISSION_GRANTED) {
                onCallback?.let { it(PERMISSION_DENIED_ERROR_CAMERA) }
                return
            } else if (settings.source === CameraSource.PHOTOS && getPermissionState(PHOTOS) !== PackageManager.PERMISSION_GRANTED) {
                onCallback?.let { it(PERMISSION_DENIED_ERROR_PHOTOS) }
                return
            }
            doShow(onCallback)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun openCamera(onCallback: ((String) -> Unit)?) {
        if (checkCameraPermissions(onCallback)) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(App.appContext.packageManager) != null) {
                // If we will be saving the photo, send the target file along
                try {
                    val appId: String = Device.getId() //getAppId()
                    val photoFile: File = CameraUtils.createImageFile()
                    imageFileSavePath = photoFile.absolutePath
                    // TODO: Verify provider config exists
                    imageFileUri =
                        FileProvider.getUriForFile(App.appContext, "$appId.fileprovider", photoFile)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri)
                } catch (ex: Exception) {
                    onCallback?.let { it("$IMAGE_FILE_SAVE_ERROR -- $ex") }
                    return
                }
                App.dwebViewActivity?.let { activity ->
                    startActivityForResult(activity, takePictureIntent, ProcessCameraImage, null)
                }
            } else {
                onCallback?.let { it(NO_CAMERA_ACTIVITY_ERROR) }
            }
        }
    }

    fun openPhotos(onCallback: ((String) -> Unit)?) {
        openPhotos(false, false, onCallback)
    }

    private fun openPhotos(
        multiple: Boolean, skipPermission: Boolean, onCallback: ((String) -> Unit)?
    ) {
        if (skipPermission || checkPhotosPermissions(onCallback)) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
            intent.type = "image/*"
            try {
                val requestCode = if (multiple) {
                    intent.putExtra("multi-pick", true)
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
                    ProcessPickedImages
                } else {
                    ProcessPickedImage
                }
                App.dwebViewActivity?.let { activity ->
                    startActivityForResult(activity, intent, requestCode, null)
                }
            } catch (ex: ActivityNotFoundException) {
                onCallback?.let { it(NO_PHOTO_ACTIVITY_ERROR) }
            }
        }
    }

    private fun processCameraImage(result: ActivityResult?, onCallback: ((String) -> Unit)?) {
        if (imageFileSavePath == null) {
            onCallback?.let { it(IMAGE_PROCESS_NO_FILE_ERROR) }
            return
        }
        // Load the image as a Bitmap
        val f = File(imageFileSavePath!!)
        val bmOptions = BitmapFactory.Options()
        val contentUri = Uri.fromFile(f)
        val bitmap = BitmapFactory.decodeFile(imageFileSavePath!!, bmOptions)
        if (bitmap == null) {
            onCallback?.let { it("User cancelled photos app") }
            return
        }
        returnResult(bitmap, contentUri, onCallback)
    }

    private fun processPickedImage(result: ActivityResult, onCallback: ((String) -> Unit)?) {
        result.data?.data?.let { uri ->
            imagePickedContentUri = uri
            processPickedImage(uri, onCallback)
        } ?: { onCallback?.let { it("No image picked") } }
    }

    private fun processPickedImages(
        result: ActivityResult, onCallback: ((String) -> Unit)?
    ): JSONObject {
        val ret = JSONObject()
        result.data?.let { data ->
            val executor: Executor = Executors.newSingleThreadExecutor()
            executor.execute {
                val photos = JSONArray()
                if (data.clipData != null) {
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        val imageUri = data.clipData!!.getItemAt(i).uri
                        val processResult: JSONObject = processPickedImages(imageUri)
                        if (processResult.getString("error").isNotEmpty()) {
                            onCallback?.let { it(processResult.getString("error")) }
                            return@execute
                        } else {
                            photos.put(processResult)
                        }
                    }
                } else if (data.data != null) {
                    val imageUri = data.data!!
                    val processResult: JSONObject = processPickedImages(imageUri)
                    if (processResult.getString("error").isNotEmpty()) {
                        onCallback?.let { it(processResult.getString("error")) }
                        return@execute
                    } else {
                        photos.put(processResult)
                    }
                } else if (data.extras != null) {
                    val bundle = data.extras
                    if (bundle!!.keySet().contains("selectedItems")) {
                        val fileUris = bundle.getParcelableArrayList<Parcelable>("selectedItems")
                        if (fileUris != null) {
                            for (fileUri in fileUris) {
                                if (fileUri is Uri) {
                                    try {
                                        val processResult: JSONObject = processPickedImages(fileUri)
                                        if (processResult.getString("error").isNotEmpty()) {
                                            onCallback?.let { it(processResult.getString("error")) }
                                            return@execute
                                        } else {
                                            photos.put(processResult)
                                        }
                                    } catch (ex: SecurityException) {
                                        onCallback?.let { it("SecurityException") }
                                    }
                                }
                            }
                        }
                    }
                }
                ret.put("photos", photos)
                //call.resolve(ret)
            }
        } ?: {
            onCallback?.let { it("No images picked") }
        }
        return ret
    }

    private fun processPickedImage(imageUri: Uri, onCallback: ((String) -> Unit)?) {
        var imageStream: InputStream? = null
        try {
            imageStream = App.appContext.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(imageStream) ?: return
            returnResult(bitmap, imageUri, onCallback)
        } catch (err: OutOfMemoryError) {
            onCallback?.let { it("Out of memory") }
        } catch (ex: FileNotFoundException) {
            onCallback?.let { it("No such image found -- $ex") }
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close()
                } catch (e: IOException) {
                    //Logger.error(getLogTag(), UNABLE_TO_PROCESS_IMAGE, e)
                }
            }
        }
    }

    private fun processPickedImages(imageUri: Uri): JSONObject {
        var imageStream: InputStream? = null
        val ret = JSONObject()
        try {
            imageStream = App.appContext.contentResolver.openInputStream(imageUri)
            var bitmap = BitmapFactory.decodeStream(imageStream)
            if (bitmap == null) {
                ret.put("error", "Unable to process bitmap")
                return ret
            }
            val exif: ExifWrapper = CameraUtils.getExifData(bitmap, imageUri)
            bitmap = try {
                prepareBitmap(bitmap, imageUri, exif)
            } catch (e: IOException) {
                ret.put("error", UNABLE_TO_PROCESS_IMAGE)
                return ret
            }
            // Compress the final image and prepare for output to client
            val bitmapOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, settings.quality, bitmapOutputStream)
            val newUri = getTempImage(imageUri, bitmapOutputStream)
            if (newUri != null) {
                newUri.path?.let { exif.copyExif(it) }
                ret.put("format", "jpeg")
                ret.put("exif", exif.toJson())
                ret.put("path", newUri.toString())
                ret.put(
                    "webPath",
                    "webPath"/*FileUtils.getPortablePath(getContext(), bridge.getLocalUrl(), newUri)*/
                )
            } else {
                ret.put("error", UNABLE_TO_PROCESS_IMAGE)
            }
            return ret
        } catch (err: OutOfMemoryError) {
            ret.put("error", "Out of memory")
        } catch (ex: FileNotFoundException) {
            ret.put("error", "No such image found")
            //Logger.error(getLogTag(), "No such image found", ex)
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close()
                } catch (e: IOException) {
                    //Logger.error(getLogTag(), UNABLE_TO_PROCESS_IMAGE, e)
                }
            }
        }
        return ret
    }

    private fun processEditedImage(result: ActivityResult, onCallback: ((String) -> Unit)?) {
        isEdited = true
        if (result.resultCode == Activity.RESULT_CANCELED) {
            // User cancelled the edit operation, if this file was picked from photos,
            // process the original picked image, otherwise process it as a camera photo
            if (imagePickedContentUri != null) {
                processPickedImage(imagePickedContentUri!!, onCallback)
            } else {
                processCameraImage(result, onCallback)
            }
        } else {
            processPickedImage(result, onCallback)
        }
    }

    /**
     * Save the modified image on the same path,
     * or on a temporary location if it's a content url
     * @param uri
     * @param is
     * @return
     * @throws IOException
     */
    private fun saveImage(uri: Uri, `is`: InputStream): Uri {
        var outFile = if (uri.scheme == "content") {
            getTempFile(uri)
        } else {
            File(uri.path)
        }
        try {
            writePhoto(outFile, `is`)
        } catch (ex: FileNotFoundException) {
            // Some gallery apps return read only file url, create a temporary file for modifications
            outFile = getTempFile(uri)
            writePhoto(outFile, `is`)
        }
        return Uri.fromFile(outFile)
    }

    private fun writePhoto(outFile: File?, `is`: InputStream) {
        val fos = FileOutputStream(outFile)
        val buffer = ByteArray(1024)
        var len: Int
        while (`is`.read(buffer).also { len = it } != -1) {
            fos.write(buffer, 0, len)
        }
        fos.close()
    }

    private fun getTempFile(uri: Uri?): File {
        var filename = Uri.parse(Uri.decode(uri.toString())).lastPathSegment
        if (!filename!!.contains(".jpg") && !filename.contains(".jpeg")) {
            filename += "." + Date().time + ".jpeg"
        }
        val cacheDir: File = App.appContext.cacheDir
        return File(cacheDir, filename)
    }

    /**
     * After processing the image, return the final result back to the caller.
     * @param call
     * @param bitmap
     * @param u
     */
    private fun returnResult(bitmap: Bitmap, u: Uri, onCallback: ((String) -> Unit)?) {
        var bitmap = bitmap
        val exif: ExifWrapper = CameraUtils.getExifData(bitmap, u)
        bitmap = try {
            prepareBitmap(bitmap, u, exif)
        } catch (e: IOException) {
            onCallback?.let { it(UNABLE_TO_PROCESS_IMAGE) }
            return
        }
        // Compress the final image and prepare for output to client
        val bitmapOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, settings.quality, bitmapOutputStream)
        if (settings.allowEditing && !isEdited) {
            editImage(u, bitmapOutputStream, onCallback)
            return
        }
        if (settings.saveToGallery && (imageEditedFileSavePath != null || imageFileSavePath != null)) {
            isSaved = true
            try {
                val fileToSavePath = imageEditedFileSavePath ?: imageFileSavePath!!
                val fileToSave = File(fileToSavePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver: ContentResolver = App.appContext.contentResolver
                    val values = ContentValues()
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileToSave.name)
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
                    val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val uri = resolver.insert(contentUri, values)
                        ?: throw IOException("Failed to create new MediaStore record.")
                    val stream =
                        resolver.openOutputStream(uri)
                            ?: throw IOException("Failed to open output stream.")
                    val inserted =
                        bitmap.compress(Bitmap.CompressFormat.JPEG, settings.quality, stream)
                    if (!inserted) {
                        isSaved = false
                    }
                } else {
                    val inserted = MediaStore.Images.Media.insertImage(
                        App.appContext.contentResolver, fileToSavePath, fileToSave.name, ""
                    )
                    if (inserted == null) {
                        isSaved = false
                    }
                }
            } catch (e: FileNotFoundException) {
                isSaved = false
                //Logger.error(getLogTag(), IMAGE_GALLERY_SAVE_ERROR, e)
            } catch (e: IOException) {
                isSaved = false
                //Logger.error(getLogTag(), IMAGE_GALLERY_SAVE_ERROR, e)
            }
        }
        if (settings.resultType === CameraResultType.BASE64) {
            returnBase64(exif, bitmapOutputStream, onCallback)
        } else if (settings.resultType === CameraResultType.URI) {
            returnFileURI(exif, u, bitmapOutputStream, onCallback)
        } else if (settings.resultType === CameraResultType.DATAURL) {
            returnDataUrl(exif, bitmapOutputStream, onCallback)
        } else {
            //call.reject(INVALID_RESULT_TYPE_ERROR)
            onCallback?.let { it(INVALID_RESULT_TYPE_ERROR) }
        }
        // Result returned, clear stored paths and images
        if (settings.resultType !== CameraResultType.URI) {
            deleteImageFile()
        }
        imageFileSavePath = null
        imageFileUri = null
        imagePickedContentUri = null
        imageEditedFileSavePath = null
    }

    private fun deleteImageFile() {
        if (imageFileSavePath != null && !settings.saveToGallery) {
            val photoFile = File(imageFileSavePath!!)
            if (photoFile.exists()) {
                photoFile.delete()
            }
        }
    }

    private fun returnFileURI(
        exif: ExifWrapper,
        u: Uri,
        bitmapOutputStream: ByteArrayOutputStream,
        onCallback: ((String) -> Unit)?
    ): JSONObject {
        val ret = JSONObject()
        getTempImage(u, bitmapOutputStream)?.let { uri ->
            uri.path?.let { exif.copyExif(it) }
            ret.put("format", "jpeg")
            ret.put("exif", exif.toJson())
            ret.put("path", uri.toString())
            ret.put(
                "webPath",
                "webPath"/*FileUtils.getPortablePath(getContext(), bridge.getLocalUrl(), newUri)*/
            )
            ret.put("saved", isSaved)
            //call.resolve(ret)
        } ?: { onCallback?.let { it(UNABLE_TO_PROCESS_IMAGE) } }
        return ret
    }

    private fun getTempImage(u: Uri, bitmapOutputStream: ByteArrayOutputStream): Uri? {
        var bis: ByteArrayInputStream? = null
        var newUri: Uri? = null
        try {
            bis = ByteArrayInputStream(bitmapOutputStream.toByteArray())
            newUri = saveImage(u, bis)
        } catch (ex: IOException) {
        } finally {
            if (bis != null) {
                try {
                    bis.close()
                } catch (e: IOException) {
                    // Logger.error(getLogTag(), UNABLE_TO_PROCESS_IMAGE, e)
                }
            }
        }
        return newUri
    }

    /**
     * Apply our standard processing of the bitmap, returning a new one and
     * recycling the old one in the process
     * @param bitmap
     * @param imageUri
     * @param exif
     * @return
     */
    private fun prepareBitmap(bitmap: Bitmap, imageUri: Uri, exif: ExifWrapper): Bitmap {
        var bitmap = bitmap
        if (settings.shouldCorrectOrientation) {
            val newBitmap: Bitmap = CameraUtils.correctOrientation(bitmap, imageUri, exif)
            bitmap = replaceBitmap(bitmap, newBitmap)
        }
        if (settings.shouldResize) {
            val newBitmap = CameraUtils.resize(bitmap, settings.width, settings.height)
            bitmap = replaceBitmap(bitmap, newBitmap)
        }
        return bitmap
    }

    private fun replaceBitmap(bitmap: Bitmap, newBitmap: Bitmap): Bitmap {
        if (bitmap != newBitmap) {
            bitmap.recycle()
        }
        return newBitmap
    }

    private fun returnDataUrl(
        exif: ExifWrapper,
        bitmapOutputStream: ByteArrayOutputStream,
        onCallback: ((String) -> Unit)?
    ): JSONObject {
        val byteArray: ByteArray = bitmapOutputStream.toByteArray()
        val encoded: String =
            android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        val data = JSONObject()
        data.put("format", "jpeg")
        data.put("dataUrl", "data:image/jpeg;base64,$encoded")
        data.put("exif", exif.toJson())
        //call.resolve(data)
        return data
    }

    private fun returnBase64(
        exif: ExifWrapper,
        bitmapOutputStream: ByteArrayOutputStream,
        onCallback: ((String) -> Unit)?
    ): JSONObject {
        val byteArray: ByteArray = bitmapOutputStream.toByteArray()
        val encoded: String =
            android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        val data = JSONObject()
        data.put("format", "jpeg")
        data.put("base64String", encoded)
        data.put("exif", exif.toJson())
        // call.resolve(data)
        return data
    }

    /*fun requestPermissions(call: PluginCall) {
      // If the camera permission is defined in the manifest, then we have to prompt the user
      // or else we will get a security exception when trying to present the camera. If, however,
      // it is not defined in the manifest then we don't need to prompt and it will just work.
      if (isPermissionDeclared(CAMERA)) {
        // just request normally
        super.requestPermissions(call)
      } else {
        // the manifest does not define camera permissions, so we need to decide what to do
        // first, extract the permissions being requested
        val providedPerms: JSArray = call.getArray("permissions")
        var permsList: List<String?>? = null
        try {
          permsList = providedPerms.toList()
        } catch (e: JSONException) {
        }
        if (permsList != null && permsList.size == 1 && permsList.contains(CAMERA)) {
          // the only thing being asked for was the camera so we can just return the current state
          checkPermissions(call)
        } else {
          // we need to ask about photos so request storage permissions
          requestPermissionForAlias(PHOTOS, call, "checkPermissions")
        }
      }
    }*/

    private fun getPermissionState(permission: String): Int {
        return PackageManager.PERMISSION_GRANTED
    }

    private fun editImage(
        uri: Uri, bitmapOutputStream: ByteArrayOutputStream, onCallback: ((String) -> Unit)?
    ) {
        try {
            val tempImage = getTempImage(uri, bitmapOutputStream)
            val editIntent = createEditIntent(tempImage)
            App.dwebViewActivity?.let { activity ->
                if (editIntent != null) {
                    startActivityForResult(activity, editIntent, ProcessEditedImage, null)
                } else {
                    onCallback?.let { it(IMAGE_EDIT_ERROR) }
                }
            }

        } catch (ex: Exception) {
            onCallback?.let { it("$IMAGE_EDIT_ERROR -- $ex") }
        }
    }

    private fun createEditIntent(origPhotoUri: Uri?): Intent? {
        return try {
            val editFile = File(origPhotoUri!!.path)
            val editUri: Uri = FileProvider.getUriForFile(
                App.appContext, App.appContext.packageName + ".fileprovider", editFile
            )
            val editIntent = Intent(Intent.ACTION_EDIT)
            editIntent.setDataAndType(editUri, "image/*")
            imageEditedFileSavePath = editFile.absolutePath
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            editIntent.addFlags(flags)
            editIntent.putExtra(MediaStore.EXTRA_OUTPUT, editUri)
            val resInfoList: List<ResolveInfo> =
                App.appContext.packageManager.queryIntentActivities(
                    editIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                App.appContext.grantUriPermission(packageName, editUri, flags)
            }
            editIntent
        } catch (ex: Exception) {
            null
        }
    }
}
