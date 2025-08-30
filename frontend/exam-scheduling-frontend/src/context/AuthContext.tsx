import React, {createContext, type ReactNode, useContext, useEffect, useState} from 'react';
import authService, {type AuthenticationRequest, type UserInfo} from '../services/authService';

interface AuthContextType {
    isAuthenticated: boolean;
    user: UserInfo | null;
    loading: boolean;
    login: (credentials: AuthenticationRequest) => Promise<void>;
    logout: () => Promise<void>;
    refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// eslint-disable-next-line
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
    const [user, setUser] = useState<UserInfo | null>(null);
    const [loading, setLoading] = useState<boolean>(true);

    const checkAuth = async () => {
        try {
            if (authService.isAuthenticated()) {
                try {
                    const userInfo = await authService.getCurrentUser();
                    setUser(userInfo);
                    setIsAuthenticated(true);
                } catch (error) {
                    console.warn('Could not fetch user info, but token exists:', error);
                    setIsAuthenticated(true);
                    setUser({
                        id: 'unknown',
                        email: 'Unknown User',
                        fullName: 'Unknown User',
                        role: authService.getCurrentUserRole() || 'GUEST'
                    });
                }
            }
        } catch (error) {
            console.error('Auth check failed:', error);
            await authService.logout();
            setIsAuthenticated(false);
            setUser(null);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        checkAuth();
    }, []);

    const login = async (credentials: AuthenticationRequest) => {
        try {
            await authService.login({ ...credentials});

            setIsAuthenticated(true);

            try {
                const userInfo = await authService.getCurrentUser();
                setUser(userInfo);
            } catch (userInfoError) {
                console.warn('Login succeeded but could not fetch user info:', userInfoError);
                setUser({
                    id: 'unknown',
                    email: credentials.email,
                    fullName: credentials.email,
                    role: authService.getCurrentUserRole() || 'GUEST'
                });
            }
        } catch (error) {
            setIsAuthenticated(false);
            setUser(null);
            throw error;
        }
    };

    const logout = async () => {
        try {
            await authService.logout();
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            setIsAuthenticated(false);
            setUser(null);
        }
    };

    const refreshUser = async () => {
        try {
            if (isAuthenticated) {
                const userInfo = await authService.getCurrentUser();
                setUser(userInfo);
            }
        } catch (error) {
            console.error('Failed to refresh user:', error);
        }
    };

    const value: AuthContextType = {
        isAuthenticated,
        user,
        loading,
        login,
        logout,
        refreshUser,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};