import React, {useCallback, useEffect, useState} from 'react';
import {
    Alert,
    Badge,
    Button,
    ButtonGroup,
    Card,
    Col,
    Container,
    Dropdown,
    Form,
    InputGroup,
    Modal,
    OverlayTrigger,
    Pagination,
    Row,
    Spinner,
    Table,
    Toast,
    ToastContainer,
    Tooltip
} from 'react-bootstrap';
import authService, {
    type ChangeUserRoleRequest,
    type CreateUserRequest,
    type DeactivateUserRequest,
    type PagedUserResponse,
    type UpdateUserProfileRequest,
    type UserResponse,
    type UserStatisticsResponse,
    type ValidationErrorResponse
} from '../services/authService';
import type {AxiosError} from 'axios';

const UserManagement: React.FC = () => {
    const [users, setUsers] = useState<UserResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalUsers, setTotalUsers] = useState(0);
    const pageSize = 15;

    const [searchEmail, setSearchEmail] = useState('');
    const [searchName, setSearchName] = useState('');
    const [filterRole, setFilterRole] = useState('');
    const [filterActive, setFilterActive] = useState<boolean | undefined>(undefined);

    const [debouncedEmail, setDebouncedEmail] = useState('');
    const [debouncedName, setDebouncedName] = useState('');

    const [statistics, setStatistics] = useState<UserStatisticsResponse | null>(null);

    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [showRoleModal, setShowRoleModal] = useState(false);
    const [showDeactivateModal, setShowDeactivateModal] = useState(false);
    const [selectedUser, setSelectedUser] = useState<UserResponse | null>(null);

    const [createForm, setCreateForm] = useState<CreateUserRequest>({
        email: '',
        firstName: '',
        lastName: '',
        middleName: '',
        role: 'STUDENT'
    });

    const [editForm, setEditForm] = useState<UpdateUserProfileRequest>({
        firstName: '',
        lastName: '',
        middleName: ''
    });

    const [roleForm, setRoleForm] = useState<ChangeUserRoleRequest>({
        currentRole: '',
        newRole: '',
        reason: ''
    });

    const [deactivateForm, setDeactivateForm] = useState<DeactivateUserRequest>({
        reason: ''
    });

    const [error, setError] = useState<string>('');
    const [fieldErrors, setFieldErrors] = useState<{ [key: string]: string }>({});

    const [toasts, setToasts] = useState<Array<{ id: number, type: 'success' | 'error', message: string }>>([]);
    const [toastCounter, setToastCounter] = useState(0);

    useEffect(() => {
        const handler = setTimeout(() => {
            setDebouncedEmail(searchEmail);
            setDebouncedName(searchName);
            setCurrentPage(0);
        }, 500);
        return () => clearTimeout(handler);
    }, [searchEmail, searchName]);

    useEffect(() => {
        loadUsers();
        loadStatistics();
    }, [currentPage, debouncedEmail, debouncedName, filterRole, filterActive]);

    const loadUsers = useCallback(async () => {
        try {
            setLoading(true);
            setError('');

            const hasFilters = debouncedEmail || debouncedName || filterRole || filterActive !== undefined;
            let response: PagedUserResponse;

            if (hasFilters) {
                response = await authService.searchUsers({
                    email: debouncedEmail || undefined,
                    fullName: debouncedName || undefined,
                    role: filterRole || undefined,
                    active: filterActive,
                    page: currentPage,
                    size: pageSize,
                    sortBy: 'fullName',
                    sortDirection: 'ASC'
                });
            } else {
                response = await authService.getAllUsers(currentPage, pageSize, 'fullName', 'ASC');
            }

            setUsers(response.users);
            setTotalPages(response.totalPages);
            setTotalUsers(response.totalElements);
        } catch (err) {
            handleApiError(err, 'Failed to load users');
        } finally {
            setLoading(false);
        }
    }, [currentPage, debouncedEmail, debouncedName, filterRole, filterActive]);

    const loadStatistics = async () => {
        try {
            const stats = await authService.getUserStatistics();
            setStatistics(stats);
        } catch (err) {
            console.error('Failed to load statistics:', err);
        }
    };

    const handleApiError = (error: unknown, defaultMessage: string) => {
        const axiosError = error as AxiosError<ValidationErrorResponse>;
        if (axiosError.response?.data?.fieldErrors) {
            setFieldErrors(axiosError.response.data.fieldErrors);
            setError('Please check the form for validation errors.');
        } else {
            setError(axiosError.response?.data?.message || defaultMessage);
            setFieldErrors({});
        }
    };

    const showToast = (type: 'success' | 'error', message: string) => {
        const newToast = {id: toastCounter, type, message};
        setToasts(prev => [...prev, newToast]);
        setToastCounter(prev => prev + 1);

        setTimeout(() => {
            setToasts(prev => prev.filter(toast => toast.id !== newToast.id));
        }, 4000);
    };

    const resetForms = () => {
        setCreateForm({email: '', firstName: '', lastName: '', middleName: '', role: 'STUDENT'});
        setEditForm({firstName: '', lastName: '', middleName: ''});
        setRoleForm({currentRole: '', newRole: '', reason: ''});
        setDeactivateForm({reason: ''});
        setError('');
        setFieldErrors({});
    };

    const handleCreateUser = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await authService.createUser(createForm);
            setShowCreateModal(false);
            resetForms();
            showToast('success', 'User created successfully');
            loadUsers();
            loadStatistics();
        } catch (err) {
            handleApiError(err, 'Failed to create user');
        }
    };

    const handleEditUser = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedUser) return;

        try {
            await authService.updateUserProfile(selectedUser.id, editForm);
            setShowEditModal(false);
            setSelectedUser(null);
            resetForms();
            showToast('success', 'User profile updated successfully');
            loadUsers();
        } catch (err) {
            handleApiError(err, 'Failed to update user profile');
        }
    };

    const handleChangeRole = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedUser) return;

        try {
            await authService.changeUserRole(selectedUser.id, roleForm);
            setShowRoleModal(false);
            setSelectedUser(null);
            resetForms();
            showToast('success', 'User role changed successfully');
            loadUsers();
            loadStatistics();
        } catch (err) {
            handleApiError(err, 'Failed to change user role');
        }
    };

    const handleDeactivateUser = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedUser) return;

        try {
            await authService.deactivateUser(selectedUser.id, deactivateForm);
            setShowDeactivateModal(false);
            setSelectedUser(null);
            resetForms();
            showToast('success', 'User deactivated successfully');
            loadUsers();
            loadStatistics();
        } catch (err) {
            handleApiError(err, 'Failed to deactivate user');
        }
    };

    const handleActivateUser = async (user: UserResponse) => {
        try {
            await authService.activateUser(user.id, {reason: 'Activated by administrator'});
            showToast('success', 'User activated successfully');
            loadUsers();
            loadStatistics();
        } catch (err) {
            handleApiError(err, 'Failed to activate user');
        }
    };

    const openCreateModal = () => {
        resetForms();
        setShowCreateModal(true);
    };

    const openEditModal = (user: UserResponse) => {
        setSelectedUser(user);
        setEditForm({
            firstName: user.firstName,
            lastName: user.lastName,
            middleName: user.middleName || ''
        });
        resetForms();
        setShowEditModal(true);
    };

    const openRoleModal = (user: UserResponse) => {
        resetForms();
        setSelectedUser(user);
        setRoleForm({
            currentRole: user.role,
            newRole: user.role,
            reason: ''
        });
        setShowRoleModal(true);
    };

    const openDeactivateModal = (user: UserResponse) => {
        setSelectedUser(user);
        setDeactivateForm({reason: ''});
        resetForms();
        setShowDeactivateModal(true);
    };

    const clearFilters = () => {
        setSearchEmail('');
        setSearchName('');
        setFilterRole('');
        setFilterActive(undefined);
        setCurrentPage(0);
    };

    const getRoleBadgeVariant = (role: string) => {
        switch (role) {
            case 'ADMIN':
                return 'danger';
            case 'PROFESSOR':
                return 'primary';
            case 'STUDENT':
                return 'success';
            default:
                return 'secondary';
        }
    };

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    return (
        <div style={{backgroundColor: '#f8f9fa', minHeight: '100vh'}}>
            <Container fluid className="py-4">
                {/* Header */}
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h2 className="mb-1" style={{color: '#2c3e50', fontWeight: '600'}}>User Management</h2>
                        <p className="text-muted mb-0">Manage system users, roles, and permissions</p>
                    </div>
                    <Button
                        variant="primary"
                        size="lg"
                        onClick={openCreateModal}
                        style={{
                            borderRadius: '8px',
                            boxShadow: '0 2px 4px rgba(0,123,255,0.15)',
                            padding: '12px 24px'
                        }}
                    >
                        <i className="bi bi-person-plus me-2"></i>
                        Add New User
                    </Button>
                </div>

                {/* Statistics Cards */}
                {statistics && (
                    <Row className="mb-4">
                        <Col lg={3} md={6} className="mb-3">
                            <Card className="border-0 shadow-sm h-100" style={{borderLeft: '4px solid #007bff'}}>
                                <Card.Body className="d-flex align-items-center">
                                    <div className="flex-grow-1">
                                        <div className="text-muted small mb-1">Total Users</div>
                                        <div
                                            className="h3 mb-0 font-weight-bold text-primary">{statistics.totalUsers}</div>
                                    </div>
                                    <div className="text-primary opacity-50">
                                        <i className="bi bi-people" style={{fontSize: '2rem'}}></i>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>
                        <Col lg={3} md={6} className="mb-3">
                            <Card className="border-0 shadow-sm h-100" style={{borderLeft: '4px solid #28a745'}}>
                                <Card.Body className="d-flex align-items-center">
                                    <div className="flex-grow-1">
                                        <div className="text-muted small mb-1">Active Users</div>
                                        <div
                                            className="h3 mb-0 font-weight-bold text-success">{statistics.activeUsers}</div>
                                    </div>
                                    <div className="text-success opacity-50">
                                        <i className="bi bi-person-check" style={{fontSize: '2rem'}}></i>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>
                        <Col lg={3} md={6} className="mb-3">
                            <Card className="border-0 shadow-sm h-100" style={{borderLeft: '4px solid #ffc107'}}>
                                <Card.Body className="d-flex align-items-center">
                                    <div className="flex-grow-1">
                                        <div className="text-muted small mb-1">Inactive Users</div>
                                        <div
                                            className="h3 mb-0 font-weight-bold text-warning">{statistics.inactiveUsers}</div>
                                    </div>
                                    <div className="text-warning opacity-50">
                                        <i className="bi bi-person-x" style={{fontSize: '2rem'}}></i>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>
                        <Col lg={3} md={6} className="mb-3">
                            <Card className="border-0 shadow-sm h-100" style={{borderLeft: '4px solid #dc3545'}}>
                                <Card.Body className="d-flex align-items-center">
                                    <div className="flex-grow-1">
                                        <div className="text-muted small mb-1">Administrators</div>
                                        <div
                                            className="h3 mb-0 font-weight-bold text-danger">{statistics.roleBreakdown?.ADMIN || 0}</div>
                                    </div>
                                    <div className="text-danger opacity-50">
                                        <i className="bi bi-shield-check" style={{fontSize: '2rem'}}></i>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>
                    </Row>
                )}

                {/* Filters Card */}
                <Card className="border-0 shadow-sm mb-4">
                    <Card.Body>
                        <Row className="g-3">
                            <Col md={3}>
                                <Form.Label className="small fw-semibold text-muted">Search by Email</Form.Label>
                                <InputGroup>
                                    <InputGroup.Text><i className="bi bi-envelope"></i></InputGroup.Text>
                                    <Form.Control
                                        type="email"
                                        placeholder="Enter email..."
                                        value={searchEmail}
                                        onChange={(e) => setSearchEmail(e.target.value)}
                                    />
                                </InputGroup>
                            </Col>
                            <Col md={3}>
                                <Form.Label className="small fw-semibold text-muted">Search by Name</Form.Label>
                                <InputGroup>
                                    <InputGroup.Text><i className="bi bi-person"></i></InputGroup.Text>
                                    <Form.Control
                                        type="text"
                                        placeholder="Enter name..."
                                        value={searchName}
                                        onChange={(e) => setSearchName(e.target.value)}
                                    />
                                </InputGroup>
                            </Col>
                            <Col md={2}>
                                <Form.Label className="small fw-semibold text-muted">Filter by Role</Form.Label>
                                <Form.Select
                                    value={filterRole}
                                    onChange={(e) => setFilterRole(e.target.value)}
                                >
                                    <option value="">All Roles</option>
                                    <option value="ADMIN">Admin</option>
                                    <option value="PROFESSOR">Professor</option>
                                    <option value="STUDENT">Student</option>
                                </Form.Select>
                            </Col>
                            <Col md={2}>
                                <Form.Label className="small fw-semibold text-muted">Filter by Status</Form.Label>
                                <Form.Select
                                    value={filterActive === undefined ? '' : filterActive.toString()}
                                    onChange={(e) => setFilterActive(e.target.value === '' ? undefined : e.target.value === 'true')}
                                >
                                    <option value="">All Users</option>
                                    <option value="true">Active</option>
                                    <option value="false">Inactive</option>
                                </Form.Select>
                            </Col>
                            <Col md={2} className="d-flex align-items-end">
                                <Button variant="outline-secondary" onClick={clearFilters} className="w-100">
                                    <i className="bi bi-arrow-clockwise me-1"></i>
                                    Reset
                                </Button>
                            </Col>
                        </Row>
                    </Card.Body>
                </Card>

                {/* Error Alert */}
                {error && (
                    <Alert variant="danger" className="mb-4" dismissible onClose={() => setError('')}>
                        <Alert.Heading className="h6 mb-2">
                            <i className="bi bi-exclamation-triangle me-2"></i>
                            Error
                        </Alert.Heading>
                        {error}
                    </Alert>
                )}

                {/* Users Table */}
                <Card className="border-0 shadow-sm">
                    <Card.Header className="bg-white border-bottom">
                        <div className="d-flex justify-content-between align-items-center">
                            <div>
                                <h5 className="mb-0">Users</h5>
                                <small className="text-muted">{totalUsers} total users</small>
                            </div>
                            {totalPages > 0 && (
                                <small className="text-muted">
                                    Page {currentPage + 1} of {totalPages}
                                </small>
                            )}
                        </div>
                    </Card.Header>
                    <Card.Body className="p-0">
                        {loading ? (
                            <div className="text-center py-5">
                                <Spinner animation="border" variant="primary"/>
                                <div className="mt-2 text-muted">Loading users...</div>
                            </div>
                        ) : users.length === 0 ? (
                            <div className="text-center py-5">
                                <i className="bi bi-people display-4 text-muted mb-3"></i>
                                <h5 className="text-muted">No users found</h5>
                                <p className="text-muted">Try adjusting your search filters.</p>
                            </div>
                        ) : (
                            <div className="table-responsive">
                                <Table className="mb-0" hover>
                                    <thead style={{backgroundColor: '#f8f9fa'}}>
                                    <tr>
                                        <th className="border-0 py-3">User</th>
                                        <th className="border-0 py-3">Role</th>
                                        <th className="border-0 py-3">Status</th>
                                        <th className="border-0 py-3">Last Login</th>
                                        <th className="border-0 py-3">Created</th>
                                        <th className="border-0 py-3 text-center">Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {users.map((user) => (
                                        <tr key={user.id}>
                                            <td className="py-3">
                                                <div>
                                                    <div className="fw-semibold">{user.fullName}</div>
                                                    <div className="text-muted small">{user.email}</div>
                                                    {user.keycloakUserId && (
                                                        <span className="badge bg-info bg-opacity-10 text-info small">
                                                                <i className="bi bi-shield-check me-1"></i>
                                                                Keycloak
                                                            </span>
                                                    )}
                                                </div>
                                            </td>
                                            <td className="py-3">
                                                <Badge bg={getRoleBadgeVariant(user.role)}>
                                                    {user.role}
                                                </Badge>
                                            </td>
                                            <td className="py-3">
                                                <div>
                                                    <Badge bg={user.active ? 'success' : 'secondary'} className="mb-1">
                                                        {user.active ? 'Active' : 'Inactive'}
                                                    </Badge>
                                                    {user.failedLoginAttempts > 0 && (
                                                        <div>
                                                            <Badge bg="warning" className="small">
                                                                <i className="bi bi-exclamation-triangle me-1"></i>
                                                                {user.failedLoginAttempts} failed attempts
                                                            </Badge>
                                                        </div>
                                                    )}
                                                </div>
                                            </td>
                                            <td className="py-3">
                                                {user.lastSuccessfulLogin ? (
                                                    <small className="text-muted">
                                                        {formatDate(user.lastSuccessfulLogin)}
                                                    </small>
                                                ) : (
                                                    <small className="text-muted fst-italic">Never</small>
                                                )}
                                            </td>
                                            <td className="py-3">
                                                <small className="text-muted">
                                                    {formatDate(user.createdAt)}
                                                </small>
                                            </td>
                                            <td className="py-3 text-center">
                                                <Dropdown as={ButtonGroup}>
                                                    <OverlayTrigger
                                                        placement="top"
                                                        overlay={<Tooltip>Edit user profile</Tooltip>}
                                                    >
                                                        <Button
                                                            variant="outline-primary"
                                                            size="sm"
                                                            onClick={() => openEditModal(user)}
                                                        >
                                                            <i className="bi bi-pencil"></i>
                                                        </Button>
                                                    </OverlayTrigger>

                                                    <Dropdown.Toggle
                                                        split
                                                        variant="outline-primary"
                                                        size="sm"
                                                    />

                                                    <Dropdown.Menu>
                                                        <Dropdown.Item onClick={() => openRoleModal(user)}>
                                                            <i className="bi bi-person-badge me-2"></i>
                                                            Change Role
                                                        </Dropdown.Item>
                                                        <Dropdown.Divider/>
                                                        {user.active ? (
                                                            <Dropdown.Item
                                                                onClick={() => openDeactivateModal(user)}
                                                                className="text-warning"
                                                            >
                                                                <i className="bi bi-person-dash me-2"></i>
                                                                Deactivate
                                                            </Dropdown.Item>
                                                        ) : (
                                                            <Dropdown.Item
                                                                onClick={() => handleActivateUser(user)}
                                                                className="text-success"
                                                            >
                                                                <i className="bi bi-person-check me-2"></i>
                                                                Activate
                                                            </Dropdown.Item>
                                                        )}
                                                    </Dropdown.Menu>
                                                </Dropdown>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </Table>
                            </div>
                        )}
                    </Card.Body>

                    {/* Pagination */}
                    {totalPages > 1 && (
                        <Card.Footer className="bg-white border-top">
                            <div className="d-flex justify-content-between align-items-center">
                                <small className="text-muted">
                                    Showing {users.length} of {totalUsers} users
                                </small>
                                <Pagination className="mb-0">
                                    <Pagination.First
                                        disabled={currentPage === 0}
                                        onClick={() => setCurrentPage(0)}
                                    />
                                    <Pagination.Prev
                                        disabled={currentPage === 0}
                                        onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                                    />

                                    {[...Array(Math.min(5, totalPages))].map((_, index) => {
                                        const pageNum = Math.max(0, currentPage - 2) + index;
                                        if (pageNum >= totalPages) return null;
                                        return (
                                            <Pagination.Item
                                                key={pageNum}
                                                active={pageNum === currentPage}
                                                onClick={() => setCurrentPage(pageNum)}
                                            >
                                                {pageNum + 1}
                                            </Pagination.Item>
                                        );
                                    })}

                                    <Pagination.Next
                                        disabled={currentPage >= totalPages - 1}
                                        onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                                    />
                                    <Pagination.Last
                                        disabled={currentPage >= totalPages - 1}
                                        onClick={() => setCurrentPage(totalPages - 1)}
                                    />
                                </Pagination>
                            </div>
                        </Card.Footer>
                    )}
                </Card>

                {/* Create User Modal */}
                <Modal show={showCreateModal} onHide={() => setShowCreateModal(false)} size="lg">
                    <Modal.Header closeButton className="bg-primary text-white">
                        <Modal.Title>
                            <i className="bi bi-person-plus me-2"></i>
                            Create New User
                        </Modal.Title>
                    </Modal.Header>
                    <Form onSubmit={handleCreateUser}>
                        <Modal.Body className="p-4">
                            {error && (
                                <Alert variant="danger" className="mb-3">
                                    <i className="bi bi-exclamation-triangle me-2"></i>
                                    {error}
                                </Alert>
                            )}

                            <Row>
                                <Col md={8}>
                                    <Form.Group className="mb-3">
                                        <Form.Label className="fw-semibold">Email Address <span
                                            className="text-danger">*</span></Form.Label>
                                        <Form.Control
                                            type="email"
                                            value={createForm.email}
                                            onChange={(e) => setCreateForm(prev => ({...prev, email: e.target.value}))}
                                            isInvalid={!!fieldErrors.email}
                                            placeholder="user@example.com"
                                            required
                                        />
                                        <Form.Control.Feedback type="invalid">
                                            {fieldErrors.email}
                                        </Form.Control.Feedback>
                                    </Form.Group>
                                </Col>
                                <Col md={4}>
                                    <Form.Group className="mb-3">
                                        <Form.Label className="fw-semibold">Role <span className="text-danger">*</span></Form.Label>
                                        <Form.Select
                                            value={createForm.role}
                                            onChange={(e) => setCreateForm(prev => ({...prev, role: e.target.value}))}
                                            required
                                        >
                                            <option value="STUDENT">Student</option>
                                            <option value="PROFESSOR">Professor</option>
                                            <option value="ADMIN">Administrator</option>
                                        </Form.Select>
                                    </Form.Group>
                                </Col>
                            </Row>

                            <Row>
                                <Col md={4}>
                                    <Form.Group className="mb-3">
                                        <Form.Label className="fw-semibold">First Name <span
                                            className="text-danger">*</span></Form.Label>
                                        <Form.Control
                                            type="text"
                                            value={createForm.firstName}
                                            onChange={(e) => setCreateForm(prev => ({
                                                ...prev,
                                                firstName: e.target.value
                                            }))}
                                            isInvalid={!!fieldErrors.firstName}
                                            required
                                        />
                                        <Form.Control.Feedback type="invalid">
                                            {fieldErrors.firstName}
                                        </Form.Control.Feedback>
                                    </Form.Group>
                                </Col>
                                <Col md={4}>
                                    <Form.Group className="mb-3">
                                        <Form.Label className="fw-semibold">Middle Name</Form.Label>
                                        <Form.Control
                                            type="text"
                                            value={createForm.middleName}
                                            onChange={(e) => setCreateForm(prev => ({
                                                ...prev,
                                                middleName: e.target.value
                                            }))}
                                        />
                                    </Form.Group>
                                </Col>
                                <Col md={4}>
                                    <Form.Group className="mb-3">
                                        <Form.Label className="fw-semibold">Last Name <span
                                            className="text-danger">*</span></Form.Label>
                                        <Form.Control
                                            type="text"
                                            value={createForm.lastName}
                                            onChange={(e) => setCreateForm(prev => ({
                                                ...prev,
                                                lastName: e.target.value
                                            }))}
                                            isInvalid={!!fieldErrors.lastName}
                                            required
                                        />
                                        <Form.Control.Feedback type="invalid">
                                            {fieldErrors.lastName}
                                        </Form.Control.Feedback>
                                    </Form.Group>
                                </Col>
                            </Row>
                        </Modal.Body>
                        <Modal.Footer className="border-top">
                            <Button variant="outline-secondary" onClick={() => setShowCreateModal(false)}>
                                Cancel
                            </Button>
                            <Button variant="primary" type="submit">
                                <i className="bi bi-check-lg me-2"></i>
                                Create User
                            </Button>
                        </Modal.Footer>
                    </Form>
                </Modal>

                {/* Edit User Modal */}
                <Modal show={showEditModal} onHide={() => setShowEditModal(false)} size="lg">
                    <Modal.Header closeButton className="bg-info text-white">
                        <Modal.Title>
                            <i className="bi bi-pencil me-2"></i>
                            Edit User Profile
                        </Modal.Title>
                    </Modal.Header>
                    <Form onSubmit={handleEditUser}>
                        <Modal.Body className="p-4">
                            {error && (
                                <Alert variant="danger" className="mb-3">
                                    <i className="bi bi-exclamation-triangle me-2"></i>
                                    {error}
                                </Alert>
                            )}

                            {selectedUser && (
                                <>
                                    <div className="bg-light rounded p-3 mb-4">
                                        <strong>Editing:</strong> {selectedUser.email}
                                    </div>

                                    <Row>
                                        <Col md={4}>
                                            <Form.Group className="mb-3">
                                                <Form.Label className="fw-semibold">First Name <span
                                                    className="text-danger">*</span></Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    value={editForm.firstName}
                                                    onChange={(e) => setEditForm(prev => ({
                                                        ...prev,
                                                        firstName: e.target.value
                                                    }))}
                                                    isInvalid={!!fieldErrors.firstName}
                                                    required
                                                />
                                                <Form.Control.Feedback type="invalid">
                                                    {fieldErrors.firstName}
                                                </Form.Control.Feedback>
                                            </Form.Group>
                                        </Col>
                                        <Col md={4}>
                                            <Form.Group className="mb-3">
                                                <Form.Label className="fw-semibold">Middle Name</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    value={editForm.middleName}
                                                    onChange={(e) => setEditForm(prev => ({
                                                        ...prev,
                                                        middleName: e.target.value
                                                    }))}
                                                />
                                            </Form.Group>
                                        </Col>
                                        <Col md={4}>
                                            <Form.Group className="mb-3">
                                                <Form.Label className="fw-semibold">Last Name <span
                                                    className="text-danger">*</span></Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    value={editForm.lastName}
                                                    onChange={(e) => setEditForm(prev => ({
                                                        ...prev,
                                                        lastName: e.target.value
                                                    }))}
                                                    isInvalid={!!fieldErrors.lastName}
                                                    required
                                                />
                                                <Form.Control.Feedback type="invalid">
                                                    {fieldErrors.lastName}
                                                </Form.Control.Feedback>
                                            </Form.Group>
                                        </Col>
                                    </Row>
                                </>
                            )}
                        </Modal.Body>
                        <Modal.Footer className="border-top">
                            <Button variant="outline-secondary" onClick={() => setShowEditModal(false)}>
                                Cancel
                            </Button>
                            <Button variant="info" type="submit">
                                <i className="bi bi-check-lg me-2"></i>
                                Update Profile
                            </Button>
                        </Modal.Footer>
                    </Form>
                </Modal>

                {/* Change Role Modal */}
                <Modal show={showRoleModal} onHide={() => setShowRoleModal(false)}>
                    <Modal.Header closeButton className="bg-warning text-dark">
                        <Modal.Title>
                            <i className="bi bi-person-badge me-2"></i>
                            Change User Role
                        </Modal.Title>
                    </Modal.Header>
                    <Form onSubmit={handleChangeRole}>
                        <Modal.Body className="p-4">
                            {error && (
                                <Alert variant="danger" className="mb-3">
                                    <i className="bi bi-exclamation-triangle me-2"></i>
                                    {error}
                                </Alert>
                            )}

                            {selectedUser && (
                                <>
                                    <div className="bg-light rounded p-3 mb-4">
                                        <strong>User:</strong> {selectedUser.fullName} ({selectedUser.email})
                                    </div>

                                    <Row>
                                        <Col md={6}>
                                            <Form.Group className="mb-3">
                                                <Form.Label className="fw-semibold">Current Role</Form.Label>
                                                <Form.Control type="text" value={selectedUser.role} disabled/>
                                            </Form.Group>
                                        </Col>
                                        <Col md={6}>
                                            <Form.Group className="mb-3">
                                                <Form.Label className="fw-semibold">New Role <span
                                                    className="text-danger">*</span></Form.Label>
                                                <Form.Select
                                                    value={roleForm.newRole}
                                                    onChange={(e) => setRoleForm(prev => ({
                                                        ...prev,
                                                        newRole: e.target.value
                                                    }))}
                                                    required
                                                >
                                                    <option value="STUDENT">Student</option>
                                                    <option value="PROFESSOR">Professor</option>
                                                    <option value="ADMIN">Administrator</option>
                                                </Form.Select>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    <Form.Group className="mb-3">
                                        <Form.Label className="fw-semibold">Reason for Change</Form.Label>
                                        <Form.Control
                                            as="textarea"
                                            rows={3}
                                            value={roleForm.reason}
                                            onChange={(e) => setRoleForm(prev => ({...prev, reason: e.target.value}))}
                                            placeholder="Enter reason for role change..."
                                        />
                                    </Form.Group>
                                </>
                            )}
                        </Modal.Body>
                        <Modal.Footer className="border-top">
                            <Button variant="outline-secondary" onClick={() => setShowRoleModal(false)}>
                                Cancel
                            </Button>
                            <Button
                                variant="warning"
                                type="submit"
                                disabled={roleForm.newRole === selectedUser?.role}
                            >
                                <i className="bi bi-arrow-repeat me-2"></i>
                                Change Role
                            </Button>
                        </Modal.Footer>
                    </Form>
                </Modal>

                {/* Deactivate User Modal */}
                <Modal show={showDeactivateModal} onHide={() => setShowDeactivateModal(false)}>
                    <Modal.Header closeButton className="bg-danger text-white">
                        <Modal.Title>
                            <i className="bi bi-exclamation-triangle me-2"></i>
                            Deactivate User
                        </Modal.Title>
                    </Modal.Header>
                    <Form onSubmit={handleDeactivateUser}>
                        <Modal.Body className="p-4">
                            {error && (
                                <Alert variant="danger" className="mb-3">
                                    <i className="bi bi-exclamation-triangle me-2"></i>
                                    {error}
                                </Alert>
                            )}

                            {selectedUser && (
                                <>
                                    <Alert variant="warning" className="mb-3">
                                        <Alert.Heading className="h6">
                                            <i className="bi bi-exclamation-triangle me-2"></i>
                                            Warning
                                        </Alert.Heading>
                                        You are about to deactivate
                                        user <strong>{selectedUser.fullName}</strong> ({selectedUser.email}).
                                        This will prevent them from logging into the system.
                                    </Alert>

                                    <Form.Group className="mb-3">
                                        <Form.Label className="fw-semibold">Reason for Deactivation <span
                                            className="text-danger">*</span></Form.Label>
                                        <Form.Control
                                            as="textarea"
                                            rows={3}
                                            value={deactivateForm.reason}
                                            onChange={(e) => setDeactivateForm(prev => ({
                                                ...prev,
                                                reason: e.target.value
                                            }))}
                                            placeholder="Enter reason for deactivation..."
                                            isInvalid={!!fieldErrors.reason}
                                            required
                                        />
                                        <Form.Control.Feedback type="invalid">
                                            {fieldErrors.reason}
                                        </Form.Control.Feedback>
                                    </Form.Group>
                                </>
                            )}
                        </Modal.Body>
                        <Modal.Footer className="border-top">
                            <Button variant="outline-secondary" onClick={() => setShowDeactivateModal(false)}>
                                Cancel
                            </Button>
                            <Button variant="danger" type="submit">
                                <i className="bi bi-person-x me-2"></i>
                                Deactivate User
                            </Button>
                        </Modal.Footer>
                    </Form>
                </Modal>

                {/* Toast Notifications */}
                <ToastContainer position="top-end" className="p-3" style={{zIndex: 9999}}>
                    {toasts.map((toast) => (
                        <Toast
                            key={toast.id}
                            onClose={() => setToasts(prev => prev.filter(t => t.id !== toast.id))}
                            show={true}
                            delay={4000}
                            autohide
                            className={`border-0 shadow-sm`}
                        >
                            <Toast.Header
                                className={toast.type === 'success' ? 'bg-success text-white' : 'bg-danger text-white'}>
                                <i className={`bi ${toast.type === 'success' ? 'bi-check-circle-fill' : 'bi-exclamation-circle-fill'} me-2`}></i>
                                <strong className="me-auto">
                                    {toast.type === 'success' ? 'Success' : 'Error'}
                                </strong>
                            </Toast.Header>
                            <Toast.Body>
                                {toast.message}
                            </Toast.Body>
                        </Toast>
                    ))}
                </ToastContainer>
            </Container>
        </div>
    );
};

export default UserManagement;