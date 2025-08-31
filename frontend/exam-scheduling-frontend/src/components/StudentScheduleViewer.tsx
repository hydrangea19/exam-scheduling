import React, {useEffect, useState} from 'react';
import {
    type ExamScheduleEntry,
    type ExamSessionPeriod,
    preferenceService,
    type ScheduleFilter
} from '../services/preferenceService';

const StudentScheduleViewer: React.FC = () => {
    const [sessions, setSessions] = useState<ExamSessionPeriod[]>([]);
    const [selectedSession, setSelectedSession] = useState<ExamSessionPeriod | null>(null);
    const [schedules, setSchedules] = useState<ExamScheduleEntry[]>([]);
    const [filteredSchedules, setFilteredSchedules] = useState<ExamScheduleEntry[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadingSchedules, setLoadingSchedules] = useState(false);
    const [error, setError] = useState<string>('');

    const [filters, setFilters] = useState<ScheduleFilter>({});
    const [showFilters, setShowFilters] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');

    const [viewMode, setViewMode] = useState<'list' | 'calendar'>('list');
    const [sortBy, setSortBy] = useState<'date' | 'course' | 'professor'>('date');
    const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');

    useEffect(() => {
        fetchSessions();
    }, []);

    useEffect(() => {
        if (selectedSession) {
            fetchSchedules(selectedSession.examSessionPeriodId);
        }
    }, [selectedSession]);

    useEffect(() => {
        applyFiltersAndSearch();
    }, [schedules, filters, searchTerm, sortBy, sortOrder]);

    const fetchSessions = async () => {
        try {
            setLoading(true);
            const sessionsData = await preferenceService.getPublishedSessions();
            setSessions(sessionsData);

            if (sessionsData.length > 0) {
                setSelectedSession(sessionsData[0]);
            }
        } catch (err: any) {
            setError(err.message || 'Failed to load published sessions');
        } finally {
            setLoading(false);
        }
    };

    const fetchSchedules = async (sessionId: string) => {
        try {
            setLoadingSchedules(true);
            const schedulesData = await preferenceService.getSchedulesBySession(sessionId);
            setSchedules(schedulesData);
        } catch (err: any) {
            setError(err.message || 'Failed to load schedules');
        } finally {
            setLoadingSchedules(false);
        }
    };

    const applyFiltersAndSearch = () => {
        let filtered = [...schedules];

        if (searchTerm) {
            filtered = filtered.filter(schedule =>
                schedule.course.courseName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                schedule.course.courseCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
                schedule.professor.professorName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                schedule.course.department.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        if (filters.department) {
            filtered = filtered.filter(s => s.course.department === filters.department);
        }
        if (filters.examType) {
            filtered = filtered.filter(s => s.examType === filters.examType);
        }

        filtered.sort((a, b) => {
            let valueA: any, valueB: any;

            switch (sortBy) {
                case 'date':
                    valueA = new Date(a.scheduledDateTime);
                    valueB = new Date(b.scheduledDateTime);
                    break;
                case 'course':
                    valueA = a.course.courseCode;
                    valueB = b.course.courseCode;
                    break;
                case 'professor':
                    valueA = a.professor.professorName;
                    valueB = b.professor.professorName;
                    break;
                default:
                    valueA = a.scheduledDateTime;
                    valueB = b.scheduledDateTime;
            }

            if (sortOrder === 'desc') {
                return valueA > valueB ? -1 : 1;
            }
            return valueA < valueB ? -1 : 1;
        });

        setFilteredSchedules(filtered);
    };

    const resetFilters = () => {
        setFilters({});
        setSearchTerm('');
    };

    const handleExport = async (format: 'PDF' | 'CSV' | 'ICS') => {
        if (!selectedSession) return;

        try {
            await preferenceService.exportSchedule(selectedSession.examSessionPeriodId, format);
        } catch (err: any) {
            setError(err.message || 'Failed to export schedule');
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'CONFIRMED':
                return 'success';
            case 'SCHEDULED':
                return 'primary';
            case 'CANCELLED':
                return 'danger';
            case 'RESCHEDULED':
                return 'warning';
            default:
                return 'secondary';
        }
    };

    const getExamTypeColor = (type: string) => {
        switch (type) {
            case 'WRITTEN':
                return 'primary';
            case 'ORAL':
                return 'info';
            case 'PRACTICAL':
                return 'warning';
            case 'PROJECT':
                return 'success';
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
                <p className="mt-2 text-muted">Loading exam schedules...</p>
            </div>
        );
    }

    return (
        <div className="container mt-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2 className="mb-1">Exam Schedules</h2>
                    <p className="text-muted mb-0">View published examination schedules and timetables</p>
                </div>
                <div className="d-flex gap-2">
                    <button
                        className="btn btn-outline-primary"
                        onClick={() => fetchSessions()}
                        disabled={loading}
                    >
                        <i className="bi bi-arrow-clockwise me-2"></i>
                        Refresh
                    </button>
                    {selectedSession && (
                        <div className="dropdown">
                            <button
                                className="btn btn-outline-success dropdown-toggle"
                                type="button"
                                data-bs-toggle="dropdown"
                            >
                                <i className="bi bi-download me-2"></i>
                                Export
                            </button>
                            <ul className="dropdown-menu">
                                <li>
                                    <button
                                        className="dropdown-item"
                                        onClick={() => handleExport('PDF')}
                                    >
                                        <i className="bi bi-file-earmark-pdf me-2"></i>
                                        Export as PDF
                                    </button>
                                </li>
                                <li>
                                    <button
                                        className="dropdown-item"
                                        onClick={() => handleExport('CSV')}
                                    >
                                        <i className="bi bi-file-earmark-spreadsheet me-2"></i>
                                        Export as CSV
                                    </button>
                                </li>
                                <li>
                                    <button
                                        className="dropdown-item"
                                        onClick={() => handleExport('ICS')}
                                    >
                                        <i className="bi bi-calendar me-2"></i>
                                        Export as Calendar
                                    </button>
                                </li>
                            </ul>
                        </div>
                    )}
                </div>
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

            {/* Session Selection */}
            {sessions.length === 0 ? (
                <div className="card text-center">
                    <div className="card-body py-5">
                        <div className="display-4 text-muted mb-3">
                            <i className="bi bi-calendar-x"></i>
                        </div>
                        <h4 className="text-muted">No Published Schedules</h4>
                        <p className="text-muted">
                            There are currently no published examination schedules available.
                        </p>
                    </div>
                </div>
            ) : (
                <>
                    <div className="card mb-4">
                        <div className="card-header">
                            <h5 className="mb-0">
                                <i className="bi bi-calendar-event me-2"></i>
                                Available Exam Sessions
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
                                                    <span className="badge bg-success">Published</span>
                                                </div>
                                                <div className="mt-2 text-center">
                                                    <div className="h5 text-primary mb-0">{session.totalExams}</div>
                                                    <small className="text-muted">Scheduled Exams</small>
                                                </div>
                                                {session.publishedAt && (
                                                    <small className="text-muted d-block mt-2">
                                                        <i className="bi bi-clock me-1"></i>
                                                        Published: {new Date(session.publishedAt).toLocaleDateString()}
                                                    </small>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    {selectedSession && (
                        <>
                            {/* Filters and Search */}
                            <div className="card mb-4">
                                <div className="card-header">
                                    <div className="d-flex justify-content-between align-items-center">
                                        <h5 className="mb-0">
                                            {selectedSession.examSession} {selectedSession.academicYear} - Schedule
                                        </h5>
                                        <div className="d-flex gap-2">
                                            <button
                                                className={`btn btn-outline-secondary btn-sm ${showFilters ? 'active' : ''}`}
                                                onClick={() => setShowFilters(!showFilters)}
                                            >
                                                <i className="bi bi-funnel me-2"></i>
                                                Filters
                                            </button>
                                            <div className="btn-group">
                                                <button
                                                    className={`btn btn-sm ${viewMode === 'list' ? 'btn-primary' : 'btn-outline-primary'}`}
                                                    onClick={() => setViewMode('list')}
                                                >
                                                    <i className="bi bi-list-ul"></i>
                                                </button>
                                                <button
                                                    className={`btn btn-sm ${viewMode === 'calendar' ? 'btn-primary' : 'btn-outline-primary'}`}
                                                    onClick={() => setViewMode('calendar')}
                                                >
                                                    <i className="bi bi-calendar-week"></i>
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div className="card-body">
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
                                                    placeholder="Search by course, professor, or department..."
                                                    value={searchTerm}
                                                    onChange={(e) => setSearchTerm(e.target.value)}
                                                />
                                            </div>
                                        </div>
                                        <div className="col-md-3">
                                            <select
                                                className="form-select"
                                                value={sortBy}
                                                onChange={(e) => setSortBy(e.target.value as 'date' | 'course' | 'professor')}
                                            >
                                                <option value="date">Sort by Date</option>
                                                <option value="course">Sort by Course</option>
                                                <option value="professor">Sort by Professor</option>
                                            </select>
                                        </div>
                                        <div className="col-md-3">
                                            <div className="btn-group w-100">
                                                <button
                                                    className={`btn ${sortOrder === 'asc' ? 'btn-primary' : 'btn-outline-primary'}`}
                                                    onClick={() => setSortOrder('asc')}
                                                >
                                                    <i className="bi bi-sort-up"></i> Asc
                                                </button>
                                                <button
                                                    className={`btn ${sortOrder === 'desc' ? 'btn-primary' : 'btn-outline-primary'}`}
                                                    onClick={() => setSortOrder('desc')}
                                                >
                                                    <i className="bi bi-sort-down"></i> Desc
                                                </button>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Advanced Filters */}
                                    {showFilters && (
                                        <div className="border rounded p-3 mb-3 bg-light">
                                            <div className="row">
                                                <div className="col-md-4">
                                                    <div className="form-group">
                                                        <label className="form-label">Department</label>
                                                        <select
                                                            className="form-select"
                                                            value={filters.department || ''}
                                                            onChange={(e) => setFilters({
                                                                ...filters,
                                                                department: e.target.value || undefined
                                                            })}
                                                        >
                                                            <option value="">All Departments</option>
                                                            {preferenceService.getDepartments().map(dept => (
                                                                <option key={dept} value={dept}>{dept}</option>
                                                            ))}
                                                        </select>
                                                    </div>
                                                </div>
                                                <div className="col-md-4">
                                                    <div className="form-group">
                                                        <label className="form-label">Exam Type</label>
                                                        <select
                                                            className="form-select"
                                                            value={filters.examType || ''}
                                                            onChange={(e) => setFilters({
                                                                ...filters,
                                                                examType: e.target.value || undefined
                                                            })}
                                                        >
                                                            <option value="">All Types</option>
                                                            {preferenceService.getExamTypes().map(type => (
                                                                <option key={type.value}
                                                                        value={type.value}>{type.label}</option>
                                                            ))}
                                                        </select>
                                                    </div>
                                                </div>
                                                <div className="col-md-4">
                                                    <div className="form-group">
                                                        <label className="form-label">Actions</label>
                                                        <div className="d-flex gap-2">
                                                            <button
                                                                className="btn btn-outline-secondary"
                                                                onClick={resetFilters}
                                                            >
                                                                <i className="bi bi-arrow-clockwise me-1"></i>
                                                                Reset
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    )}

                                    {/* Results Summary */}
                                    <div className="d-flex justify-content-between align-items-center mb-3">
                                        <span className="text-muted">
                                            Showing {filteredSchedules.length} of {schedules.length} exams
                                        </span>
                                        {(searchTerm || Object.values(filters).some(v => v)) && (
                                            <button
                                                className="btn btn-outline-secondary btn-sm"
                                                onClick={resetFilters}
                                            >
                                                <i className="bi bi-x me-1"></i>
                                                Clear all filters
                                            </button>
                                        )}
                                    </div>
                                </div>
                            </div>

                            {/* Schedule Display */}
                            {loadingSchedules ? (
                                <div className="text-center py-5">
                                    <div className="spinner-border text-primary" role="status">
                                        <span className="visually-hidden">Loading schedules...</span>
                                    </div>
                                </div>
                            ) : filteredSchedules.length > 0 ? (
                                <div className="row">
                                    {filteredSchedules.map((schedule) => (
                                        <div key={schedule.scheduleId} className="col-lg-6 mb-4">
                                            <div className="card border-0 shadow-sm h-100">
                                                <div
                                                    className="card-header d-flex justify-content-between align-items-center">
                                                    <h6 className="mb-0">
                                                        <strong>{schedule.course.courseCode}</strong> - {schedule.course.courseName}
                                                    </h6>
                                                    <div className="d-flex gap-1">
                                                        <span
                                                            className={`badge bg-${getExamTypeColor(schedule.examType)}`}>
                                                            {schedule.examType}
                                                        </span>
                                                        <span className={`badge bg-${getStatusColor(schedule.status)}`}>
                                                            {schedule.status}
                                                        </span>
                                                    </div>
                                                </div>
                                                <div className="card-body">
                                                    <div className="row mb-3">
                                                        <div className="col-6">
                                                            <small className="text-muted d-block">Date & Time</small>
                                                            <strong>
                                                                {new Date(schedule.scheduledDateTime).toLocaleDateString()}
                                                            </strong>
                                                            <br/>
                                                            <span className="text-primary">
                                                                {new Date(schedule.scheduledDateTime).toLocaleTimeString([], {
                                                                    hour: '2-digit',
                                                                    minute: '2-digit'
                                                                })}
                                                                {' - '}
                                                                {new Date(new Date(schedule.scheduledDateTime).getTime() + schedule.duration * 60000).toLocaleTimeString([], {
                                                                    hour: '2-digit',
                                                                    minute: '2-digit'
                                                                })}
                                                            </span>
                                                        </div>
                                                        <div className="col-6">
                                                            <small className="text-muted d-block">Location</small>
                                                            <strong>{schedule.location.building}</strong>
                                                            <br/>
                                                            <span className="text-info">
                                                                {schedule.location.room} (Cap: {schedule.location.capacity})
                                                            </span>
                                                        </div>
                                                    </div>

                                                    <div className="mb-3">
                                                        <small className="text-muted d-block">Professor</small>
                                                        <strong>{schedule.professor.professorName}</strong>
                                                        <br/>
                                                        <small
                                                            className="text-muted">{schedule.professor.professorEmail}</small>
                                                    </div>

                                                    <div className="mb-3">
                                                        <small className="text-muted d-block">Department</small>
                                                        <span
                                                            className="badge bg-secondary">{schedule.course.department}</span>
                                                        <small className="ms-2 text-muted">
                                                            Duration: {schedule.duration} minutes
                                                        </small>
                                                    </div>

                                                    {schedule.notes && (
                                                        <div className="alert alert-info py-2 px-3">
                                                            <small>
                                                                <i className="bi bi-info-circle me-1"></i>
                                                                <strong>Note:</strong> {schedule.notes}
                                                            </small>
                                                        </div>
                                                    )}
                                                </div>
                                                <div className="card-footer bg-light text-muted">
                                                    <small>
                                                        <i className="bi bi-clock me-1"></i>
                                                        Last updated: {new Date(schedule.lastUpdated).toLocaleString()}
                                                    </small>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="text-center py-5">
                                    <i className="bi bi-search display-4 text-muted d-block mb-3"></i>
                                    <h5 className="text-muted">No Exams Found</h5>
                                    <p className="text-muted">
                                        {searchTerm || Object.values(filters).some(v => v)
                                            ? 'No exams match your current search and filter criteria.'
                                            : 'No exams have been scheduled for this session yet.'}
                                    </p>
                                </div>
                            )}
                        </>
                    )}
                </>
            )}
        </div>
    );
};

export default StudentScheduleViewer;