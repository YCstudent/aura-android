package com.edistrive.aura.data.network

import com.edistrive.aura.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // -------------------------------------------------------------------------
    // Auth & User
    // -------------------------------------------------------------------------
    @POST("users/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("users/code_login/")
    suspend fun codeLogin(@Body request: CodeLoginRequest): Response<LoginResponse>

    @POST("users/send_code/")
    suspend fun sendCode(@Body request: SendCodeRequest): Response<ApiMessageResponse>

    @POST("users/register/")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("users/logout/")
    suspend fun logout(): Response<Unit>

    @GET("users/me/")
    suspend fun getCurrentUser(): Response<User>

    @PUT("users/update_profile/")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<User>

    @Multipart
    @POST("users/upload_avatar/")
    suspend fun uploadAvatar(@Part image: MultipartBody.Part): Response<User>

    @DELETE("users/delete_account/")
    suspend fun deleteAccount(): Response<Unit>

    @POST("users/change_password/")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiMessageResponse>

    @POST("users/reset_password/")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiMessageResponse>

    @POST("users/change_email/")
    suspend fun changeEmail(@Body request: ChangeEmailRequest): Response<ApiMessageResponse>

    @POST("users/change_phone/")
    suspend fun changePhone(@Body request: ChangePhoneRequest): Response<ApiMessageResponse>

    @GET("users/recent_activities/")
    suspend fun getRecentActivities(@Query("limit") limit: Int = 5): Response<ActivitiesResponse>

    // -------------------------------------------------------------------------
    // Family
    // -------------------------------------------------------------------------
    @GET("family/members/")
    suspend fun getFamilyMembers(
        @Query("list_type") listType: String = "active"
    ): Response<FamilyMembersResponse>

    @GET("family/members/{id}/")
    suspend fun getFamilyMember(@Path("id") id: Int): Response<FamilyMember>

    @POST("family/members/")
    suspend fun createFamilyMember(@Body request: CreateFamilyMemberRequest): Response<FamilyMember>

    @PUT("family/members/{id}/")
    suspend fun updateFamilyMember(
        @Path("id") id: Int,
        @Body request: UpdateFamilyMemberRequest
    ): Response<FamilyMember>

    @PATCH("family/members/{id}/")
    suspend fun patchFamilyMember(
        @Path("id") id: Int,
        @Body request: UpdateDisplayNameRequest
    ): Response<FamilyMember>

    @DELETE("family/members/{id}/")
    suspend fun deleteFamilyMember(@Path("id") id: Int): Response<Unit>

    @POST("family/members/{id}/activate/")
    suspend fun activateFamilyMember(@Path("id") id: Int): Response<ApiMessageResponse>

    @POST("family/members/{id}/deactivate/")
    suspend fun deactivateFamilyMember(@Path("id") id: Int): Response<ApiMessageResponse>

    @POST("family/members/{id}/unlink/")
    suspend fun unlinkFamilyMember(@Path("id") id: Int): Response<ApiMessageResponse>

    @POST("family/members/{id}/unlink_account/")
    suspend fun unlinkFamilyAccount(@Path("id") id: Int): Response<ApiMessageResponse>

    @POST("family/members/{id}/send_invitation/")
    suspend fun sendMemberInvitation(@Path("id") id: Int): Response<GenerateInviteCodeResponse>

    @POST("family/members/create_self/")
    suspend fun createSelfFamily(): Response<FamilyMember>

    @POST("family/members/generate_invite_code/")
    suspend fun generateInviteCode(): Response<GenerateInviteCodeResponse>

    @POST("family/members/preview_invitation/")
    suspend fun previewInvitation(@Body body: InvitationCodeBody): Response<PreviewInvitationResponse>

    @POST("family/members/accept_invitation/")
    suspend fun acceptInvitation(@Body body: InvitationCodeBody): Response<AcceptInvitationResponse>

    @GET("family/members/my_linked_profiles/")
    suspend fun getMyLinkedProfiles(): Response<List<FamilyMember>>

    // -------------------------------------------------------------------------
    // Appointments
    // -------------------------------------------------------------------------
    @GET("appointments/appointments/")
    suspend fun getAppointments(): Response<AppointmentsResponse>

    @GET("appointments/appointments/upcoming/")
    suspend fun getUpcomingAppointments(): Response<AppointmentsResponse>

    @GET("appointments/appointments/statistics/")
    suspend fun getAppointmentStatistics(): Response<AppointmentStatisticsResponse>

    @POST("appointments/appointments/{id}/complete/")
    suspend fun completeAppointment(@Path("id") id: Int): Response<ApiMessageResponse>

    @POST("appointments/appointments/")
    suspend fun createAppointment(@Body request: CreateAppointmentRequest): Response<Appointment>

    @DELETE("appointments/appointments/{id}/")
    suspend fun deleteAppointment(@Path("id") id: Int): Response<Unit>

    @POST("appointments/appointments/{id}/cancel/")
    suspend fun cancelAppointment(@Path("id") id: Int): Response<ApiMessageResponse>

    // -------------------------------------------------------------------------
    // Hospitals
    // -------------------------------------------------------------------------
    @GET("hospitals/")
    suspend fun getHospitals(
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null
    ): Response<List<Hospital>>

    @GET("hospitals/nearby/")
    suspend fun getNearbyHospitals(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int? = null
    ): Response<List<Hospital>>

    @GET("hospitals/geocode/")
    suspend fun geocodeCity(@Query("city") city: String): Response<GeocodeResponse>

    // -------------------------------------------------------------------------
    // Medical Records
    // -------------------------------------------------------------------------
    @GET("medical-records/records/")
    suspend fun getMedicalRecords(
        @Query("member_id") memberId: Int? = null,
        @Query("type") type: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("search") search: String? = null
    ): Response<MedicalRecordsResponse>

    @GET("medical-records/records/{id}/")
    suspend fun getMedicalRecord(@Path("id") id: Int): Response<MedicalRecord>

    @POST("medical-records/records/")
    suspend fun createMedicalRecord(@Body request: CreateMedicalRecordRequest): Response<MedicalRecord>

    @PUT("medical-records/records/{id}/")
    suspend fun updateMedicalRecord(
        @Path("id") id: Int,
        @Body request: UpdateMedicalRecordRequest
    ): Response<MedicalRecord>

    @DELETE("medical-records/records/{id}/")
    suspend fun deleteMedicalRecord(@Path("id") id: Int): Response<Unit>

    @POST("medical-records/records/ai_analyze/")
    suspend fun aiAnalyze(@Body body: AiAnalyzeRequest): Response<AiAnalyzeResponse>

    @Multipart
    @POST("medical-records/records/ocr_recognize/")
    suspend fun ocrRecognize(@Part image: MultipartBody.Part): Response<OcrRecognizeResponse>

    @Multipart
    @POST("medical-records/records/{id}/images/")
    suspend fun uploadMedicalRecordImages(
        @Path("id") id: Int,
        @Part images: List<MultipartBody.Part>
    ): Response<UploadImagesResponse>

    // -------------------------------------------------------------------------
    // Medications
    // -------------------------------------------------------------------------
    @GET("medications/medications/")
    suspend fun getMedications(
        @Query("member") memberId: Int? = null
    ): Response<MedicationsResponse>

    @GET("medications/medications/today/")
    suspend fun getTodayMedications(
        @Query("member") memberId: Int? = null
    ): Response<List<Medication>>

    @GET("medications/medications/{id}/")
    suspend fun getMedication(@Path("id") id: Int): Response<Medication>

    @POST("medications/medications/")
    suspend fun createMedication(@Body request: CreateMedicationRequest): Response<Medication>

    @PUT("medications/medications/{id}/")
    suspend fun updateMedication(
        @Path("id") id: Int,
        @Body request: UpdateMedicationRequest
    ): Response<Medication>

    @DELETE("medications/medications/{id}/")
    suspend fun deleteMedication(@Path("id") id: Int): Response<Unit>

    @POST("medications/medications/{id}/toggle/")
    suspend fun toggleMedication(@Path("id") id: Int): Response<ApiMessageResponse>

    @POST("medications/medications/{id}/check_in/")
    suspend fun checkInMedication(
        @Path("id") id: Int,
        @Body request: CheckInRequest
    ): Response<CheckInResponse>

    @POST("medications/records/take/")
    suspend fun takeMedication(@Body body: TakeMedicationBody): Response<TakeMedicationResponse>

    @POST("medications/records/cancel/")
    suspend fun cancelMedicationCheckIn(@Body body: TakeMedicationBody): Response<TakeMedicationResponse>

    // -------------------------------------------------------------------------
    // Medication Requests
    // -------------------------------------------------------------------------
    @GET("medications/requests/")
    suspend fun getMedicationRequests(): Response<MedicationRequestListResponse>

    @POST("medications/requests/")
    suspend fun sendMedicationRequest(
        @Body body: SendMedicationRequestBody
    ): Response<MedicationRequestModel>

    @POST("medications/requests/{id}/accept/")
    suspend fun acceptMedicationRequest(@Path("id") id: Int): Response<ApiMessageResponse>

    @POST("medications/requests/{id}/reject/")
    suspend fun rejectMedicationRequest(@Path("id") id: Int): Response<ApiMessageResponse>

    @POST("medications/requests/{id}/withdraw/")
    suspend fun withdrawMedicationRequest(@Path("id") id: Int): Response<ApiMessageResponse>

    @DELETE("medications/requests/{id}/")
    suspend fun deleteMedicationRequest(@Path("id") id: Int): Response<Unit>

    // -------------------------------------------------------------------------
    // Conversations (医小智 AI Chat)
    // -------------------------------------------------------------------------
    @GET("medical-records/conversations/")
    suspend fun getConversations(
        @Query("member_id") memberId: Int? = null
    ): Response<ConversationsListResponse>

    @POST("medical-records/conversations/")
    suspend fun createConversation(
        @Body request: CreateConversationRequest
    ): Response<Conversation>

    @GET("medical-records/conversations/{id}/")
    suspend fun getConversation(
        @Path("id") id: Int
    ): Response<Conversation>

    @PUT("medical-records/conversations/{id}/")
    suspend fun updateConversationTitle(
        @Path("id") id: Int,
        @Body request: UpdateConversationTitleRequest
    ): Response<Conversation>

    @DELETE("medical-records/conversations/{id}/")
    suspend fun deleteConversation(@Path("id") id: Int): Response<Unit>

    @POST("medical-records/conversations/{id}/set-feedback/")
    suspend fun setConversationFeedback(
        @Path("id") conversationId: Int,
        @Body request: FeedbackRequest
    ): Response<FeedbackResponse>

    @DELETE("medical-records/conversations/{id}/messages/{messageId}/")
    suspend fun deleteConversationMessage(
        @Path("id") conversationId: Int,
        @Path("messageId") messageId: Int
    ): Response<Unit>

    @POST("medical-records/conversations/{id}/favorite-message/")
    suspend fun favoriteMessage(
        @Path("id") conversationId: Int,
        @Body request: FavoriteMessageRequest
    ): Response<FavoriteResponse>

    @DELETE("medical-records/conversations/{id}/favorite-message/{messageId}/")
    suspend fun unfavoriteMessage(
        @Path("id") conversationId: Int,
        @Path("messageId") messageId: Int
    ): Response<Unit>

    @GET("medical-records/conversations/{id}/favorite-messages/")
    suspend fun getFavoriteMessages(
        @Path("id") conversationId: Int
    ): Response<FavoriteListResponse>

    @POST("medical-records/conversations/{id}/generate-title/")
    suspend fun generateConversationTitle(
        @Path("id") conversationId: Int,
        @Body request: GenerateTitleRequest
    ): Response<GenerateTitleResponse>

    // -------------------------------------------------------------------------
    // Notifications
    // -------------------------------------------------------------------------
    @GET("notifications/")
    suspend fun getNotifications(
        @Query("type") type: String? = null,
        @Query("is_read") isRead: Boolean? = null,
        @Query("search") search: String? = null
    ): Response<NotificationListResponse>

    @GET("notifications/{id}/")
    suspend fun getNotification(@Path("id") id: Int): Response<NotificationItem>

    @POST("notifications/{id}/mark_read/")
    suspend fun markNotificationRead(@Path("id") id: Int): Response<ApiMessageResponse>

    @POST("notifications/mark_all_read/")
    suspend fun markAllNotificationsRead(): Response<ApiMessageResponse>

    @POST("notifications/batch_delete/")
    suspend fun batchDeleteNotifications(@Body request: BatchDeleteRequest): Response<BatchDeleteResponse>

    @GET("notifications/unread_count/")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    // -------------------------------------------------------------------------
    // Schedules
    // -------------------------------------------------------------------------
    @GET("schedules/")
    suspend fun getSchedules(
        @Query("is_completed") isCompleted: Boolean? = null
    ): Response<ScheduleListResponse>

    @POST("schedules/")
    suspend fun createSchedule(@Body request: CreateScheduleRequest): Response<ScheduleResponse>

    @PUT("schedules/{id}/")
    suspend fun updateSchedule(
        @Path("id") id: Int,
        @Body request: CreateScheduleRequest
    ): Response<ScheduleResponse>

    @DELETE("schedules/{id}/")
    suspend fun deleteSchedule(@Path("id") id: Int): Response<Unit>

    @POST("schedules/{id}/toggle_complete/")
    suspend fun toggleScheduleComplete(@Path("id") id: Int): Response<ToggleCompleteResponse>

    @GET("schedules/stats/")
    suspend fun getScheduleStats(): Response<ScheduleStatsResponse>
}
