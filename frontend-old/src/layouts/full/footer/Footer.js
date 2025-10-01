'use client';
import React from "react";
import { Box, Typography } from "@mui/material";
import { Link } from "react-router-dom";

const Footer = () => {
    return (
        <Box sx={{ pt: 6, pb: 3, textAlign: "center" }}>
            <Typography>
                © {new Date().getFullYear()} All rights reserved by{" "}
                <Link to="https://www.adminmart.com">
                    <Typography color='primary.main' component='span'>
                        AdminMart.com</Typography>
                </Link>{" "}
            </Typography>
        </Box>
    );
};

export default Footer;
