import React, {useEffect, useState} from 'react';
import {
    type CloseSubmissionWindowRequest,
    type CreateExamSessionPeriodRequest,
    type OpenSubmissionWindowRequest,
    preferenceService
} from '../services/preferenceService';

interface ExamSessionPeriodViewUpdated {
    examSessionPeriodId: string;
    academicYear: string;
    examSession: string;
    submissionDeadline?: string;
    totalSubmissions: number;
    uniqueProfessors: number;
    windowOpenedAt?: string;
    windowClosedAt?: string;
    createdAt: string;
    description?: string;
    windowOpen: boolean;  // Changed from isWindowOpen
    instructions?: string;
    windowClosedReason?: string;
}

const AdminExamSessions: React.FC = () => {
    const [sessions, setSessions] = useState<ExamSessionPeriodViewUpdated[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string>('');
    const [success, setSuccess] = useState<string>('');

    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showOpenWindowModal, setShowOpenWindowModal] = useState(false);
    const [showCloseWindowModal, setShowCloseWindowModal] = useState(false);
    const [selectedSession, setSelectedSession] = useState<ExamSessionPeriodViewUpdated | null>(null);

    const [createForm, setCreateForm] = useState<CreateExamSessionPeriodRequest>({
        academicYear: '2024/2025',
        examSession: 'WINTER',
        plannedStartDate: '',
        plannedEndDate: '',
        description: '',
        createdBy: ''
    });

    const [openWindowForm, setOpenWindowForm] = useState<OpenSubmissionWindowRequest>({
        examSessionPeriodId: '',
        academicYear: '',
        examSession: '',
        openedBy: '',
        submissionDeadline: '',
    });

    const [closeWindowForm, setCloseWindowForm] = useState<CloseSubmissionWindowRequest>({
        reason: ''
    });

    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        fetchSessions();
    }, []);

    const fetchSessions = async () => {
        try {
            setLoading(true);
            const sessionsData = await preferenceService.getAllExamSessionPeriods();
            setSessions(sessionsData as ExamSessionPeriodViewUpdated[]);
        } catch (err: any) {
            setError(err.message || 'Failed to load exam sessions');
        } finally {
            setLoading(false);
        }
    };

    const handleCreateSession = async () => {
        try {
            setSubmitting(true);
            const payload = {
                ...createForm,
                plannedStartDate: new Date(createForm.plannedStartDate).toISOString(),
                plannedEndDate: new Date(createForm.plannedEndDate).toISOString(),
            };

            await preferenceService.createExamSessionPeriod(payload);
            setSuccess('Exam session created successfully!');
            setShowCreateModal(false);
            resetCreateForm();
            await fetchSessions();
        } catch (err: any) {
            setError(err.message || 'Failed to create exam session');
        } finally {
            setSubmitting(false);
        }
    };

    const handleOpenWindow = async () => {
        if (!selectedSession) return;

        try {
            setSubmitting(true);

            const payload = {
                ...openWindowForm,
                submissionDeadline: new Date(openWindowForm.submissionDeadline).toISOString(),
            };

            await preferenceService.openSubmissionWindow(selectedSession.examSessionPeriodId, payload);
            setSuccess('Submission window opened successfully!');
            setShowOpenWindowModal(false);
            resetOpenWindowForm();
            await fetchSessions();
        } catch (err: any) {
            setError(err.message || 'Failed to open submission window');
        } finally {
            setSubmitting(false);
        }
    };

    const handleCloseWindow = async () => {
        if (!selectedSession) return;

        try {
            setSubmitting(true);

            const payload = {
                examSessionPeriodId: selectedSession.examSessionPeriodId,
                reason: closeWindowForm.reason,
                closedBy: 'web'
            };

            await preferenceService.closeSubmissionWindow(selectedSession.examSessionPeriodId, payload);
            setSuccess('Submission window closed successfully!');
            setShowCloseWindowModal(false);
            resetCloseWindowForm();
            await fetchSessions();
        } catch (err: any) {
            setError(err.message || 'Failed to close submission window');
        } finally {
            setSubmitting(false);
        }
    };

    const resetCreateForm = () => {
        setCreateForm({
            academicYear: '2024/2025',
            examSession: 'WINTER',
            plannedStartDate: '',
            plannedEndDate: '',
            description: '',
            createdBy: ''
        });
    };

    const resetOpenWindowForm = () => {
        setOpenWindowForm({
            examSessionPeriodId: '',
            academicYear: '',
            examSession: '',
            openedBy: '',
            submissionDeadline: '',
        });
        setSelectedSession(null);
    };

    const resetCloseWindowForm = () => {
        setCloseWindowForm({
            reason: ''
        });
        setSelectedSession(null);
    };

    const openCreateModal = () => {
        setShowCreateModal(true);
        resetCreateForm();
    };

    const openOpenWindowModal = (session: ExamSessionPeriodViewUpdated) => {
        setSelectedSession(session);
        setShowOpenWindowModal(true);
        setOpenWindowForm({
            examSessionPeriodId: session.examSessionPeriodId,
            submissionDeadline: '',
            academicYear: session.academicYear,
            examSession: session.examSession,
            openedBy: 'web'
        });
    };

    const openCloseWindowModal = (session: ExamSessionPeriodViewUpdated) => {
        setSelectedSession(session);
        setShowCloseWindowModal(true);
        setCloseWindowForm({
            reason: ''
        });
    };

    if (loading) {
        return (
            <div className="min-vh-100 d-flex align-items-center justify-content-center bg-light">
                <div className="text-center">
                    <div className="spinner-border text-primary mb-3" style={{width: '3rem', height: '3rem'}} role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                    <h5 className="text-muted">Loading exam sessions...</h5>
                </div>
            </div>
        );
    }

    return (
        <div className="container-fluid py-4 bg-light min-vh-100">
            {/* Header Section */}
            <div className="row mb-4">
                <div className="col">
                    <div className="d-flex justify-content-between align-items-center">
                        <div>
                            <h1 className="h3 mb-1 text-dark fw-bold">Exam Session Management</h1>
                            <p className="text-muted mb-0 fs-6">
                                Create and manage exam periods with preference submission windows
                            </p>
                        </div>
                        <div className="d-flex gap-2">
                            <button
                                className="btn btn-outline-primary btn-sm px-3"
                                onClick={() => fetchSessions()}
                                disabled={loading}
                            >
                                <i className="bi bi-arrow-clockwise me-2"></i>
                                Refresh
                            </button>
                            <button
                                className="btn btn-primary btn-sm px-3"
                                onClick={openCreateModal}
                            >
                                <i className="bi bi-plus-circle me-2"></i>
                                New Session
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Alert Messages */}
            {error && (
                <div className="row mb-4">
                    <div className="col">
                        <div className="alert alert-danger alert-dismissible fade show border-0 shadow-sm" role="alert">
                            <div className="d-flex align-items-center">
                                <i className="bi bi-exclamation-triangle-fill me-2 fs-5"></i>
                                <div className="flex-grow-1">{error}</div>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setError('')}
                                    aria-label="Close"
                                ></button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {success && (
                <div className="row mb-4">
                    <div className="col">
                        <div className="alert alert-success alert-dismissible fade show border-0 shadow-sm" role="alert">
                            <div className="d-flex align-items-center">
                                <i className="bi bi-check-circle-fill me-2 fs-5"></i>
                                <div className="flex-grow-1">{success}</div>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setSuccess('')}
                                    aria-label="Close"
                                ></button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Statistics Overview */}
            <div className="row mb-4 g-3">
                <div className="col-lg-3 col-md-6">
                    <div className="card border-0 shadow-sm h-100 bg-primary text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="card-title mb-1 opacity-75 fs-6">Total Sessions</p>
                                    <h2 className="mb-0 fw-bold">{sessions.length}</h2>
                                </div>
                                <div className="opacity-75">
                                    <i className="bi bi-calendar-event fs-1"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="col-lg-3 col-md-6">
                    <div className="card border-0 shadow-sm h-100 bg-success text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="card-title mb-1 opacity-75 fs-6">Active Windows</p>
                                    <h2 className="mb-0 fw-bold">{sessions.filter(s => s.windowOpen).length}</h2>
                                </div>
                                <div className="opacity-75">
                                    <i className="bi bi-door-open fs-1"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="col-lg-3 col-md-6">
                    <div className="card border-0 shadow-sm h-100 bg-info text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="card-title mb-1 opacity-75 fs-6">Total Submissions</p>
                                    <h2 className="mb-0 fw-bold">{sessions.reduce((sum, s) => sum + s.totalSubmissions, 0)}</h2>
                                </div>
                                <div className="opacity-75">
                                    <i className="bi bi-file-earmark-text fs-1"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="col-lg-3 col-md-6">
                    <div className="card border-0 shadow-sm h-100 bg-warning text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="card-title mb-1 opacity-75 fs-6">Active Professors</p>
                                    <h2 className="mb-0 fw-bold">{sessions.reduce((sum, s) => sum + s.uniqueProfessors, 0)}</h2>
                                </div>
                                <div className="opacity-75">
                                    <i className="bi bi-people fs-1"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Sessions Grid */}
            <div className="row">
                {sessions.length === 0 ? (
                    <div className="col">
                        <div className="card border-0 shadow-sm text-center py-5">
                            <div className="card-body">
                                <i className="bi bi-calendar-x display-1 text-muted mb-3"></i>
                                <h4 className="text-muted mb-3">No Exam Sessions</h4>
                                <p className="text-muted mb-4">
                                    Create your first exam session to start managing preference submissions.
                                </p>
                                <button
                                    className="btn btn-primary"
                                    onClick={openCreateModal}
                                >
                                    <i className="bi bi-plus-circle me-2"></i>
                                    Create First Session
                                </button>
                            </div>
                        </div>
                    </div>
                ) : (
                    sessions.map((session) => (
                        <div key={session.examSessionPeriodId} className="col-xl-6 col-lg-8 col-12 mb-4">
                            <div className="card border-0 shadow-sm h-100">
                                <div className="card-header bg-white border-bottom">
                                    <div className="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h5 className="mb-1 fw-bold text-dark">
                                                {session.examSession} {session.academicYear}
                                            </h5>
                                            <small className="text-muted">
                                                Created {new Date(session.createdAt).toLocaleDateString()}
                                            </small>
                                        </div>
                                        <span className={`badge rounded-pill px-3 py-2 ${
                                            session.windowOpen ? 'bg-success' : 'bg-secondary'
                                        }`}>
                                            <i className={`bi ${session.windowOpen ? 'bi-door-open' : 'bi-door-closed'} me-1`}></i>
                                            {session.windowOpen ? 'Window Open' : 'Window Closed'}
                                        </span>
                                    </div>
                                </div>

                                <div className="card-body">
                                    <div className="row text-center mb-3">
                                        <div className="col-6">
                                            <div className="border-end pe-3">
                                                <h4 className="text-primary mb-1 fw-bold">{session.totalSubmissions}</h4>
                                                <small className="text-muted">Submissions</small>
                                            </div>
                                        </div>
                                        <div className="col-6">
                                            <div className="ps-3">
                                                <h4 className="text-success mb-1 fw-bold">{session.uniqueProfessors}</h4>
                                                <small className="text-muted">Professors</small>
                                            </div>
                                        </div>
                                    </div>

                                    {session.description && (
                                        <div className="mb-3">
                                            <div className="bg-light rounded p-3">
                                                <small className="text-muted">{session.description}</small>
                                            </div>
                                        </div>
                                    )}

                                    {session.submissionDeadline && (
                                        <div className="mb-3">
                                            <div className={`alert ${session.windowOpen ? 'alert-warning' : 'alert-info'} mb-0 py-2`}>
                                                <i className="bi bi-clock me-1"></i>
                                                <small>
                                                    <strong>Deadline:</strong> {new Date(session.submissionDeadline).toLocaleString()}
                                                </small>
                                            </div>
                                        </div>
                                    )}

                                    {session.windowOpen && session.instructions && (
                                        <div className="mb-3">
                                            <div className="alert alert-info py-2">
                                                <i className="bi bi-info-circle me-1"></i>
                                                <small><strong>Instructions:</strong> {session.instructions}</small>
                                            </div>
                                        </div>
                                    )}

                                    {session.windowClosedReason && (
                                        <div className="mb-3">
                                            <div className="alert alert-warning py-2">
                                                <i className="bi bi-exclamation-triangle me-1"></i>
                                                <small><strong>Closure Reason:</strong> {session.windowClosedReason}</small>
                                            </div>
                                        </div>
                                    )}
                                </div>

                                <div className="card-footer bg-white border-top">
                                    <div className="d-flex justify-content-between align-items-center">
                                        <div>
                                            {session.windowOpen ? (
                                                <button
                                                    className="btn btn-outline-danger btn-sm"
                                                    onClick={() => openCloseWindowModal(session)}
                                                >
                                                    <i className="bi bi-door-closed me-1"></i>
                                                    Close Window
                                                </button>
                                            ) : (
                                                <button
                                                    className="btn btn-outline-success btn-sm"
                                                    onClick={() => openOpenWindowModal(session)}
                                                >
                                                    <i className="bi bi-door-open me-1"></i>
                                                    Open Window
                                                </button>
                                            )}
                                        </div>
                                        <div className="btn-group">
                                            <button className="btn btn-outline-primary btn-sm">
                                                <i className="bi bi-eye me-1"></i>
                                                Details
                                            </button>
                                            <button className="btn btn-outline-info btn-sm">
                                                <i className="bi bi-graph-up me-1"></i>
                                                Analytics
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))
                )}
            </div>

            {/* Create Session Modal */}
            {showCreateModal && (
                <div className="modal fade show d-block" style={{backgroundColor: 'rgba(0,0,0,0.5)'}}>
                    <div className="modal-dialog modal-lg">
                        <div className="modal-content border-0 shadow">
                            <div className="modal-header bg-primary text-white">
                                <h5 className="modal-title fw-bold">
                                    <i className="bi bi-plus-circle me-2"></i>
                                    Create New Exam Session
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close btn-close-white"
                                    onClick={() => setShowCreateModal(false)}
                                ></button>
                            </div>
                            <div className="modal-body p-4">
                                <div className="row">
                                    <div className="col-md-6">
                                        <div className="form-group mb-3">
                                            <label className="form-label fw-semibold">Academic Year <span className="text-danger">*</span></label>
                                            <input
                                                type="text"
                                                className="form-control"
                                                value={createForm.academicYear}
                                                onChange={(e) => setCreateForm({
                                                    ...createForm,
                                                    academicYear: e.target.value
                                                })}
                                                placeholder="e.g., 2024/2025"
                                                required
                                            />
                                        </div>
                                    </div>
                                    <div className="col-md-6">
                                        <div className="form-group mb-3">
                                            <label className="form-label fw-semibold">Exam Session <span className="text-danger">*</span></label>
                                            <select
                                                className="form-select"
                                                value={createForm.examSession}
                                                onChange={(e) => setCreateForm({
                                                    ...createForm,
                                                    examSession: e.target.value
                                                })}
                                                required
                                            >
                                                {preferenceService.getExamSessionOptions().map(option => (
                                                    <option key={option.value} value={option.value}>
                                                        {option.label}
                                                    </option>
                                                ))}
                                            </select>
                                        </div>
                                    </div>
                                </div>

                                <div className="row">
                                    <div className="col-md-6">
                                        <div className="form-group mb-3">
                                            <label className="form-label fw-semibold">Planned Start Date <span className="text-danger">*</span></label>
                                            <input
                                                type="date"
                                                className="form-control"
                                                value={createForm.plannedStartDate}
                                                onChange={(e) => setCreateForm({
                                                    ...createForm,
                                                    plannedStartDate: e.target.value
                                                })}
                                                required
                                            />
                                        </div>
                                    </div>
                                    <div className="col-md-6">
                                        <div className="form-group mb-3">
                                            <label className="form-label fw-semibold">Planned End Date <span className="text-danger">*</span></label>
                                            <input
                                                type="date"
                                                className="form-control"
                                                value={createForm.plannedEndDate}
                                                onChange={(e) => setCreateForm({
                                                    ...createForm,
                                                    plannedEndDate: e.target.value
                                                })}
                                                required
                                            />
                                        </div>
                                    </div>
                                </div>

                                <div className="form-group mb-3">
                                    <label className="form-label fw-semibold">Description (Optional)</label>
                                    <textarea
                                        className="form-control"
                                        rows={3}
                                        value={createForm.description}
                                        onChange={(e) => setCreateForm({...createForm, description: e.target.value})}
                                        placeholder="Brief description of the exam session..."
                                    />
                                </div>
                            </div>
                            <div className="modal-footer bg-light">
                                <button
                                    type="button"
                                    className="btn btn-outline-secondary"
                                    onClick={() => setShowCreateModal(false)}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-primary"
                                    onClick={handleCreateSession}
                                    disabled={submitting || !createForm.academicYear || !createForm.plannedStartDate || !createForm.plannedEndDate}
                                >
                                    {submitting && <span className="spinner-border spinner-border-sm me-2"/>}
                                    <i className="bi bi-plus-circle me-2"></i>
                                    Create Session
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Open Submission Window Modal */}
            {showOpenWindowModal && selectedSession && (
                <div className="modal fade show d-block" style={{backgroundColor: 'rgba(0,0,0,0.5)'}}>
                    <div className="modal-dialog">
                        <div className="modal-content border-0 shadow">
                            <div className="modal-header bg-success text-white">
                                <h5 className="modal-title fw-bold">
                                    <i className="bi bi-door-open me-2"></i>
                                    Open Submission Window
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close btn-close-white"
                                    onClick={() => setShowOpenWindowModal(false)}
                                ></button>
                            </div>
                            <div className="modal-body p-4">
                                <div className="alert alert-info border-0">
                                    <div className="d-flex align-items-center">
                                        <i className="bi bi-info-circle me-2"></i>
                                        <div>
                                            <strong>Session:</strong> {selectedSession.examSession} {selectedSession.academicYear}
                                        </div>
                                    </div>
                                </div>

                                <div className="form-group mb-3">
                                    <label className="form-label fw-semibold">Submission Deadline <span className="text-danger">*</span></label>
                                    <input
                                        type="datetime-local"
                                        className="form-control"
                                        value={openWindowForm.submissionDeadline}
                                        onChange={(e) => setOpenWindowForm({
                                            ...openWindowForm,
                                            submissionDeadline: e.target.value
                                        })}
                                        required
                                    />
                                    <small className="form-text text-muted">
                                        Professors must submit their preferences before this deadline
                                    </small>
                                </div>
                            </div>
                            <div className="modal-footer bg-light">
                                <button
                                    type="button"
                                    className="btn btn-outline-secondary"
                                    onClick={() => setShowOpenWindowModal(false)}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-success"
                                    onClick={handleOpenWindow}
                                    disabled={submitting || !openWindowForm.submissionDeadline}
                                >
                                    {submitting && <span className="spinner-border spinner-border-sm me-2"/>}
                                    <i className="bi bi-door-open me-2"></i>
                                    Open Window
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Close Submission Window Modal */}
            {showCloseWindowModal && selectedSession && (
                <div className="modal fade show d-block" style={{backgroundColor: 'rgba(0,0,0,0.5)'}}>
                    <div className="modal-dialog">
                        <div className="modal-content border-0 shadow">
                            <div className="modal-header bg-warning text-dark">
                                <h5 className="modal-title fw-bold">
                                    <i className="bi bi-exclamation-triangle me-2"></i>
                                    Close Submission Window
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowCloseWindowModal(false)}
                                ></button>
                            </div>
                            <div className="modal-body p-4">
                                <div className="alert alert-warning border-0">
                                    <div className="d-flex align-items-start">
                                        <i className="bi bi-exclamation-triangle me-2 mt-1"></i>
                                        <div>
                                            Are you sure you want to close the submission window
                                            for <strong>{selectedSession.examSession} {selectedSession.academicYear}</strong>?
                                            <br/>
                                            <small className="text-muted">
                                                Current submissions: {selectedSession.totalSubmissions} from {selectedSession.uniqueProfessors} professors
                                            </small>
                                        </div>
                                    </div>
                                </div>

                                <div className="form-group mb-3">
                                    <label className="form-label fw-semibold">Reason for closing (Optional)</label>
                                    <textarea
                                        className="form-control"
                                        rows={3}
                                        value={closeWindowForm.reason}
                                        onChange={(e) => setCloseWindowForm({
                                            ...closeWindowForm,
                                            reason: e.target.value
                                        })}
                                        placeholder="Reason for closing the submission window early..."
                                    />
                                </div>
                            </div>
                            <div className="modal-footer bg-light">
                                <button
                                    type="button"
                                    className="btn btn-outline-secondary"
                                    onClick={() => setShowCloseWindowModal(false)}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-danger"
                                    onClick={handleCloseWindow}
                                    disabled={submitting}
                                >
                                    {submitting && <span className="spinner-border spinner-border-sm me-2"/>}
                                    <i className="bi bi-door-closed me-2"></i>
                                    Close Window
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminExamSessions;