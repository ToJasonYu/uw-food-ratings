export interface Review {
  id: number;
  userId: number;
  restaurantName: string;
  dishName: string;
  stars: number;
  comment: string;
  upvotes: number;
  timestamp: string;
}