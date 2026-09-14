import { BrowserRouter } from "react-router-dom";
import Layout from "./components/Layout";
import AnimatedRoutes from "./components/AnimatedRoutes";
import ErrorBoundary from "./components/ErrorBoundary";
import { AuthProvider } from "./context/AuthContext";
import { NotificationProvider } from "./context/NotificationContext";
import { ToastProvider } from './components/ToastSystem';
import { ConfirmProvider } from './components/ConfirmDialog';

// Main App Component
export default function App() {
    return (
        <ErrorBoundary>
            <ToastProvider>
                <AuthProvider>
                    <NotificationProvider>
                        <ConfirmProvider>
                            <BrowserRouter>
                                <Layout>
                                    <AnimatedRoutes />
                                </Layout>
                            </BrowserRouter>
                        </ConfirmProvider>
                    </NotificationProvider>
                </AuthProvider>
            </ToastProvider>
        </ErrorBoundary>
    );
}
