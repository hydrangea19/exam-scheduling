import React, {useEffect, useState} from 'react';
import {
    type CloseSubmissionWindowRequest,
    type CreateExamSessionPeriodRequest,
    type ExamSessionPeriodView,
    type OpenSubmissionWindowRequest,
    preferenceService
} from '../services/preferenceService';

const AdminExamSessions: React.FC = () => {
    const [sessions, setSessions] = useState<ExamSessionPeriodView[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string>('');
    const [success, setSuccess] = useState<string>('');

    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showOpenWindowModal, setShowOpenWindowModal] = useState(false);
    const [showCloseWindowModal, setShowCloseWindowModal] = useState(false);
    const [selectedSession, setSelectedSession] = useState<ExamSessionPeriodView | null>(null);

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
            setSessions(sessionsData);
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
            await preferenceService.closeSubmissionWindow(selectedSession.examSessionPeriodId, closeWindowForm);
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

    const openOpenWindowModal = (session: ExamSessionPeriodView) => {
        setSelectedSession(session);
        setShowOpenWindowModal(true);
        setOpenWindowForm({
            examSessionPeriodId: session.examSessionPeriodId,
            submissionDeadline: '',
            // instructions: '',
            academicYear: session.academicYear,
            examSession: session.examSession,
            openedBy: 'web'
        });
    };

    const openCloseWindowModal = (session: ExamSessionPeriodView) => {
        setSelectedSession(session);
        setShowCloseWindowModal(true);
        setCloseWindowForm({
            reason: ''
        });
    };

    if (loading) {
        return (
            <div className="container mt-4 text-center">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
                <p className="mt-2 text-muted">Loading exam sessions...</p>
            </div>
        );
    }

    return (
        <div className="container mt-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2 className="mb-1">Exam Session Management</h2>
                    <p className="text-muted mb-0">Create and manage exam periods and preference submission windows</p>
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
                    <button
                        className="btn btn-primary"
                        onClick={openCreateModal}
                    >
                        <i className="bi bi-plus-circle me-2"></i>
                        Create New Session
                    </button>
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

            {/* Sessions Overview */}
            <div className="row mb-4">
                <div className="col-md-3">
                    <div className="card bg-primary text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <h6 className="card-title">Total Sessions</h6>
                                    <h3 className="mb-0">{sessions.length}</h3>
                                </div>
                                <div className="align-self-center">
                                    <i className="bi bi-calendar-event display-4"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="col-md-3">
                    <div className="card bg-success text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <h6 className="card-title">Active Windows</h6>
                                    <h3 className="mb-0">{sessions.filter(s => s.isWindowOpen).length}</h3>
                                </div>
                                <div className="align-self-center">
                                    <i className="bi bi-door-open display-4"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="col-md-3">
                    <div className="card bg-info text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <h6 className="card-title">Total Submissions</h6>
                                    <h3 className="mb-0">{sessions.reduce((sum, s) => sum + s.totalSubmissions, 0)}</h3>
                                </div>
                                <div className="align-self-center">
                                    <i className="bi bi-file-earmark-text display-4"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="col-md-3">
                    <div className="card bg-warning text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <h6 className="card-title">Participating Professors</h6>
                                    <h3 className="mb-0">{sessions.reduce((sum, s) => sum + s.uniqueProfessors, 0)}</h3>
                                </div>
                                <div className="align-self-center">
                                    <i className="bi bi-people display-4"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Sessions List */}
            <div className="row">
                {sessions.length === 0 ? (
                    <div className="col">
                        <div className="card text-center">
                            <div className="card-body py-5">
                                <div className="display-4 text-muted mb-3">
                                    <i className="bi bi-calendar-x"></i>
                                </div>
                                <h4 className="text-muted">No Exam Sessions</h4>
                                <p className="text-muted">
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
                        <div key={session.examSessionPeriodId} className="col-lg-6 mb-4">
                            <div className="card h-100 border-0 shadow-sm">
                                <div className="card-header bg-light border-0">
                                    <div className="d-flex justify-content-between align-items-center">
                                        <h5 className="mb-0">
                                            {session.examSession} {session.academicYear}
                                        </h5>
                                        <span className={`badge ${
                                            session.isWindowOpen ? 'bg-success' : 'bg-secondary'
                                        }`}>
                                            {session.isWindowOpen ? 'Window Open' : 'Window Closed'}
                                        </span>
                                    </div>
                                </div>
                                <div className="card-body">
                                    <p className="text-muted mb-3">
                                        {session.description}
                                    </p>

                                    <div className="row text-center mb-3">
                                        <div className="col-6">
                                            <div className="border-end">
                                                <div className="h5 text-primary mb-1">{session.totalSubmissions}</div>
                                                <small className="text-muted">Submissions</small>
                                            </div>
                                        </div>
                                        <div className="col-6">
                                            <div className="h5 text-success mb-1">{session.uniqueProfessors}</div>
                                            <small className="text-muted">Professors</small>
                                        </div>
                                    </div>

                                    <div className="mb-3">
                                        <small className="text-muted d-block">
                                            <i className="bi bi-calendar-range me-1"></i>
                                            Exam
                                            Period: {new Date(session.startDate).toLocaleDateString()} - {new Date(session.endDate).toLocaleDateString()}
                                        </small>
                                        <small className="text-muted d-block">
                                            <i className="bi bi-person me-1"></i>
                                            Created
                                            by: {session.createdBy} on {new Date(session.createdAt).toLocaleDateString()}
                                        </small>
                                        {session.submissionDeadline && (
                                            <small className="text-danger d-block">
                                                <i className="bi bi-clock me-1"></i>
                                                Submission
                                                Deadline: {new Date(session.submissionDeadline).toLocaleString()}
                                            </small>
                                        )}
                                    </div>

                                    {session.isWindowOpen && session.instructions && (
                                        <div className="alert alert-info py-2 px-3 mb-3">
                                            <small>
                                                <i className="bi bi-info-circle me-1"></i>
                                                <strong>Instructions:</strong> {session.instructions}
                                            </small>
                                        </div>
                                    )}

                                    {session.windowClosedReason && (
                                        <div className="alert alert-warning py-2 px-3 mb-3">
                                            <small>
                                                <i className="bi bi-exclamation-triangle me-1"></i>
                                                <strong>Closed:</strong> {session.windowClosedReason}
                                            </small>
                                        </div>
                                    )}
                                </div>
                                <div className="card-footer bg-light border-0">
                                    <div className="d-flex justify-content-between align-items-center">
                                        {session.isWindowOpen ? (
                                            <button
                                                className="btn btn-outline-danger btn-sm"
                                                onClick={() => openCloseWindowModal(session)}
                                            >
                                                <i className="bi bi-door-closed me-2"></i>
                                                Close Window
                                            </button>
                                        ) : (
                                            <button
                                                className="btn btn-outline-success btn-sm"
                                                onClick={() => openOpenWindowModal(session)}
                                            >
                                                <i className="bi bi-door-open me-2"></i>
                                                Open Window
                                            </button>
                                        )}
                                        <div className="btn-group">
                                            <button className="btn btn-outline-primary btn-sm">
                                                <i className="bi bi-eye me-2"></i>
                                                View Details
                                            </button>
                                            <button className="btn btn-outline-info btn-sm">
                                                <i className="bi bi-graph-up me-2"></i>
                                                Statistics
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
                <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}}>
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">
                                    <i className="bi bi-plus-circle me-2"></i>
                                    Create New Exam Session
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowCreateModal(false)}
                                ></button>
                            </div>
                            <div className="modal-body">
                                <div className="row">
                                    <div className="col-md-6">
                                        <div className="form-group mb-3">
                                            <label className="form-label">Academic Year <span
                                                className="text-danger">*</span></label>
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
                                            <label className="form-label">Exam Session <span
                                                className="text-danger">*</span></label>
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
                                            <label className="form-label">Start Date <span
                                                className="text-danger">*</span></label>
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
                                            <label className="form-label">End Date <span
                                                className="text-danger">*</span></label>
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
                                    <label className="form-label">Description (Optional)</label>
                                    <textarea
                                        className="form-control"
                                        rows={3}
                                        value={createForm.description}
                                        onChange={(e) => setCreateForm({...createForm, description: e.target.value})}
                                        placeholder="Brief description of the exam session..."
                                    />
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
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
                <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}}>
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">
                                    <i className="bi bi-door-open me-2"></i>
                                    Open Submission Window
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowOpenWindowModal(false)}
                                ></button>
                            </div>
                            <div className="modal-body">
                                <div className="alert alert-info">
                                    <strong>Session:</strong> {selectedSession.examSession} {selectedSession.academicYear}<br/>
                                    <strong>Exam
                                        Period:</strong> {new Date(selectedSession.startDate).toLocaleDateString()} - {new Date(selectedSession.endDate).toLocaleDateString()}
                                </div>

                                <div className="form-group mb-3">
                                    <label className="form-label">Submission Deadline <span
                                        className="text-danger">*</span></label>
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
                                {/*
                                <div className="form-group mb-3">
                                    <label className="form-label">Instructions (Optional)</label>
                                    <textarea
                                        className="form-control"
                                        rows={4}
                                        value={openWindowForm.instructions}
                                        onChange={(e) => setOpenWindowForm({
                                            ...openWindowForm,
                                            instructions: e.target.value
                                        })}
                                        placeholder="Special instructions for professors submitting preferences..."
                                    />
                                </div>*/}
                            </div>
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
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
                <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}}>
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title text-warning">
                                    <i className="bi bi-exclamation-triangle me-2"></i>
                                    Close Submission Window
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowCloseWindowModal(false)}
                                ></button>
                            </div>
                            <div className="modal-body">
                                <div className="alert alert-warning">
                                    Are you sure you want to close the submission window
                                    for <strong>{selectedSession.examSession} {selectedSession.academicYear}</strong>?
                                    <br/>
                                    <small>Current
                                        submissions: {selectedSession.totalSubmissions} from {selectedSession.uniqueProfessors} professors</small>
                                </div>

                                <div className="form-group mb-3">
                                    <label className="form-label">Reason for closing (Optional)</label>
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
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
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