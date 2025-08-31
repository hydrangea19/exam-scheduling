import React from 'react';
import {BrowserRouter as Router, Navigate, Route, Routes} from 'react-router-dom';
import {AuthProvider} from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navigation from './components/Navigation';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import UserManagement from './components/UserManagement';
import ProfessorPreferences from './components/ProfessorPreferences';
import AdminExamSessions from './components/AdminExamSessions';
import AdminPreferenceOverview from './components/AdminPreferenceOverview';
import SchedulingManagement from './components/SchedulingManagement'; // Add this import
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import PublishedSchedules from "./components/PublishedSchedules.tsx";

const ComingSoon: React.FC<{ title: string }> = ({title}) => (
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

const AuthenticatedLayout: React.FC<{ children: React.ReactNode }> = ({children}) => (
    <>
        <Navigation/>
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
                        <Route path="/login" element={<Login/>}/>

                        {/* Protected routes */}
                        <Route
                            path="/dashboard"
                            element={
                                <ProtectedRoute>
                                    <AuthenticatedLayout>
                                        <Dashboard/>
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        {/* Administrator routes */}
                        <Route
                            path="/exam-sessions"
                            element={
                                <ProtectedRoute requiredRole="ADMIN">
                                    <AuthenticatedLayout>
                                        <AdminExamSessions/>
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        {/* Updated to use actual SchedulingManagement component */}
                        <Route
                            path="/schedule-management"
                            element={
                                <ProtectedRoute requiredRole="ADMIN">
                                    <AuthenticatedLayout>
                                        <SchedulingManagement/> {/* Now using actual component */}
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/user-management"
                            element={
                                <ProtectedRoute requiredRole="ADMIN">
                                    <AuthenticatedLayout>
                                        <UserManagement/>
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/reports"
                            element={
                                <ProtectedRoute requiredRole="ADMIN">
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Reports & Analytics"/>
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
                                        <ProfessorPreferences/>
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/my-schedule"
                            element={
                                <ProtectedRoute requiredRole="PROFESSOR">
                                    <AuthenticatedLayout>
                                        <ComingSoon title="My Schedule"/>
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/feedback"
                            element={
                                <ProtectedRoute requiredRole="PROFESSOR">
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Schedule Feedback"/>
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
                                        <AdminPreferenceOverview/>
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/published-schedules"
                            element={
                                <ProtectedRoute>
                                    <AuthenticatedLayout>
                                        <PublishedSchedules/> {/* Instead of ComingSoon */}
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/schedules"
                            element={
                                <ProtectedRoute>
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Schedule Viewer"/>
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="/settings"
                            element={
                                <ProtectedRoute>
                                    <AuthenticatedLayout>
                                        <ComingSoon title="Settings"/>
                                    </AuthenticatedLayout>
                                </ProtectedRoute>
                            }
                        />

                        {/* Default redirects */}
                        <Route path="/" element={<Navigate to="/dashboard" replace/>}/>
                        <Route path="*" element={<Navigate to="/dashboard" replace/>}/>
                    </Routes>
                </div>
            </Router>
        </AuthProvider>
    );
}

export default App;