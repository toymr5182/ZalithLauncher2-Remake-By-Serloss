package com.movtery.zalithlauncher.utils.network

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import org.json.JSONObject

/**
 * 错误详情最多展示的字符数，避免把整页响应体塞进弹窗
 */
private const val MAX_ERROR_DETAIL_LENGTH = 300

fun ResponseException.toLocal(): AndroidStringText {
    val statusCode = response.status
    val codeString = statusCode.value.toString()
    val textRes = when (statusCode) {
        HttpStatusCode.BadRequest -> R.string.error_bad_request
        HttpStatusCode.Unauthorized -> R.string.error_unauthorized
        HttpStatusCode.Forbidden -> R.string.error_forbidden
        HttpStatusCode.NotFound -> R.string.error_notfound
        HttpStatusCode.NotAcceptable -> R.string.error_not_acceptable
        HttpStatusCode.RequestTimeout -> R.string.error_request_timeout
        HttpStatusCode.Conflict -> R.string.error_conflict
        HttpStatusCode.Gone -> R.string.error_gone
        HttpStatusCode.TooManyRequests -> R.string.error_too_many_requests
        HttpStatusCode.InternalServerError -> R.string.error_internal_server_error
        HttpStatusCode.BadGateway -> R.string.error_bad_gateway
        HttpStatusCode.ServiceUnavailable -> R.string.error_service_unavailable
        HttpStatusCode.GatewayTimeout -> R.string.error_gateway_timeout
        else -> return androidText(
            "($codeString) ${statusCode.description}"
        )
    }
    return androidText(textRes, codeString)
}

/**
 * 从 4xx/5xx 的响应体中提取服务端给出的真实失败原因。
 *
 * 微软登录相关接口会在响应体里返回可读的错误码（例如未配置 client_id 时的
 * AADSTS900144、XSTS 返回的错误说明），如果只展示 “(400) 服务器无法理解请求”，
 * 用户和开发者都无法定位问题。
 *
 * @return 适合展示的单行文本；服务端没有给出可用信息时返回 null
 */
suspend fun ResponseException.serverErrorDetail(): String? {
    //响应体可能已被上游消费，此时退回异常消息中缓存的 Text
    val bodyText = runCatching { response.safeBodyAsText() }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: message?.substringAfter("Text: \"", "")?.substringBeforeLast("\"")?.takeIf { it.isNotBlank() }
        ?: return null

    val detail = runCatching {
        JSONObject(bodyText).let { json ->
            listOf("error_description", "error", "Message", "message")
                .firstNotNullOfOrNull { key ->
                    json.optString(key).takeIf { it.isNotBlank() }
                }
        }
    }.getOrNull() ?: bodyText

    return detail
        //AAD 会在描述后附带 Trace/Correlation ID，对用户没有意义
        .substringBefore(" Trace ID:")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(MAX_ERROR_DETAIL_LENGTH)
        .takeIf { it.isNotBlank() }
}
