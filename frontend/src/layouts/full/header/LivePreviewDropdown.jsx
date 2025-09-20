import * as React from 'react';
import { styled, alpha } from '@mui/material/styles';
import Button from '@mui/material/Button';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import { IconDeviceLaptop } from '@tabler/icons-react';
import { Box } from '@mui/material';

const StyledMenu = styled((props) => (
  <Menu
    elevation={0}
    anchorOrigin={{
      vertical: 'bottom',
      horizontal: 'right',
    }}
    transformOrigin={{
      vertical: 'top',
      horizontal: 'right',
    }}
    {...props}
  />
))(({ theme }) => ({
  '& .MuiPaper-root': {
    borderRadius: 7,
    marginTop: theme.spacing(1),
    minWidth: 200,
    color: '#000c29',
    boxShadow:
      'rgb(255, 255, 255) 0px 0px 0px 0px, rgba(0, 0, 0, 0.05) 0px 0px 0px 1px, rgba(0, 0, 0, 0.1) 0px 10px 15px -3px, rgba(0, 0, 0, 0.05) 0px 4px 6px -2px',
    '& .MuiMenu-list': {
      padding: '16px',
    },
    '& .MuiMenuItem-root': {
      '& .MuiSvgIcon-root': {
        fontSize: 18,
        color: theme.palette.text.secondary,
        marginRight: theme.spacing(1.5),
      },
      '&:active': {
        backgroundColor: alpha(
          theme.palette.primary.main,
          theme.palette.action.selectedOpacity,
        ),
      },
    },
    ...theme.applyStyles('dark', {
      color: theme.palette.grey[300],
    }),
  },
}));

// Styled button with outline style
const StyledButton = styled(Button)(() => ({
  border: `1px solid rgba(255,255,255,.4)`,
  fontSize: '16px',
  color: '#ffffff',
  padding: '5px 16px',
  textTransform: 'none',
  display: 'flex',
  borderRadius: '4px',
  justifyContent: 'space-between',
  alignItems: 'center',
  '&:hover': {
    backgroundColor: '#5d87ff',
  },
  '& .MuiButton-endIcon': {
    marginLeft: '4px',
  },
}));

const MenuItems = [
  {
    id: 1,
    title: "React Version",
    href: "https://adminmart.com/product/modernize-react-mui-dashboard-theme/?ref=56#product-demo-section",
    color: "#61DAFB"
  },
  {
    id: 2,
    title: "Angular Version", 
    href: "https://adminmart.com/product/modernize-angular-material-dashboard/?ref=56#product-demo-section",
    color: "#DD0031"
  },
  {
    id: 3,
    title: "VueJs Version",
    href: "https://adminmart.com/product/modernize-vuetify-vue-admin-dashboard/?ref=56#product-demo-section",
    color: "#4FC08D"
  },
  {
    id: 4,
    title: "NextJs Version",
    href: "https://adminmart.com/product/modernize-next-js-admin-dashboard/?ref=56#product-demo-section",
    color: "#000000"
  },
  {
    id: 5,
    title: "Bootstrap Version",
    href: "https://adminmart.com/product/modernize-bootstrap-5-admin-template/?ref=56#product-demo-section",
    color: "#7952B3"
  },
  {
    id: 6,
    title: "NuxtJs Version",
    href: "https://adminmart.com/product/modernize-nuxt-js-admin-dashboard/?ref=56#product-demo-section",
    color: "#00DC82"
  },
  {
    id: 7,
    title: "Tailwind Version",
    href: "https://adminmart.com/product/modernize-tailwind-nextjs-dashboard-template/?ref=56#product-demo-section",
    color: "#06B6D4"
  },
]

export default function LivePreviewDropdown() {
  const [anchorEl, setAnchorEl] = React.useState(null);
  const open = Boolean(anchorEl);
  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
  };

  return (
    <div>
      <StyledButton
        id="demo-customized-button"
        aria-controls={open ? 'demo-customized-menu' : undefined} 
        aria-haspopup="true"
        aria-expanded={open ? 'true' : undefined}
        variant="outlined"
        disableElevation
        onClick={handleClick}
        endIcon={<KeyboardArrowDownIcon />}
        startIcon={<IconDeviceLaptop size={18} />}
      >
        Live Preview
      </StyledButton>
      <StyledMenu
        id="demo-customized-menu"
        MenuListProps={{
          'aria-labelledby': 'demo-customized-button', 
        }}
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
      >
        {MenuItems.map((item, index) => (
          <a target='_blank' key={index} href={item.href} rel="noopener noreferrer">
            <MenuItem 
              sx={{ 
                gap: '12px', 
                borderRadius: '7px', 
                fontSize: '16px', 
                color: '#000c29', 
                padding: '12px 18px', 
                ":hover": { backgroundColor: "#000c290d" } 
              }} 
              onClick={handleClose} 
              disableRipple
            >
              <Box
                sx={{
                  width: 18,
                  height: 18,
                  borderRadius: '50%',
                  backgroundColor: item.color,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  fontSize: '10px',
                  fontWeight: 'bold'
                }}
              >
                {item.title.charAt(0)}
              </Box>
              {item.title}
            </MenuItem>
          </a>
        ))}
      </StyledMenu>
    </div>
  );
}
