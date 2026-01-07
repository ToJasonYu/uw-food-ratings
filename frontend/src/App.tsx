import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import HomePage from './components/HomePage';
import FeedPage from './components/FeedPage';
import LoginPage from './components/LoginPage';
import './App.css';

function App() {
  return (
    <Router>
      <div className="app-container">
        
        <Routes>
          <Route path="/" element={<HomePage />} />
          
          <Route path="/login" element={<LoginPage />} />
          
          <Route path="/feed" element={<FeedPage />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;