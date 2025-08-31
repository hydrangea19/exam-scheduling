import React, {useEffect, useState} from 'react';
import {
    type ExamSessionPeriodView,
    preferenceService,
    type PreferenceStatistics,
    type PreferenceSubmissionSummary,
    type TimeSlotConflict,
} from '../services/preferenceService';

const AdminPreferenceOverview: React.FC = () => {
    const [sessions, setSessions] = useState<ExamSessionPeriodView[]>([]);
    const [selectedSession, setSelectedSession] = useState<ExamSessionPeriodView | null>(null);
    const [preferences, setPreferences] = useState<PreferenceSubmissionSummary[]>([]);
    const [statistics, setStatistics] = useState<PreferenceStatistics | null>(null);
    const [conflicts, setConflicts] = useState<TimeSlotConflict[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadingPreferences, setLoadingPreferences] = useState(false);
    const [error, setError] = useState<string>('');
    const [success, setSuccess] = useState<string>('');

    const [activeTab, setActiveTab] = useState('overview');
    //  const [expandedPreference, setExpandedPreference] = useState<string | null>(null);
    const [professorSearch, setProfessorSearch] = useState('');
    const [filteredPreferences, setFilteredPreferences] = useState<PreferenceSubmissionSummary[]>([]);

    useEffect(() => {
        fetchSessions();
    }, []);

    useEffect(() => {
        if (selectedSession) {
            fetchSessionPreferences(selectedSession.examSessionPeriodId);
        }
    }, [selectedSession]);

    useEffect(() => {
        if (professorSearch) {
            setFilteredPreferences(
                preferences.filter(pref =>
                    pref.professorName.toLowerCase().includes(professorSearch.toLowerCase()) ||
                    pref.professorEmail.toLowerCase().includes(professorSearch.toLowerCase())
                )
            );
        } else {
            setFilteredPreferences(preferences);
        }
    }, [professorSearch, preferences]);

    const fetchSessions = async () => {
        try {
            setLoading(true);
            const sessionsData = await preferenceService.getAllExamSessionPeriods();
            setSessions(sessionsData);

            // Auto-select first session with submissions
            const sessionWithSubmissions = sessionsData.find(s => s.totalSubmissions > 0);
            if (sessionWithSubmissions) {
                setSelectedSession(sessionWithSubmissions);
            } else if (sessionsData.length > 0) {
                setSelectedSession(sessionsData[0]);
            }
        } catch (err: any) {
            setError(err.message || 'Failed to load sessions');
        } finally {
            setLoading(false);
        }
    };

    const fetchSessionPreferences = async (sessionId: string) => {
        try {
            setLoadingPreferences(true);
            const [preferencesData, conflictsData] = await Promise.all([
                preferenceService.getPreferencesBySession(sessionId),
                preferenceService.getConflictingPreferences(sessionId)
            ]);

            setPreferences(preferencesData.preferences);
            setStatistics(preferencesData.statistics);
            setConflicts(conflictsData);
        } catch (err: any) {
            setError(err.message || 'Failed to load preference data');
        } finally {
            setLoadingPreferences(false);
        }
    };

    const getSeverityColor = (severity: string) => {
        switch (severity) {
            case 'HIGH':
                return 'danger';
            case 'MEDIUM':
                return 'warning';
            case 'LOW':
                return 'info';
            default:
                return 'secondary';
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'SUBMITTED':
                return 'success';
            case 'UPDATED':
                return 'info';
            case 'WITHDRAWN':
                return 'warning';
            default:
                return 'secondary';
        }
    };

    if (loading) {
        return (
            <div className="container mt-4 text-center">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
                <p className="mt-2 text-muted">Loading preference overview...</p>
            </div>
        );
    }

    return (
        <div className="container mt-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2 className="mb-1">Preference Management Overview</h2>
                    <p className="text-muted mb-0">Monitor and analyze professor exam scheduling preferences</p>
                </div>
                <button
                    className="btn btn-outline-primary"
                    onClick={() => fetchSessions()}
                    disabled={loading}
                >
                    <i className="bi bi-arrow-clockwise me-2"></i>
                    Refresh
                </button>
            </div>

            {error && (
                <div className="alert alert-danger alert-dismissible fade show" role="alert">
                    <i className="bi bi-exclamation-triangle me-2"></i>
                    {error}
                    <button
                        type="button"
                        className="btn-close"
                        onClick={() => setError('')}
                        aria-label="Close"
                    ></button>
                </div>
            )}

            {success && (
                <div className="alert alert-success alert-dismissible fade show" role="alert">
                    <i className="bi bi-check-circle me-2"></i>
                    {success}
                    <button
                        type="button"
                        className="btn-close"
                        onClick={() => setSuccess('')}
                        aria-label="Close"
                    ></button>
                </div>
            )}

            {/* Session Selection */}
            <div className="card mb-4">
                <div className="card-header">
                    <h5 className="mb-0">
                        <i className="bi bi-calendar-event me-2"></i>
                        Exam Sessions
                    </h5>
                </div>
                <div className="card-body">
                    <div className="row">
                        {sessions.map((session) => (
                            <div key={session.examSessionPeriodId} className="col-md-4 mb-3">
                                <div
                                    className={`card cursor-pointer border-2 ${
                                        selectedSession?.examSessionPeriodId === session.examSessionPeriodId
                                            ? 'border-primary bg-light'
                                            : 'border-light'
                                    }`}
                                    onClick={() => setSelectedSession(session)}
                                    style={{cursor: 'pointer'}}
                                >
                                    <div className="card-body py-3">
                                        <div className="d-flex justify-content-between align-items-start">
                                            <div>
                                                <h6 className="mb-1">{session.examSession} {session.academicYear}</h6>
                                                <small className="text-muted">
                                                    {new Date(session.startDate).toLocaleDateString()} - {new Date(session.endDate).toLocaleDateString()}
                                                </small>
                                            </div>
                                            <span
                                                className={`badge ${session.isWindowOpen ? 'bg-success' : 'bg-secondary'}`}>
                                                {session.isWindowOpen ? 'Open' : 'Closed'}
                                            </span>
                                        </div>
                                        <div className="row mt-2 text-center">
                                            <div className="col-6">
                                                <div className="h6 text-primary mb-0">{session.totalSubmissions}</div>
                                                <small className="text-muted">Submissions</small>
                                            </div>
                                            <div className="col-6">
                                                <div className="h6 text-success mb-0">{session.uniqueProfessors}</div>
                                                <small className="text-muted">Professors</small>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {selectedSession && (
                <>
                    {/* Session Details */}
                    <div className="card mb-4">
                        <div className="card-header">
                            <div className="d-flex justify-content-between align-items-center">
                                <h5 className="mb-0">
                                    {selectedSession.examSession} {selectedSession.academicYear} - Preference Details
                                </h5>
                                <span
                                    className={`badge bg-${selectedSession.isWindowOpen ? 'success' : 'secondary'} fs-6`}>
                                    {selectedSession.isWindowOpen ? 'Accepting Submissions' : 'Submissions Closed'}
                                </span>
                            </div>
                        </div>
                        <div className="card-body">
                            {loadingPreferences ? (
                                <div className="text-center py-4">
                                    <div className="spinner-border text-primary" role="status">
                                        <span className="visually-hidden">Loading preferences...</span>
                                    </div>
                                </div>
                            ) : statistics ? (
                                <div className="row">
                                    <div className="col-md-3">
                                        <div className="text-center">
                                            <div
                                                className="h3 text-primary mb-1">{statistics.totalSubmissions || 0}</div>
                                            <small className="text-muted">Total Submissions</small>
                                        </div>
                                    </div>
                                    <div className="col-md-3">
                                        <div className="text-center">
                                            <div
                                                className="h3 text-success mb-1">{statistics.uniqueProfessors || 0}</div>
                                            <small className="text-muted">Participating Professors</small>
                                        </div>
                                    </div>
                                    <div className="col-md-3">
                                        <div className="text-center">
                                            <div
                                                className="h3 text-info mb-1">{(statistics.averagePreferredSlots || 0).toFixed(1)}</div>
                                            <small className="text-muted">Avg Preferred Slots</small>
                                        </div>
                                    </div>
                                    <div className="col-md-3">
                                        <div className="text-center">
                                            <div className="h3 text-warning mb-1">{conflicts.length}</div>
                                            <small className="text-muted">Time Conflicts</small>
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                <div className="row">
                                    <div className="col-md-3">
                                        <div className="text-center">
                                            <div className="h3 text-primary mb-1">0</div>
                                            <small className="text-muted">Total Submissions</small>
                                        </div>
                                    </div>
                                    <div className="col-md-3">
                                        <div className="text-center">
                                            <div className="h3 text-success mb-1">0</div>
                                            <small className="text-muted">Participating Professors</small>
                                        </div>
                                    </div>
                                    <div className="col-md-3">
                                        <div className="text-center">
                                            <div className="h3 text-info mb-1">0</div>
                                            <small className="text-muted">Avg Preferred Slots</small>
                                        </div>
                                    </div>
                                    <div className="col-md-3">
                                        <div className="text-center">
                                            <div className="h3 text-warning mb-1">0</div>
                                            <small className="text-muted">Time Conflicts</small>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Navigation Tabs */}
                    <ul className="nav nav-tabs mb-4" role="tablist">
                        <li className="nav-item" role="presentation">
                            <button
                                className={`nav-link ${activeTab === 'overview' ? 'active' : ''}`}
                                onClick={() => setActiveTab('overview')}
                                type="button"
                            >
                                <i className="bi bi-grid-3x3-gap me-2"></i>
                                Overview ({preferences.length})
                            </button>
                        </li>
                        <li className="nav-item" role="presentation">
                            <button
                                className={`nav-link ${activeTab === 'preferences' ? 'active' : ''}`}
                                onClick={() => setActiveTab('preferences')}
                                type="button"
                            >
                                <i className="bi bi-person-lines-fill me-2"></i>
                                All Preferences
                            </button>
                        </li>
                        <li className="nav-item" role="presentation">
                            <button
                                className={`nav-link ${activeTab === 'conflicts' ? 'active' : ''}`}
                                onClick={() => setActiveTab('conflicts')}
                                type="button"
                            >
                                <i className="bi bi-exclamation-triangle me-2"></i>
                                Conflicts ({conflicts.length})
                            </button>
                        </li>
                        <li className="nav-item" role="presentation">
                            <button
                                className={`nav-link ${activeTab === 'analytics' ? 'active' : ''}`}
                                onClick={() => setActiveTab('analytics')}
                                type="button"
                            >
                                <i className="bi bi-bar-chart me-2"></i>
                                Analytics
                            </button>
                        </li>
                    </ul>

                    {/* Overview Tab */}
                    {activeTab === 'overview' && (
                        <div className="row">
                            <div className="col-md-6">
                                <div className="card">
                                    <div className="card-header">
                                        <h6 className="mb-0">
                                            <i className="bi bi-trophy me-2"></i>
                                            Most Popular Time Slots
                                        </h6>
                                    </div>
                                    <div className="card-body">
                                        {statistics && statistics.mostPopularTimeSlots && statistics.mostPopularTimeSlots.length > 0 ? (
                                            <div className="list-group list-group-flush">
                                                {statistics.mostPopularTimeSlots.map((slot, index) => (
                                                    <div key={index} className="list-group-item px-0">
                                                        <div
                                                            className="d-flex justify-content-between align-items-center">
                                                            <div>
                                                                <strong>{slot.dayOfWeek}</strong> {slot.startTime}-{slot.endTime}
                                                            </div>
                                                            <div className="text-end">
                                                                <span className="badge bg-primary">{slot.count}</span>
                                                                <small
                                                                    className="text-muted ms-2">{slot.percentage}%</small>
                                                            </div>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <p className="text-muted">No preference data available</p>
                                        )}
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-6">
                                <div className="card">
                                    <div className="card-header">
                                        <h6 className="mb-0">
                                            <i className="bi bi-exclamation-triangle-fill me-2"></i>
                                            High Priority Conflicts
                                        </h6>
                                    </div>
                                    <div className="card-body">
                                        {conflicts.filter(c => c.severity === 'HIGH').length > 0 ? (
                                            <div className="list-group list-group-flush">
                                                {conflicts.filter(c => c.severity === 'HIGH').map((conflict, index) => (
                                                    <div key={index} className="list-group-item px-0">
                                                        <div
                                                            className="d-flex justify-content-between align-items-start">
                                                            <div>
                                                                <strong
                                                                    className="text-danger">{conflict.dayOfWeek}</strong> {conflict.startTime}-{conflict.endTime}
                                                                <small className="d-block text-muted">
                                                                    {conflict.conflictCount} professors competing
                                                                </small>
                                                            </div>
                                                            <span
                                                                className={`badge bg-${getSeverityColor(conflict.severity)}`}>
                                                                {conflict.severity}
                                                            </span>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <p className="text-success mb-0">
                                                <i className="bi bi-check-circle me-2"></i>
                                                No high-priority conflicts detected
                                            </p>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* All Preferences Tab */}
                    {activeTab === 'preferences' && (
                        <div>
                            {/* Search */}
                            <div className="row mb-3">
                                <div className="col-md-6">
                                    <div className="input-group">
                                        <span className="input-group-text">
                                            <i className="bi bi-search"></i>
                                        </span>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="Search professors by name or email..."
                                            value={professorSearch}
                                            onChange={(e) => setProfessorSearch(e.target.value)}
                                        />
                                    </div>
                                </div>
                                <div className="col-md-6 text-end">
                                    <span className="text-muted">
                                        Showing {filteredPreferences.length} of {preferences.length} submissions
                                    </span>
                                </div>
                            </div>

                            {/* Preferences List */}
                            {filteredPreferences.length > 0 ? (
                                <div className="accordion" id="preferencesAccordion">
                                    {filteredPreferences.map((pref, index) => (
                                        <div key={pref.submissionId} className="accordion-item">
                                            <h2 className="accordion-header" id={`heading-${index}`}>
                                                <button
                                                    className="accordion-button collapsed"
                                                    type="button"
                                                    data-bs-toggle="collapse"
                                                    data-bs-target={`#collapse-${index}`}
                                                    aria-expanded="false"
                                                >
                                                    <div
                                                        className="d-flex justify-content-between align-items-center w-100 me-3">
                                                        <div>
                                                            <strong>{pref.professorName}</strong>
                                                            <span
                                                                className="text-muted ms-2">({pref.professorEmail})</span>
                                                            <small className="d-block text-muted">
                                                                Submitted: {new Date(pref.submittedAt).toLocaleDateString()}
                                                                {pref.lastUpdatedAt && (
                                                                    <> •
                                                                        Updated: {new Date(pref.lastUpdatedAt).toLocaleDateString()}</>
                                                                )}
                                                            </small>
                                                        </div>
                                                        <div className="d-flex align-items-center gap-2">
                                                            <span
                                                                className="badge bg-primary">{pref.preferredSlotsCount} preferred</span>
                                                            <span
                                                                className="badge bg-warning">{pref.unavailableSlotsCount} unavailable</span>
                                                            <span className={`badge bg-${getStatusColor(pref.status)}`}>
                                                                {pref.status}
                                                            </span>
                                                        </div>
                                                    </div>
                                                </button>
                                            </h2>
                                            <div
                                                id={`collapse-${index}`}
                                                className="accordion-collapse collapse"
                                                data-bs-parent="#preferencesAccordion"
                                            >
                                                <div className="accordion-body">
                                                    <div className="row">
                                                        <div className="col-md-6">
                                                            <h6 className="text-success">
                                                                <i className="bi bi-heart-fill me-2"></i>
                                                                Preferred Time Slots
                                                            </h6>
                                                            {pref.preferredTimeSlots && pref.preferredTimeSlots.length > 0 ? (
                                                                <div className="list-group list-group-flush">
                                                                    {pref.preferredTimeSlots.map((slot, i) => (
                                                                        <div key={i} className="list-group-item px-0">
                                                                            <div
                                                                                className="d-flex justify-content-between">
                                                                                <span>
                                                                                    <strong>{slot.dayOfWeek}</strong> {slot.startTime}-{slot.endTime}
                                                                                </span>
                                                                                <span className="badge bg-success">
                                                                                    Priority {slot.priority}
                                                                                </span>
                                                                            </div>
                                                                        </div>
                                                                    ))}
                                                                </div>
                                                            ) : (
                                                                <p className="text-muted">No preferred slots
                                                                    specified</p>
                                                            )}
                                                        </div>
                                                        <div className="col-md-6">
                                                            <h6 className="text-danger">
                                                                <i className="bi bi-x-circle-fill me-2"></i>
                                                                Unavailable Time Slots
                                                            </h6>
                                                            {pref.unavailableTimeSlots && pref.unavailableTimeSlots.length > 0 ? (
                                                                <div className="list-group list-group-flush">
                                                                    {pref.unavailableTimeSlots.map((slot, i) => (
                                                                        <div key={i} className="list-group-item px-0">
                                                                            <div>
                                                                                <strong>{slot.dayOfWeek}</strong> {slot.startTime}-{slot.endTime}
                                                                                {slot.reason && (
                                                                                    <small
                                                                                        className="d-block text-muted">
                                                                                        {slot.reason}
                                                                                    </small>
                                                                                )}
                                                                            </div>
                                                                        </div>
                                                                    ))}
                                                                </div>
                                                            ) : (
                                                                <p className="text-muted">No unavailable slots
                                                                    specified</p>
                                                            )}
                                                        </div>
                                                    </div>
                                                    {pref.hasAdditionalNotes && pref.additionalNotes && (
                                                        <div className="mt-3">
                                                            <h6 className="text-info">
                                                                <i className="bi bi-chat-text me-2"></i>
                                                                Additional Notes
                                                            </h6>
                                                            <div className="bg-light p-3 rounded">
                                                                {pref.additionalNotes}
                                                            </div>
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="text-center py-5">
                                    <i className="bi bi-inbox display-4 text-muted d-block mb-3"></i>
                                    <h5 className="text-muted">No preferences found</h5>
                                    <p className="text-muted">
                                        {professorSearch ? 'Try adjusting your search criteria.' : 'No professors have submitted preferences for this session yet.'}
                                    </p>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Conflicts Tab */}
                    {activeTab === 'conflicts' && (
                        <div>
                            {conflicts.length > 0 ? (
                                <div className="row">
                                    {conflicts.map((conflict, index) => (
                                        <div key={index} className="col-lg-6 mb-4">
                                            <div className="card border-0 shadow-sm">
                                                <div
                                                    className="card-header d-flex justify-content-between align-items-center">
                                                    <h6 className="mb-0">
                                                        <strong>{conflict.dayOfWeek}</strong> {conflict.startTime} - {conflict.endTime}
                                                    </h6>
                                                    <span className={`badge bg-${getSeverityColor(conflict.severity)}`}>
                                                        {conflict.severity} CONFLICT
                                                    </span>
                                                </div>
                                                <div className="card-body">
                                                    <p className="mb-2">
                                                        <strong>{conflict.conflictCount} professors</strong> have
                                                        requested this time slot:
                                                    </p>
                                                    <ul className="list-unstyled mb-0">
                                                        {conflict.conflictingProfessors.map((prof, i) => (
                                                            <li key={i} className="mb-1">
                                                                <i className="bi bi-person-fill me-2 text-muted"></i>
                                                                {prof}
                                                            </li>
                                                        ))}
                                                    </ul>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="text-center py-5">
                                    <i className="bi bi-check-circle display-4 text-success d-block mb-3"></i>
                                    <h5 className="text-success">No Conflicts Detected</h5>
                                    <p className="text-muted">
                                        All professor preferences are compatible with the current schedule.
                                    </p>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Analytics Tab */}
                    {activeTab === 'analytics' && (
                        <div className="row">
                            <div className="col-md-12">
                                <div className="card mb-4">
                                    <div className="card-header">
                                        <h6 className="mb-0">
                                            <i className="bi bi-graph-up me-2"></i>
                                            Preference Analytics Summary
                                        </h6>
                                    </div>
                                    <div className="card-body">
                                        {statistics ? (
                                            <>
                                                <div className="row text-center">
                                                    <div className="col-md-3">
                                                        <div className="border-end">
                                                            <div
                                                                className="h4 text-primary mb-1">{statistics.totalSubmissions || 0}</div>
                                                            <small className="text-muted">Total Submissions</small>
                                                        </div>
                                                    </div>
                                                    <div className="col-md-3">
                                                        <div className="border-end">
                                                            <div
                                                                className="h4 text-success mb-1">{statistics.uniqueProfessors || 0}</div>
                                                            <small className="text-muted">Unique Professors</small>
                                                        </div>
                                                    </div>
                                                    <div className="col-md-3">
                                                        <div className="border-end">
                                                            <div
                                                                className="h4 text-info mb-1">{(statistics.averagePreferredSlots || 0).toFixed(1)}</div>
                                                            <small className="text-muted">Avg Preferred Slots</small>
                                                        </div>
                                                    </div>
                                                    <div className="col-md-3">
                                                        <div
                                                            className="h4 text-warning mb-1">{(statistics.averageUnavailableSlots || 0).toFixed(1)}</div>
                                                        <small className="text-muted">Avg Unavailable Slots</small>
                                                    </div>
                                                </div>
                                                <hr/>
                                                <small className="text-muted">
                                                    <i className="bi bi-clock me-1"></i>
                                                    Generated: {statistics.generatedAt ? new Date(statistics.generatedAt).toLocaleString() : 'N/A'}
                                                </small>
                                            </>
                                        ) : (
                                            <p className="text-muted text-center">No analytics data available</p>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default AdminPreferenceOverview;