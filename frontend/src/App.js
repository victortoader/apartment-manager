import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { AuthProvider, useAuth } from './AuthContext';
import ApartmentList from './ApartmentList';
import ApartmentDetail from './ApartmentDetail';
import ManagePhotos from './ManagePhotos';
import UserManagement from './UserManagement';
import Tickets from './Tickets';
import Presentation from './Presentation';
import PresentationList from './PresentationList';
import ApartmentApplications from './ApartmentApplications';
import AuditLog from './AuditLog';
import InspectionForm from './InspectionForm';
import Login from './Login';
import './App.css';

function ProtectedRoute({ children }) {
  const { t } = useTranslation();
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <div className="app"><p>{t('loading')}</p></div>;
  return isAuthenticated ? children : <Navigate to="/login" />;
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/presentations" element={<PresentationList />} />
          <Route path="/presentations/apartments/:id" element={<Presentation />} />
          <Route path="/" element={<ProtectedRoute><ApartmentList /></ProtectedRoute>} />
          <Route path="/apartments/:id" element={<ProtectedRoute><ApartmentDetail /></ProtectedRoute>} />
          <Route path="/apartments/:id/photos" element={<ProtectedRoute><ManagePhotos /></ProtectedRoute>} />
          <Route path="/users" element={<ProtectedRoute><UserManagement /></ProtectedRoute>} />
          <Route path="/tickets" element={<ProtectedRoute><Tickets /></ProtectedRoute>} />
          <Route path="/audit" element={<ProtectedRoute><AuditLog /></ProtectedRoute>} />
          <Route path="/applications/:id" element={<ProtectedRoute><ApartmentApplications /></ProtectedRoute>} />
          <Route path="/apartments/:id/inspections/new" element={<ProtectedRoute><InspectionForm /></ProtectedRoute>} />
          <Route path="/apartments/:id/inspections/:inspectionId" element={<ProtectedRoute><InspectionForm /></ProtectedRoute>} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
