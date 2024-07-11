package org.dweb_browser.sys.keychain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String
import org.dweb_browser.pure.crypto.hash.jvmSha256

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterQuestion(viewModel: RegisterQuestionViewModel, modifier: Modifier = Modifier) {
  Column(modifier) {
    LazyColumn {
      items(viewModel.qaList) { qa ->
        SwipeToDismissBox(rememberSwipeToDismissBoxState(), backgroundContent = {
          FilledTonalButton(
            {
              viewModel.qaList.remove(qa)
            },
            shape = RectangleShape,
            modifier = Modifier.fillMaxHeight(),
            colors = ButtonDefaults.filledTonalButtonColors().copy(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError
            )
          ) {
            Icon(Icons.Default.Remove, contentDescription = "remove item")
            Text("删除")
          }
        }) {
          Column {
            TextField(
              qa.question,
              { qa.question = it },
              label = { Text("问题") },
              placeholder = { Text("请输入您的问题") },
            )
            TextField(
              qa.answer,
              { qa.answer = it },
              label = { Text("答案") },
              placeholder = { Text("请输入您的回答") },
            )
          }
        }
      }
    }
    Row(Modifier.align(Alignment.End).padding(vertical = 8.dp)) {
      FilledTonalButton({}) {
        Text("确定")
      }
    }
  }
}


class QAController {
  var question by mutableStateOf("")
  var answer by mutableStateOf("")
}

class RegisterQuestionViewModel(override val task: CompletableDeferred<ByteArray>) :
  RegisterViewModelTask() {
  override val method: KeychainMethod = KeychainMethod.Question
  val qaList = mutableListOf(QAController())

  override fun doFinish(keyTipCallback: (ByteArray) -> Unit): ByteArray {
    /// 整理数据
    qaList.toMutableList().forEach {
      it.question = it.question.trim()
      it.answer = it.answer.trim()
      if (it.question.isEmpty() && it.answer.isEmpty()) {
        qaList.remove(it)
      }
    }

    /// 保存tip
    val questionList = qaList.map { it.question }
    keyTipCallback(Json.encodeToString(questionList).utf8Binary)

    /// 返回key
    val answerList = qaList.map { it.answer }
    val keyRawData = Json.encodeToString(answerList).utf8Binary
    return keyRawData
  }
}


class VerifyQuestionViewModel(override val task: CompletableDeferred<ByteArray>) :
  VerifyViewModelTask() {
  override val method = KeychainMethod.Question
  val qaList = mutableListOf<QAController>()
  override fun keyTipCallback(keyTip: ByteArray?) {
    if (keyTip == null || keyTip.isEmpty()) {
      throw Exception("invalid root-key empty tip")
    }
    val questionList = runCatching {
      Json.decodeFromString<List<String>>(keyTip.utf8String)
    }.getOrElse {
      throw Exception("invalid root-key tip format", it)
    }
    questionList.forEach { q ->
      qaList.add(QAController().apply {
        question = q
      })
    }
  }

  override fun doFinish(): ByteArray {
    qaList.toMutableList().forEach {
      it.answer = it.answer.trim()
    }
    val answerList = qaList.map { it.answer }
    val keyRawData = jvmSha256(Json.encodeToString(answerList).utf8Binary)
    return keyRawData
  }
}