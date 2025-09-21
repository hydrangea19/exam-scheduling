import React, { useState } from 'react';
import {
    Container,
    Row,
    Col,
    Card,
    Button,
    Form,
    Alert,
    Spinner,
    Badge
} from 'react-bootstrap';
import publishingService, { type PublishedScheduleResponse } from '../services/publishingService';

const PublishedSchedules: React.FC = () => {
    const [schedule, setSchedule] = useState<PublishedScheduleResponse | null>(null);
    const [scheduleId, setScheduleId] = useState<string>('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string>('');
    const [success, setSuccess] = useState<string>('');

    const handleLookupSchedule = async () => {
        if (!scheduleId.trim()) {
            setError('Please enter a schedule ID');
            return;
        }

        try {
            setLoading(true);
            setError('');
            setSuccess('');

            const data = await publishingService.getPublishedSchedule(scheduleId);
            setSchedule(data);
            setSuccess('Published schedule found!');
        } catch (err) {
            setError('Schedule not found or not published yet');
            setSchedule(null);
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleClear = () => {
        setSchedule(null);
        setScheduleId('');
        setError('');
        setSuccess('');
    };

    const getStatusBadgeVariant = (status: string) => {
        return publishingService.getStatusColor(status);
    };

    const handleExportPdf = async (scheduleId: string) => {
        try {
            setLoading(true);
            setError('');

            const blob = await publishingService.exportScheduleToPdf(scheduleId);

            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `exam_schedule_${scheduleId.substring(0, 8)}.pdf`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            setSuccess('PDF downloaded successfully');
        } catch (err) {
            setError('Failed to export PDF. Please try again.');
            console.error('PDF export error:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleExportExcel = async (scheduleId: string) => {
        try {
            setLoading(true);
            setError('');

            const blob = await publishingService.exportScheduleToExcel(scheduleId);

            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `exam_schedule_${scheduleId.substring(0, 8)}.xlsx`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            setSuccess('Excel file downloaded successfully');
        } catch (err) {
            setError('Failed to export Excel. Please try again.');
            console.error('Excel export error:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleViewInBrowser = async (scheduleId: string) => {
        try {
            setLoading(true);
            setError('');

            const blob = await publishingService.exportScheduleToPdf(scheduleId);
            const url = window.URL.createObjectURL(blob);

            // Open PDF in new tab
            window.open(url, '_blank');

            setSuccess('Schedule opened in new tab');

            // Clean up the URL after a delay
            setTimeout(() => {
                window.URL.revokeObjectURL(url);
            }, 1000);
        } catch (err) {
            setError('Failed to open schedule. Please try again.');
            console.error('View error:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Container className="mt-4">
            <Row>
                <Col>
                    <h2>
                        <i className="bi bi-calendar-check me-2"></i>
                        Published Schedules
                    </h2>
                    <p className="text-muted">
                        View published exam schedules and their details.
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
                        <Card.Header>
                            <h5 className="mb-0">
                                <i className="bi bi-search me-2"></i>
                                Schedule Lookup
                            </h5>
                        </Card.Header>
                        <Card.Body>
                            <Form onSubmit={(e) => { e.preventDefault(); handleLookupSchedule(); }}>
                                <Row className="align-items-end">
                                    <Col md={8}>
                                        <Form.Group className="mb-3">
                                            <Form.Label>Schedule ID</Form.Label>
                                            <Form.Control
                                                type="text"
                                                value={scheduleId}
                                                onChange={(e) => setScheduleId(e.target.value)}
                                                placeholder="Enter schedule ID (UUID)"
                                                disabled={loading}
                                            />
                                            <Form.Text className="text-muted">
                                                Enter the UUID of the schedule you want to view
                                            </Form.Text>
                                        </Form.Group>
                                    </Col>
                                    <Col md={4}>
                                        <div className="d-flex gap-2 mb-3">
                                            <Button
                                                variant="primary"
                                                onClick={handleLookupSchedule}
                                                disabled={loading || !scheduleId.trim()}
                                                className="flex-grow-1"
                                            >
                                                {loading && <Spinner animation="border" size="sm" className="me-2" />}
                                                <i className="bi bi-search me-2"></i>
                                                Lookup
                                            </Button>
                                            <Button
                                                variant="outline-secondary"
                                                onClick={handleClear}
                                                disabled={loading}
                                            >
                                                <i className="bi bi-arrow-clockwise"></i>
                                            </Button>
                                        </div>
                                    </Col>
                                </Row>
                            </Form>
                        </Card.Body>
                    </Card>
                </Col>
            </Row>

            {schedule && (
                <Row>
                    <Col>
                        <Card>
                            <Card.Header className="d-flex justify-content-between align-items-center">
                                <h5 className="mb-0">
                                    <i className="bi bi-calendar-event me-2"></i>
                                    Schedule Details
                                </h5>
                                <Badge bg={getStatusBadgeVariant(schedule.publicationStatus)}>
                                    {publishingService.formatPublicationStatus(schedule.publicationStatus)}
                                </Badge>
                            </Card.Header>
                            <Card.Body>
                                <Row>
                                    <Col md={6}>
                                        <div className="mb-3">
                                            <strong>Title:</strong>
                                            <div className="mt-1">{schedule.title}</div>
                                        </div>

                                        <div className="mb-3">
                                            <strong>Academic Year:</strong>
                                            <div className="mt-1">{schedule.academicYear}</div>
                                        </div>

                                        <div className="mb-3">
                                            <strong>Exam Session:</strong>
                                            <div className="mt-1">{schedule.examSession}</div>
                                        </div>

                                        <div className="mb-3">
                                            <strong>Session Period ID:</strong>
                                            <div className="mt-1">
                                                <code>{schedule.examSessionPeriodId}</code>
                                            </div>
                                        </div>
                                    </Col>

                                    <Col md={6}>
                                        <div className="mb-3">
                                            <strong>Original Schedule ID:</strong>
                                            <div className="mt-1">
                                                <code>{schedule.scheduleId}</code>
                                            </div>
                                        </div>

                                        <div className="mb-3">
                                            <strong>Created:</strong>
                                            <div className="mt-1">
                                                {new Date(schedule.createdAt).toLocaleString()}
                                            </div>
                                        </div>

                                        {schedule.publishedAt && (
                                            <div className="mb-3">
                                                <strong>Published:</strong>
                                                <div className="mt-1">
                                                    {new Date(schedule.publishedAt).toLocaleString()}
                                                    {schedule.publishedBy && (
                                                        <div className="text-muted small">
                                                            by {schedule.publishedBy}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        )}

                                        <div className="mb-3">
                                            <strong>Visibility:</strong>
                                            <div className="mt-1">
                                                {schedule.isPublic ? (
                                                    <Badge bg="success">
                                                        <i className="bi bi-globe me-1"></i>
                                                        Public
                                                    </Badge>
                                                ) : (
                                                    <Badge bg="warning">
                                                        <i className="bi bi-lock me-1"></i>
                                                        Private
                                                    </Badge>
                                                )}
                                            </div>
                                        </div>
                                    </Col>
                                </Row>

                                {schedule.description && (
                                    <Row>
                                        <Col>
                                            <div className="mb-0">
                                                <strong>Description:</strong>
                                                <div className="mt-2 p-3 bg-light rounded">
                                                    {schedule.description}
                                                </div>
                                            </div>
                                        </Col>
                                    </Row>
                                )}
                                <Row>
                                    <Col>
                                        <div className="border-top pt-3">
                                            <strong className="mb-3 d-block">Export Options:</strong>
                                            <div className="d-flex gap-2 flex-wrap">
                                                <Button
                                                    variant="outline-danger"
                                                    onClick={() => handleExportPdf(schedule.scheduleId)}
                                                    disabled={loading}
                                                    size="sm"
                                                >
                                                    {loading ? (
                                                        <Spinner animation="border" size="sm" className="me-2" />
                                                    ) : (
                                                        <i className="bi bi-file-pdf me-2"></i>
                                                    )}
                                                    Export PDF
                                                </Button>
                                                <Button
                                                    variant="outline-success"
                                                    onClick={() => handleExportExcel(schedule.scheduleId)}
                                                    disabled={loading}
                                                    size="sm"
                                                >
                                                    {loading ? (
                                                        <Spinner animation="border" size="sm" className="me-2" />
                                                    ) : (
                                                        <i className="bi bi-file-excel me-2"></i>
                                                    )}
                                                    Export Excel
                                                </Button>
                                                <Button
                                                    variant="outline-primary"
                                                    onClick={() => handleViewInBrowser(schedule.scheduleId)}
                                                    disabled={loading}
                                                    size="sm"
                                                >
                                                    <i className="bi bi-eye me-2"></i>
                                                    View in Browser
                                                </Button>
                                            </div>
                                        </div>
                                    </Col>
                                </Row>
                            </Card.Body>
                        </Card>
                    </Col>
                </Row>
            )}

            {!schedule && !loading && (
                <Row>
                    <Col>
                        <div className="text-center py-5">
                            <i className="bi bi-calendar-x display-1 text-muted mb-3"></i>
                            <h4 className="text-muted">No Schedule Selected</h4>
                            <p className="text-muted">
                                Enter a schedule ID above to view published schedule details.
                            </p>
                        </div>
                    </Col>
                </Row>
            )}
        </Container>
    );
};

export default PublishedSchedules;