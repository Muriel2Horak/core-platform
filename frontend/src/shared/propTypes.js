/**
 * 🎯 Shared PropTypes definitions
 * 
 * Centrální definice PropTypes pro opakovaně používané objekty
 */
import PropTypes from 'prop-types';

// User object PropTypes
export const UserPropType = PropTypes.shape({
  id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  username: PropTypes.string,
  email: PropTypes.string,
  firstName: PropTypes.string,
  lastName: PropTypes.string,
  roles: PropTypes.arrayOf(PropTypes.string),
  tenant: PropTypes.string,
  tenantKey: PropTypes.string,
  profilePicture: PropTypes.string,
  department: PropTypes.string,
  position: PropTypes.string,
  manager: PropTypes.string,
  costCenter: PropTypes.string,
  phone: PropTypes.string,
  location: PropTypes.string,
  enabled: PropTypes.bool,
  isFederated: PropTypes.bool,
  directorySource: PropTypes.string,
  createdAt: PropTypes.string,
  lastLogin: PropTypes.string,
});

// Task object PropTypes (for Kanban)
export const TaskPropType = PropTypes.shape({
  id: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  description: PropTypes.string,
  assignee: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.shape({
      name: PropTypes.string,
      avatar: PropTypes.string,
    })
  ]),
  priority: PropTypes.oneOf(['low', 'medium', 'high']),
  tags: PropTypes.arrayOf(PropTypes.string),
  columnId: PropTypes.string.isRequired,
  dueDate: PropTypes.instanceOf(Date),
});

// Column object PropTypes (for Kanban)
export const ColumnPropType = PropTypes.shape({
  id: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  color: PropTypes.string,
  tasks: PropTypes.arrayOf(TaskPropType),
});

export default {
  UserPropType,
  TaskPropType,
  ColumnPropType,
};