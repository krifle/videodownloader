package com.example.videodownaloder.ui.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.videodownaloder.domain.UrlValidationResult
import com.example.videodownaloder.domain.UrlValidator
import com.example.videodownaloder.ui.theme.VideodownaloderTheme
import com.example.videodownaloder.util.ErrorMessageMapper

@Composable
fun DownloadScreen(modifier: Modifier = Modifier) {
    var url by rememberSaveable { mutableStateOf("") }
    val validation = remember(url) { UrlValidator.validate(url) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Header()
                UrlInput(
                    url = url,
                    validation = validation,
                    onUrlChange = { url = it },
                    onClear = { url = "" },
                )
                DownloadActions(
                    validation = validation,
                    onDownloadClick = {
                        // 실제 다운로드 흐름은 다음 단계에서 Repository와 연결한다.
                    },
                )
                StatusPanel(validation = validation)
                PolicyNotice()
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "SNS Downloader",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Instagram과 Threads 공개 영상 URL을 입력해 기기에 저장합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UrlInput(
    url: String,
    validation: UrlValidationResult,
    onUrlChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val errorText = (validation as? UrlValidationResult.Unsupported)
        ?.takeIf { url.isNotBlank() }
        ?.let { ErrorMessageMapper.fromValidationFailure(it.reason) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("게시물 URL") },
            placeholder = { Text("https://www.instagram.com/reel/...") },
            supportingText = {
                if (errorText != null) {
                    Text(errorText)
                } else {
                    Text("공개 Reel, 공개 동영상 게시물, Threads post URL을 지원합니다.")
                }
            },
            isError = errorText != null,
            singleLine = false,
            minLines = 2,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(
                onClick = onClear,
                enabled = url.isNotBlank(),
            ) {
                Text("초기화")
            }
        }
    }
}

@Composable
private fun DownloadActions(
    validation: UrlValidationResult,
    onDownloadClick: () -> Unit,
) {
    val supported = validation is UrlValidationResult.Supported

    Button(
        onClick = onDownloadClick,
        enabled = supported,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("다운로드")
    }
}

@Composable
private fun StatusPanel(validation: UrlValidationResult) {
    val text = when (validation) {
        is UrlValidationResult.Supported -> {
            "${validation.value.platform.displayName} ${validation.value.contentType.id} URL을 확인했습니다."
        }
        is UrlValidationResult.Unsupported -> {
            ErrorMessageMapper.fromValidationFailure(validation.reason)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "상태",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { 0f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PolicyNotice() {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "이 앱은 개인 보관 용도로만 사용해야 하며, 콘텐츠에 대한 권리와 책임은 사용자에게 있습니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "비공개 콘텐츠, 접근 제한 콘텐츠 또는 타인의 저작물을 무단 저장하거나 재배포하는 용도로 사용하지 마세요.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun DownloadScreenPreview() {
    VideodownaloderTheme {
        DownloadScreen()
    }
}
