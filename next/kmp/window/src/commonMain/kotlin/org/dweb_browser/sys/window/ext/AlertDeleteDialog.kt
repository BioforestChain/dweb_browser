package org.dweb_browser.sys.window.ext


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.CommonI18n

@Composable
fun AlertDeleteDialog(
  onDismissRequest: () -> Unit,
  onDelete: suspend () -> Unit,
  icon: (@Composable () -> Unit)? = null,
  message: (@Composable () -> Unit)? = null,
  title: (@Composable () -> Unit)? = null,
  deleteText: String,
  cancelText: String = CommonI18n.cancel(),
  jobCancelable: Boolean = false,
) {
  val scope = rememberCoroutineScope()
  var deleteJob by remember { mutableStateOf<Job?>(null) }
  AlertDialog(
    onDismissRequest = onDismissRequest,
    icon = icon ?: {
      Icon(
        Icons.TwoTone.DeleteForever,
        CommonI18n.delete(),
        tint = MaterialTheme.colorScheme.error
      )
    },
    title = title,
    text = message,
    confirmButton = {
      FilledTonalButton(
        {
          deleteJob = scope.launch {
            onDelete()
          }.apply {
            invokeOnCompletion {
              onDismissRequest()
            }
          }
        },
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.error,
        ),
        enabled = deleteJob == null
      ) {
        Text(deleteText)
      }
    },
    dismissButton = {
      TextButton(
        {
          if (jobCancelable) {
            deleteJob?.cancel()
            deleteJob = null
          }
          onDismissRequest()
        }, enabled = when {
          jobCancelable -> true
          else -> deleteJob == null
        }
      ) {
        Text(cancelText)
      }
    },
  )
}
