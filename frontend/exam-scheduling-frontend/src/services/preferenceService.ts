import axios, {AxiosError, type AxiosResponse} from 'axios';

// ===== REQUEST INTERFACES =====

export interface SubmitPreferencesRequest {
    professorId?: string;
    examSessionPeriodId: string;
    preferredTimeSlots: PreferredTimeSlot[];
    unavailableTimeSlots: UnavailableTimeSlot[];
    additionalNotes?: string;
}

export interface PreferredTimeSlot {
    dayOfWeek: string;
    startTime: string;
    endTime: string;
    priority: number;
}

export interface UnavailableTimeSlot {
    dayOfWeek: string;
    startTime: string;
    endTime: string;
    reason: string;
}

export interface UpdatePreferencesRequest {
    submissionId?: string;
    professorId: string;
    examSessionPeriodId: string;
    preferredTimeSlots: PreferredTimeSlot[];
    unavailableTimeSlots: UnavailableTimeSlot[];
    additionalNotes?: string;
}

export interface WithdrawPreferencesRequest {
    submissionId?: string;
    professorId: string;
    reason: string;
}

export interface CreateExamSessionPeriodRequest {
    academicYear: string;
    examSession: string;
    createdBy: string;
    plannedStartDate: string;
    plannedEndDate: string;
    description?: string;
}

export interface OpenSubmissionWindowRequest {
    examSessionPeriodId?: string;
    submissionDeadline: string;
    openedBy?: string;
    academicYear: string;
    examSession: string;
}

export interface CloseSubmissionWindowRequest {
    examSessionPeriodId?: string;
    reason?: string;
    closedBy?: string;
}

// ===== RESPONSE INTERFACES =====

export interface PreferenceSubmissionSummary {
    submissionId: string;
    professorId: string;
    professorName: string;
    professorEmail: string;
    examSessionPeriodId: string;
    examSessionInfo: string;
    submittedAt: string;
    lastUpdatedAt?: string;
    status: 'SUBMITTED' | 'UPDATED' | 'WITHDRAWN';
    preferredSlotsCount: number;
    unavailableSlotsCount: number;
    hasAdditionalNotes: boolean;
    additionalNotes?: string;
    preferredTimeSlots: PreferredTimeSlot[];
    unavailableTimeSlots: UnavailableTimeSlot[];
}

export interface ExamSessionPeriodView {
    examSessionPeriodId: string;
    academicYear: string;
    examSession: string;
    startDate: string;
    endDate: string;
    description?: string;
    isWindowOpen: boolean;
    submissionDeadline?: string;
    instructions?: string;
    createdAt: string;
    createdBy: string;
    windowOpenedAt?: string;
    windowOpenedBy?: string;
    windowClosedAt?: string;
    windowClosedBy?: string;
    windowClosedReason?: string;
    totalSubmissions: number;
    uniqueProfessors: number;
}

export interface PreferenceStatistics {
    examSessionPeriodId: string;
    totalSubmissions: number;
    uniqueProfessors: number;
    averagePreferredSlots: number;
    averageUnavailableSlots: number;
    mostPopularTimeSlots: TimeSlotStatistic[];
    leastPopularTimeSlots: TimeSlotStatistic[];
    conflictingTimeSlots: TimeSlotConflict[];
    generatedAt: string;
}

export interface TimeSlotStatistic {
    dayOfWeek: string;
    startTime: string;
    endTime: string;
    count: number;
    percentage: number;
}

export interface TimeSlotConflict {
    dayOfWeek: string;
    startTime: string;
    endTime: string;
    conflictingProfessors: string[];
    conflictCount: number;
    severity: 'LOW' | 'MEDIUM' | 'HIGH';
}

export interface ApiResponse<T> {
    success: boolean;
    data?: T;
    error?: string;
    message?: string;
    timestamp: string;
    submissionId?: string;
    examSessionPeriodId?: string;
    count?: number;
    sessionId?: string;
    totalSubmissions?: number;
    uniqueProfessors?: number;
    conflictCount?: number;
    // Backend specific response fields
    preferences?: T;
    availableSessions?: T;
    sessions?: T;
    statistics?: any;
    conflicts?: T;
}

export interface ExamScheduleEntry {
    scheduleId: string;
    examSessionPeriodId: string;
    examSessionInfo: string;
    course: {
        courseId: string;
        courseName: string;
        courseCode: string;
        department: string;
    };
    professor: {
        professorId: string;
        professorName: string;
        professorEmail: string;
    };
    scheduledDateTime: string;
    duration: number;
    location: {
        building: string;
        room: string;
        capacity: number;
    };
    examType: 'WRITTEN' | 'ORAL' | 'PRACTICAL' | 'PROJECT';
    status: 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'RESCHEDULED';
    notes?: string;
    lastUpdated: string;
}

export interface ExamSessionPeriod {
    examSessionPeriodId: string;
    academicYear: string;
    examSession: string;
    startDate: string;
    endDate: string;
    description?: string;
    isPublished: boolean;
    publishedAt?: string;
    totalExams: number;
}

export interface ScheduleFilter {
    sessionId?: string;
    department?: string;
    courseCode?: string;
    professorName?: string;
    examType?: string;
    dateFrom?: string;
    dateTo?: string;
}

// ===== PREFERENCE SERVICE =====

class PreferenceService {
    private baseUrl = 'http://localhost:8000/api';

    constructor() {
    }

    // ===== PROFESSOR ENDPOINTS =====

    async getMyPreferences(sessionId?: string): Promise<PreferenceSubmissionSummary[]> {
        try {
            const url = sessionId
                ? `${this.baseUrl}/professor/my-preferences?sessionId=${sessionId}`
                : `${this.baseUrl}/professor/my-preferences`;

            const response: AxiosResponse<ApiResponse<PreferenceSubmissionSummary[]>> =
                await axios.get(url);

            return response.data.preferences || response.data.data || [];
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch preferences");
            throw error;
        }
    }

    async submitMyPreferences(request: SubmitPreferencesRequest): Promise<string> {
        try {
            const response: AxiosResponse<ApiResponse<string>> = await axios.post(
                `${this.baseUrl}/professor/preferences`,
                request
            );

            return response.data.submissionId || response.data.data || '';
        } catch (error: unknown) {
            this.handleError(error, "Failed to submit preferences");
            throw error;
        }
    }

    async getAvailableSessions(): Promise<ExamSessionPeriodView[]> {
        try {
            const response: AxiosResponse<ApiResponse<ExamSessionPeriodView[]>> =
                await axios.get(`${this.baseUrl}/professor/available-sessions`);

            return response.data.availableSessions || response.data.data || [];
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch available sessions");
            throw error;
        }
    }

    // ===== GENERAL PREFERENCE ENDPOINTS =====

    async updatePreferences(submissionId: string, request: UpdatePreferencesRequest): Promise<void> {
        try {
            await axios.put(`${this.baseUrl}/preferences/${submissionId}`, request);
        } catch (error: unknown) {
            this.handleError(error, "Failed to update preferences");
            throw error;
        }
    }

    async withdrawPreferences(submissionId: string, request: WithdrawPreferencesRequest): Promise<void> {
        try {
            await axios.delete(`${this.baseUrl}/preferences/${submissionId}`, {data: request});
        } catch (error: unknown) {
            this.handleError(error, "Failed to withdraw preferences");
            throw error;
        }
    }

    async getPreferencesByProfessor(professorId: string, sessionId?: string): Promise<PreferenceSubmissionSummary[]> {
        try {
            const url = sessionId
                ? `${this.baseUrl}/preferences/professor/${professorId}?sessionId=${sessionId}`
                : `${this.baseUrl}/preferences/professor/${professorId}`;

            const response: AxiosResponse<ApiResponse<PreferenceSubmissionSummary[]>> =
                await axios.get(url);

            return response.data.preferences || response.data.data || [];
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch professor preferences");
            throw error;
        }
    }

    async getPreferencesBySession(sessionId: string): Promise<{
        preferences: PreferenceSubmissionSummary[];
        statistics: PreferenceStatistics;
    }> {
        try {
            const response: AxiosResponse<ApiResponse<any>> =
                await axios.get(`${this.baseUrl}/preferences/session/${sessionId}`);

            return {
                preferences: response.data.preferences || response.data.data || [],
                statistics: response.data.statistics || {}
            };
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch session preferences");
            throw error;
        }
    }

    async getConflictingPreferences(sessionId: string): Promise<TimeSlotConflict[]> {
        try {
            const response: AxiosResponse<ApiResponse<TimeSlotConflict[]>> =
                await axios.get(`${this.baseUrl}/preferences/session/${sessionId}/conflicts`);

            return response.data.conflicts || response.data.data || [];
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch conflicting preferences");
            throw error;
        }
    }

    // ===== ADMIN SESSION ENDPOINTS =====

    async createExamSessionPeriod(request: CreateExamSessionPeriodRequest): Promise<string> {
        try {
            const response: AxiosResponse<ApiResponse<string>> = await axios.post(
                `${this.baseUrl}/admin/sessions`,
                request
            );

            return response.data.examSessionPeriodId || response.data.data || '';
        } catch (error: unknown) {
            this.handleError(error, "Failed to create exam session period");
            throw error;
        }
    }

    async openSubmissionWindow(sessionId: string, request: OpenSubmissionWindowRequest): Promise<void> {
        try {
            await axios.post(`${this.baseUrl}/admin/sessions/open-window?sessionId=${encodeURIComponent(sessionId)}`, request);
        } catch (error: unknown) {
            this.handleError(error, "Failed to open submission window");
            throw error;
        }
    }

    async closeSubmissionWindow(sessionId: string, request: CloseSubmissionWindowRequest): Promise<void> {
        try {
            await axios.post(`${this.baseUrl}/admin/sessions/close-window?sessionId=${encodeURIComponent(sessionId)}`, request);
        } catch (error: unknown) {
            this.handleError(error, "Failed to close submission window");
            throw error;
        }
    }

    async getAllExamSessionPeriods(): Promise<ExamSessionPeriodView[]> {
        try {
            const response: AxiosResponse<ApiResponse<ExamSessionPeriodView[]>> =
                await axios.get(`${this.baseUrl}/admin/sessions`);

            return response.data.sessions || response.data.data || [];
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch exam session periods");
            throw error;
        }
    }

    // ===== SCHEDULE VIEWING ENDPOINTS =====

    async getPublishedSessions(): Promise<ExamSessionPeriod[]> {
        try {
            // This endpoint would be implemented in your scheduling service
            const response: AxiosResponse<ApiResponse<ExamSessionPeriod[]>> =
                await axios.get(`${this.baseUrl}/schedules/published-sessions`);

            return response.data.data || [];
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch published sessions");
            throw error;
        }
    }

    async getSchedulesBySession(sessionId: string, filters?: ScheduleFilter): Promise<ExamScheduleEntry[]> {
        try {
            const params = new URLSearchParams();
            if (filters) {
                Object.entries(filters).forEach(([key, value]) => {
                    if (value !== undefined && value !== null && value !== '') {
                        params.append(key, value.toString());
                    }
                });
            }

            const url = params.toString()
                ? `${this.baseUrl}/schedules/session/${sessionId}?${params.toString()}`
                : `${this.baseUrl}/schedules/session/${sessionId}`;

            const response: AxiosResponse<ApiResponse<ExamScheduleEntry[]>> = await axios.get(url);

            return response.data.data || [];
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch schedules");
            throw error;
        }
    }

    async exportSchedule(sessionId: string, format: 'PDF' | 'CSV' | 'ICS'): Promise<void> {
        try {
            const response = await axios.get(
                `${this.baseUrl}/schedules/session/${sessionId}/export?format=${format}`,
                {responseType: 'blob'}
            );

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `exam-schedule-${sessionId}.${format.toLowerCase()}`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (error: unknown) {
            this.handleError(error, "Failed to export schedule");
            throw error;
        }
    }

    // ===== UTILITY METHODS =====

    private handleError(error: unknown, defaultMessage: string): void {
        const axiosError = error as AxiosError<ApiResponse<any>>;

        if (axiosError.response?.data) {
            const errorData = axiosError.response.data;
            console.error(`${defaultMessage}:`, {
                status: axiosError.response.status,
                error: errorData.error,
                message: errorData.message,
                timestamp: errorData.timestamp
            });
        } else {
            console.error(`${defaultMessage}:`, axiosError.message);
        }
    }

    // ===== HELPER METHODS =====

    formatTimeSlot(timeSlot: PreferredTimeSlot | UnavailableTimeSlot): string {
        return `${timeSlot.dayOfWeek} ${timeSlot.startTime}-${timeSlot.endTime}`;
    }

    getDayOfWeekOptions(): { value: string; label: string }[] {
        return [
            {value: 'MONDAY', label: 'Monday'},
            {value: 'TUESDAY', label: 'Tuesday'},
            {value: 'WEDNESDAY', label: 'Wednesday'},
            {value: 'THURSDAY', label: 'Thursday'},
            {value: 'FRIDAY', label: 'Friday'},
            {value: 'SATURDAY', label: 'Saturday'},
            {value: 'SUNDAY', label: 'Sunday'}
        ];
    }

    getExamSessionOptions(): { value: string; label: string }[] {
        return [
            {value: 'WINTER', label: 'Winter Session'},
            {value: 'SUMMER', label: 'Summer Session'},
            {value: 'AUTUMN', label: 'Autumn Session'}
        ];
    }

    getPriorityOptions(): { value: number; label: string }[] {
        return [
            {value: 1, label: '1 - Highest Priority'},
            {value: 2, label: '2 - High Priority'},
            {value: 3, label: '3 - Medium Priority'},
            {value: 4, label: '4 - Low Priority'},
            {value: 5, label: '5 - Lowest Priority'}
        ];
    }

    getDepartments(): string[] {
        return ['Computer Science', 'Mathematics', 'Physics', 'Chemistry', 'Biology'];
    }

    getExamTypes(): { value: string; label: string }[] {
        return [
            {value: 'WRITTEN', label: 'Written Exam'},
            {value: 'ORAL', label: 'Oral Exam'},
            {value: 'PRACTICAL', label: 'Practical Exam'},
            {value: 'PROJECT', label: 'Project Presentation'}
        ];
    }
}

export const preferenceService = new PreferenceService();
export default preferenceService;