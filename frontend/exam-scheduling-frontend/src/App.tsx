import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navigation from './components/Navigation';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';

// Placeholder components for future implementation
const ComingSoon: React.FC<{ title: string }> = ({ title }) => (
    <div className="container mt-4">
        <div className="text-center">
            <div className="display-1 text-muted mb-4">
                <i className="bi bi-hourglass-split"></i>
            </div>
            <h2 className="text-muted">{title}</h2>
            <p className="lead text-muted">
                This feature will be implemented in Phase 2 with the business logic.
            </p>
        </div>
    </div>
);

// Layout component for authenticated pages
const AuthenticatedLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => (
    <>
        <Navigation />
        <main>{children}</main>
    </>
);

function App() {
    return (
        <AuthProvider>
            <Router>
                <div className="App">
                    <Routes>
                        {/* Public routes */}
                        <Route path="/login" element={<Login />} />

                        {/* Protected routes */}
                        <Route
                            path="/dashboard"
                            element={
                                <ProtectedRoute>
                                    <AuthenticatedLayout>
                                        <Dashboard />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        {/* Administrator routes */}
                        <Route
                            path="/exam-sessions"
                            element={
                                <ProtectedRoute requiredRole="ADMINISTRATOR">
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Exam Session Management" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/schedule-management"
                            element={
                                <ProtectedRoute requiredRole="ADMINISTRATOR">
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Schedule Management" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/user-management"
                            element={
                                <ProtectedRoute requiredRole="ADMINISTRATOR">
                                    <AuthenticatedLayout>
                                        <ComingSoon title="User Management" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/reports"
                            element={
                                <ProtectedRoute requiredRole="ADMINISTRATOR">
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Reports & Analytics" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        {/* Professor routes */}
                        <Route
                            path="/my-preferences"
                            element={
                                <ProtectedRoute requiredRole="PROFESSOR">
                                    <AuthenticatedLayout>
                                        <ComingSoon title="My Preferences" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/my-schedule"
                            element={
                                <ProtectedRoute requiredRole="PROFESSOR">
                                    <AuthenticatedLayout>
                                        <ComingSoon title="My Schedule" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/feedback"
                            element={
                                <ProtectedRoute requiredRole="PROFESSOR">
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Schedule Feedback" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        {/* General protected routes */}
                        <Route
                            path="/preferences"
                            element={
                                <ProtectedRoute>
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Preference Management" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/published-schedules"
                            element={
                                <ProtectedRoute>
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Published Schedules" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/schedules"
                            element={
                                <ProtectedRoute>
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Schedule Viewer" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/profile"
                            element={
                                <ProtectedRoute>
                                    <AuthenticatedLayout>
                                        <ComingSoon title="User Profile" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/settings"
                            element={
                                <ProtectedRoute>
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Settings" />
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        {/* Default redirects */}
                        <Route path="/" element={<Navigate to="/dashboard" replace />} />
                        <Route path="*" element={<Navigate to="/dashboard" replace />} />
                    </Routes>
                </div>
            </Router>
        </AuthProvider>
    );
}

export default App;
