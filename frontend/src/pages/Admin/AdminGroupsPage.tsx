import { Box } from '@mui/material';
import Groups from '../../components/Groups';

export const AdminGroupsPage = ({ user }: { user: any }) => {
  return (
    <Box>
      <Groups user={user} />
    </Box>
  );
};
