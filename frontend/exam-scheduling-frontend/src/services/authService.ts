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

class AuthService {
    private baseUrl = 'http://localhost:8000/api/auth';

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
            const response: AxiosResponse<UserInfo> = await axios.get(`${this.baseUrl}/me`);
            return response.data;
        } catch (error: unknown) {
            const axiosError = error as AxiosError<{ message?: string }>;
            throw new Error(axiosError.response?.data?.message || "Current user failed");
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

    // Token management
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