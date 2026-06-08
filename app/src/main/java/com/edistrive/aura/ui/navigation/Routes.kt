package com.edistrive.aura.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RESET_PASSWORD = "reset_password"
    const val EMAIL_BINDING = "email_binding"
    const val PHONE_BINDING = "phone_binding"
    const val PROFILE_COMPLETION = "profile_completion"
    const val PROFILE_CENTER = "profile_center"
    const val MAIN_TABS = "main_tabs"

    // Family
    const val FAMILY_MANAGEMENT = "family_management"
    const val FAMILY_ADD = "family_add"
    const val FAMILY_EDIT = "family_edit/{memberId}"
    fun familyEdit(memberId: Int) = "family_edit/$memberId"
    const val FAMILY_DETAIL = "family_detail/{memberId}"
    fun familyDetail(memberId: Int) = "family_detail/$memberId"
    const val ACCEPT_INVITATION = "accept_invitation"

    // Medical Records
    const val MEDICAL_RECORDS = "medical_records"
    const val MEDICAL_RECORD_ADD = "medical_record_add"
    const val MEDICAL_RECORD_EDIT = "medical_record_edit/{recordId}"
    fun medicalRecordEdit(recordId: Int) = "medical_record_edit/$recordId"
    const val MEDICAL_RECORD_DETAIL = "medical_record_detail/{recordId}"
    fun medicalRecordDetail(recordId: Int) = "medical_record_detail/$recordId"
    const val MEMBER_MEDICAL_RECORDS = "member_medical_records/{memberId}"
    fun memberMedicalRecords(memberId: Int) = "member_medical_records/$memberId"

    // Medications
    const val MEDICATIONS = "medications"
    const val MEDICATION_ADD = "medication_add"
    const val MEDICATION_EDIT = "medication_edit/{medicationId}"
    fun medicationEdit(medicationId: Int) = "medication_edit/$medicationId"
    const val MEMBER_MEDICATION = "member_medication/{memberId}"
    fun memberMedication(memberId: Int) = "member_medication/$memberId"
    const val SEND_MEDICATION_REQUEST = "send_medication_request/{memberId}"
    fun sendMedicationRequest(memberId: Int) = "send_medication_request/$memberId"

    // Health Report
    const val HEALTH_REPORT = "health_report/{memberId}"
    fun healthReport(memberId: Int) = "health_report/$memberId"

    // Digital Human (医小智)
    const val DIGITAL_HUMAN = "digital_human"

    // Hospitals
    const val HOSPITALS_MAP = "hospitals_map"
    const val HOSPITAL_DETAIL = "hospital_detail/{hospitalJson}"
    fun hospitalDetail(hospitalJson: String) = "hospital_detail/${android.net.Uri.encode(hospitalJson)}"

    // Appointments
    const val APPOINTMENTS = "appointments"
    const val ADD_APPOINTMENT = "add_appointment"

    // Schedule
    const val SCHEDULE = "schedule"

    // Activities
    const val RECENT_ACTIVITIES = "recent_activities"

    // Settings
    const val SETTINGS = "settings"
    const val CHANGE_PASSWORD = "change_password"
    const val PRIVACY_POLICY = "privacy_policy"
    const val USER_AGREEMENT = "user_agreement"
}
