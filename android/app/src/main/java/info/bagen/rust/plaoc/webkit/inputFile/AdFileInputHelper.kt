package info.bagen.rust.plaoc.webkit.inputFile

import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember


private const val TAG = "AdFileInputHelper"

@Stable
class AdFileInputHelper(
    val inputFileLauncher: ManagedActivityResultLauncher<InputFileOptions, List<Uri>>,
    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    var filePathCallback: ValueCallback<Array<Uri>>?
        get() = chromeFilePathCallback
        set(value) {
            chromeFilePathCallback = value
        }

    var requestPermissionCallback: ValueCallback<Boolean>?
        get() = chromeRequestPermissionCallback
        set(value) {
            chromeRequestPermissionCallback = value
        }

}

internal var chromeFilePathCallback: ValueCallback<Array<Uri>>? = null


internal var chromeRequestPermissionCallback: ValueCallback<Boolean>? = null

internal fun runChromeFilePathCallback(uriList: List<Uri>) {
    if (uriList.isNotEmpty()) {
        chromeFilePathCallback?.onReceiveValue(uriList.toTypedArray())
    } else {
        chromeFilePathCallback?.onReceiveValue(null)
    }
    chromeFilePathCallback = null
}

internal fun runRequestPermissionCallback(granted: Boolean) {
    chromeRequestPermissionCallback?.onReceiveValue(granted)
    chromeRequestPermissionCallback = null
}


@Composable
fun rememberAdFileInputHelper(
    inputFileLauncher: ManagedActivityResultLauncher<InputFileOptions, List<Uri>> = rememberLauncherForActivityResult(
        contract = InputFileActivityResultContract(),
        onResult = { result ->
            Log.i(TAG, "InputFile Result:$result")
            runChromeFilePathCallback(result)
        }),
    requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            Log.i(TAG, "RequestPermission Result:$granted")
            runRequestPermissionCallback(granted)
        }
    )
): AdFileInputHelper = remember(inputFileLauncher, requestPermissionLauncher) {
    AdFileInputHelper(
        inputFileLauncher,
        requestPermissionLauncher
    )
}
