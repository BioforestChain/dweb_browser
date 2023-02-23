package info.bagen.libappmgr.ui.camera

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class QRCodeUIState(
  val show: MutableState<Boolean> = mutableStateOf(false)
)

sealed class QRCodeIntent {
  class OpenOrHide(val show: Boolean) : QRCodeIntent()
}

class QRCodeViewModel: ViewModel() {
  val uiState = QRCodeUIState()

  fun handleIntent(action: QRCodeIntent) {
    viewModelScope.launch {
      when (action) {
        is QRCodeIntent.OpenOrHide -> {
          uiState.show.value = action.show
        }
      }
    }
  }
}