package com.edistrive.aura.data.model

import com.google.gson.annotations.SerializedName

// ============================================================================
// Base wrappers
// ============================================================================

data class ApiMessageResponse(
    val success: Boolean? = null,
    val message: String? = null
)

// ============================================================================
// Auth
// ============================================================================

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String? = null,
    val user: User? = null,
    val success: Boolean? = null,
    val message: String? = null
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val phone: String? = null,
    val email: String? = null,
    val code: String? = null
)

data class RegisterResponse(
    val token: String? = null,
    val user: User? = null,
    val success: Boolean? = null,
    val message: String? = null
)

data class SendCodeRequest(
    val phone: String? = null,
    val email: String? = null,
    val purpose: String? = null
)

data class CodeLoginRequest(
    val phone: String? = null,
    val email: String? = null,
    val code: String
)

// ============================================================================
// User
// ============================================================================

data class User(
    val id: Int? = null,
    val username: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val birth_date: String? = null,
    val gender: String? = null,
    val signature: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val blood_type: String? = null,
    val medical_history: String? = null,
    val allergy_history: String? = null,
    val chronic_diseases: String? = null,
    val surgery_history: String? = null,
    val medication_history: String? = null,
    val notes: String? = null,
    val created_at: String? = null,
    val profile_completed: Boolean? = null
)

data class UpdateProfileRequest(
    val username: String? = null,
    val signature: String? = null,
    val birth_date: String? = null,
    val gender: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val blood_type: String? = null,
    val medical_history: String? = null,
    val allergy_history: String? = null,
    val chronic_diseases: String? = null,
    val surgery_history: String? = null,
    val medication_history: String? = null,
    val notes: String? = null,
    val profile_completed: Boolean? = null
)

// ============================================================================
// Family
// ============================================================================

data class FamilyMember(
    val id: Int? = null,
    val name: String? = null,
    val display_name: String? = null,
    val avatar: String? = null,
    val avatar_url: String? = null,
    val relation: String? = null,
    val relation_display: String? = null,
    val gender: String? = null,
    val gender_display: String? = null,
    val birth_date: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val id_number: String? = null,
    val age: Int? = null,
    val blood_type: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val allergy_history: String? = null,
    val medical_history: String? = null,
    val surgery_history: String? = null,
    val medication_history: String? = null,
    val chronic_diseases: String? = null,
    val notes: String? = null,
    val authorization_status: String? = null,
    val authorization_status_display: String? = null,
    val linked_user: Int? = null,
    val linked_user_info: LinkedUserInfo? = null,
    val is_active: Boolean? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val invitation_code: String? = null,
    val invitation_sent_at: String? = null,
    val authorized_at: String? = null,
    val can_edit: Boolean? = null,
    val can_view: Boolean? = null,
    val is_linked: Boolean? = null
) {
    val preferredName: String
        get() = display_name?.takeIf { it.isNotBlank() } ?: name.orEmpty()

    val hasDisplayName: Boolean
        get() = !display_name.isNullOrBlank() && display_name != name

    val initial: String
        get() = preferredName.firstOrNull()?.toString() ?: "家"

    val displayAvatarUrl: String?
        get() {
            linked_user_info?.let { linked ->
                linked.avatar_url?.takeIf { it.isNotBlank() }?.let { return it }
                linked.avatar?.takeIf { it.isNotBlank() }?.let {
                    return if (it.startsWith("http")) it else "https://zgjcyl.com$it"
                }
            }
            avatar_url?.takeIf { it.isNotBlank() }?.let { return it }
            avatar?.takeIf { it.isNotBlank() }?.let {
                return if (it.startsWith("http")) it else "https://zgjcyl.com$it"
            }
            return null
        }

    val isAuthorized: Boolean get() = authorization_status == "authorized"
    val isStandalone: Boolean get() = authorization_status == "standalone"
    val isPending: Boolean get() = authorization_status == "pending"
}

data class LinkedUserInfo(
    val id: Int? = null,
    val username: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val avatar_url: String? = null
)

data class FamilyMembersResponse(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<FamilyMember>? = null,
    val success: Boolean? = null,
    val message: String? = null
)

data class CreateFamilyMemberRequest(
    val name: String,
    val relation: String,
    val gender: String,
    val birth_date: String? = null,
    val phone: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val blood_type: String? = null,
    val allergy_history: String? = null,
    val medical_history: String? = null,
    val surgery_history: String? = null,
    val medication_history: String? = null,
    val chronic_diseases: String? = null,
    val notes: String? = null
)

data class UpdateFamilyMemberRequest(
    val name: String? = null,
    val display_name: String? = null,
    val relation: String? = null,
    val gender: String? = null,
    val birth_date: String? = null,
    val phone: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val blood_type: String? = null,
    val allergy_history: String? = null,
    val medical_history: String? = null,
    val surgery_history: String? = null,
    val medication_history: String? = null,
    val chronic_diseases: String? = null,
    val notes: String? = null,
    val is_active: Boolean? = null
)

data class UpdateDisplayNameRequest(val display_name: String)

data class GenerateInviteCodeResponse(
    val invitation_code: String? = null,
    val message: String? = null,
    val success: Boolean? = null
)

data class InvitationCodeBody(val invitation_code: String)

data class PreviewMemberInfo(
    val name: String? = null,
    val avatar: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    val birth_date: String? = null,
    val phone: String? = null
)

data class PreviewInvitationResponse(
    val success: Boolean? = null,
    val member: PreviewMemberInfo? = null,
    val creator_name: String? = null,
    val creator_phone: String? = null,
    val is_generic_invite: Boolean? = null,
    val message: String? = null
)

data class AcceptInvitationResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val member: FamilyMember? = null,
    val requires_activation: Boolean? = null,
    val bidirectional: Boolean? = null,
    val synced: Boolean? = null
)

// ============================================================================
// Medical Records
// ============================================================================

data class MedicalRecord(
    val id: Int? = null,
    val title: String? = null,
    val type: String? = null,
    val record_type: String? = null,
    val visit_date: String? = null,
    val date: String? = null,
    val symptoms: String? = null,
    val possible_diseases: String? = null,
    val suggestion_desc: String? = null,
    val suggestion_tests: String? = null,
    val treatment_rx: String? = null,
    val treatment_otc: String? = null,
    val hospital: String? = null,
    val department: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val images: List<MedicalRecordImage>? = null,
    val family_member: Int? = null,
    val member_name: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
) {
    val effectiveDate: String? get() = visit_date ?: date
    val effectiveType: String? get() = type ?: record_type
}

data class MedicalRecordImage(
    val id: Int? = null,
    val image: String? = null,
    val image_url: String? = null
) {
    val displayUrl: String?
        get() {
            image_url?.takeIf { it.isNotBlank() }?.let {
                return if (it.startsWith("http")) it else "https://zgjcyl.com$it"
            }
            image?.takeIf { it.isNotBlank() }?.let {
                return if (it.startsWith("http")) it else "https://zgjcyl.com$it"
            }
            return null
        }
}

data class MedicalRecordsResponse(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<MedicalRecord>? = null,
    val success: Boolean? = null,
    val message: String? = null
)

data class CreateMedicalRecordRequest(
    val title: String,
    val type: String? = null,
    val visit_date: String? = null,
    val symptoms: String? = null,
    val possible_diseases: String? = null,
    val suggestion_desc: String? = null,
    val suggestion_tests: String? = null,
    val treatment_rx: String? = null,
    val treatment_otc: String? = null,
    val hospital: String? = null,
    val department: String? = null,
    val notes: String? = null,
    val family_member: Int? = null
)

data class UpdateMedicalRecordRequest(
    val title: String? = null,
    val type: String? = null,
    val visit_date: String? = null,
    val symptoms: String? = null,
    val possible_diseases: String? = null,
    val suggestion_desc: String? = null,
    val suggestion_tests: String? = null,
    val treatment_rx: String? = null,
    val treatment_otc: String? = null,
    val hospital: String? = null,
    val department: String? = null,
    val notes: String? = null
)

data class StructuredMedicalInfo(
    val title: String? = null,
    val type: String? = null,
    val hospital: String? = null,
    val department: String? = null,
    val symptoms: String? = null,
    val possible_diseases: String? = null,
    val suggestion_desc: String? = null,
    val suggestion_tests: String? = null,
    val treatment_rx: String? = null,
    val treatment_otc: String? = null
)

data class OcrRecognizeResponse(
    val success: Boolean? = null,
    val ocr_text: String? = null,
    val structured_data: StructuredMedicalInfo? = null,
    val error: String? = null,
    val message: String? = null
)

data class AiAnalyzeRequest(
    val symptoms: String,
    val ocr_text: String? = null
)

data class AiAnalyzeResponse(
    val success: Boolean? = null,
    val possible_diseases: String? = null,
    val suggestion_desc: String? = null,
    val suggestion_tests: String? = null,
    val treatment_rx: String? = null,
    val treatment_otc: String? = null,
    val structured_data: StructuredMedicalInfo? = null,
    val error: String? = null
)

data class UploadImagesResponse(
    val success: Boolean? = null,
    val images: List<MedicalRecordImage>? = null,
    val message: String? = null
)

// ============================================================================
// Medications
// ============================================================================

data class Medication(
    val id: Int? = null,
    val name: String? = null,
    val dosage: String? = null,
    val frequency: String? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val is_active: Boolean? = null,
    val notes: String? = null,
    val reminder_times: List<String>? = null,
    val today_times: List<String>? = null,
    val records_today: List<MedicationRecord>? = null,
    val family_member: Int? = null,
    val member_name: String? = null,
    val days_remaining: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class MedicationRecord(
    val id: Int? = null,
    val record_id: Int? = null,
    val medication_id: Int? = null,
    val time: String? = null,
    val reminder_time: String? = null,
    val status: String? = null,
    val taken_at: String? = null
) {
    val effectiveTime: String? get() = time ?: reminder_time
}

data class MedicationsResponse(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<Medication>? = null,
    val success: Boolean? = null,
    val message: String? = null
)

data class TodayMedicationsResponse(
    val results: List<Medication>? = null,
    val success: Boolean? = null,
    val message: String? = null
)

data class CreateMedicationRequest(
    val name: String,
    val dosage: String? = null,
    val frequency: String? = null,
    val reminder_times: List<String>? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val notes: String? = null,
    val family_member: Int? = null
)

data class UpdateMedicationRequest(
    val name: String? = null,
    val dosage: String? = null,
    val frequency: String? = null,
    val reminder_times: List<String>? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val notes: String? = null,
    val is_active: Boolean? = null,
    val family_member: Int? = null
)

data class TakeMedicationBody(
    val medication_id: Int,
    val time: String
)

data class TakeMedicationResponse(
    val success: Boolean? = null,
    val message: String? = null
)

data class CheckInRequest(
    @SerializedName("time_slot")
    val timeSlot: String
)

data class CheckInResponse(
    val success: Boolean? = null,
    val message: String? = null
)

// ----------------------------------------------------------------------------
// Medication Requests
// ----------------------------------------------------------------------------

data class MedicationRequestModel(
    val id: Int? = null,
    val from_user: Int? = null,
    val to_user: Int? = null,
    val from_username: String? = null,
    val to_username: String? = null,
    val member_name: String? = null,
    val medication_name: String? = null,
    val dosage: String? = null,
    val frequency: String? = null,
    val reminder_times: List<String>? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val duration_days: Int? = null,
    val instructions: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val status_display: String? = null,
    val message: String? = null,
    val family_member: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class MedicationRequestListResponse(
    val count: Int? = null,
    val results: List<MedicationRequestModel>? = null
)

data class SendMedicationRequestBody(
    val to_user: Int,
    val family_member: Int? = null,
    val medication_name: String,
    val dosage: String,
    val frequency: String,
    val reminder_times: List<String>,
    val start_date: String,
    val end_date: String? = null,
    val duration_days: Int? = null,
    val instructions: String? = null,
    val notes: String? = null,
    val message: String? = null
)

// ============================================================================
// Activities
// ============================================================================

data class UserActivity(
    val type: String? = null,
    val title: String? = null,
    @SerializedName("detail")
    val subtitle: String? = null,
    @SerializedName("time_ago")
    val time: String? = null,
    val icon: String? = null,
    val color: String? = null
)

data class ActivitiesResponse(
    val success: Boolean = false,
    val activities: List<UserActivity> = emptyList()
)

// ============================================================================
// Appointments
// ============================================================================

data class Appointment(
    val id: Int? = null,
    val appointment_type: String? = null,
    val title: String? = null,
    val hospital: String? = null,
    val department: String? = null,
    val doctor: String? = null,
    val appointment_date: String? = null,
    val appointment_time: String? = null,
    val duration: Int? = null,
    val remind_before: Int? = null,
    val location: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val reminder_sent: Boolean? = null,
    val family_member: Int? = null,
    val member_name: String? = null,
    val medical_record: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class AppointmentsResponse(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<Appointment>? = null
)

data class AppointmentStatisticsResponse(
    val upcoming: Int? = null,
    val today: Int? = null,
    val completed: Int? = null
)

data class CreateAppointmentRequest(
    val appointment_type: String = "outpatient",
    val title: String,
    val hospital: String,
    val department: String? = null,
    val doctor: String? = null,
    val appointment_date: String,
    val appointment_time: String,
    val duration: Int? = null,
    val remind_before: Int? = null,
    val location: String? = null,
    val notes: String? = null,
    val family_member: Int? = null
)

// ============================================================================
// Hospitals
// ============================================================================

data class Hospital(
    val id: String? = null,
    val name: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val level: String? = null,
    val location: String? = null,
    val rating: Double? = null,
    val distance: Int? = null
)

data class GeocodeResponse(
    val city: String? = null,
    val location: String? = null,
    val formatted_address: String? = null,
    val error: String? = null
)

// ============================================================================
// Notifications
// ============================================================================

data class NotificationItem(
    val id: Int? = null,
    val message_type: String? = null,
    val message_type_display: String? = null,
    val title: String? = null,
    val content: String? = null,
    val is_read: Boolean? = null,
    val related_medication_id: Int? = null,
    val related_appointment_id: Int? = null,
    val related_family_member_id: Int? = null,
    val extra_data: Map<String, Any?>? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val time_ago: String? = null
)

data class NotificationListResponse(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<NotificationItem>? = null
)

data class BatchDeleteRequest(val ids: List<Int>)

data class BatchDeleteResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val deleted_count: Int? = null
)

data class UnreadCountResponse(
    val total: Int? = null,
    val by_type: Map<String, TypeCount>? = null
)

data class TypeCount(
    val name: String? = null,
    val total: Int? = null,
    val unread: Int? = null
)

// ============================================================================
// Schedules
// ============================================================================

data class ScheduleResponse(
    val id: Int,
    val schedule_type: String? = null,
    val title: String? = null,
    val description: String? = null,
    val schedule_date: String? = null,
    val schedule_time: String? = null,
    val priority: String? = null,
    val is_completed: Boolean? = null,
    val reminder: Boolean? = null,
    val location: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class ScheduleListResponse(
    val success: Boolean? = null,
    val count: Int? = null,
    val schedules: List<ScheduleResponse>? = null,
    val results: List<ScheduleResponse>? = null
)

data class CreateScheduleRequest(
    val schedule_type: String,
    val title: String,
    val description: String,
    val schedule_date: String,
    val schedule_time: String,
    val priority: String,
    val reminder: Boolean,
    val location: String
)

data class ScheduleStatsResponse(
    val pending: Int? = null,
    val completed: Int? = null,
    val high_priority: Int? = null
)

data class ToggleCompleteResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val schedule: ScheduleResponse? = null
)

// ============================================================================
// Change Password
// ============================================================================

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

data class ResetPasswordRequest(
    val code: String,
    val new_password: String,
    val phone: String? = null,
    val email: String? = null
)

data class ChangeEmailRequest(
    val new_email: String,
    val code: String
)

data class ChangePhoneRequest(
    val new_phone: String,
    val code: String
)
