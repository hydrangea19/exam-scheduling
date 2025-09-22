import React, {useEffect, useState} from 'react';
import {
    Calendar,
    Users,
    Clock,
    AlertTriangle,
    Search,
    RefreshCw,
    TrendingUp,
    CheckCircle,
    XCircle,
    Heart,
    MessageSquare,
    BarChart3,
    Grid3X3,
    UserCheck,
    Activity,
} from 'lucide-react';

import {
    type ExamSessionPeriodView,
    preferenceService,
    type PreferenceStatistics,
    type PreferenceSubmissionSummary,
    type TimeSlotConflict,
} from '../services/preferenceService';

// Helper functions for safe property access
const getTimeSlotProperty = (slot: any, property: string, defaultValue: any = '') => {
    return slot && typeof slot === 'object' ? (slot[property] ?? defaultValue) : defaultValue;
};

const formatTimeSlot = (slot: any): string => {
    const dayOfWeek = getTimeSlotProperty(slot, 'dayOfWeek', 'Unknown');
    const startTime = getTimeSlotProperty(slot, 'startTime', '--:--');
    const endTime = getTimeSlotProperty(slot, 'endTime', '--:--');
    return `${dayOfWeek} ${startTime}-${endTime}`;
};

const AdminPreferenceOverview = () => {
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
    const [professorSearch, setProfessorSearch] = useState('');
    const [filteredPreferences, setFilteredPreferences] = useState<PreferenceSubmissionSummary[]>([]);
    const [expandedPreference, setExpandedPreference] = useState<string | null>(null);

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
                    (pref.professorName || '').toLowerCase().includes(professorSearch.toLowerCase()) ||
                    (pref.professorEmail || '').toLowerCase().includes(professorSearch.toLowerCase())
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
            case 'HIGH': return 'danger';
            case 'MEDIUM': return 'warning';
            case 'LOW': return 'info';
            default: return 'secondary';
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'SUBMITTED': return 'success';
            case 'UPDATED': return 'info';
            case 'WITHDRAWN': return 'warning';
            default: return 'secondary';
        }
    };

    if (loading) {
        return (
            <div className="min-vh-100 d-flex align-items-center justify-content-center bg-light">
                <div className="text-center">
                    <div className="spinner-border text-primary" role="status" style={{width: '3rem', height: '3rem'}}>
                        <span className="visually-hidden">Loading...</span>
                    </div>
                    <p className="mt-3 text-muted">Loading preference overview...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="min-vh-100 bg-light">
            {/* Header */}
            <div className="bg-white border-bottom shadow-sm">
                <div className="container-fluid">
                    <div className="d-flex justify-content-between align-items-center py-4">
                        <div>
                            <h1 className="h2 mb-1 text-dark fw-bold">Preference Management</h1>
                            <p className="text-muted small mb-0">Monitor and analyze professor exam scheduling preferences</p>
                        </div>
                        <button
                            onClick={fetchSessions}
                            disabled={loading}
                            className="btn btn-outline-primary d-flex align-items-center"
                        >
                            <RefreshCw size={16} className="me-2" />
                            Refresh
                        </button>
                    </div>
                </div>
            </div>

            <div className="container-fluid py-4">
                {/* Alerts */}
                {error && (
                    <div className="alert alert-danger alert-dismissible fade show mb-4" role="alert">
                        <div className="d-flex align-items-center">
                            <AlertTriangle size={20} className="me-2" />
                            <div className="flex-grow-1">{error}</div>
                            <button
                                type="button"
                                className="btn-close"
                                onClick={() => setError('')}
                                aria-label="Close"
                            ></button>
                        </div>
                    </div>
                )}

                {success && (
                    <div className="alert alert-success alert-dismissible fade show mb-4" role="alert">
                        <div className="d-flex align-items-center">
                            <CheckCircle size={20} className="me-2" />
                            <div className="flex-grow-1">{success}</div>
                            <button
                                type="button"
                                className="btn-close"
                                onClick={() => setSuccess('')}
                                aria-label="Close"
                            ></button>
                        </div>
                    </div>
                )}

                {/* Session Selection */}
                <div className="card shadow-sm mb-4">
                    <div className="card-header bg-white">
                        <div className="d-flex align-items-center">
                            <Calendar size={20} className="text-muted me-2" />
                            <h3 className="card-title mb-0 h5">Exam Sessions</h3>
                        </div>
                    </div>
                    <div className="card-body">
                        <div className="row g-3">
                            {sessions.map((session) => (
                                <div key={session.examSessionPeriodId} className="col-lg-4 col-md-6">
                                    <div
                                        onClick={() => setSelectedSession(session)}
                                        className={`card h-100 cursor-pointer border-2 transition-all ${
                                            selectedSession?.examSessionPeriodId === session.examSessionPeriodId
                                                ? 'border-primary bg-primary bg-opacity-10'
                                                : 'border-light-subtle'
                                        }`}
                                        style={{cursor: 'pointer', transition: 'all 0.2s ease'}}
                                        onMouseEnter={(e) => {
                                            if (selectedSession?.examSessionPeriodId !== session.examSessionPeriodId) {
                                                e.currentTarget.classList.add('border-secondary');
                                            }
                                        }}
                                        onMouseLeave={(e) => {
                                            if (selectedSession?.examSessionPeriodId !== session.examSessionPeriodId) {
                                                e.currentTarget.classList.remove('border-secondary');
                                            }
                                        }}
                                    >
                                        <div className="card-body">
                                            <div className="d-flex justify-content-between align-items-start mb-3">
                                                <div>
                                                    <h5 className="card-title mb-1">{session.examSession}</h5>
                                                    <p className="card-text text-muted small">{session.academicYear}</p>
                                                </div>
                                                <span className={`badge ${session.windowOpen ? 'bg-success' : 'bg-secondary'}`}>
                                                    {session.windowOpen ? 'Open' : 'Closed'}
                                                </span>
                                            </div>
                                            <div className="row text-center">
                                                <div className="col-6">
                                                    <div className="h4 text-primary mb-0">{session.totalSubmissions}</div>
                                                    <small className="text-muted">Submissions</small>
                                                </div>
                                                <div className="col-6">
                                                    <div className="h4 text-success mb-0">{session.uniqueProfessors}</div>
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
                        <div className="card shadow-sm mb-4">
                            <div className="card-header bg-white">
                                <div className="d-flex justify-content-between align-items-center">
                                    <h3 className="card-title mb-0 h5">
                                        {selectedSession.examSession} {selectedSession.academicYear} - Details
                                    </h3>
                                    <span className={`badge fs-6 ${selectedSession.windowOpen ? 'bg-success' : 'bg-secondary'}`}>
                                        {selectedSession.windowOpen ? 'Accepting Submissions' : 'Submissions Closed'}
                                    </span>
                                </div>
                            </div>
                            <div className="card-body">
                                {loadingPreferences ? (
                                    <div className="text-center py-5">
                                        <div className="spinner-border text-primary" role="status">
                                            <span className="visually-hidden">Loading preferences...</span>
                                        </div>
                                        <p className="mt-2 text-muted">Loading preferences...</p>
                                    </div>
                                ) : statistics ? (
                                    <div className="row g-4">
                                        <div className="col-md-3 col-6">
                                            <div className="text-center p-3 bg-primary bg-opacity-10 rounded">
                                                <div className="h2 text-primary mb-1">{statistics.totalSubmissions || 0}</div>
                                                <small className="text-muted">Total Submissions</small>
                                            </div>
                                        </div>
                                        <div className="col-md-3 col-6">
                                            <div className="text-center p-3 bg-success bg-opacity-10 rounded">
                                                <div className="h2 text-success mb-1">{statistics.uniqueProfessors || 0}</div>
                                                <small className="text-muted">Participating Professors</small>
                                            </div>
                                        </div>
                                        <div className="col-md-3 col-6">
                                            <div className="text-center p-3 bg-info bg-opacity-10 rounded">
                                                <div className="h2 text-info mb-1">{(statistics.averagePreferredSlots || 0).toFixed(1)}</div>
                                                <small className="text-muted">Avg Preferred Slots</small>
                                            </div>
                                        </div>
                                        <div className="col-md-3 col-6">
                                            <div className="text-center p-3 bg-danger bg-opacity-10 rounded">
                                                <div className="h2 text-danger mb-1">{conflicts.length}</div>
                                                <small className="text-muted">Time Conflicts</small>
                                            </div>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="row g-4">
                                        <div className="col-md-3 col-6">
                                            <div className="text-center p-3 bg-light rounded">
                                                <div className="h2 text-muted mb-1">0</div>
                                                <small className="text-muted">Total Submissions</small>
                                            </div>
                                        </div>
                                        <div className="col-md-3 col-6">
                                            <div className="text-center p-3 bg-light rounded">
                                                <div className="h2 text-muted mb-1">0</div>
                                                <small className="text-muted">Participating Professors</small>
                                            </div>
                                        </div>
                                        <div className="col-md-3 col-6">
                                            <div className="text-center p-3 bg-light rounded">
                                                <div className="h2 text-muted mb-1">0</div>
                                                <small className="text-muted">Avg Preferred Slots</small>
                                            </div>
                                        </div>
                                        <div className="col-md-3 col-6">
                                            <div className="text-center p-3 bg-light rounded">
                                                <div className="h2 text-muted mb-1">0</div>
                                                <small className="text-muted">Time Conflicts</small>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Navigation Tabs */}
                        <div className="card shadow-sm mb-4">
                            <div className="card-header bg-white p-0">
                                <ul className="nav nav-tabs card-header-tabs" role="tablist">
                                    <li className="nav-item">
                                        <button
                                            className={`nav-link ${activeTab === 'overview' ? 'active' : ''}`}
                                            onClick={() => setActiveTab('overview')}
                                            type="button"
                                        >
                                            <Grid3X3 size={16} className="me-2" />
                                            Overview ({preferences.length})
                                        </button>
                                    </li>
                                    <li className="nav-item">
                                        <button
                                            className={`nav-link ${activeTab === 'preferences' ? 'active' : ''}`}
                                            onClick={() => setActiveTab('preferences')}
                                            type="button"
                                        >
                                            <UserCheck size={16} className="me-2" />
                                            All Preferences
                                        </button>
                                    </li>
                                    <li className="nav-item">
                                        <button
                                            className={`nav-link ${activeTab === 'conflicts' ? 'active' : ''}`}
                                            onClick={() => setActiveTab('conflicts')}
                                            type="button"
                                        >
                                            <AlertTriangle size={16} className="me-2" />
                                            Conflicts ({conflicts.length})
                                        </button>
                                    </li>
                                    <li className="nav-item">
                                        <button
                                            className={`nav-link ${activeTab === 'analytics' ? 'active' : ''}`}
                                            onClick={() => setActiveTab('analytics')}
                                            type="button"
                                        >
                                            <BarChart3 size={16} className="me-2" />
                                            Analytics
                                        </button>
                                    </li>
                                </ul>
                            </div>

                            <div className="card-body">
                                {/* Overview Tab */}
                                {activeTab === 'overview' && (
                                    <div className="row g-4">
                                        <div className="col-lg-6">
                                            <div className="card bg-primary bg-opacity-5 border-primary border-opacity-25 h-100">
                                                <div className="card-header bg-transparent border-primary border-opacity-25">
                                                    <div className="d-flex align-items-center">
                                                        <TrendingUp size={20} className="text-primary me-2" />
                                                        <h5 className="card-title mb-0">Most Popular Time Slots</h5>
                                                    </div>
                                                </div>
                                                <div className="card-body">
                                                    {statistics && statistics.mostPopularTimeSlots && statistics.mostPopularTimeSlots.length > 0 ? (
                                                        <div className="list-group list-group-flush">
                                                            {statistics.mostPopularTimeSlots.map((slot, index) => (
                                                                <div key={index} className="list-group-item bg-white rounded mb-2 shadow-sm">
                                                                    <div className="d-flex justify-content-between align-items-center">
                                                                        <div>
                                                                            <div className="fw-semibold">{slot.dayOfWeek}</div>
                                                                            <small className="text-muted">{slot.startTime} - {slot.endTime}</small>
                                                                        </div>
                                                                        <div className="text-end">
                                                                            <div className="h5 text-primary mb-0">{slot.count}</div>
                                                                            <small className="text-muted">{slot.percentage}%</small>
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            ))}
                                                        </div>
                                                    ) : (
                                                        <div className="text-center py-4">
                                                            <Clock size={48} className="text-muted mb-3" />
                                                            <p className="text-muted">No preference data available</p>
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        </div>

                                        <div className="col-lg-6">
                                            <div className="card bg-danger bg-opacity-5 border-danger border-opacity-25 h-100">
                                                <div className="card-header bg-transparent border-danger border-opacity-25">
                                                    <div className="d-flex align-items-center">
                                                        <AlertTriangle size={20} className="text-danger me-2" />
                                                        <h5 className="card-title mb-0">High Priority Conflicts</h5>
                                                    </div>
                                                </div>
                                                <div className="card-body">
                                                    {conflicts.filter(c => c.severity === 'HIGH').length > 0 ? (
                                                        <div className="list-group list-group-flush">
                                                            {conflicts.filter(c => c.severity === 'HIGH').map((conflict, index) => (
                                                                <div key={index} className="list-group-item bg-white rounded mb-2 shadow-sm">
                                                                    <div className="d-flex justify-content-between align-items-start">
                                                                        <div>
                                                                            <div className="fw-semibold text-danger">{conflict.dayOfWeek}</div>
                                                                            <small className="text-muted">{conflict.startTime} - {conflict.endTime}</small>
                                                                            <div className="small text-muted mt-1">
                                                                                {conflict.conflictCount} professors competing
                                                                            </div>
                                                                        </div>
                                                                        <span className={`badge bg-${getSeverityColor(conflict.severity)}`}>
                                                                            {conflict.severity}
                                                                        </span>
                                                                    </div>
                                                                </div>
                                                            ))}
                                                        </div>
                                                    ) : (
                                                        <div className="text-center py-4">
                                                            <CheckCircle size={48} className="text-success mb-3" />
                                                            <p className="text-success fw-semibold">No high-priority conflicts detected</p>
                                                            <small className="text-muted">All preferences are well distributed</small>
                                                        </div>
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
                                        <div className="row mb-4">
                                            <div className="col-md-8">
                                                <div className="input-group">
                                                    <span className="input-group-text bg-white">
                                                        <Search size={16} />
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
                                            <div className="col-md-4 text-end d-flex align-items-center justify-content-end">
                                                <small className="text-muted">
                                                    Showing {filteredPreferences.length} of {preferences.length} submissions
                                                </small>
                                            </div>
                                        </div>

                                        {/* Preferences List */}
                                        {filteredPreferences.length > 0 ? (
                                            <div className="accordion" id="preferencesAccordion">
                                                {filteredPreferences.map((pref) => (
                                                    <div key={pref.submissionId} className="accordion-item border shadow-sm mb-3">
                                                        <h2 className="accordion-header">
                                                            <button
                                                                className={`accordion-button ${expandedPreference !== pref.submissionId ? 'collapsed' : ''}`}
                                                                type="button"
                                                                onClick={() => setExpandedPreference(
                                                                    expandedPreference === pref.submissionId ? null : pref.submissionId
                                                                )}
                                                            >
                                                                <div className="d-flex justify-content-between align-items-center w-100 me-3">
                                                                    <div className="flex-grow-1">
                                                                        <div className="d-flex align-items-center">
                                                                            <strong className="me-2">{pref.professorName || 'Unknown Professor'}</strong>
                                                                            <span className="text-muted">({pref.professorEmail || 'No email'})</span>
                                                                        </div>
                                                                        <small className="text-muted d-block mt-1">
                                                                            Submitted: {pref.submittedAt ? new Date(pref.submittedAt).toLocaleDateString() : 'Unknown'}
                                                                            {pref.lastUpdatedAt && (
                                                                                <> • Updated: {new Date(pref.lastUpdatedAt).toLocaleDateString()}</>
                                                                            )}
                                                                        </small>
                                                                    </div>
                                                                    <div className="d-flex align-items-center gap-2">
                                                                        <span className="badge bg-primary">{pref.preferredSlotsCount} preferred</span>
                                                                        <span className="badge bg-warning">{pref.unavailableSlotsCount} unavailable</span>
                                                                        <span className={`badge bg-${getStatusColor(pref.status)}`}>
                                                                            {pref.status}
                                                                        </span>
                                                                    </div>
                                                                </div>
                                                            </button>
                                                        </h2>
                                                        {expandedPreference === pref.submissionId && (
                                                            <div className="accordion-body">
                                                                <div className="row g-4">
                                                                    {/* Updated Preferred Time Slots Section */}
                                                                    <div className="col-lg-6">
                                                                        <div className="d-flex align-items-center mb-3">
                                                                            <Heart size={16} className="text-success me-2" />
                                                                            <h6 className="text-success mb-0">Preferred Time Slots</h6>
                                                                        </div>
                                                                        {pref.preferredTimeSlots && Array.isArray(pref.preferredTimeSlots) && pref.preferredTimeSlots.length > 0 ? (
                                                                            <div className="list-group">
                                                                                {pref.preferredTimeSlots.map((slot, i) => (
                                                                                    <div key={i} className="list-group-item d-flex justify-content-between align-items-center bg-success bg-opacity-10">
                                                                                        <div>
                                                                                            <strong>{getTimeSlotProperty(slot, 'dayOfWeek', 'Unknown Day')}</strong>
                                                                                            <div className="small text-muted">
                                                                                                {getTimeSlotProperty(slot, 'startTime', '--:--')} - {getTimeSlotProperty(slot, 'endTime', '--:--')}
                                                                                            </div>
                                                                                        </div>
                                                                                        <span className="badge bg-success">
                                                                                            {getTimeSlotProperty(slot, 'priority') ?
                                                                                                `Priority ${getTimeSlotProperty(slot, 'priority')}` :
                                                                                                getTimeSlotProperty(slot, 'preferenceLevel', 'PREFERRED')
                                                                                            }
                                                                                        </span>
                                                                                    </div>
                                                                                ))}
                                                                            </div>
                                                                        ) : (
                                                                            <p className="text-muted">No preferred slots specified</p>
                                                                        )}
                                                                    </div>

                                                                    {/* Updated Unavailable Time Slots Section */}
                                                                    <div className="col-lg-6">
                                                                        <div className="d-flex align-items-center mb-3">
                                                                            <XCircle size={16} className="text-danger me-2" />
                                                                            <h6 className="text-danger mb-0">Unavailable Time Slots</h6>
                                                                        </div>
                                                                        {pref.unavailableTimeSlots && Array.isArray(pref.unavailableTimeSlots) && pref.unavailableTimeSlots.length > 0 ? (
                                                                            <div className="list-group">
                                                                                {pref.unavailableTimeSlots.map((slot, i) => (
                                                                                    <div key={i} className="list-group-item bg-danger bg-opacity-10">
                                                                                        <strong>{getTimeSlotProperty(slot, 'dayOfWeek', 'Unknown Day')}</strong>
                                                                                        <div className="small text-muted">
                                                                                            {getTimeSlotProperty(slot, 'startTime', '--:--')} - {getTimeSlotProperty(slot, 'endTime', '--:--')}
                                                                                        </div>
                                                                                        {getTimeSlotProperty(slot, 'reason') && (
                                                                                            <div className="small text-muted mt-1">{getTimeSlotProperty(slot, 'reason')}</div>
                                                                                        )}
                                                                                    </div>
                                                                                ))}
                                                                            </div>
                                                                        ) : (
                                                                            <p className="text-muted">No unavailable slots specified</p>
                                                                        )}
                                                                    </div>
                                                                </div>

                                                                {pref.hasAdditionalNotes && pref.additionalNotes && (
                                                                    <div className="mt-4">
                                                                        <div className="d-flex align-items-center mb-2">
                                                                            <MessageSquare size={16} className="text-info me-2" />
                                                                            <h6 className="text-info mb-0">Additional Notes</h6>
                                                                        </div>
                                                                        <div className="bg-info bg-opacity-10 rounded p-3">
                                                                            <p className="mb-0 small">{pref.additionalNotes}</p>
                                                                        </div>
                                                                    </div>
                                                                )}
                                                            </div>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <div className="text-center py-5">
                                                <Users size={64} className="text-muted mb-3" />
                                                <h4 className="text-muted">No preferences found</h4>
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
                                            <div className="row g-4">
                                                {conflicts.map((conflict, index) => (
                                                    <div key={index} className="col-lg-6">
                                                        <div className="card h-100 shadow-sm">
                                                            <div className="card-header bg-light">
                                                                <div className="d-flex justify-content-between align-items-center">
                                                                    <h6 className="mb-0 fw-semibold">
                                                                        {conflict.dayOfWeek} {conflict.startTime} - {conflict.endTime}
                                                                    </h6>
                                                                    <span className={`badge bg-${getSeverityColor(conflict.severity)}`}>
                                                                        {conflict.severity} CONFLICT
                                                                    </span>
                                                                </div>
                                                            </div>
                                                            <div className="card-body">
                                                                <p className="mb-3 small">
                                                                    <strong>{conflict.conflictCount} professors</strong> have requested this time slot:
                                                                </p>
                                                                <ul className="list-unstyled mb-0">
                                                                    {conflict.conflictingProfessors.map((prof, i) => (
                                                                        <li key={i} className="mb-2 d-flex align-items-center small">
                                                                            <Users size={16} className="text-muted me-2" />
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
                                                <CheckCircle size={64} className="text-success mb-3" />
                                                <h4 className="text-success">No Conflicts Detected</h4>
                                                <p className="text-muted">
                                                    All professor preferences are compatible with the current schedule.
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Analytics Tab */}
                                {activeTab === 'analytics' && (
                                    <div className="card bg-light border-0">
                                        <div className="card-header bg-transparent">
                                            <div className="d-flex align-items-center">
                                                <Activity size={20} className="text-primary me-2" />
                                                <h5 className="card-title mb-0">Preference Analytics Summary</h5>
                                            </div>
                                        </div>
                                        <div className="card-body">
                                            {statistics ? (
                                                <div>
                                                    <div className="row g-4 mb-4">
                                                        <div className="col-md-3 col-sm-6">
                                                            <div className="text-center bg-white rounded shadow-sm p-3">
                                                                <div className="h3 text-primary mb-1">{statistics.totalSubmissions || 0}</div>
                                                                <small className="text-muted">Total Submissions</small>
                                                            </div>
                                                        </div>
                                                        <div className="col-md-3 col-sm-6">
                                                            <div className="text-center bg-white rounded shadow-sm p-3">
                                                                <div className="h3 text-success mb-1">{statistics.uniqueProfessors || 0}</div>
                                                                <small className="text-muted">Unique Professors</small>
                                                            </div>
                                                        </div>
                                                        <div className="col-md-3 col-sm-6">
                                                            <div className="text-center bg-white rounded shadow-sm p-3">
                                                                <div className="h3 text-info mb-1">{(statistics.averagePreferredSlots || 0).toFixed(1)}</div>
                                                                <small className="text-muted">Avg Preferred Slots</small>
                                                            </div>
                                                        </div>
                                                        <div className="col-md-3 col-sm-6">
                                                            <div className="text-center bg-white rounded shadow-sm p-3">
                                                                <div className="h3 text-warning mb-1">{(statistics.averageUnavailableSlots || 0).toFixed(1)}</div>
                                                                <small className="text-muted">Avg Unavailable Slots</small>
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div className="d-flex align-items-center small text-muted bg-white rounded p-3">
                                                        <Clock size={16} className="me-2" />
                                                        Generated: {statistics.generatedAt ? new Date(statistics.generatedAt).toLocaleString() : 'N/A'}
                                                    </div>
                                                </div>
                                            ) : (
                                                <div className="text-center py-4">
                                                    <BarChart3 size={48} className="text-muted mb-3" />
                                                    <p className="text-muted">No analytics data available</p>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

export default AdminPreferenceOverview;