import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ApartmentList from './ApartmentList';
import ApartmentDetail from './ApartmentDetail';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ApartmentList />} />
        <Route path="/apartments/:id" element={<ApartmentDetail />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
