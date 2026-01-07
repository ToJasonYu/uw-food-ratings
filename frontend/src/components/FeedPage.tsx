import { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import type { Review } from '../types';
import '../App.css';

const FeedPage = () => {
  const navigate = useNavigate();
  const [reviews, setReviews] = useState<Review[]>([]);
  
  const API_BASE = import.meta.env.VITE_API_BASE_URL || "";

  const currentUserId = Number(localStorage.getItem('userId'));
  const currentUsername = localStorage.getItem('username');

  const [searchTerm, setSearchTerm] = useState("");
  const [sortBy, setSortBy] = useState("newest");

  const [restaurantName, setRestaurantName] = useState("");
  const [dishName, setDishName] = useState("");
  const [stars, setStars] = useState(5);
  const [comment, setComment] = useState("");

  useEffect(() => {
    if (!currentUserId) {
      navigate('/login');
      return;
    }
    fetchReviews();
  }, [navigate, currentUserId]);

  const fetchReviews = () => {
    axios.get(`${API_BASE}/api/ratings`)
      .then(response => {
        setReviews(response.data);
      })
      .catch(error => console.error("Error fetching data:", error));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault(); 
    const newReviewRaw = { 
      userId: currentUserId,
      restaurantName, 
      dishName, 
      stars, 
      comment,
      upvotes: 0
    };

    axios.post(`${API_BASE}/api/ratings`, newReviewRaw)
      .then(() => {
        fetchReviews();
        setRestaurantName("");
        setDishName("");
        setComment("");
        setStars(5);
      })
      .catch(error => console.error("Error adding review:", error));
  };

  const handleUpvote = (id: number) => {
    axios.put(`${API_BASE}/api/ratings/${id}/upvote?userId=${currentUserId}`)
      .then(response => {
        const updatedReview = response.data;
        setReviews(reviews.map(r => r.id === id ? updatedReview : r));
      })
      .catch(error => {
        if (error.response && error.response.status === 400) {
            alert("⚠️ You already voted for this!");
        }
      });
  };

  const handleDelete = (id: number) => {
    if (!window.confirm("Are you sure you want to delete this post?")) return;

    axios.delete(`${API_BASE}/api/ratings/${id}?userId=${currentUserId}`)
      .then(() => {
        setReviews(reviews.filter(r => r.id !== id));
      })
      .catch(error => {
        if (error.response && error.response.status === 403) {
           alert("❌ You can only delete your own posts!");
        }
      });
  };

  const filteredReviews = reviews
    .filter((review) => {
      const searchLower = searchTerm.toLowerCase();
      return (
        review.restaurantName.toLowerCase().includes(searchLower) ||
        review.dishName.toLowerCase().includes(searchLower)
      );
    })
    .sort((a, b) => {
      if (sortBy === "highest") return b.stars - a.stars;
      if (sortBy === "lowest") return a.stars - b.stars;
      return new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime();
    });

  return (
    <div className="feed-container">
      
      <div className="header-row">
        <h1>UW Food Ratings</h1>
        
        <div className="user-section">
            <span className="user-info">
              Logged in as: 
              <span className="username-highlight">{currentUsername}</span>
            </span>
            <button 
                onClick={() => {
                    localStorage.clear();
                    navigate('/login');
                }}
                className="logout-button"
            >
                Log Out
            </button>
        </div>
      </div>
      
      <div className="create-post-card">
        <h3>Rate something yummy (or gross)</h3>
        <form onSubmit={handleSubmit}>
          <input 
            type="text" 
            placeholder="Restaurant Name" 
            value={restaurantName}
            onChange={(e) => setRestaurantName(e.target.value)}
            required 
          />
          <input 
            type="text" 
            placeholder="Dish Name" 
            value={dishName}
            onChange={(e) => setDishName(e.target.value)}
            required 
          />
          <select value={stars} onChange={(e) => setStars(Number(e.target.value))}>
             {[1,2,3,4,5].map(num => <option key={num} value={num}>{num} ⭐</option>)}
          </select>
          <textarea 
            placeholder="How was it? Spicy? Salty? Life-changing?" 
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            required
            rows={3}
          />
          <button type="submit" className="action-btn">Post Review</button>
        </form>
      </div>

      <div className="search-container">
        <input 
          type="text" 
          placeholder="🔍 Find a craving..." 
          className="search-input"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
        <select 
          className="sort-select"
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value)}
        >
          <option value="newest">Newest First</option>
          <option value="highest">Highest Rated</option>
          <option value="lowest">Lowest Rated</option>
        </select>
      </div>

      <div className="feed-list">
        {filteredReviews.length === 0 ? (
            <div style={{ textAlign: 'center', color: '#64748b', marginTop: '40px' }}>
                <p style={{ fontSize: '1.2rem' }}>No reviews found for "{searchTerm}"</p>
                <p style={{ fontSize: '3rem', marginTop: '10px' }}>🍽️</p>
            </div>
        ) : (
            filteredReviews.map((review) => (
            <div key={review.id} className="review-card">
                
                <div className="like-column">
                  <button onClick={() => handleUpvote(review.id)} className="like-button">👍</button>
                  <span className="like-count">{review.upvotes}</span>
                </div>

                <div className="content-column">
                  <div className="card-header">
                      <div style={{ display: 'flex', alignItems: 'baseline' }}>
                        <span className="restaurant-name">{review.restaurantName}</span>
                        <span className="posted-date">
                          {new Date(review.timestamp).toLocaleDateString(undefined, {
                              month: 'short', day: 'numeric'
                          })}
                        </span>
                      </div>
                      
                      {(review.userId === currentUserId || currentUserId === 1) && (
                          <button onClick={() => handleDelete(review.id)} className="delete-icon" title="Delete Post">
                              🗑️
                          </button>
                      )}
                  </div>
                  
                  <div className="post-title">
                    {review.dishName} 
                    <span className="star-badge">
                      {review.stars} ★
                    </span>
                  </div>
                  <p className="post-body">{review.comment}</p>
                </div>
            </div>
            ))
        )}
      </div>
    </div>
  );
};

export default FeedPage;