import type {AxiosError, AxiosResponse} from "axios";
import axios from 'axios';

export interface AuthenticationRequest {
    email: string;
    password: string;
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
    tokenType: string;
}

export interface UserInfo {
    id: string;
    email: string;
    fullName: string;
    role: string;
}

export interface TokenValidationRequest {
    token: string;
}

export interface TokenValidationResponse {
    valid: boolean;
    error?: string;
}

// New User Management Types
export interface UserResponse {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    middleName?: string;
    fullName: string;
    role: string;
    hasPassword: boolean;
    active: boolean;
    createdAt: string;
    lastUpdatedAt: string;
    keycloakUserId?: string;
    lastKeycloakSync?: string;
    lastSuccessfulLogin?: string;
    failedLoginAttempts: number;
    notificationPreferences?: { [key: string]: boolean };
    uiPreferences?: { [key: string]: string };
    lastRoleChange?: string;
    lastRoleChangedBy?: string;
    deactivationReason?: string;
    deactivatedBy?: string;
    deactivatedAt?: string;
}

export interface PagedUserResponse {
    users: UserResponse[];
    page: number;
    size: number;
    totalPages: number;
    totalElements: number;
    first: boolean;
    last: boolean;
    empty: boolean;
}

export interface CreateUserRequest {
    email: string;
    firstName: string;
    lastName: string;
    middleName?: string;
    role: string;
}

export interface UpdateUserProfileRequest {
    firstName: string;
    lastName: string;
    middleName?: string;
}

export interface ChangeUserEmailRequest {
    currentEmail: string;
    newEmail: string;
}

export interface ChangeUserRoleRequest {
    currentRole: string;
    newRole: string;
    reason?: string;
}

export interface DeactivateUserRequest {
    reason: string;
}

export interface ActivateUserRequest {
    reason?: string;
}

export interface UpdateUserPreferencesRequest {
    notificationPreferences: { [key: string]: boolean };
    uiPreferences: { [key: string]: string };
}

export interface UserStatisticsResponse {
    totalUsers: number;
    activeUsers: number;
    inactiveUsers: number;
    usersWithPassword: number;
    keycloakUsers: number;
    usersWithRecentLogin: number;
    roleBreakdown: { [key: string]: number };
    generatedAt: string;
}

export interface ValidationErrorResponse {
    error: string;
    message: string;
    timestamp: string;
    fieldErrors?: { [key: string]: string };
}

export interface ErrorResponse {
    error: string;
    message: string;
    timestamp: string;
    details?: { [key: string]: any };
}

class AuthService {
    private baseUrl = 'http://localhost:8000/api/auth';
    private userManagementUrl = 'http://localhost:8000/api/users';

    constructor() {
        axios.interceptors.request.use((config) => {
            const token = this.getToken();
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
            return config;
        });

        axios.interceptors.response.use(
            (response) => response,
            async (error) => {
                if (error.response?.status === 401 && this.getToken()) {
                    try {
                        await this.refreshToken();
                        return axios.request(error.config);
                    } catch {
                        this.logout();
                        window.location.href = '/login';
                    }
                }
                return Promise.reject(error);
            }
        );
    }

    // ===== AUTHENTICATION METHODS =====

    async login(credentials: AuthenticationRequest): Promise<AuthResponse> {
        try {
            const response: AxiosResponse<AuthResponse> = await axios.post(
                `${this.baseUrl}/login`,
                credentials
            );

            const authData = response.data;
            this.setToken(authData.accessToken);
            this.setRefreshToken(authData.refreshToken);

            return authData;
        } catch (error: unknown) {
            const axiosError = error as AxiosError<{ message?: string }>;
            throw new Error(axiosError.response?.data?.message || "Login failed");
        }
    }

    async validateToken(token: string): Promise<TokenValidationResponse> {
        try {
            const response: AxiosResponse<TokenValidationResponse> = await axios.post(
                `${this.baseUrl}/validate`,
                {token}
            );
            return response.data;
        } catch (error: unknown) {
            const axiosError = error as AxiosError<{ message?: string }>;
            throw new Error(axiosError.response?.data?.message || "Token validation failed");
        }
    }

    async refreshToken(): Promise<AuthResponse> {
        try {
            const refreshToken = this.getRefreshToken();
            if (!refreshToken) {
                throw new Error('No refresh token available');
            }

            const response: AxiosResponse<AuthResponse> = await axios.post(
                `${this.baseUrl}/refresh`,
                {refreshToken}
            );

            const authData = response.data;
            this.setToken(authData.accessToken);
            this.setRefreshToken(authData.refreshToken);

            return authData;
        } catch (error: unknown) {
            const axiosError = error as AxiosError<{ message?: string }>;
            throw new Error(axiosError.response?.data?.message || "Token refresh failed");
        }
    }

    async getCurrentUser(): Promise<UserInfo> {
        try {
            const response: AxiosResponse<UserResponse> = await axios.get(`${this.userManagementUrl}/me`);
            const userData = response.data;

            return {
                id: userData.id,
                email: userData.email,
                fullName: userData.fullName,
                role: userData.role
            };
        } catch (error: unknown) {
            try {
                const response: AxiosResponse<UserInfo> = await axios.get(`${this.baseUrl}/me`);
                return response.data;
            } catch (fallbackError: unknown) {
                const axiosError = fallbackError as AxiosError<{ message?: string }>;
                throw new Error(axiosError.response?.data?.message || "Current user failed");
            }
        }
    }

    async logout(): Promise<void> {
        try {
            await axios.post(`${this.baseUrl}/logout`);
        } catch (error) {
            console.warn('Logout request failed:', error);
        } finally {
            this.clearTokens();
        }
    }

    // ===== USER MANAGEMENT METHODS =====

    async getCurrentUserProfile(): Promise<UserResponse> {
        try {
            const response: AxiosResponse<UserResponse> = await axios.get(`${this.userManagementUrl}/me`);
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to get current user profile");
            throw error;
        }
    }

    async updateCurrentUserProfile(request: UpdateUserProfileRequest): Promise<UserResponse> {
        try {
            const response: AxiosResponse<UserResponse> = await axios.put(
                `${this.userManagementUrl}/me/profile`,
                request
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to update profile");
            throw error;
        }
    }

    async updateCurrentUserPreferences(request: UpdateUserPreferencesRequest): Promise<UserResponse> {
        try {
            const response: AxiosResponse<UserResponse> = await axios.put(
                `${this.userManagementUrl}/me/preferences`,
                request
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to update preferences");
            throw error;
        }
    }

    async getAllUsers(page: number = 0, size: number = 20, sortBy: string = 'fullName', sortDirection: string = 'ASC'): Promise<PagedUserResponse> {
        try {
            const response: AxiosResponse<PagedUserResponse> = await axios.get(
                `${this.userManagementUrl}?page=${page}&size=${size}&sortBy=${sortBy}&sortDirection=${sortDirection}`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to get all users");
            throw error;
        }
    }

    async getUserById(userId: string): Promise<UserResponse> {
        try {
            const response: AxiosResponse<UserResponse> = await axios.get(
                `${this.userManagementUrl}/${userId}`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to get user");
            throw error;
        }
    }

    async createUser(request: CreateUserRequest): Promise<UserResponse> {
        try {
            const response: AxiosResponse<UserResponse> = await axios.post(
                `${this.userManagementUrl}`,
                request
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to create user");
            throw error;
        }
    }

    async updateUserProfile(userId: string, request: UpdateUserProfileRequest): Promise<UserResponse> {
        try {
            const response: AxiosResponse<UserResponse> = await axios.put(
                `${this.userManagementUrl}/${userId}/profile`,
                request
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to update user profile");
            throw error;
        }
    }

    async changeUserRole(userId: string, request: ChangeUserRoleRequest): Promise<UserResponse> {
        try {
            const response: AxiosResponse<UserResponse> = await axios.put(
                `${this.userManagementUrl}/${userId}/role`,
                request
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to change user role");
            throw error;
        }
    }

    async activateUser(userId: string, request?: ActivateUserRequest): Promise<UserResponse> {
        try {
            const response: AxiosResponse<UserResponse> = await axios.put(
                `${this.userManagementUrl}/${userId}/activate`,
                request || {}
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to activate user");
            throw error;
        }
    }

    async deactivateUser(userId: string, request: DeactivateUserRequest): Promise<UserResponse> {
        try {
            const response: AxiosResponse<UserResponse> = await axios.put(
                `${this.userManagementUrl}/${userId}/deactivate`,
                request
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to deactivate user");
            throw error;
        }
    }

    async searchUsers(
        filters: {
            email?: string;
            fullName?: string;
            role?: string;
            active?: boolean;
            page?: number;
            size?: number;
            sortBy?: string;
            sortDirection?: string;
        }
    ): Promise<PagedUserResponse> {
        try {
            const params = new URLSearchParams();
            Object.entries(filters).forEach(([key, value]) => {
                if (value !== undefined && value !== null && value !== '') {
                    params.append(key, value.toString());
                }
            });

            const response: AxiosResponse<PagedUserResponse> = await axios.get(
                `${this.userManagementUrl}/search?${params.toString()}`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to search users");
            throw error;
        }
    }

    async getUsersByRole(role: string, activeOnly: boolean = true, page: number = 0, size: number = 20): Promise<PagedUserResponse> {
        try {
            const response: AxiosResponse<PagedUserResponse> = await axios.get(
                `${this.userManagementUrl}/role/${role}?activeOnly=${activeOnly}&page=${page}&size=${size}`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to get users by role");
            throw error;
        }
    }

    async getUserStatistics(): Promise<UserStatisticsResponse> {
        try {
            const response: AxiosResponse<UserStatisticsResponse> = await axios.get(
                `${this.userManagementUrl}/statistics`
            );
            return response.data;
        } catch (error: unknown) {
            this.handleError(error, "Failed to get user statistics");
            throw error;
        }
    }

    // ===== UTILITY METHODS =====

    private handleError(error: unknown, defaultMessage: string): void {
        const axiosError = error as AxiosError<ValidationErrorResponse | ErrorResponse>;

        if (axiosError.response?.data) {
            const errorData = axiosError.response.data;
            console.error(`${defaultMessage}:`, {
                status: axiosError.response.status,
                error: errorData.error,
                message: errorData.message,
                fieldErrors: 'fieldErrors' in errorData ? errorData.fieldErrors : undefined,
                details: 'details' in errorData ? errorData.details : undefined
            });
        } else {
            console.error(`${defaultMessage}:`, axiosError.message);
        }
    }

    // ===== TOKEN MANAGEMENT =====

    getToken(): string | null {
        return localStorage.getItem('accessToken');
    }

    private setToken(token: string): void {
        localStorage.setItem('accessToken', token);
    }

    private getRefreshToken(): string | null {
        return localStorage.getItem('refreshToken');
    }

    private setRefreshToken(token: string): void {
        localStorage.setItem('refreshToken', token);
    }

    private clearTokens(): void {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
    }

    isAuthenticated(): boolean {
        return !!this.getToken();
    }

    getCurrentUserRole(): string | null {
        const token = this.getToken();
        if (!token) return null;

        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.role || null;
        } catch {
            console.warn("Failed to decode token payload");
            return null;
        }
    }
}

export const authService = new AuthService();
export default authService;