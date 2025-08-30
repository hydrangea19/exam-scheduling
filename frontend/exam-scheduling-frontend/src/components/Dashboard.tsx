import React from 'react';
import {Alert, Badge, Card, Col, Container, Row} from 'react-bootstrap';
import {useAuth} from '../context/AuthContext';

const Dashboard: React.FC = () => {
    const {user} = useAuth();

    const getRoleBasedContent = () => {
        const role = user?.role;

        switch (role) {
            case 'ADMIN':
                return (
                    <Row>
                        <Col md={4} className="mb-4">
                            <Card className="h-100 border-0 shadow-sm">
                                <Card.Body className="text-center">
                                    <div className="display-4 text-primary mb-3">
                                        <i className="bi bi-calendar3"></i>
                                    </div>
                                    <Card.Title>Exam Sessions</Card.Title>
                                    <Card.Text>
                                        Manage and configure exam periods, set deadlines, and control the scheduling
                                        workflow.
                                    </Card.Text>
                                    <Badge bg="info">Admin Only</Badge>
                                </Card.Body>
                            </Card>
                        </Col>

                        <Col md={4} className="mb-4">
                            <Card className="h-100 border-0 shadow-sm">
                                <Card.Body className="text-center">
                                    <div className="display-4 text-success mb-3">
                                        <i className="bi bi-table"></i>
                                    </div>
                                    <Card.Title>Schedule Management</Card.Title>
                                    <Card.Text>
                                        Generate schedules, review professor feedback, and make administrative
                                        adjustments.
                                    </Card.Text>
                                    <Badge bg="success">Active</Badge>
                                </Card.Body>
                            </Card>
                        </Col>

                        <Col md={4} className="mb-4">
                            <Card className="h-100 border-0 shadow-sm">
                                <Card.Body className="text-center">
                                    <div className="display-4 text-warning mb-3">
                                        <i className="bi bi-people"></i>
                                    </div>
                                    <Card.Title>User Management</Card.Title>
                                    <Card.Text>
                                        Manage user accounts, roles, and permissions for the system.
                                    </Card.Text>
                                    <Badge bg="primary">Implemented</Badge>
                                </Card.Body>
                            </Card>
                        </Col>

                        <Col md={6} className="mb-4">
                            <Card className="h-100 border-0 shadow-sm">
                                <Card.Body>
                                    <Card.Title className="d-flex align-items-center">
                                        <i className="bi bi-graph-up text-primary me-2"></i>
                                        System Statistics
                                    </Card.Title>
                                    <div className="row text-center">
                                        <div className="col">
                                            <div className="h4 text-primary">0</div>
                                            <small className="text-muted">Active Sessions</small>
                                        </div>
                                        <div className="col">
                                            <div className="h4 text-success">0</div>
                                            <small className="text-muted">Generated Schedules</small>
                                        </div>
                                        <div className="col">
                                            <div className="h4 text-info">0</div>
                                            <small className="text-muted">Total Users</small>
                                        </div>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>

                        <Col md={6} className="mb-4">
                            <Card className="h-100 border-0 shadow-sm">
                                <Card.Body>
                                    <Card.Title className="d-flex align-items-center">
                                        <i className="bi bi-bell text-warning me-2"></i>
                                        Recent Activity
                                    </Card.Title>
                                    <div className="text-muted">
                                        <p className="mb-2">
                                            <small><i className="bi bi-dot"></i> User Management system is
                                                active</small>
                                        </p>
                                        <p className="mb-2">
                                            <small><i className="bi bi-dot"></i> Ready for user administration</small>
                                        </p>
                                        <p className="mb-0">
                                            <small><i className="bi bi-dot"></i> Welcome to the Exam Scheduling
                                                System</small>
                                        </p>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>
                    </Row>
                );

            case 'PROFESSOR':
                return (
                    <Row>
                        <Col md={6} className="mb-4">
                            <Card className="h-100 border-0 shadow-sm">
                                <Card.Body className="text-center">
                                    <div className="display-4 text-primary mb-3">
                                        <i className="bi bi-heart"></i>
                                    </div>
                                    <Card.Title>My Preferences</Card.Title>
                                    <Card.Text>
                                        Submit and manage your exam scheduling preferences for upcoming sessions.
                                    </Card.Text>
                                    <Badge bg="primary">Available</Badge>
                                </Card.Body>
                            </Card>
                        </Col>

                        <Col md={6} className="mb-4">
                            <Card className="h-100 border-0 shadow-sm">
                                <Card.Body className="text-center">
                                    <div className="display-4 text-success mb-3">
                                        <i className="bi bi-calendar-check"></i>
                                    </div>
                                    <Card.Title>My Schedule</Card.Title>
                                    <Card.Text>
                                        View your assigned exam schedules and provide feedback on arrangements.
                                    </Card.Text>
                                    <Badge bg="secondary">Pending</Badge>
                                </Card.Body>
                            </Card>
                        </Col>

                        <Col md={12} className="mb-4">
                            <Card className="border-0 shadow-sm">
                                <Card.Body>
                                    <Card.Title className="d-flex align-items-center">
                                        <i className="bi bi-info-circle text-info me-2"></i>
                                        Quick Status
                                    </Card.Title>
                                    <Row className="text-center">
                                        <Col md={4}>
                                            <div className="h5 text-primary">0</div>
                                            <small className="text-muted">Preferences Submitted</small>
                                        </Col>
                                        <Col md={4}>
                                            <div className="h5 text-success">0</div>
                                            <small className="text-muted">Schedules Assigned</small>
                                        </Col>
                                        <Col md={4}>
                                            <div className="h5 text-warning">0</div>
                                            <small className="text-muted">Feedback Pending</small>
                                        </Col>
                                    </Row>
                                </Card.Body>
                            </Card>
                        </Col>
                    </Row>
                );

            case 'STUDENT':
            default:
                return (
                    <Row>
                        <Col md={12}>
                            <Card className="border-0 shadow-sm">
                                <Card.Body className="text-center">
                                    <div className="display-4 text-info mb-3">
                                        <i className="bi bi-calendar2-week"></i>
                                    </div>
                                    <Card.Title>View Schedules</Card.Title>
                                    <Card.Text>
                                        Access published exam schedules and calendar information.
                                    </Card.Text>
                                    <Badge bg="info">Public Access</Badge>
                                </Card.Body>
                            </Card>
                        </Col>
                    </Row>
                );
        }
    };

    return (
        <Container className="mt-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h1 className="h3 mb-0">Welcome back, {user?.fullName || user?.email}</h1>
                    <p className="text-muted mb-0">
                        Role: <Badge bg="outline-secondary">{user?.role}</Badge>
                    </p>
                </div>
            </div>

            <Alert variant="info" className="mb-4">
                <Alert.Heading className="h6">
                    <i className="bi bi-info-circle me-2"></i>
                    System Status
                </Alert.Heading>
                User Management system is now integrated with the production API.
                {user?.role === 'ADMIN' && ' You can now manage users through the User Management section.'}
            </Alert>

            {getRoleBasedContent()}
        </Container>
    );
};

export default Dashboard;