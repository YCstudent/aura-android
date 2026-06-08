package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.Hospital
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HospitalsMapUiState(
    val isLoading: Boolean = false,
    val hospitals: List<Hospital> = emptyList(),
    val filteredHospitals: List<Hospital> = emptyList(),
    val searchText: String = "",
    val selectedLevel: String? = null,
    val selectedRadius: Int = 10,
    val manualLocationName: String? = null,
    val error: String? = null,
    val retryCount: Int = 0
)

@HiltViewModel
class HospitalsMapViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HospitalsMapUiState())
    val uiState: StateFlow<HospitalsMapUiState> = _uiState.asStateFlow()

    fun loadNearbyHospitals(
        latitude: Double,
        longitude: Double,
        locationName: String? = null,
        retryOnEmpty: Boolean = true,
        retryCount: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                manualLocationName = locationName,
                retryCount = retryCount
            )
            try {
                val resp = apiService.getNearbyHospitals(
                    latitude = latitude,
                    longitude = longitude,
                    radius = _uiState.value.selectedRadius * 1000
                )
                if (resp.isSuccessful) {
                    val hospitals = resp.body().orEmpty()

                    // 0 家医院 → 自动扩大半径重试一次（与 iOS 一致）
                    if (hospitals.isEmpty() && retryOnEmpty) {
                        val expandedRadius = maxOf(_uiState.value.selectedRadius, 20)
                        _uiState.value = _uiState.value.copy(selectedRadius = expandedRadius)
                        loadNearbyHospitals(
                            latitude = latitude,
                            longitude = longitude,
                            locationName = locationName,
                            retryOnEmpty = false,
                            retryCount = retryCount
                        )
                        return@launch
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hospitals = hospitals
                    )
                    applyFilters(distanceFilterEnabled = retryOnEmpty)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "加载医院数据失败"
                    )
                }
            } catch (e: Exception) {
                // 网络错误自动重试最多3次（与 iOS 一致）
                if (retryCount < 3) {
                    _uiState.value = _uiState.value.copy(retryCount = retryCount + 1)
                    kotlinx.coroutines.delay(2000)
                    loadNearbyHospitals(
                        latitude = latitude,
                        longitude = longitude,
                        locationName = locationName,
                        retryOnEmpty = retryOnEmpty,
                        retryCount = retryCount + 1
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "网络连接失败，请检查网络后下拉刷新重试"
                    )
                }
            }
        }
    }

    fun setSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
        applyFilters()
    }

    fun setSelectedLevel(level: String?) {
        _uiState.value = _uiState.value.copy(selectedLevel = level)
        applyFilters()
    }

    fun setSelectedRadius(radius: Int) {
        _uiState.value = _uiState.value.copy(selectedRadius = radius)
    }

    fun geocodeAndSetCity(cityName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                apiService.geocodeCity(cityName)
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null && body.location != null) {
                        val parts = body.location.split(",")
                        if (parts.size == 2) {
                            val lng = parts[0].toDoubleOrNull()
                            val lat = parts[1].toDoubleOrNull()
                            if (lat != null && lng != null) {
                                _uiState.value = _uiState.value.copy(manualLocationName = cityName)
                                loadNearbyHospitals(lat, lng)
                                return@launch
                            }
                        }
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "未找到该城市的位置信息")
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "城市定位失败")
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "网络连接失败")
            }
        }
    }

    fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading, error = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun applyFilters(distanceFilterEnabled: Boolean = true) {
        val state = _uiState.value
        var filtered = state.hospitals

        // Level filter（与 iOS HospitalLevelClassifier.matches 一致）
        state.selectedLevel?.let { level ->
            filtered = filtered.filter { hospital ->
                val normalized = normalizeLevel(hospital.level)
                when (level) {
                    "三甲" -> normalized == "三甲"
                    "二甲" -> normalized == "二甲"
                    "其他" -> normalized != "三甲" && normalized != "二甲"
                    else -> false
                }
            }
        }

        // Distance filter（与 iOS 一致：扩大半径重试时不重复过滤距离）
        if (distanceFilterEnabled) {
            val maxDistance = state.selectedRadius * 1000
            filtered = filtered.filter { hospital ->
                val dist = hospital.distance ?: return@filter true
                dist <= maxDistance
            }
        }

        // Name search
        if (state.searchText.isNotBlank()) {
            val keyword = state.searchText.lowercase()
            filtered = filtered.filter { it.name?.lowercase()?.contains(keyword) == true }
        }

        _uiState.value = state.copy(filteredHospitals = filtered)
    }

    private fun normalizeLevel(level: String?): String? {
        if (level == null) return null
        val n = level.lowercase().replace(" ", "").replace("级", "").replace("等", "").replace("医院", "")
        return when {
            listOf("三甲", "三级甲", "3甲", "3级甲", "三级甲等").any { n.contains(it.lowercase()) } -> "三甲"
            listOf("二甲", "二级甲", "2甲", "2级甲", "二级甲等").any { n.contains(it.lowercase()) } -> "二甲"
            // 宽松匹配（与 iOS HospitalLevelClassifier 一致）
            n.contains("三") && n.contains("甲") -> "三甲"
            n.contains("二") && n.contains("甲") -> "二甲"
            else -> null
        }
    }
}
