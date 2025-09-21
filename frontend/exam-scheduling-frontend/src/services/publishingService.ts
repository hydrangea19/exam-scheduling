import axios, { AxiosError, type AxiosResponse } from 'axios';


export interface PublishedScheduleResponse {
    id: string;
    scheduleId: string;
    examSessionPeriodId: string;
    academicYear: string;
    examSession: string;
    title: string;
    description?: string;
    publicationStatus: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' | 'WITHDRAWN';
    publishedAt?: string;
    publishedBy?: string;
    isPublic: boolean;
    createdAt: string;
}

export interface ApiResponse<T> {
    data?: T;
    error?: string;
    message?: string;
    timestamp: string;
}


class PublishingService {
    private baseUrl = 'http://localhost:8000/api/publishing';

    async getPublishedSchedule(scheduleId: string): Promise<PublishedScheduleResponse> {
        try {
            const response: AxiosResponse<PublishedScheduleResponse> = await axios.get(
                `${this.baseUrl}/schedules/${scheduleId}`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to fetch published schedule");
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

    formatPublicationStatus(status: string): string {
        const statusMap: { [key: string]: string } = {
            'DRAFT': 'Draft',
            'PUBLISHED': 'Published',
            'ARCHIVED': 'Archived',
            'WITHDRAWN': 'Withdrawn'
        };
        return statusMap[status] || status;
    }

    getStatusColor(status: string): string {
        const colorMap: { [key: string]: string } = {
            'DRAFT': 'secondary',
            'PUBLISHED': 'success',
            'ARCHIVED': 'info',
            'WITHDRAWN': 'warning'
        };
        return colorMap[status] || 'secondary';
    }

    async exportScheduleToPdf(scheduleId: string): Promise<Blob> {
        try {
            const response = await axios.get(
                `${this.baseUrl}/schedules/${scheduleId}/export/pdf`,
                { responseType: 'blob' }
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to export PDF");
            throw error;
        }
    }

    async exportScheduleToExcel(scheduleId: string): Promise<Blob> {
        try {
            const response = await axios.get(
                `${this.baseUrl}/schedules/${scheduleId}/export/excel`,
                { responseType: 'blob' }
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to export Excel");
            throw error;
        }
    }
}

export const publishingService = new PublishingService();
export default publishingService;