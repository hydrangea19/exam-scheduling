import React from 'react';
import { Navbar, Nav, NavDropdown, Container } from 'react-bootstrap';
import { LinkContainer } from "react-router-bootstrap";
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const Navigation: React.FC = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        try {
            await logout();
            navigate('/login');
        } catch (error) {
            console.error('Logout failed:', error);
        }
    };

    const getRoleBasedNavItems = () => {
        const role = user?.role;

        switch (role) {
            case 'ADMINISTRATOR':
                return (
                    <>
                        <LinkContainer to="/dashboard">
                            <Nav.Link>Dashboard</Nav.Link>
                        </LinkContainer>
                        <NavDropdown title="Management" id="admin-dropdown">
                            <LinkContainer to="/exam-sessions">
                                <NavDropdown.Item>Exam Sessions</NavDropdown.Item>
                            </LinkContainer>
                            <LinkContainer to="/schedule-management">
                                <NavDropdown.Item>Schedule Management</NavDropdown.Item>
                            </LinkContainer>
                            <LinkContainer to="/user-management">
                                <NavDropdown.Item>User Management</NavDropdown.Item>
                            </LinkContainer>
                            <NavDropdown.Divider />
                            <LinkContainer to="/reports">
                                <NavDropdown.Item>Reports</NavDropdown.Item>
                            </LinkContainer>
                        </NavDropdown>
                        <LinkContainer to="/preferences">
                            <Nav.Link>Preferences</Nav.Link>
                        </LinkContainer>
                        <LinkContainer to="/published-schedules">
                            <Nav.Link>Published Schedules</Nav.Link>
                        </LinkContainer>
                    </>
                );

            case 'PROFESSOR':
                return (
                    <>
                        <LinkContainer to="/dashboard">
                            <Nav.Link>Dashboard</Nav.Link>
                        </LinkContainer>
                        <LinkContainer to="/my-preferences">
                            <Nav.Link>My Preferences</Nav.Link>
                        </LinkContainer>
                        <LinkContainer to="/my-schedule">
                            <Nav.Link>My Schedule</Nav.Link>
                        </LinkContainer>
                        <LinkContainer to="/feedback">
                            <Nav.Link>Schedule Feedback</Nav.Link>
                        </LinkContainer>
                    </>
                );

            default:
                return (
                    <>
                        <LinkContainer to="/dashboard">
                            <Nav.Link>Dashboard</Nav.Link>
                        </LinkContainer>
                        <LinkContainer to="/schedules">
                            <Nav.Link>View Schedules</Nav.Link>
                        </LinkContainer>
                    </>
                );
        }
    };

    return (
        <Navbar bg="dark" variant="dark" expand="lg" className="shadow-sm">
            <Container>
                <LinkContainer to="/dashboard">
                    <Navbar.Brand className="fw-bold">
                        Exam Scheduling System
                    </Navbar.Brand>
                </LinkContainer>

                <Navbar.Toggle aria-controls="basic-navbar-nav" />

                <Navbar.Collapse id="basic-navbar-nav">
                    <Nav className="me-auto">
                        {getRoleBasedNavItems()}
                    </Nav>

                    <Nav>
                        <NavDropdown
                            title={
                                <span>
                  <i className="bi bi-person-circle me-1"></i>
                                    {user?.fullName || user?.email || 'User'}
                </span>
                            }
                            id="user-dropdown"
                            align="end"
                        >
                            <NavDropdown.Header>
                                <div className="small text-muted">Signed in as</div>
                                <div className="fw-bold">{user?.email}</div>
                                <div className="small text-muted">Role: {user?.role}</div>
                            </NavDropdown.Header>
                            <NavDropdown.Divider />
                            <LinkContainer to="/profile">
                                <NavDropdown.Item>
                                    <i className="bi bi-person me-2"></i>
                                    Profile
                                </NavDropdown.Item>
                            </LinkContainer>
                            <LinkContainer to="/settings">
                                <NavDropdown.Item>
                                    <i className="bi bi-gear me-2"></i>
                                    Settings
                                </NavDropdown.Item>
                            </LinkContainer>
                            <NavDropdown.Divider />
                            <NavDropdown.Item onClick={handleLogout}>
                                <i className="bi bi-box-arrow-right me-2"></i>
                                Sign Out
                            </NavDropdown.Item>
                        </NavDropdown>
                    </Nav>
                </Navbar.Collapse>
            </Container>
        </Navbar>
    );
};

export default Navigation;