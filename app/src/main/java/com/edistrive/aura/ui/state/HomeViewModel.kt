package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.FamilyMember
import com.edistrive.aura.data.model.MedicalRecord
import com.edistrive.aura.data.model.Medication
import com.edistrive.aura.data.model.TakeMedicationBody
import com.edistrive.aura.data.model.UserActivity
import com.edistrive.aura.data.model.User
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.exp
import kotlin.math.roundToInt

data class HomeUiState(
    val isLoading: Boolean = false,
    val username: String = "用户",
    val greeting: String = "你好",
    val currentDate: String = "",
    val healthScore: Int = 100,
    val healthTip: String = "",
    val medicalRecordsCount: Int = 0,
    val familyMembersCount: Int = 0,
    val todayMedicationsCount: Int = 0,
    val recentActivitiesCount: Int = 0,
    val medicationsCount: Int = 0,
    val appointmentsCount: Int = 0,
    val currentUser: User? = null,
    val familyMembers: List<FamilyMember> = emptyList(),
    val todayMedications: List<Medication> = emptyList(),
    val recentActivities: List<UserActivity> = emptyList(),
    val medicalRecords: List<MedicalRecord> = emptyList(),
    val toast: String? = null,
    val toastIsError: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            greeting = calcGreeting(),
            currentDate = calcCurrentDate(),
            healthTip = calcHealthTip(100)
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                greeting = calcGreeting(),
                currentDate = calcCurrentDate()
            )

            val me = async { runCatching { apiService.getCurrentUser() }.getOrNull() }
            val family = async { runCatching { apiService.getFamilyMembers() }.getOrNull() }
            val todayMeds = async { runCatching { apiService.getTodayMedications() }.getOrNull() }
            val meds = async { runCatching { apiService.getMedications() }.getOrNull() }
            val activities = async { runCatching { apiService.getRecentActivities(limit = 5) }.getOrNull() }
            val appointments = async { runCatching { apiService.getAppointments() }.getOrNull() }
            val medicalRecords = async { runCatching { apiService.getMedicalRecords() }.getOrNull() }

            val meBody = me.await()?.takeIf { it.isSuccessful }?.body()
            val familyBody = family.await()?.takeIf { it.isSuccessful }?.body()
            val todayMedsBody = todayMeds.await()?.takeIf { it.isSuccessful }?.body()
            val medsBody = meds.await()?.takeIf { it.isSuccessful }?.body()
            val activitiesBody = activities.await()?.takeIf { it.isSuccessful }?.body()
            val appointmentsBody = appointments.await()?.takeIf { it.isSuccessful }?.body()
            val medicalRecordsBody = medicalRecords.await()?.takeIf { it.isSuccessful }?.body()

            val username = meBody?.username?.ifBlank { null } ?: "用户"
            val records = medicalRecordsBody?.results ?: emptyList()
            val newHealthScore = calculateHealthScore(records)

            // 调试日志
            android.util.Log.d("HomeViewModel", "今日用药数据: ${todayMedsBody?.size ?: 0} 条")
            todayMedsBody?.forEach { med ->
                android.util.Log.d("HomeViewModel", "用药: ${med.name}, 剂量: ${med.dosage}, 提醒时间: ${med.reminder_times}")
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                username = username,
                currentUser = meBody,
                medicalRecordsCount = records.size,
                medicalRecords = records,
                healthScore = newHealthScore,
                familyMembersCount = familyBody?.results?.size ?: 0,
                todayMedicationsCount = todayMedsBody?.size ?: 0,
                recentActivitiesCount = if (activitiesBody?.success == true) activitiesBody.activities.size else 0,
                medicationsCount = medsBody?.results?.count { it.is_active == true } ?: 0,
                appointmentsCount = appointmentsBody?.results?.count { it.status == "pending" } ?: 0,
                healthTip = calcHealthTip(newHealthScore),
                familyMembers = familyBody?.results ?: emptyList(),
                todayMedications = todayMedsBody ?: emptyList(),
                recentActivities = if (activitiesBody?.success == true) activitiesBody.activities else emptyList()
            )
            
            android.util.Log.d("HomeViewModel", "UI State 更新后 - todayMedications: ${_uiState.value.todayMedications.size} 条")
        }
    }

    fun takeMedication(medicationId: Int, time: String) {
        viewModelScope.launch {
            runCatching {
                apiService.takeMedication(TakeMedicationBody(medicationId, time))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(toast = "已标记 $time 服用")
                    refreshTodayMedications()
                } else {
                    _uiState.value = _uiState.value.copy(toast = "签到失败", toastIsError = true)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(toast = "网络异常", toastIsError = true)
            }
        }
    }

    fun cancelMedicationCheckIn(medicationId: Int, time: String) {
        viewModelScope.launch {
            runCatching {
                apiService.cancelMedicationCheckIn(TakeMedicationBody(medicationId, time))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(toast = "已取消 $time 签到")
                    refreshTodayMedications()
                } else {
                    _uiState.value = _uiState.value.copy(toast = "取消失败", toastIsError = true)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(toast = "网络异常", toastIsError = true)
            }
        }
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toast = null, toastIsError = false)
    }

    private fun refreshTodayMedications() {
        viewModelScope.launch {
            runCatching { apiService.getTodayMedications() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val meds = resp.body().orEmpty()
                        _uiState.value = _uiState.value.copy(
                            todayMedications = meds,
                            todayMedicationsCount = meds.size
                        )
                    }
                }
        }
    }

    private fun calcGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..5 -> "凌晨好"
            in 6..8 -> "早上好"
            in 9..11 -> "上午好"
            in 12..13 -> "中午好"
            in 14..17 -> "下午好"
            in 18..21 -> "晚上好"
            else -> "夜深了，注意休息"
        }
    }

    private fun calcCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
        return formatter.format(Date())
    }

    private fun calcHealthTip(score: Int): String {
        return when (score) {
            in 90..100 -> "您的健康状况良好，继续保持！"
            in 70..89 -> "健康状况一般，建议加强锻炼"
            in 50..69 -> "需要关注健康问题，建议就医"
            else -> "健康状况堪忧，请尽快就医"
        }
    }

    // ---------- 健康分计算 (匹配 iOS 算法) ----------

    private fun calculateHealthScore(records: List<MedicalRecord>): Int {
        if (records.isEmpty()) {
            android.util.Log.d("HomeViewModel", "🏥 健康分计算: 无病历记录 → 100分")
            return 100
        }

        var totalPenalty = 0.0
        val threeMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.time
        val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time

        // 筛选近3个月的病历
        val recentRecords = records.filter { record ->
            parseDate(record.effectiveDate)?.let { it >= threeMonthsAgo } ?: false
        }

        android.util.Log.d("HomeViewModel", "📊 病历分析: 总共${records.size}条，近3个月${recentRecords.size}条")

        // 对每条病历计算扣分
        var veryRecentCount = 0
        for (record in recentRecords) {
            val typeWeight = getTypeWeight(record.effectiveType ?: "门诊")
            if (typeWeight == 0.0) continue // 体检不扣分

            val severity = assessDiseaseSeverity(record)
            val timeDecay = getTimeDecayFactor(record.effectiveDate)
            val penalty = 5.0 * typeWeight * severity * timeDecay
            totalPenalty += penalty

            // 统计近30天内的就诊次数
            parseDate(record.effectiveDate)?.let { date ->
                if (date >= thirtyDaysAgo) veryRecentCount++
            }
        }

        // 频繁就诊惩罚
        if (veryRecentCount > 3) {
            val frequencyPenalty = (veryRecentCount - 3) * 3.0
            totalPenalty += frequencyPenalty
            android.util.Log.d("HomeViewModel", "  ⚠️ 频繁就诊惩罚: ${veryRecentCount}次 → -${frequencyPenalty}分")
        }

        val result = (100.0 - totalPenalty).roundToInt().coerceIn(40, 100)
        android.util.Log.d("HomeViewModel", "🏥 健康分计算: 基础100 - 总扣分${String.format("%.1f", totalPenalty)} = ${result}分")
        return result
    }

    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.parse(dateString.substringBefore("T").substringBefore(" "))
        } catch (_: Exception) {
            null
        }
    }

    private fun getTypeWeight(type: String): Double {
        return when (type) {
            "急诊" -> 2.5
            "住院" -> 3.0
            "门诊" -> 1.0
            "体检" -> 0.0
            else -> 1.0
        }
    }

    private fun assessDiseaseSeverity(record: MedicalRecord): Double {
        val symptoms = (record.symptoms ?: "").lowercase()
        val diagnosis = (record.possible_diseases ?: "").lowercase()
        val allText = "$symptoms $diagnosis"

        val severeKeywords = listOf("癌症", "肿瘤", "心梗", "中风", "脑梗", "肝硬化", "肾衰竭", "骨折", "急性", "重度", "危重")
        for (kw in severeKeywords) { if (kw in allText) return 3.0 }

        val moderateKeywords = listOf("肺炎", "高血压", "糖尿病", "冠心病", "胃溃疡", "肠胃炎", "发烧", "感染", "炎症")
        for (kw in moderateKeywords) { if (kw in allText) return 2.0 }

        val mildKeywords = listOf("感冒", "咳嗽", "头痛", "过敏", "鼻炎", "轻度", "小病")
        for (kw in mildKeywords) { if (kw in allText) return 1.0 }

        return 1.5
    }

    private fun getTimeDecayFactor(dateString: String?): Double {
        val recordDate = parseDate(dateString) ?: return 1.0
        val daysDiff = ((Date().time - recordDate.time) / (1000 * 60 * 60 * 24)).toDouble()
        return exp(-daysDiff / 30.0)
    }
}
