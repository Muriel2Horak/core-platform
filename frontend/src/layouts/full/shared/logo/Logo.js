import { Link } from "react-router";
import { Box, Typography } from "@mui/material";
import { styled } from "@mui/material";
import { IconShield } from "@tabler/icons-react";

const LinkStyled = styled(Link)(() => ({
  height: "70px",
  width: "180px",
  overflow: "hidden",
  display: "block",
}));

const Logo = () => {
  return (
    <LinkStyled
      to="/"
      height={70}
      style={{
        display: "flex",
        alignItems: "center",
        textDecoration: "none"
      }}
    >
      <Box display="flex" alignItems="center" gap={1}>
        <IconShield 
          size={32} 
          style={{ 
            color: '#667eea',
            strokeWidth: 2.5
          }} 
        />
        <Typography 
          variant="h5" 
          sx={{ 
            fontWeight: 'bold',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            fontSize: '1.4rem'
          }}
        >
          CORE Platform
        </Typography>
      </Box>
    </LinkStyled>
  );
};

export default Logo;
