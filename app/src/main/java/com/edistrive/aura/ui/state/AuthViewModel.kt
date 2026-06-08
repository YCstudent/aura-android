package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.local.AuthPreferences
import com.edistrive.aura.data.model.CodeLoginRequest
import com.edistrive.aura.data.model.LoginRequest
import com.edistrive.aura.data.model.RegisterRequest
import com.edistrive.aura.data.model.ResetPasswordRequest
import com.edistrive.aura.data.model.SendCodeRequest
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val authPreferences: AuthPreferences
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun passwordLogin(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (username.isBlank() || password.isBlank()) {
            onError("请输入用户名和密码")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiService.login(LoginRequest(username = username, password = password))
                val code = resp.code()
                if (!resp.isSuccessful) {
                    onError(friendlyErrorMessage(parseErrorBody(resp.errorBody()?.string()), code, "login"))
                    return@launch
                }

                val body = resp.body()
                val token = body?.token
                if (token.isNullOrBlank()) {
                    onError(friendlyErrorMessage(body?.message, code, "login"))
                    return@launch
                }

                authPreferences.saveToken(token)
                authPreferences.setLoggedIn(true)

                val user = body.user
                if (user?.id != null) authPreferences.saveUserId(user.id)
                if (!user?.username.isNullOrBlank()) authPreferences.saveUsername(user!!.username!!)
                if (!user?.avatar.isNullOrBlank()) authPreferences.saveAvatarUrl(user!!.avatar!!)

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "网络错误")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendLoginCode(
        contact: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (contact.isBlank()) {
            onError("请输入手机号或邮箱")
            return
        }
        if (!isValidContact(contact)) {
            onError("请输入正确的手机号或邮箱")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = if (contact.contains("@")) {
                    SendCodeRequest(email = contact, purpose = "login")
                } else {
                    SendCodeRequest(phone = contact, purpose = "login")
                }
                val resp = apiService.sendCode(req)
                val code = resp.code()
                if (!resp.isSuccessful) {
                    onError(friendlyErrorMessage(parseErrorBody(resp.errorBody()?.string()), code, "send_code"))
                    return@launch
                }

                val msg = resp.body()
                if (msg?.success == false) {
                    onError(friendlyErrorMessage(msg.message, code, "send_code"))
                    return@launch
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "网络错误")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun codeLogin(
        contact: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (contact.isBlank() || code.isBlank()) {
            onError("请输入手机号/邮箱和验证码")
            return
        }
        if (!isValidContact(contact)) {
            onError("请输入正确的手机号或邮箱")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = if (contact.contains("@")) {
                    CodeLoginRequest(email = contact, code = code)
                } else {
                    CodeLoginRequest(phone = contact, code = code)
                }
                val resp = apiService.codeLogin(req)
                val httpStatus = resp.code()
                if (!resp.isSuccessful) {
                    onError(friendlyErrorMessage(parseErrorBody(resp.errorBody()?.string()), httpStatus, "login"))
                    return@launch
                }

                val body = resp.body()
                val token = body?.token
                if (token.isNullOrBlank()) {
                    onError(friendlyErrorMessage(body?.message, httpStatus, "login"))
                    return@launch
                }

                authPreferences.saveToken(token)
                authPreferences.setLoggedIn(true)

                val user = body.user
                if (user?.id != null) authPreferences.saveUserId(user.id)
                if (!user?.username.isNullOrBlank()) authPreferences.saveUsername(user!!.username!!)
                if (!user?.avatar.isNullOrBlank()) authPreferences.saveAvatarUrl(user!!.avatar!!)

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "网络错误")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendResetPasswordCode(
        contact: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (contact.isBlank()) {
            onError("请输入手机号或邮箱")
            return
        }
        if (!isValidContact(contact)) {
            onError("请输入正确的手机号或邮箱")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = if (contact.contains("@")) {
                    SendCodeRequest(email = contact, purpose = "reset_password")
                } else {
                    SendCodeRequest(phone = contact, purpose = "reset_password")
                }
                val resp = apiService.sendCode(req)
                val code = resp.code()
                if (!resp.isSuccessful) {
                    onError(friendlyErrorMessage(parseErrorBody(resp.errorBody()?.string()), code, "send_code"))
                    return@launch
                }
                val msg = resp.body()
                if (msg?.success == false) {
                    onError(friendlyErrorMessage(msg.message, code, "send_code"))
                    return@launch
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "网络错误")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetPassword(
        contact: String,
        code: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (contact.isBlank() || code.isBlank() || newPassword.isBlank()) {
            onError("请填写所有字段")
            return
        }
        if (newPassword.length < 6) {
            onError("密码至少6位")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val isEmail = contact.contains("@")
                val req = ResetPasswordRequest(
                    code = code,
                    new_password = newPassword,
                    phone = if (isEmail) null else contact,
                    email = if (isEmail) contact else null
                )
                val resp = apiService.resetPassword(req)
                val httpStatus = resp.code()
                if (!resp.isSuccessful) {
                    onError(friendlyErrorMessage(parseErrorBody(resp.errorBody()?.string()), httpStatus, "reset_password"))
                    return@launch
                }
                val msg = resp.body()
                if (msg?.success == false) {
                    onError(friendlyErrorMessage(msg.message, httpStatus, "reset_password"))
                    return@launch
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "网络错误")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendRegisterCode(
        contact: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (contact.isBlank()) {
            onError("请输入手机号或邮箱")
            return
        }
        if (!isValidContact(contact)) {
            onError("请输入正确的手机号或邮箱")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = if (contact.contains("@")) {
                    SendCodeRequest(email = contact, purpose = "register")
                } else {
                    SendCodeRequest(phone = contact, purpose = "register")
                }
                val resp = apiService.sendCode(req)
                val code = resp.code()
                if (!resp.isSuccessful) {
                    onError(friendlyErrorMessage(parseErrorBody(resp.errorBody()?.string()), code, "send_code"))
                    return@launch
                }

                val msg = resp.body()
                if (msg?.success == false) {
                    onError(friendlyErrorMessage(msg.message, code, "send_code"))
                    return@launch
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "网络错误")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun isValidContact(contact: String): Boolean {
        val trimmed = contact.trim()
        return trimmed.matches(Regex("^1[3-9]\\d{9}$")) ||
            trimmed.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
    }

    fun register(
        username: String,
        password: String,
        phone: String?,
        email: String?,
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (username.isBlank()) {
            onError("请输入用户名")
            return
        }
        if (username.length !in 3..10) {
            onError("用户名长度需为 3-10 位")
            return
        }
        if (!username.matches(Regex("^[\\w\\u4e00-\\u9fa5]+$"))) {
            onError("用户名只能包含字母、数字、下划线和中文")
            return
        }
        if (username.matches(Regex("^\\d+$"))) {
            onError("用户名不能为纯数字")
            return
        }
        if (username.first().isDigit()) {
            onError("用户名不能以数字开头")
            return
        }
        if (password.length < 6) {
            onError("密码至少6位")
            return
        }
        if ((phone.isNullOrBlank() && email.isNullOrBlank()) || (!phone.isNullOrBlank() && !email.isNullOrBlank())) {
            onError("请填写手机号或邮箱")
            return
        }
        if (!phone.isNullOrBlank() && !phone.matches(Regex("^1[3-9]\\d{9}$"))) {
            onError("请输入正确的手机号")
            return
        }
        if (!email.isNullOrBlank() && !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            onError("请输入正确的邮箱")
            return
        }
        if (code.isBlank()) {
            onError("请输入验证码")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiService.register(
                    RegisterRequest(
                        username = username,
                        password = password,
                        phone = phone,
                        email = email,
                        code = code
                    )
                )
                val httpCode = resp.code()
                if (!resp.isSuccessful) {
                    onError(friendlyErrorMessage(parseErrorBody(resp.errorBody()?.string()), httpCode, "register"))
                    return@launch
                }

                // 后端 /users/register/ 仅返回 {message, user}，不返回 token。
                // 注册成功后引导用户回登录页输入凭据登录，与 iOS 行为一致。
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "网络错误")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 解析后端返回的字段错误，DRF 的 errors 形如：
     *   {"username": ["该用户名已存在"], "code": ["验证码错误"]}
     * 或者 {"detail": "..."}, {"non_field_errors": ["..."]}
     */
    private fun parseErrorBody(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val json = com.google.gson.JsonParser.parseString(body)
            if (!json.isJsonObject) return null
            val obj = json.asJsonObject
            obj["detail"]?.takeIf { it.isJsonPrimitive }?.asString?.let { return it }
            obj["message"]?.takeIf { it.isJsonPrimitive }?.asString?.let { return it }
            obj["error"]?.takeIf { it.isJsonPrimitive }?.asString?.let { return it }
            obj["non_field_errors"]?.let { arr ->
                if (arr.isJsonArray && arr.asJsonArray.size() > 0) return arr.asJsonArray[0].asString
            }
            // Take the first field error
            for ((k, v) in obj.entrySet()) {
                if (k == "success") continue
                if (v.isJsonArray && v.asJsonArray.size() > 0) {
                    val first = v.asJsonArray[0]
                    if (first.isJsonPrimitive) return first.asString
                }
                if (v.isJsonPrimitive) return v.asString
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将服务端原始错误信息映射为用户友好文案，与 iOS LoginView 的错误分类逻辑一致。
     */
    private fun friendlyErrorMessage(
        rawMessage: String?,
        httpCode: Int,
        context: String // "login" | "send_code" | "register" | "reset_password"
    ): String {
        val msg = rawMessage?.trim().orEmpty()

        // HTTP 状态码兜底
        if (httpCode == 429) return "请求过于频繁，请稍后再试"
        if (httpCode in 500..599) return "服务器繁忙，请稍后重试"

        // ---- 验证码相关错误映射 ----
        if (context == "send_code" || context == "register" || context == "login" || context == "reset_password") {
            when {
                msg.contains("频繁") || msg.contains("太快") ||
                    msg.contains("too many") || msg.contains("rate limit") ||
                    msg.contains("throttled") ->
                    return "验证码请求过于频繁，请稍后再试"

                msg.contains("无效") && msg.contains("验证码") ||
                    msg.contains("invalid") && msg.contains("code") ->
                    return "验证码错误，请检查后重新输入"

                msg.contains("过期") || msg.contains("expired") ->
                    return "验证码已过期，请重新获取验证码"

                msg.contains("验证码") && (msg.contains("无效") || msg.contains("错误")) ->
                    return "验证码错误，请检查后重新输入"
            }
        }

        // ---- 用户状态相关 ----
        when {
            msg.contains("未注册") || msg.contains("不存在") || msg.contains("not registered") ||
                msg.contains("No user found") ->
                return "该账号尚未注册，请先注册后再登录"

            msg.contains("密码") && msg.contains("错误") ||
                msg.contains("password") && msg.contains("incorrect") ->
                return "密码错误，请检查后重新输入"

            msg.contains("该用户名已存在") || msg.contains("already exists") &&
                (msg.contains("username") || msg.contains("用户名")) ->
                return "该用户名已被注册，请更换用户名"

            msg.contains("该手机号已存在") || msg.contains("phone") && msg.contains("already exists") ->
                return "该手机号已被注册"

            msg.contains("该邮箱已存在") || msg.contains("email") && msg.contains("already exists") ->
                return "该邮箱已被注册"
        }

        // ---- 兜底 ----
        if (msg.isBlank() || msg.equals("UNKNOWN", ignoreCase = true) || msg.equals("unknown", ignoreCase = true)) {
            return when (context) {
                "login" -> "登录失败，请稍后重试"
                "send_code" -> "发送失败，请稍后重试"
                "register" -> "注册失败，请稍后重试"
                "reset_password" -> "重置失败，请稍后重试"
                else -> "服务器繁忙，请稍后重试"
            }
        }

        // 展示原始错误信息（已从前端格式化过）
        return msg
    }
}
