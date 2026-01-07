import { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import '../App.css';

const LoginPage = () => {
  const navigate = useNavigate();
  
  
  const [isRegistering, setIsRegistering] = useState(false);
  
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(""); 

    const endpoint = isRegistering ? '/register' : '/login';
    const API_BASE = import.meta.env.VITE_API_BASE_URL || ""; 
    const url = `${API_BASE}/api/auth${endpoint}`;

    try {
      const response = await axios.post(url, { username, password });
      
      
      const user = response.data;
      localStorage.setItem('userId', user.id);
      localStorage.setItem('username', user.username);

      navigate('/feed');

    } catch (err: any) {
      console.error("Auth failed:", err);
      if (err.response && err.response.status === 401) {
        setError("Invalid username or password.");
      } else if (err.response && err.response.status === 400) {
        setError("Username already taken.");
      } else {
        setError("Something went wrong. Is the backend running?");
      }
    }
  };

  return (
    <div className="home-centered">
      <div className="auth-card" style={{ width: '400px', textAlign: 'left' }}>
        <h2 style={{ textAlign: 'center', fontSize: '2rem' }}>
          {isRegistering ? "Create Account" : "Welcome Back"}
        </h2>

        {error && (
          <div style={{ color: '#f43f5e', marginBottom: '15px', textAlign: 'center' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleAuth}>
          <input 
            type="text" 
            placeholder="Username" 
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
          <input 
            type="password" 
            placeholder="Password" 
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          
          <button type="submit" className="action-btn">
            {isRegistering ? "Sign Up" : "Log In"}
          </button>
        </form>

        <p style={{ marginTop: '20px', textAlign: 'center', color: '#94a3b8' }}>
          {isRegistering ? "Already have an account?" : "New to UW Food Ratings?"}
          <button 
            onClick={() => setIsRegistering(!isRegistering)}
            style={{ 
                background: 'none', 
                border: 'none', 
                color: '#38bdf8', 
                fontWeight: 'bold', 
                cursor: 'pointer',
                marginLeft: '8px',
                fontSize: '1rem',
                fontFamily: 'inherit'
            }}
          >
             {isRegistering ? "Log In" : "Register"}
          </button>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;