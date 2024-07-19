package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AddCircle
import androidx.compose.material.icons.twotone.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String
import org.dweb_browser.pure.crypto.hash.sha256Sync

@Composable
fun RegisterQuestion(viewModel: RegisterQuestionViewModel, modifier: Modifier = Modifier) {
  Column(modifier) {
    CardTitle("请自定义您的问题和答案")
    viewModel.tipMessage?.also { tipMessage ->
      CardDescription(
        tipMessage.tip,
        style = if (tipMessage.isError) TextStyle(color = MaterialTheme.colorScheme.error) else null
      )
    }
    val uiScope = rememberCoroutineScope()
    val qaItemsState = rememberLazyListState()
    LazyColumn(Modifier.weight(1f, false), state = qaItemsState) {
      itemsIndexed(viewModel.qaList, key = { _, it -> it.id }) { index, qa ->
        Row(
          Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(Modifier.padding(end = 8.dp).weight(1f)) {
            OutlinedTextField(
              qa.question,
              { qa.question = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("${index + 1}. 问题和提示") },
              placeholder = { Text("请输入您的问题") },
            )
            OutlinedTextField(
              qa.answer,
              { qa.answer = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("答案") },
              placeholder = { Text("请输入您的回答") },
            )
          }
          FilledTonalIconButton(
            {
              viewModel.removeItem(qa)
            },
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors().copy(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError
            ),
            enabled = viewModel.canRemoveItem,
          ) {
            Icon(Icons.TwoTone.Remove, "remove q&a item")
          }
        }
      }
    }
    CardActions(Modifier.align(Alignment.End)) {
      val scope = rememberCoroutineScope()
      OutlinedButton(
        {
          viewModel.addItem()
          uiScope.launch {
            qaItemsState.animateScrollToItem(viewModel.qaList.size - 1)
          }
        },
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
      ) {
        Icon(Icons.TwoTone.AddCircle, null)
        Text("添加条目")
      }
      Button(
        { scope.launch { viewModel.confirm() } },
        enabled = viewModel.canConfirm && !viewModel.registering
      ) {
        Text("确定")
      }
    }
  }
}


class QAController {
  val id = randomUUID()
  var question by mutableStateOf("")
  var answer by mutableStateOf("")
}

class RegisterQuestionViewModel(override val task: CompletableDeferred<ByteArray>) :
  RegisterViewModelTask(KeychainMethod.Question) {
  val qaList = mutableStateListOf(QAController())

  override fun doFinish(keyTipCallback: (ByteArray) -> Unit): ByteArray {


    /// 保存tip
    val questionList = qaList.map { it.question }
    keyTipCallback(Json.encodeToString(questionList).utf8Binary)

    /// 返回key
    val answerList = qaList.map { it.answer }
    val keyRawData = Json.encodeToString(answerList).utf8Binary
    return keyRawData
  }

  val canRemoveItem get() = qaList.size > 1
  fun removeItem(qa: QAController) {
    qaList.remove(qa)
  }


  fun addItem() {
    qaList.add(QAController())
  }

  var tipMessage by mutableStateOf<TipMessage?>(null)
    private set
  val canConfirm
    get() :Boolean {
      qaList.all { it.answer.isNotEmpty() }.falseAlso {
        tipMessage = TipMessage("回答不能放空")
        return false
      }.trueAlso {
        tipMessage = null
      }
      qaList.all { it.question.isNotEmpty() }.falseAlso {
        tipMessage = TipMessage("问题不能放空")
        return false
      }.trueAlso {
        tipMessage = null
      }

      return true
    }

  suspend fun confirm() {
    /// 整理数据
    qaList.toMutableList().forEach {
      it.question = it.question.trim()
      it.answer = it.answer.trim()
      if (it.question.isEmpty() && it.answer.isEmpty()) {
        qaList.remove(it)
      }
    }
    if (canConfirm) {
      finish()
    }
  }
}

@Composable
fun VerifyQuestion(viewModel: VerifyQuestionViewModel, modifier: Modifier = Modifier) {
  Column(modifier) {
    CardTitle("请回答您自定义的问题")
    LazyColumn {
      items(viewModel.qaList) { qa ->
        Column {
          Text(buildAnnotatedString {
            append("问题：")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
              append(qa.question)
            }
          })
          TextField(
            qa.answer,
            { qa.answer = it },
            label = { Text("答案") },
            placeholder = { Text("请输入您的回答") },
          )
        }
      }
    }
    Row(Modifier.align(Alignment.End).padding(vertical = 8.dp)) {
      val scope = rememberCoroutineScope()
      FilledTonalButton(
        { scope.launch { viewModel.confirm() } },
        enabled = viewModel.canConfirm && !viewModel.verifying
      ) {
        Text("确定")
      }
    }
  }
}

class VerifyQuestionViewModel(override val task: CompletableDeferred<ByteArray>) :
  VerifyViewModelTask(KeychainMethod.Question) {
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

  val canConfirm get() = qaList.all { it.answer.isNotEmpty() }
  suspend fun confirm() {
    qaList.forEach {
      it.answer = it.answer
    }
    if (canConfirm) {
      finish()
    }
  }

  override fun doFinish(): ByteArray {
    qaList.toMutableList().forEach {
      it.answer = it.answer.trim()
    }
    val answerList = qaList.map { it.answer }
    val keyRawData = sha256Sync(Json.encodeToString(answerList).utf8Binary)
    return keyRawData
  }
}