import React, {useEffect, useState} from 'react';
import {
    type ExamSessionPeriodView,
    preferenceService,
    type PreferenceSubmissionSummary,
    type PreferredTimeSlot,
    type SubmitPreferencesRequest,
    type UnavailableTimeSlot,
    type UpdatePreferencesRequest,
    type WithdrawPreferencesRequest
} from '../services/preferenceService';

const ProfessorPreferences: React.FC = () => {
    const [availableSessions, setAvailableSessions] = useState<ExamSessionPeriodView[]>([]);
    const [myPreferences, setMyPreferences] = useState<PreferenceSubmissionSummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string>('');
    const [success, setSuccess] = useState<string>('');

    const [showSubmitModal, setShowSubmitModal] = useState(false);
    const [showUpdateModal, setShowUpdateModal] = useState(false);
    const [showWithdrawModal, setShowWithdrawModal] = useState(false);
    const [selectedSession, setSelectedSession] = useState<ExamSessionPeriodView | null>(null);
    const [selectedSubmission, setSelectedSubmission] = useState<PreferenceSubmissionSummary | null>(null);

    const [preferredSlots, setPreferredSlots] = useState<PreferredTimeSlot[]>([]);
    const [unavailableSlots, setUnavailableSlots] = useState<UnavailableTimeSlot[]>([]);
    const [additionalNotes, setAdditionalNotes] = useState('');
    const [withdrawReason, setWithdrawReason] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [activeTab, setActiveTab] = useState('available');
    const [activeModalTab, setActiveModalTab] = useState('preferred');

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            setLoading(true);
            const [sessions, preferences] = await Promise.all([
                preferenceService.getAvailableSessions(),
                preferenceService.getMyPreferences()
            ]);

            setAvailableSessions(sessions);
            setMyPreferences(preferences);
        } catch (err: any) {
            setError(err.message || 'Failed to load data');
        } finally {
            setLoading(false);
        }
    };

    const handleSubmitPreferences = async () => {
        if (!selectedSession) return;

        try {
            setSubmitting(true);
            const request: SubmitPreferencesRequest = {
                examSessionPeriodId: selectedSession.examSessionPeriodId,
                preferredTimeSlots: preferredSlots,
                unavailableTimeSlots: unavailableSlots,
                additionalNotes: additionalNotes || undefined
            };

            await preferenceService.submitMyPreferences(request);
            setSuccess('Preferences submitted successfully!');
            setShowSubmitModal(false);
            resetForm();
            await fetchData();
        } catch (err: any) {
            setError(err.message || 'Failed to submit preferences');
        } finally {
            setSubmitting(false);
        }
    };

    const handleUpdatePreferences = async () => {
        if (!selectedSubmission) return;

        try {
            setSubmitting(true);
            const request: UpdatePreferencesRequest = {
                professorId: selectedSubmission.professorId,
                examSessionPeriodId: selectedSubmission.examSessionPeriodId,
                preferredTimeSlots: preferredSlots,
                unavailableTimeSlots: unavailableSlots,
                additionalNotes: additionalNotes || undefined
            };

            await preferenceService.updatePreferences(selectedSubmission.submissionId, request);
            setSuccess('Preferences updated successfully!');
            setShowUpdateModal(false);
            resetForm();
            await fetchData();
        } catch (err: any) {
            setError(err.message || 'Failed to update preferences');
        } finally {
            setSubmitting(false);
        }
    };

    const handleWithdrawPreferences = async () => {
        if (!selectedSubmission) return;

        try {
            setSubmitting(true);
            const request: WithdrawPreferencesRequest = {
                professorId: selectedSubmission.professorId,
                reason: withdrawReason
            };

            await preferenceService.withdrawPreferences(selectedSubmission.submissionId, request);
            setSuccess('Preferences withdrawn successfully!');
            setShowWithdrawModal(false);
            setWithdrawReason('');
            await fetchData();
        } catch (err: any) {
            setError(err.message || 'Failed to withdraw preferences');
        } finally {
            setSubmitting(false);
        }
    };

    const openSubmitModal = (session: ExamSessionPeriodView) => {
        setSelectedSession(session);
        setShowSubmitModal(true);
        resetForm();
    };

    const openUpdateModal = (submission: PreferenceSubmissionSummary) => {
        setSelectedSubmission(submission);
        setPreferredSlots([...submission.preferredTimeSlots]);
        setUnavailableSlots([...submission.unavailableTimeSlots]);
        setAdditionalNotes(submission.additionalNotes || '');
        setShowUpdateModal(true);
    };

    const openWithdrawModal = (submission: PreferenceSubmissionSummary) => {
        setSelectedSubmission(submission);
        setShowWithdrawModal(true);
    };

    const resetForm = () => {
        setPreferredSlots([]);
        setUnavailableSlots([]);
        setAdditionalNotes('');
        setSelectedSession(null);
        setSelectedSubmission(null);
        setActiveModalTab('preferred');
    };

    const addPreferredSlot = () => {
        setPreferredSlots([...preferredSlots, {
            dayOfWeek: 'MONDAY',
            startTime: '09:00',
            endTime: '11:00',
            priority: 1
        }]);
    };

    const addUnavailableSlot = () => {
        setUnavailableSlots([...unavailableSlots, {
            dayOfWeek: 'MONDAY',
            startTime: '09:00',
            endTime: '11:00',
            reason: ''
        }]);
    };

    const updatePreferredSlot = (index: number, field: keyof PreferredTimeSlot, value: any) => {
        const updated = [...preferredSlots];
        updated[index] = {...updated[index], [field]: value};
        setPreferredSlots(updated);
    };

    const updateUnavailableSlot = (index: number, field: keyof UnavailableTimeSlot, value: any) => {
        const updated = [...unavailableSlots];
        updated[index] = {...updated[index], [field]: value};
        setUnavailableSlots(updated);
    };

    const removePreferredSlot = (index: number) => {
        setPreferredSlots(preferredSlots.filter((_, i) => i !== index));
    };

    const removeUnavailableSlot = (index: number) => {
        setUnavailableSlots(unavailableSlots.filter((_, i) => i !== index));
    };

    if (loading) {
        return (
            <div className="container mt-4 text-center">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
                <p className="mt-2 text-muted">Loading preferences...</p>
            </div>
        );
    }

    return (
        <div className="container mt-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2 className="mb-1">My Preferences</h2>
                    <p className="text-muted mb-0">Manage your exam scheduling preferences</p>
                </div>
                <button
                    className="btn btn-primary"
                    onClick={() => fetchData()}
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

            {/* Navigation Tabs */}
            <ul className="nav nav-tabs mb-4" role="tablist">
                <li className="nav-item" role="presentation">
                    <button
                        className={`nav-link ${activeTab === 'available' ? 'active' : ''}`}
                        onClick={() => setActiveTab('available')}
                        type="button"
                        role="tab"
                    >
                        <i className="bi bi-calendar-plus me-2"></i>
                        Available Sessions ({availableSessions.length})
                    </button>
                </li>
                <li className="nav-item" role="presentation">
                    <button
                        className={`nav-link ${activeTab === 'submitted' ? 'active' : ''}`}
                        onClick={() => setActiveTab('submitted')}
                        type="button"
                        role="tab"
                    >
                        <i className="bi bi-check-circle me-2"></i>
                        My Submissions ({myPreferences.length})
                    </button>
                </li>
            </ul>

            {/* Available Sessions Tab */}
            {activeTab === 'available' && (
                <div className="tab-content">
                    <div className="row">
                        {availableSessions.length === 0 ? (
                            <div className="col">
                                <div className="card text-center">
                                    <div className="card-body py-5">
                                        <div className="display-4 text-muted mb-3">
                                            <i className="bi bi-calendar-x"></i>
                                        </div>
                                        <h4 className="text-muted">No Available Sessions</h4>
                                        <p className="text-muted">
                                            There are currently no exam sessions accepting preference submissions.
                                        </p>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            availableSessions.map((session) => (
                                <div key={session.examSessionPeriodId} className="col-md-6 col-lg-4 mb-4">
                                    <div className="card h-100 border-0 shadow-sm">
                                        <div className="card-body">
                                            <div className="d-flex justify-content-between align-items-start mb-3">
                                                <span className="badge bg-success">Open for Submissions</span>
                                                <small className="text-muted">
                                                    {session.totalSubmissions} submissions
                                                </small>
                                            </div>

                                            <h5 className="card-title">
                                                {session.examSession} {session.academicYear}
                                            </h5>

                                            <p className="card-text text-muted small mb-3">
                                                {session.description}
                                            </p>

                                            <div className="mb-3">
                                                <small className="text-muted d-block">
                                                    <i className="bi bi-calendar-range me-1"></i>
                                                    {new Date(session.startDate).toLocaleDateString()} - {new Date(session.endDate).toLocaleDateString()}
                                                </small>
                                                {session.submissionDeadline && (
                                                    <small className="text-danger d-block">
                                                        <i className="bi bi-clock me-1"></i>
                                                        Deadline: {new Date(session.submissionDeadline).toLocaleString()}
                                                    </small>
                                                )}
                                            </div>

                                            <button
                                                className="btn btn-primary btn-sm w-100"
                                                onClick={() => openSubmitModal(session)}
                                            >
                                                <i className="bi bi-plus-circle me-2"></i>
                                                Submit Preferences
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}

            {/* My Submissions Tab */}
            {activeTab === 'submitted' && (
                <div className="tab-content">
                    {myPreferences.length === 0 ? (
                        <div className="card text-center">
                            <div className="card-body py-5">
                                <div className="display-4 text-muted mb-3">
                                    <i className="bi bi-clipboard-x"></i>
                                </div>
                                <h4 className="text-muted">No Submitted Preferences</h4>
                                <p className="text-muted">
                                    You haven't submitted any preferences yet. Check the Available Sessions tab to
                                    submit your preferences.
                                </p>
                            </div>
                        </div>
                    ) : (
                        <div className="accordion" id="preferencesAccordion">
                            {myPreferences.map((submission, index) => (
                                <div key={submission.submissionId} className="accordion-item">
                                    <h2 className="accordion-header" id={`heading${index}`}>
                                        <button
                                            className="accordion-button collapsed"
                                            type="button"
                                            data-bs-toggle="collapse"
                                            data-bs-target={`#collapse${index}`}
                                            aria-expanded="false"
                                            aria-controls={`collapse${index}`}
                                        >
                                            <div
                                                className="d-flex justify-content-between align-items-center w-100 me-3">
                                                <div>
                                                    <strong>{submission.examSessionInfo}</strong>
                                                    <span className="text-muted ms-2">
                                                        Submitted {new Date(submission.submittedAt).toLocaleDateString()}
                                                    </span>
                                                </div>
                                                <span className={`badge ${
                                                    submission.status === 'SUBMITTED' ? 'bg-success' :
                                                        submission.status === 'UPDATED' ? 'bg-info' : 'bg-warning'
                                                }`}>
                                                    {submission.status}
                                                </span>
                                            </div>
                                        </button>
                                    </h2>
                                    <div
                                        id={`collapse${index}`}
                                        className="accordion-collapse collapse"
                                        aria-labelledby={`heading${index}`}
                                        data-bs-parent="#preferencesAccordion"
                                    >
                                        <div className="accordion-body">
                                            <div className="row">
                                                <div className="col-md-6">
                                                    <h6 className="text-primary">
                                                        <i className="bi bi-heart me-2"></i>
                                                        Preferred Time Slots ({submission.preferredSlotsCount})
                                                    </h6>
                                                    {submission.preferredTimeSlots.length > 0 ? (
                                                        <ul className="list-group list-group-flush mb-3">
                                                            {submission.preferredTimeSlots.map((slot, i) => (
                                                                <li key={i} className="list-group-item px-0">
                                                                    <div className="d-flex justify-content-between">
                                                                        <span>
                                                                            <strong>{slot.dayOfWeek}</strong> {slot.startTime}-{slot.endTime}
                                                                        </span>
                                                                        <span className="badge bg-outline-primary">
                                                                            Priority {slot.priority}
                                                                        </span>
                                                                    </div>
                                                                </li>
                                                            ))}
                                                        </ul>
                                                    ) : (
                                                        <p className="text-muted">No preferred slots specified</p>
                                                    )}
                                                </div>
                                                <div className="col-md-6">
                                                    <h6 className="text-danger">
                                                        <i className="bi bi-x-circle me-2"></i>
                                                        Unavailable Time Slots ({submission.unavailableSlotsCount})
                                                    </h6>
                                                    {submission.unavailableTimeSlots.length > 0 ? (
                                                        <ul className="list-group list-group-flush mb-3">
                                                            {submission.unavailableTimeSlots.map((slot, i) => (
                                                                <li key={i} className="list-group-item px-0">
                                                                    <div>
                                                                        <strong>{slot.dayOfWeek}</strong> {slot.startTime}-{slot.endTime}
                                                                        {slot.reason && (
                                                                            <small className="d-block text-muted">
                                                                                Reason: {slot.reason}
                                                                            </small>
                                                                        )}
                                                                    </div>
                                                                </li>
                                                            ))}
                                                        </ul>
                                                    ) : (
                                                        <p className="text-muted">No unavailable slots specified</p>
                                                    )}
                                                </div>
                                            </div>

                                            {submission.hasAdditionalNotes && submission.additionalNotes && (
                                                <div className="mb-3">
                                                    <h6 className="text-info">
                                                        <i className="bi bi-chat-text me-2"></i>
                                                        Additional Notes
                                                    </h6>
                                                    <div className="bg-light p-3 rounded">
                                                        {submission.additionalNotes}
                                                    </div>
                                                </div>
                                            )}

                                            <div className="d-flex gap-2">
                                                <button
                                                    className="btn btn-outline-primary btn-sm"
                                                    onClick={() => openUpdateModal(submission)}
                                                >
                                                    <i className="bi bi-pencil me-2"></i>
                                                    Update
                                                </button>
                                                <button
                                                    className="btn btn-outline-danger btn-sm"
                                                    onClick={() => openWithdrawModal(submission)}
                                                >
                                                    <i className="bi bi-trash me-2"></i>
                                                    Withdraw
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* Submit Modal */}
            {showSubmitModal && (
                <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}}>
                    <div className="modal-dialog modal-lg">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">
                                    <i className="bi bi-plus-circle me-2"></i>
                                    Submit Preferences
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowSubmitModal(false)}
                                ></button>
                            </div>
                            <div className="modal-body">
                                {selectedSession && (
                                    <div className="alert alert-info mb-4">
                                        <strong>Session:</strong> {selectedSession.examSession} {selectedSession.academicYear}<br/>
                                        <strong>Period:</strong> {new Date(selectedSession.startDate).toLocaleDateString()} - {new Date(selectedSession.endDate).toLocaleDateString()}
                                        {selectedSession.submissionDeadline && (
                                            <>
                                                <br/>
                                                <strong
                                                    className="text-danger">Deadline:</strong> {new Date(selectedSession.submissionDeadline).toLocaleString()}
                                            </>
                                        )}
                                    </div>
                                )}

                                {/* Modal Tabs */}
                                <ul className="nav nav-tabs mb-3" role="tablist">
                                    <li className="nav-item" role="presentation">
                                        <button
                                            className={`nav-link ${activeModalTab === 'preferred' ? 'active' : ''}`}
                                            onClick={() => setActiveModalTab('preferred')}
                                            type="button"
                                            role="tab"
                                        >
                                            Preferred Times
                                        </button>
                                    </li>
                                    <li className="nav-item" role="presentation">
                                        <button
                                            className={`nav-link ${activeModalTab === 'unavailable' ? 'active' : ''}`}
                                            onClick={() => setActiveModalTab('unavailable')}
                                            type="button"
                                            role="tab"
                                        >
                                            Unavailable Times
                                        </button>
                                    </li>
                                    <li className="nav-item" role="presentation">
                                        <button
                                            className={`nav-link ${activeModalTab === 'notes' ? 'active' : ''}`}
                                            onClick={() => setActiveModalTab('notes')}
                                            type="button"
                                            role="tab"
                                        >
                                            Additional Notes
                                        </button>
                                    </li>
                                </ul>

                                {/* Preferred Times Tab */}
                                {activeModalTab === 'preferred' && (
                                    <div className="tab-pane fade show active">
                                        <div className="d-flex justify-content-between align-items-center mb-3">
                                            <h6 className="mb-0">Preferred Time Slots</h6>
                                            <button
                                                className="btn btn-outline-primary btn-sm"
                                                onClick={addPreferredSlot}
                                            >
                                                <i className="bi bi-plus me-1"></i>
                                                Add Slot
                                            </button>
                                        </div>

                                        {preferredSlots.map((slot, index) => (
                                            <div key={index} className="card mb-3">
                                                <div className="card-body">
                                                    <div className="row align-items-center">
                                                        <div className="col-md-3">
                                                            <select
                                                                className="form-select"
                                                                value={slot.dayOfWeek}
                                                                onChange={(e) => updatePreferredSlot(index, 'dayOfWeek', e.target.value)}
                                                            >
                                                                {preferenceService.getDayOfWeekOptions().map(option => (
                                                                    <option key={option.value} value={option.value}>
                                                                        {option.label}
                                                                    </option>
                                                                ))}
                                                            </select>
                                                        </div>
                                                        <div className="col-md-2">
                                                            <input
                                                                type="time"
                                                                className="form-control"
                                                                value={slot.startTime}
                                                                onChange={(e) => updatePreferredSlot(index, 'startTime', e.target.value)}
                                                            />
                                                        </div>
                                                        <div className="col-md-2">
                                                            <input
                                                                type="time"
                                                                className="form-control"
                                                                value={slot.endTime}
                                                                onChange={(e) => updatePreferredSlot(index, 'endTime', e.target.value)}
                                                            />
                                                        </div>
                                                        <div className="col-md-3">
                                                            <select
                                                                className="form-select"
                                                                value={slot.priority}
                                                                onChange={(e) => updatePreferredSlot(index, 'priority', parseInt(e.target.value))}
                                                            >
                                                                {preferenceService.getPriorityOptions().map(option => (
                                                                    <option key={option.value} value={option.value}>
                                                                        {option.label}
                                                                    </option>
                                                                ))}
                                                            </select>
                                                        </div>
                                                        <div className="col-md-1">
                                                            <button
                                                                className="btn btn-outline-danger btn-sm"
                                                                onClick={() => removePreferredSlot(index)}
                                                            >
                                                                <i className="bi bi-trash"></i>
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}

                                        {preferredSlots.length === 0 && (
                                            <div className="text-center text-muted py-4">
                                                <i className="bi bi-heart display-4 d-block mb-2"></i>
                                                No preferred time slots added yet.<br/>
                                                Click "Add Slot" to specify your preferences.
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Unavailable Times Tab */}
                                {activeModalTab === 'unavailable' && (
                                    <div className="tab-pane fade show active">
                                        <div className="d-flex justify-content-between align-items-center mb-3">
                                            <h6 className="mb-0">Unavailable Time Slots</h6>
                                            <button
                                                className="btn btn-outline-danger btn-sm"
                                                onClick={addUnavailableSlot}
                                            >
                                                <i className="bi bi-plus me-1"></i>
                                                Add Slot
                                            </button>
                                        </div>

                                        {unavailableSlots.map((slot, index) => (
                                            <div key={index} className="card mb-3">
                                                <div className="card-body">
                                                    <div className="row align-items-center">
                                                        <div className="col-md-2">
                                                            <select
                                                                className="form-select"
                                                                value={slot.dayOfWeek}
                                                                onChange={(e) => updateUnavailableSlot(index, 'dayOfWeek', e.target.value)}
                                                            >
                                                                {preferenceService.getDayOfWeekOptions().map(option => (
                                                                    <option key={option.value} value={option.value}>
                                                                        {option.label}
                                                                    </option>
                                                                ))}
                                                            </select>
                                                        </div>
                                                        <div className="col-md-2">
                                                            <input
                                                                type="time"
                                                                className="form-control"
                                                                value={slot.startTime}
                                                                onChange={(e) => updateUnavailableSlot(index, 'startTime', e.target.value)}
                                                            />
                                                        </div>
                                                        <div className="col-md-2">
                                                            <input
                                                                type="time"
                                                                className="form-control"
                                                                value={slot.endTime}
                                                                onChange={(e) => updateUnavailableSlot(index, 'endTime', e.target.value)}
                                                            />
                                                        </div>
                                                        <div className="col-md-4">
                                                            <input
                                                                type="text"
                                                                className="form-control"
                                                                placeholder="Reason (optional)"
                                                                value={slot.reason}
                                                                onChange={(e) => updateUnavailableSlot(index, 'reason', e.target.value)}
                                                            />
                                                        </div>
                                                        <div className="col-md-1">
                                                            <button
                                                                className="btn btn-outline-danger btn-sm"
                                                                onClick={() => removeUnavailableSlot(index)}
                                                            >
                                                                <i className="bi bi-trash"></i>
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}

                                        {unavailableSlots.length === 0 && (
                                            <div className="text-center text-muted py-4">
                                                <i className="bi bi-x-circle display-4 d-block mb-2"></i>
                                                No unavailable time slots specified.<br/>
                                                Click "Add Slot" to mark times when you're not available.
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Notes Tab */}
                                {activeModalTab === 'notes' && (
                                    <div className="tab-pane fade show active">
                                        <div className="form-group">
                                            <label className="form-label">Additional Notes (Optional)</label>
                                            <textarea
                                                className="form-control"
                                                rows={4}
                                                value={additionalNotes}
                                                onChange={(e) => setAdditionalNotes(e.target.value)}
                                                placeholder="Any additional information or special requirements..."
                                            />
                                        </div>
                                    </div>
                                )}
                            </div>
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => setShowSubmitModal(false)}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-primary"
                                    onClick={handleSubmitPreferences}
                                    disabled={submitting || (preferredSlots.length === 0 && unavailableSlots.length === 0)}
                                >
                                    {submitting && <span className="spinner-border spinner-border-sm me-2"/>}
                                    <i className="bi bi-check-circle me-2"></i>
                                    Submit Preferences
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Update Modal - Simplified version */}
            {showUpdateModal && (
                <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}}>
                    <div className="modal-dialog modal-lg">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">
                                    <i className="bi bi-pencil me-2"></i>
                                    Update Preferences
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowUpdateModal(false)}
                                ></button>
                            </div>
                            <div className="modal-body">
                                <div className="alert alert-warning mb-4">
                                    <strong>Updating:</strong> {selectedSubmission?.examSessionInfo}<br/>
                                    <small>Last
                                        updated: {selectedSubmission?.lastUpdatedAt ? new Date(selectedSubmission.lastUpdatedAt).toLocaleString() : 'Never'}</small>
                                </div>

                                <div className="row mb-3">
                                    <div className="col-6">
                                        <label className="form-label">Preferred Slots: {preferredSlots.length}</label>
                                        <div className="btn-group w-100">
                                            <button
                                                className="btn btn-outline-primary btn-sm"
                                                onClick={addPreferredSlot}
                                            >
                                                <i className="bi bi-plus me-1"></i>
                                                Add Preferred
                                            </button>
                                        </div>
                                    </div>
                                    <div className="col-6">
                                        <label className="form-label">Unavailable
                                            Slots: {unavailableSlots.length}</label>
                                        <div className="btn-group w-100">
                                            <button
                                                className="btn btn-outline-danger btn-sm"
                                                onClick={addUnavailableSlot}
                                            >
                                                <i className="bi bi-plus me-1"></i>
                                                Add Unavailable
                                            </button>
                                        </div>
                                    </div>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Additional Notes</label>
                                    <textarea
                                        className="form-control"
                                        rows={3}
                                        value={additionalNotes}
                                        onChange={(e) => setAdditionalNotes(e.target.value)}
                                        placeholder="Additional notes..."
                                    />
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => setShowUpdateModal(false)}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-primary"
                                    onClick={handleUpdatePreferences}
                                    disabled={submitting}
                                >
                                    {submitting && <span className="spinner-border spinner-border-sm me-2"/>}
                                    <i className="bi bi-check-circle me-2"></i>
                                    Update Preferences
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Withdraw Modal */}
            {showWithdrawModal && (
                <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}}>
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title text-danger">
                                    <i className="bi bi-exclamation-triangle me-2"></i>
                                    Withdraw Preferences
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowWithdrawModal(false)}
                                ></button>
                            </div>
                            <div className="modal-body">
                                <div className="alert alert-warning">
                                    Are you sure you want to withdraw your preferences
                                    for <strong>{selectedSubmission?.examSessionInfo}</strong>?
                                    This action cannot be undone.
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Reason for withdrawal <span
                                        className="text-danger">*</span></label>
                                    <textarea
                                        className="form-control"
                                        rows={3}
                                        value={withdrawReason}
                                        onChange={(e) => setWithdrawReason(e.target.value)}
                                        placeholder="Please provide a reason for withdrawing your preferences..."
                                        required
                                    />
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => setShowWithdrawModal(false)}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-danger"
                                    onClick={handleWithdrawPreferences}
                                    disabled={submitting || !withdrawReason.trim()}
                                >
                                    {submitting && <span className="spinner-border spinner-border-sm me-2"/>}
                                    <i className="bi bi-trash me-2"></i>
                                    Withdraw Preferences
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ProfessorPreferences;