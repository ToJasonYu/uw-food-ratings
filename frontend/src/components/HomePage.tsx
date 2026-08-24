import { useNavigate } from 'react-router-dom';
import '../App.css';

const HomePage = () => {
  const navigate = useNavigate();

  return (
    <div className="home-centered">
      <h1 style={{ fontSize: '2.5rem', marginBottom: '10px' }}>UW Food Ratings</h1>
      <p style={{ fontSize: '1.1rem', color: '#8b9198', marginBottom: '32px' }}>
        Restaurant and dish reviews from UW students.
      </p>

      <button
        className="action-btn"
        style={{
          padding: '14px 36px',
          fontSize: '1rem',
          width: 'auto',
          maxWidth: '220px'
        }}
        onClick={() => navigate('/login')}
      >
        Enter the Feed
      </button>
    </div>
  );
};

export default HomePage;