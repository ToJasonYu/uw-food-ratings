import { useNavigate } from 'react-router-dom';
import '../App.css';

const HomePage = () => {
  const navigate = useNavigate();

  return (
    <div className="home-centered">
      <h1 style={{ fontSize: '3rem', marginBottom: '10px' }}>🍔 UW Food Ratings</h1>
      <p style={{ fontSize: '1.2rem', color: '#b3b3b3', marginBottom: '40px' }}>
        The front page of campus dining.
      </p>
      
      <button 
        style={{ 
          padding: '15px 40px', 
          fontSize: '1.2rem', 
          cursor: 'pointer',
          maxWidth: '200px'
        }}
        onClick={() => navigate('/login')}
      >
        Enter the Feed
      </button>
    </div>
  );
};

export default HomePage;