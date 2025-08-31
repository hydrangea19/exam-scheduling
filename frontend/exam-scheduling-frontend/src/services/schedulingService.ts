import axios, { AxiosError, type AxiosResponse } from 'axios';


export interface CreateScheduleRequest {
    examSessionPeriodId: string;
    academicYear: string;
    examSession: string;
    startDate: string;
    endDate: string;
}

export interface AddExamRequest {
    scheduledExamId?: string;
    courseId: string;
    courseName: string;
    examDate: string;
    startTime: string;
    endTime: string;
    roomId?: string;
    roomName?: string;
    roomCapacity?: number;
    studentCount: number;
    mandatoryStatus: 'MANDATORY' | 'ELECTIVE';
    professorIds: string[];
}

export interface UpdateExamTimeRequest {
    examDate: string;
    startTime: string;
    endTime: string;
    reason: string;
}

export interface UpdateExamSpaceRequest {
    roomId: string;
    roomName: string;
    roomCapacity: number;
    reason: string;
}

export interface SubmitFeedbackRequest {
    scheduledExamId: string;
    commentText: string;
    commentType: string;
}

export interface PublishForReviewRequest {
    notes?: string;
}

export interface FinalizeScheduleRequest {
    notes?: string;
}

export interface ResolveConflictRequest {
    resolutionNotes: string;
}

export interface CreateVersionRequest {
    versionType: 'DRAFT' | 'REVIEW' | 'FINAL' | 'BACKUP';
    notes?: string;
}


export interface ScheduleResponse {
    id: string;
    examSessionPeriodId: string;
    academicYear: string;
    examSession: string;
    status: 'DRAFT' | 'IN_REVIEW' | 'FINALIZED' | 'PUBLISHED';
    startDate: string;
    endDate: string;
    createdAt: string;
    createdBy: string;
    updatedAt?: string;
    finalizedAt?: string;
    finalizedBy?: string;
}

export interface ExamResponse {
    scheduledExamId: string;
    courseId: string;
    courseName: string;
    examDate: string;
    startTime: string;
    endTime: string;
    roomId?: string;
    roomName?: string;
    roomCapacity?: number;
    studentCount: number;
    mandatoryStatus: 'MANDATORY' | 'ELECTIVE';
    professorIds: string[];
}

export interface CommentResponse {
    commentId: string;
    professorId: string;
    scheduledExamId: string;
    commentText: string;
    commentType: string;
    status: 'PENDING' | 'REVIEWED' | 'RESOLVED';
    submittedAt: string;
}

export interface QualityMetricsResponse {
    qualityScore: number;
    preferenceSatisfactionRate: number;
    totalConflicts: number;
    resolvedConflicts: number;
    roomUtilizationRate: number;
    averageStudentExamsPerDay: number;
    processingTimeMs: number;
    recordedAt: string;
}

export interface ConflictResponse {
    conflictId: string;
    conflictType: string;
    severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    description: string;
    affectedExamIds: string[];
    suggestedResolution: string;
    status: 'DETECTED' | 'RESOLVED';
}

export interface DetailedScheduleResponse {
    schedule: ScheduleResponse;
    exams: ExamResponse[];
    comments: CommentResponse[];
    adjustments: any[];
    metrics?: QualityMetricsResponse;
    conflicts: ConflictResponse[];
}

export interface GenerationResponse {
    scheduleId: string;
    status: 'INITIATED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
    message: string;
    estimatedCompletionTime?: string;
}

export interface VersionResponse {
    versionId: string;
    versionNumber: number;
    versionType: string;
    versionNotes?: string;
    createdAt: string;
    createdBy: string;
}

export interface ApiResponse<T> {
    data?: T;
    error?: string;
    message?: string;
    timestamp: string;
}


class SchedulingService {
    private baseUrl = 'http://localhost:8000/api/scheduling';


    async createSchedule(request: CreateScheduleRequest): Promise<ScheduleResponse> {
        try {
            const response: AxiosResponse<ScheduleResponse> = await axios.post(
                `${this.baseUrl}/schedules`,
                request
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to create schedule");
            throw error;
        }
    }

    async getSchedules(
        academicYear?: string,
        examSession?: string,
        status?: string
    ): Promise<ScheduleResponse[]> {
        try {
            const params = new URLSearchParams();
            if (academicYear) params.append('academicYear', academicYear);
            if (examSession) params.append('examSession', examSession);
            if (status) params.append('status', status);

            const url = params.toString()
                ? `${this.baseUrl}/schedules?${params.toString()}`
                : `${this.baseUrl}/schedules`;

            const response: AxiosResponse<{ content: ScheduleResponse[] }> = await axios.get(url);
            return response.data.content || [];
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch schedules");
            throw error;
        }
    }

    async getSchedule(scheduleId: string): Promise<DetailedScheduleResponse> {
        try {
            const response: AxiosResponse<DetailedScheduleResponse> = await axios.get(
                `${this.baseUrl}/schedules/${scheduleId}`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch schedule details");
            throw error;
        }
    }

    async generateSchedule(scheduleId: string): Promise<GenerationResponse> {
        try {
            const response: AxiosResponse<GenerationResponse> = await axios.post(
                `${this.baseUrl}/schedules/${scheduleId}/generate`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to generate schedule");
            throw error;
        }
    }

    async publishForReview(scheduleId: string, request: PublishForReviewRequest): Promise<void> {
        try {
            await axios.post(
                `${this.baseUrl}/schedules/${scheduleId}/publish-for-review`,
                request
            );
        } catch (error: unknown) {
            this.handleError(error, "Failed to publish for review");
            throw error;
        }
    }

    async finalizeSchedule(scheduleId: string, request: FinalizeScheduleRequest): Promise<void> {
        try {
            await axios.post(
                `${this.baseUrl}/schedules/${scheduleId}/finalize`,
                request
            );
        } catch (error: unknown) {
            this.handleError(error, "Failed to finalize schedule");
            throw error;
        }
    }


    async addExam(scheduleId: string, request: AddExamRequest): Promise<ExamResponse> {
        try {
            const response: AxiosResponse<ExamResponse> = await axios.post(
                `${this.baseUrl}/schedules/${scheduleId}/exams`,
                request
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to add exam");
            throw error;
        }
    }

    async updateExamTime(scheduleId: string, examId: string, request: UpdateExamTimeRequest): Promise<void> {
        try {
            await axios.put(
                `${this.baseUrl}/schedules/${scheduleId}/exams/${examId}/time`,
                request
            );
        } catch (error: unknown) {
            this.handleError(error, "Failed to update exam time");
            throw error;
        }
    }

    async updateExamSpace(scheduleId: string, examId: string, request: UpdateExamSpaceRequest): Promise<void> {
        try {
            await axios.put(
                `${this.baseUrl}/schedules/${scheduleId}/exams/${examId}/space`,
                request
            );
        } catch (error: unknown) {
            this.handleError(error, "Failed to update exam space");
            throw error;
        }
    }


    async submitFeedback(scheduleId: string, request: SubmitFeedbackRequest): Promise<CommentResponse> {
        try {
            const response: AxiosResponse<CommentResponse> = await axios.post(
                `${this.baseUrl}/schedules/${scheduleId}/feedback`,
                request
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to submit feedback");
            throw error;
        }
    }

    async getFeedback(scheduleId: string, professorId?: string, status?: string): Promise<CommentResponse[]> {
        try {
            const params = new URLSearchParams();
            if (professorId) params.append('professorId', professorId);
            if (status) params.append('status', status);

            const url = params.toString()
                ? `${this.baseUrl}/schedules/${scheduleId}/feedback?${params.toString()}`
                : `${this.baseUrl}/schedules/${scheduleId}/feedback`;

            const response: AxiosResponse<CommentResponse[]> = await axios.get(url);
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch feedback");
            throw error;
        }
    }


    async getQualityMetrics(scheduleId: string): Promise<QualityMetricsResponse> {
        try {
            const response: AxiosResponse<QualityMetricsResponse> = await axios.get(
                `${this.baseUrl}/schedules/${scheduleId}/quality`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch quality metrics");
            throw error;
        }
    }

    async getConflicts(scheduleId: string): Promise<ConflictResponse[]> {
        try {
            const response: AxiosResponse<ConflictResponse[]> = await axios.get(
                `${this.baseUrl}/schedules/${scheduleId}/conflicts`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch conflicts");
            throw error;
        }
    }

    async resolveConflict(scheduleId: string, conflictId: string, request: ResolveConflictRequest): Promise<void> {
        try {
            await axios.post(
                `${this.baseUrl}/schedules/${scheduleId}/conflicts/${conflictId}/resolve`,
                request
            );
        } catch (error: unknown) {
            this.handleError(error, "Failed to resolve conflict");
            throw error;
        }
    }


    async createVersion(scheduleId: string, request: CreateVersionRequest): Promise<VersionResponse> {
        try {
            const response: AxiosResponse<VersionResponse> = await axios.post(
                `${this.baseUrl}/schedules/${scheduleId}/versions`,
                request
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to create version");
            throw error;
        }
    }

    async getVersions(scheduleId: string): Promise<VersionResponse[]> {
        try {
            const response: AxiosResponse<VersionResponse[]> = await axios.get(
                `${this.baseUrl}/schedules/${scheduleId}/versions`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch versions");
            throw error;
        }
    }


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

    formatScheduleStatus(status: string): string {
        const statusMap: { [key: string]: string } = {
            'DRAFT': 'Draft',
            'IN_REVIEW': 'In Review',
            'FINALIZED': 'Finalized',
            'PUBLISHED': 'Published'
        };
        return statusMap[status] || status;
    }

    formatConflictSeverity(severity: string): string {
        const severityMap: { [key: string]: string } = {
            'LOW': 'Low',
            'MEDIUM': 'Medium',
            'HIGH': 'High',
            'CRITICAL': 'Critical'
        };
        return severityMap[severity] || severity;
    }

    getStatusColor(status: string): string {
        const colorMap: { [key: string]: string } = {
            'DRAFT': 'secondary',
            'IN_REVIEW': 'warning',
            'FINALIZED': 'success',
            'PUBLISHED': 'primary'
        };
        return colorMap[status] || 'secondary';
    }

    getSeverityColor(severity: string): string {
        const colorMap: { [key: string]: string } = {
            'LOW': 'success',
            'MEDIUM': 'warning',
            'HIGH': 'warning',
            'CRITICAL': 'danger'
        };
        return colorMap[severity] || 'secondary';
    }
}

export const schedulingService = new SchedulingService();
export default schedulingService;