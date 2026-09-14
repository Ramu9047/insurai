import { Routes, Route, useLocation } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";

import Home from "../pages/Home";
import Login from "../pages/Login";
import Register from "../pages/Register";
import Dashboard from "../pages/Dashboard";
import ChooseAgent from "../pages/ChooseAgent";
import ScheduleAppointment from "../pages/ScheduleAppointment";
import AgentRequests from "../pages/AgentRequests";
import PlansEnhanced from "../pages/PlansEnhanced";
import MyClaims from "../pages/MyClaims";
import MyBookings from "../pages/MyBookings";
import MyPolicies from "../pages/MyPolicies";
import UserAgentProfile from "../pages/UserAgentProfile";
import AgentConsultations from "../pages/AgentConsultations";
import AgentPerformance from "../pages/AgentPerformance";
import AdminAnalytics from "../pages/AdminAnalytics";
import AgentGovernance from "../pages/AgentGovernance";
import ExceptionHandling from "../pages/ExceptionHandling";
import AdminUsers from '../pages/AdminUsers';
import AdminPolicies from '../pages/AdminPolicies';
import AdminPlans from '../pages/AdminPlans';
import Notifications from '../pages/Notifications';
import AgentAppointmentsEnhanced from '../pages/AgentAppointmentsEnhanced';
import MyAppointmentsEnhanced from '../pages/MyAppointmentsEnhanced';
import CompanyDashboard from '../pages/CompanyDashboard';
import SuperAdminDashboard from '../pages/SuperAdminDashboard';
import UserFeedbackPage from '../pages/UserFeedbackPage';
import AdminFeedbackDashboard from "../pages/AdminFeedbackDashboard";
import CompanyAgentReviews from "../pages/CompanyAgentReviews";
import ForgotPassword from "../pages/ForgotPassword";
import ResetPassword from "../pages/ResetPassword";
import RequireAuth from "./RequireAuth";
import PolicyWorkflowPage from '../pages/PolicyWorkflowPage';
import AdminProfile from '../pages/AdminProfile';

const pageVariants = {
    initial: { opacity: 0, y: 15 },
    in: { opacity: 1, y: 0 },
    out: { opacity: 0, y: -15 }
};

const pageTransition = {
    type: "tween",
    ease: "circOut",
    duration: 0.3
};

const PageWrapper = ({ children }) => (
    <motion.div
        initial="initial"
        animate="in"
        exit="out"
        variants={pageVariants}
        transition={pageTransition}
        style={{ width: "100%", height: "100%", position: "relative" }}
    >
        {children}
    </motion.div>
);

export default function AnimatedRoutes() {
    const location = useLocation();

    return (
        <AnimatePresence mode="wait">
            <Routes location={location} key={location.pathname}>
                {/* Public Routes */}
                <Route path="/" element={<PageWrapper><Home /></PageWrapper>} />
                <Route path="/login" element={<PageWrapper><Login /></PageWrapper>} />
                <Route path="/register" element={<PageWrapper><Register /></PageWrapper>} />
                <Route path="/forgot-password" element={<PageWrapper><ForgotPassword /></PageWrapper>} />
                <Route path="/reset-password" element={<PageWrapper><ResetPassword /></PageWrapper>} />

                {/* Protected Routes (Any Authenticated User) */}
                <Route element={<RequireAuth allowedRoles={['USER', 'AGENT', 'SUPER_ADMIN', 'COMPANY', 'COMPANY_ADMIN']} />}>
                    <Route path="/dashboard" element={<PageWrapper><Dashboard /></PageWrapper>} />
                    <Route path="/notifications" element={<PageWrapper><Notifications /></PageWrapper>} />
                    <Route path="/plans" element={<PageWrapper><PlansEnhanced /></PageWrapper>} />
                    <Route path="/plans-enhanced" element={<PageWrapper><PlansEnhanced /></PageWrapper>} />
                </Route>

                {/* User Routes */}
                <Route element={<RequireAuth allowedRoles={['USER', 'AGENT']} />}>
                    <Route path="/choose-agent" element={<PageWrapper><ChooseAgent /></PageWrapper>} />
                    <Route path="/schedule-appointment" element={<PageWrapper><ScheduleAppointment /></PageWrapper>} />
                    <Route path="/my-bookings" element={<PageWrapper><MyBookings /></PageWrapper>} />
                    <Route path="/my-policies" element={<PageWrapper><MyPolicies /></PageWrapper>} />
                    <Route path="/claims" element={<PageWrapper><MyClaims /></PageWrapper>} />
                    <Route path="/profile" element={<PageWrapper><UserAgentProfile /></PageWrapper>} />
                    <Route path="/feedback" element={<PageWrapper><UserFeedbackPage /></PageWrapper>} />
                </Route>

                {/* Agent Routes */}
                <Route element={<RequireAuth allowedRoles={['AGENT']} />}>
                    <Route path="/agent/requests" element={<PageWrapper><AgentRequests /></PageWrapper>} />
                    <Route path="/agent/consultations" element={<PageWrapper><AgentConsultations /></PageWrapper>} />
                    <Route path="/agent/performance" element={<PageWrapper><AgentPerformance /></PageWrapper>} />
                    <Route path="/agent/appointments-enhanced" element={<PageWrapper><AgentAppointmentsEnhanced /></PageWrapper>} />
                </Route>

                {/* User Enhanced Routes */}
                <Route element={<RequireAuth allowedRoles={['USER']} />}>
                    <Route path="/my-appointments-enhanced" element={<PageWrapper><MyAppointmentsEnhanced /></PageWrapper>} />
                </Route>

                {/* Shared Admin Routes */}
                <Route element={<RequireAuth allowedRoles={['SUPER_ADMIN', 'COMPANY', 'COMPANY_ADMIN']} />}>
                    <Route path="/analytics" element={<PageWrapper><AdminAnalytics /></PageWrapper>} />
                    <Route path="/agents-list" element={<PageWrapper><AgentGovernance /></PageWrapper>} />
                    <Route path="/exceptions" element={<PageWrapper><ExceptionHandling /></PageWrapper>} />
                    <Route path="/users" element={<PageWrapper><AdminUsers /></PageWrapper>} />
                    <Route path="/issued-policies" element={<PageWrapper><AdminPolicies /></PageWrapper>} />
                    <Route path="/plans-admin" element={<PageWrapper><AdminPlans /></PageWrapper>} />
                    <Route path="/my-consultations" element={<PageWrapper><PolicyWorkflowPage /></PageWrapper>} />
                    <Route path="/admin-profile" element={<PageWrapper><AdminProfile /></PageWrapper>} />
                </Route>

                {/* Company Routes */}
                <Route element={<RequireAuth allowedRoles={['COMPANY', 'COMPANY_ADMIN']} />}>
                    <Route path="/company" element={<PageWrapper><CompanyDashboard /></PageWrapper>} />
                    <Route path="/company-agent-reviews" element={<PageWrapper><CompanyAgentReviews /></PageWrapper>} />
                </Route>

                {/* Super Admin Routes */}
                <Route element={<RequireAuth allowedRoles={['SUPER_ADMIN']} />}>
                    <Route path="/super-admin" element={<PageWrapper><SuperAdminDashboard /></PageWrapper>} />
                    <Route path="/feedback-list" element={<PageWrapper><AdminFeedbackDashboard /></PageWrapper>} />
                </Route>
            </Routes>
        </AnimatePresence>
    );
}
