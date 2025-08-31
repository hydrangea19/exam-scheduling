import React, { useState, useEffect } from 'react';
import {
    Container,
    Row,
    Col,
    Card,
    Button,
    Table,
    Badge,
    Modal,
    Form,
    Alert,
    Spinner,
    Tabs,
    Tab
} from 'react-bootstrap';
import schedulingService, {
    type ScheduleResponse,
    type DetailedScheduleResponse,
    type CreateScheduleRequest,
    type AddExamRequest,
    type SubmitFeedbackRequest,
    type GenerationResponse
} from '../services/schedulingService';
import { useAuth } from '../context/AuthContext';

const SchedulingManagement: React.FC = () => {
    const { user } = useAuth();
    const [schedules, setSchedules] = useState<ScheduleResponse[]>([]);
    const [selectedSchedule, setSelectedSchedule] = useState<DetailedScheduleResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string>('');
    const [success, setSuccess] = useState<string>('');

    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showAddExamModal, setShowAddExamModal] = useState(false);
    const [showFeedbackModal, setShowFeedbackModal] = useState(false);
    const [showDetailsModal, setShowDetailsModal] = useState(false);

    const [createForm, setCreateForm] = useState<CreateScheduleRequest>({
        examSessionPeriodId: '',
        academicYear: '2025',
        examSession: 'WINTER',
        startDate: '',
        endDate: ''
    });

    const [examForm, setExamForm] = useState<AddExamRequest>({
        courseId: '',
        courseName: '',
        examDate: '',
        startTime: '09:00',
        endTime: '12:00',
        studentCount: 0,
        mandatoryStatus: 'MANDATORY',
        professorIds: []
    });

    const [feedbackForm, setFeedbackForm] = useState<SubmitFeedbackRequest>({
        scheduledExamId: '',
        commentText: '',
        commentType: 'GENERAL'
    });

    useEffect(() => {
        loadSchedules();
    }, []);

    const loadSchedules = async () => {
        try {
            setLoading(true);
            setError('');
            const data = await schedulingService.getSchedules();
            setSchedules(data);
        } catch (err) {
            setError('Failed to load schedules');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const loadScheduleDetails = async (scheduleId: string) => {
        try {
            setLoading(true);
            const data = await schedulingService.getSchedule(scheduleId);
            setSelectedSchedule(data);
            setShowDetailsModal(true);
        } catch (err) {
            setError('Failed to load schedule details');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleCreateSchedule = async () => {
        try {
            setLoading(true);
            setError('');

            const examSessionPeriodId = `${createForm.academicYear}_${createForm.examSession}_SCHEDULE`;

            const request = {
                ...createForm,
                examSessionPeriodId
            };

            await schedulingService.createSchedule(request);
            setSuccess('Schedule created successfully');
            setShowCreateModal(false);
            loadSchedules();

            setCreateForm({
                examSessionPeriodId: '',
                academicYear: '2025',
                examSession: 'WINTER',
                startDate: '',
                endDate: ''
            });
        } catch (err) {
            setError('Failed to create schedule');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleGenerateSchedule = async (scheduleId: string) => {
        try {
            setLoading(true);
            setError('');
            const response: GenerationResponse = await schedulingService.generateSchedule(scheduleId);
            setSuccess(`Schedule generation ${response.status.toLowerCase()}: ${response.message}`);
            loadSchedules();
        } catch (err) {
            setError('Failed to generate schedule');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handlePublishForReview = async (scheduleId: string) => {
        try {
            setLoading(true);
            setError('');
            await schedulingService.publishForReview(scheduleId, {});
            setSuccess('Schedule published for review');
            loadSchedules();
        } catch (err) {
            setError('Failed to publish schedule for review');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleFinalizeSchedule = async (scheduleId: string) => {
        try {
            setLoading(true);
            setError('');
            await schedulingService.finalizeSchedule(scheduleId, {});
            setSuccess('Schedule finalized');
            loadSchedules();
        } catch (err) {
            setError('Failed to finalize schedule');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleAddExam = async () => {
        if (!selectedSchedule) return;

        try {
            setLoading(true);
            setError('');
            await schedulingService.addExam(selectedSchedule.schedule.id, examForm);
            setSuccess('Exam added successfully');
            setShowAddExamModal(false);
            loadScheduleDetails(selectedSchedule.schedule.id);
        } catch (err) {
            setError('Failed to add exam');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmitFeedback = async () => {
        if (!selectedSchedule) return;

        try {
            setLoading(true);
            setError('');
            await schedulingService.submitFeedback(selectedSchedule.schedule.id, feedbackForm);
            setSuccess('Feedback submitted successfully');
            setShowFeedbackModal(false);
            loadScheduleDetails(selectedSchedule.schedule.id);
        } catch (err) {
            setError('Failed to submit feedback');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const getStatusBadgeVariant = (status: string) => {
        return schedulingService.getStatusColor(status);
    };

    const getSeverityBadgeVariant = (severity: string) => {
        return schedulingService.getSeverityColor(severity);
    };

    const canPerformAction = (action: string, schedule: ScheduleResponse) => {
        if (user?.role !== 'ADMIN') return false;

        switch (action) {
            case 'generate':
                return schedule.status === 'DRAFT';
            case 'publish':
                return schedule.status === 'DRAFT';
            case 'finalize':
                return schedule.status === 'IN_REVIEW';
            default:
                return true;
        }
    };

    return (
        <Container className="mt-4">
            <Row>
                <Col>
                    <h2>
                        <i className="bi bi-calendar-event me-2"></i>
                        Schedule Management
                    </h2>
                    <p className="text-muted">
                        Manage exam schedules, generate optimized timetables, and handle feedback.
                    </p>
                </Col>
            </Row>

            {error && (
                <Alert variant="danger" dismissible onClose={() => setError('')}>
                    <i className="bi bi-exclamation-triangle me-2"></i>
                    {error}
                </Alert>
            )}

            {success && (
                <Alert variant="success" dismissible onClose={() => setSuccess('')}>
                    <i className="bi bi-check-circle me-2"></i>
                    {success}
                </Alert>
            )}

            <Row className="mb-4">
                <Col>
                    <Card>
                        <Card.Header className="d-flex justify-content-between align-items-center">
                            <h5 className="mb-0">
                                <i className="bi bi-list me-2"></i>
                                Schedules
                            </h5>
                            {user?.role === 'ADMIN' && (
                                <Button
                                    variant="primary"
                                    onClick={() => setShowCreateModal(true)}
                                    disabled={loading}
                                >
                                    <i className="bi bi-plus-lg me-2"></i>
                                    Create Schedule
                                </Button>
                            )}
                        </Card.Header>
                        <Card.Body>
                            {loading ? (
                                <div className="text-center py-4">
                                    <Spinner animation="border" role="status">
                                        <span className="visually-hidden">Loading...</span>
                                    </Spinner>
                                </div>
                            ) : schedules.length === 0 ? (
                                <div className="text-center py-4 text-muted">
                                    <i className="bi bi-inbox display-4 d-block mb-3"></i>
                                    <p>No schedules found. Create your first schedule to get started.</p>
                                </div>
                            ) : (
                                <Table responsive hover>
                                    <thead>
                                    <tr>
                                        <th>Academic Year</th>
                                        <th>Exam Session</th>
                                        <th>Period</th>
                                        <th>Status</th>
                                        <th>Created</th>
                                        <th>Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {schedules.map((schedule) => (
                                        <tr key={schedule.id}>
                                            <td>{schedule.academicYear}</td>
                                            <td>{schedule.examSession}</td>
                                            <td>
                                                {new Date(schedule.startDate).toLocaleDateString()} - {' '}
                                                {new Date(schedule.endDate).toLocaleDateString()}
                                            </td>
                                            <td>
                                                <Badge bg={getStatusBadgeVariant(schedule.status)}>
                                                    {schedulingService.formatScheduleStatus(schedule.status)}
                                                </Badge>
                                            </td>
                                            <td>
                                                {new Date(schedule.createdAt).toLocaleDateString()}
                                            </td>
                                            <td>
                                                <div className="d-flex gap-2">
                                                    <Button
                                                        size="sm"
                                                        variant="outline-info"
                                                        onClick={() => loadScheduleDetails(schedule.id)}
                                                    >
                                                        <i className="bi bi-eye"></i>
                                                    </Button>

                                                    {canPerformAction('generate', schedule) && (
                                                        <Button
                                                            size="sm"
                                                            variant="outline-success"
                                                            onClick={() => handleGenerateSchedule(schedule.id)}
                                                            disabled={loading}
                                                            title="Generate Schedule"
                                                        >
                                                            <i className="bi bi-gear"></i>
                                                        </Button>
                                                    )}

                                                    {canPerformAction('publish', schedule) && (
                                                        <Button
                                                            size="sm"
                                                            variant="outline-warning"
                                                            onClick={() => handlePublishForReview(schedule.id)}
                                                            disabled={loading}
                                                            title="Publish for Review"
                                                        >
                                                            <i className="bi bi-share"></i>
                                                        </Button>
                                                    )}

                                                    {canPerformAction('finalize', schedule) && (
                                                        <Button
                                                            size="sm"
                                                            variant="outline-primary"
                                                            onClick={() => handleFinalizeSchedule(schedule.id)}
                                                            disabled={loading}
                                                            title="Finalize Schedule"
                                                        >
                                                            <i className="bi bi-check-square"></i>
                                                        </Button>
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </Table>
                            )}
                        </Card.Body>
                    </Card>
                </Col>
            </Row>

            {/* Create Schedule Modal */}
            <Modal show={showCreateModal} onHide={() => setShowCreateModal(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>Create New Schedule</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form>
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Academic Year</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={createForm.academicYear}
                                        onChange={(e) => setCreateForm({...createForm, academicYear: e.target.value})}
                                        placeholder="2025"
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Exam Session</Form.Label>
                                    <Form.Select
                                        value={createForm.examSession}
                                        onChange={(e) => setCreateForm({...createForm, examSession: e.target.value})}
                                    >
                                        <option value="WINTER">Winter</option>
                                        <option value="SUMMER">Summer</option>
                                        <option value="AUTUMN">Autumn</option>
                                    </Form.Select>
                                </Form.Group>
                            </Col>
                        </Row>
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Start Date</Form.Label>
                                    <Form.Control
                                        type="date"
                                        value={createForm.startDate}
                                        onChange={(e) => setCreateForm({...createForm, startDate: e.target.value})}
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>End Date</Form.Label>
                                    <Form.Control
                                        type="date"
                                        value={createForm.endDate}
                                        onChange={(e) => setCreateForm({...createForm, endDate: e.target.value})}
                                    />
                                </Form.Group>
                            </Col>
                        </Row>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowCreateModal(false)}>
                        Cancel
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleCreateSchedule}
                        disabled={loading || !createForm.startDate || !createForm.endDate}
                    >
                        {loading && <Spinner animation="border" size="sm" className="me-2" />}
                        Create Schedule
                    </Button>
                </Modal.Footer>
            </Modal>

            {/* Schedule Details Modal */}
            <Modal show={showDetailsModal} onHide={() => setShowDetailsModal(false)} size="xl">
                <Modal.Header closeButton>
                    <Modal.Title>
                        Schedule Details - {selectedSchedule?.schedule.academicYear} {selectedSchedule?.schedule.examSession}
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {selectedSchedule && (
                        <Tabs defaultActiveKey="exams">
                            <Tab eventKey="exams" title={`Exams (${selectedSchedule.exams.length})`}>
                                <div className="mt-3">
                                    {user?.role === 'ADMIN' && (
                                        <Button
                                            variant="primary"
                                            size="sm"
                                            className="mb-3"
                                            onClick={() => setShowAddExamModal(true)}
                                        >
                                            <i className="bi bi-plus me-2"></i>
                                            Add Exam
                                        </Button>
                                    )}

                                    {selectedSchedule.exams.length === 0 ? (
                                        <div className="text-center py-4 text-muted">
                                            No exams scheduled yet.
                                        </div>
                                    ) : (
                                        <Table responsive size="sm">
                                            <thead>
                                            <tr>
                                                <th>Course</th>
                                                <th>Date</th>
                                                <th>Time</th>
                                                <th>Room</th>
                                                <th>Students</th>
                                                <th>Type</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            {selectedSchedule.exams.map((exam) => (
                                                <tr key={exam.scheduledExamId}>
                                                    <td>
                                                        <strong>{exam.courseId}</strong><br/>
                                                        <small className="text-muted">{exam.courseName}</small>
                                                    </td>
                                                    <td>{new Date(exam.examDate).toLocaleDateString()}</td>
                                                    <td>{exam.startTime} - {exam.endTime}</td>
                                                    <td>
                                                        {exam.roomName || 'TBD'}<br/>
                                                        <small className="text-muted">
                                                            Capacity: {exam.roomCapacity || 'N/A'}
                                                        </small>
                                                    </td>
                                                    <td>{exam.studentCount}</td>
                                                    <td>
                                                        <Badge bg={exam.mandatoryStatus === 'MANDATORY' ? 'primary' : 'secondary'}>
                                                            {exam.mandatoryStatus}
                                                        </Badge>
                                                    </td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </Table>
                                    )}
                                </div>
                            </Tab>

                            <Tab eventKey="conflicts" title={`Conflicts (${selectedSchedule.conflicts.length})`}>
                                <div className="mt-3">
                                    {selectedSchedule.conflicts.length === 0 ? (
                                        <div className="text-center py-4 text-success">
                                            <i className="bi bi-check-circle display-4 d-block mb-3"></i>
                                            No conflicts detected!
                                        </div>
                                    ) : (
                                        selectedSchedule.conflicts.map((conflict) => (
                                            <Alert
                                                key={conflict.conflictId}
                                                variant={getSeverityBadgeVariant(conflict.severity)}
                                                className="d-flex justify-content-between align-items-start"
                                            >
                                                <div>
                                                    <strong>{conflict.conflictType}</strong><br/>
                                                    {conflict.description}<br/>
                                                    <small>Suggested: {conflict.suggestedResolution}</small>
                                                </div>
                                                <Badge bg={getSeverityBadgeVariant(conflict.severity)}>
                                                    {conflict.severity}
                                                </Badge>
                                            </Alert>
                                        ))
                                    )}
                                </div>
                            </Tab>

                            <Tab eventKey="feedback" title={`Feedback (${selectedSchedule.comments.length})`}>
                                <div className="mt-3">
                                    <Button
                                        variant="primary"
                                        size="sm"
                                        className="mb-3"
                                        onClick={() => setShowFeedbackModal(true)}
                                    >
                                        <i className="bi bi-plus me-2"></i>
                                        Add Feedback
                                    </Button>

                                    {selectedSchedule.comments.length === 0 ? (
                                        <div className="text-center py-4 text-muted">
                                            No feedback submitted yet.
                                        </div>
                                    ) : (
                                        selectedSchedule.comments.map((comment) => (
                                            <Alert key={comment.commentId} variant="info">
                                                <div className="d-flex justify-content-between mb-2">
                                                    <strong>Professor {comment.professorId}</strong>
                                                    <Badge bg="info">{comment.status}</Badge>
                                                </div>
                                                <p className="mb-1">{comment.commentText}</p>
                                                <small className="text-muted">
                                                    {new Date(comment.submittedAt).toLocaleString()}
                                                </small>
                                            </Alert>
                                        ))
                                    )}
                                </div>
                            </Tab>

                            {selectedSchedule.metrics && (
                                <Tab eventKey="metrics" title="Quality Metrics">
                                    <div className="mt-3">
                                        <Row>
                                            <Col md={4}>
                                                <Card className="text-center">
                                                    <Card.Body>
                                                        <h2 className="text-primary">
                                                            {(selectedSchedule.metrics.qualityScore * 100).toFixed(1)}%
                                                        </h2>
                                                        <p className="text-muted mb-0">Quality Score</p>
                                                    </Card.Body>
                                                </Card>
                                            </Col>
                                            <Col md={4}>
                                                <Card className="text-center">
                                                    <Card.Body>
                                                        <h2 className="text-success">
                                                            {(selectedSchedule.metrics.preferenceSatisfactionRate * 100).toFixed(1)}%
                                                        </h2>
                                                        <p className="text-muted mb-0">Preference Satisfaction</p>
                                                    </Card.Body>
                                                </Card>
                                            </Col>
                                            <Col md={4}>
                                                <Card className="text-center">
                                                    <Card.Body>
                                                        <h2 className="text-warning">
                                                            {selectedSchedule.metrics.totalConflicts}
                                                        </h2>
                                                        <p className="text-muted mb-0">Total Conflicts</p>
                                                    </Card.Body>
                                                </Card>
                                            </Col>
                                        </Row>
                                    </div>
                                </Tab>
                            )}
                        </Tabs>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowDetailsModal(false)}>
                        Close
                    </Button>
                </Modal.Footer>
            </Modal>

            {/* Add Exam Modal */}
            <Modal show={showAddExamModal} onHide={() => setShowAddExamModal(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>Add New Exam</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form>
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Course ID</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={examForm.courseId}
                                        onChange={(e) => setExamForm({...examForm, courseId: e.target.value})}
                                        placeholder="F18L3S155"
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Course Name</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={examForm.courseName}
                                        onChange={(e) => setExamForm({...examForm, courseName: e.target.value})}
                                        placeholder="Service Oriented Architecture"
                                    />
                                </Form.Group>
                            </Col>
                        </Row>
                        <Form.Group className="mb-3">
                            <Form.Label>Exam Date</Form.Label>
                            <Form.Control
                                type="date"
                                value={examForm.examDate}
                                onChange={(e) => setExamForm({...examForm, examDate: e.target.value})}
                            />
                        </Form.Group>
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Start Time</Form.Label>
                                    <Form.Control
                                        type="time"
                                        value={examForm.startTime}
                                        onChange={(e) => setExamForm({...examForm, startTime: e.target.value})}
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>End Time</Form.Label>
                                    <Form.Control
                                        type="time"
                                        value={examForm.endTime}
                                        onChange={(e) => setExamForm({...examForm, endTime: e.target.value})}
                                    />
                                </Form.Group>
                            </Col>
                        </Row>
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Student Count</Form.Label>
                                    <Form.Control
                                        type="number"
                                        value={examForm.studentCount}
                                        onChange={(e) => setExamForm({...examForm, studentCount: parseInt(e.target.value) || 0})}
                                        min="1"
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Type</Form.Label>
                                    <Form.Select
                                        value={examForm.mandatoryStatus}
                                        onChange={(e) => setExamForm({...examForm, mandatoryStatus: e.target.value as 'MANDATORY' | 'ELECTIVE'})}
                                    >
                                        <option value="MANDATORY">Mandatory</option>
                                        <option value="ELECTIVE">Elective</option>
                                    </Form.Select>
                                </Form.Group>
                            </Col>
                        </Row>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowAddExamModal(false)}>
                        Cancel
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleAddExam}
                        disabled={loading || !examForm.courseId || !examForm.courseName || !examForm.examDate}
                    >
                        {loading && <Spinner animation="border" size="sm" className="me-2" />}
                        Add Exam
                    </Button>
                </Modal.Footer>
            </Modal>

            {/* Feedback Modal */}
            <Modal show={showFeedbackModal} onHide={() => setShowFeedbackModal(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>Submit Feedback</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>Exam</Form.Label>
                            <Form.Select
                                value={feedbackForm.scheduledExamId}
                                onChange={(e) => setFeedbackForm({...feedbackForm, scheduledExamId: e.target.value})}
                            >
                                <option value="">Select an exam...</option>
                                {selectedSchedule?.exams.map((exam) => (
                                    <option key={exam.scheduledExamId} value={exam.scheduledExamId}>
                                        {exam.courseId} - {exam.courseName}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Comment Type</Form.Label>
                            <Form.Select
                                value={feedbackForm.commentType}
                                onChange={(e) => setFeedbackForm({...feedbackForm, commentType: e.target.value})}
                            >
                                <option value="GENERAL">General</option>
                                <option value="TIME_CONFLICT">Time Conflict</option>
                                <option value="ROOM_ISSUE">Room Issue</option>
                                <option value="SCHEDULING_ERROR">Scheduling Error</option>
                            </Form.Select>
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Comment</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={4}
                                value={feedbackForm.commentText}
                                onChange={(e) => setFeedbackForm({...feedbackForm, commentText: e.target.value})}
                                placeholder="Enter your feedback or concerns about this exam schedule..."
                            />
                        </Form.Group>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowFeedbackModal(false)}>
                        Cancel
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleSubmitFeedback}
                        disabled={loading || !feedbackForm.scheduledExamId || !feedbackForm.commentText}
                    >
                        {loading && <Spinner animation="border" size="sm" className="me-2" />}
                        Submit Feedback
                    </Button>
                </Modal.Footer>
            </Modal>
        </Container>
    );
};

export default SchedulingManagement;