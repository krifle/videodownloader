package com.example.videodownloader.ui.download

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.videodownloader.app.DefaultDownloadRepositoryFactory
import com.example.videodownloader.domain.DownloadRepository
import com.example.videodownloader.domain.DownloadRepositoryResult
import com.example.videodownloader.domain.UrlValidationResult
import com.example.videodownloader.domain.UrlValidator
import com.example.videodownloader.ui.theme.VideoDownloaderTheme
import com.example.videodownloader.util.ErrorMessageMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun DownloadScreen(
    modifier: Modifier = Modifier,
    repositoryFactory: (android.content.Context) -> DownloadRepository = {
        DefaultDownloadRepositoryFactory.create(it)
    },
) {
    val context = LocalContext.current
    val repository = remember(context) { repositoryFactory(context) }
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf(DownloadUiState()) }
    val validation = remember(uiState.url) { UrlValidator.validate(uiState.url) }
    val isWorking = uiState.status in setOf(
        DownloadStatus.FetchingPage,
        DownloadStatus.Extracting,
        DownloadStatus.VerifyingVideo,
        DownloadStatus.Downloading,
        DownloadStatus.Saving,
    )

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
                    url = uiState.url,
                    validation = validation,
                    onUrlChange = {
                        uiState = DownloadUiState(
                            url = it,
                            status = if (it.isBlank()) DownloadStatus.Idle else uiState.status,
                        )
                    },
                    onClear = { uiState = DownloadUiState() },
                    enabled = !isWorking,
                )
                DownloadActions(
                    validation = validation,
                    isWorking = isWorking,
                    onDownloadClick = {
                        val supported = (validation as? UrlValidationResult.Supported)?.value
                            ?: return@DownloadActions

                        scope.launch {
                            uiState = uiState.copy(
                                status = DownloadStatus.FetchingPage,
                                progress = null,
                                message = "게시물 페이지를 확인하는 중입니다.",
                                savedFileName = null,
                            )

                            val result = withContext(Dispatchers.IO) {
                                repository.download(supported) { progress ->
                                    scope.launch {
                                        uiState = uiState.copy(
                                            status = DownloadStatus.Downloading,
                                            progress = progress.fraction,
                                            message = "다운로드 중입니다.",
                                        )
                                    }
                                }
                            }

                            uiState = when (result) {
                                is DownloadRepositoryResult.Success -> uiState.copy(
                                    status = DownloadStatus.Completed,
                                    progress = 1f,
                                    message = "갤러리에 저장되었습니다.",
                                    savedFileName = result.fileName,
                                )
                                is DownloadRepositoryResult.Failure -> uiState.copy(
                                    status = DownloadStatus.Failed,
                                    progress = null,
                                    message = ErrorMessageMapper.fromExtractError(result.error),
                                )
                            }
                        }
                    },
                )
                StatusPanel(validation = validation, uiState = uiState)
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
    enabled: Boolean,
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
            enabled = enabled,
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
                enabled = enabled && url.isNotBlank(),
            ) {
                Text("초기화")
            }
        }
    }
}

@Composable
private fun DownloadActions(
    validation: UrlValidationResult,
    isWorking: Boolean,
    onDownloadClick: () -> Unit,
) {
    val supported = validation is UrlValidationResult.Supported

    Button(
        onClick = onDownloadClick,
        enabled = supported && !isWorking,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (isWorking) "처리 중" else "다운로드")
    }
}

@Composable
private fun StatusPanel(
    validation: UrlValidationResult,
    uiState: DownloadUiState,
) {
    val text = uiState.message ?: when (validation) {
        is UrlValidationResult.Supported ->
            "${validation.value.platform.displayName} ${validation.value.contentType.id} URL을 확인했습니다."
        is UrlValidationResult.Unsupported ->
            ErrorMessageMapper.fromValidationFailure(validation.reason)
    }
    val progress = uiState.progress

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
        if (uiState.savedFileName != null) {
            Text(
                text = uiState.savedFileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            progress != null -> LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.status in setOf(
                DownloadStatus.FetchingPage,
                DownloadStatus.Extracting,
                DownloadStatus.VerifyingVideo,
                DownloadStatus.Downloading,
                DownloadStatus.Saving,
            ) -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
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
    VideoDownloaderTheme {
        DownloadScreen()
    }
}
