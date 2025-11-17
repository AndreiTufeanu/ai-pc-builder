import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface User {
  id: number; // Now we'll get this from backend
  username: string;
  roles: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  userId: number; // Add this
  username: string;
  roles: string[];
  message: string;
}

export interface SignupRequest {
  username: string;
  password: string;
  confirmPassword: string;
}

export interface SignupResponse {
  success: boolean;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth';
  
  currentUser = signal<User | null>(null);
  isAuthenticated = signal(false);

  constructor(private http: HttpClient, private router: Router) {
    this.initializeAuth();
  }

  private initializeAuth(): void {
    const savedUser = localStorage.getItem('currentUser');
    if (savedUser) {
      const user = JSON.parse(savedUser);
      this.currentUser.set(user);
      this.isAuthenticated.set(true);
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, credentials)
      .pipe(
        tap(response => {
          if (response.username && response.roles && response.userId) {
            const user: User = {
              id: response.userId, // Now we get this from backend
              username: response.username,
              roles: response.roles
            };
            
            this.currentUser.set(user);
            this.isAuthenticated.set(true);
            localStorage.setItem('currentUser', JSON.stringify(user));
            
            console.log('User logged in with ID:', response.userId);
          }
        })
      );
  }

  signup(signupData: SignupRequest): Observable<SignupResponse> {
    return this.http.post<SignupResponse>(`${this.baseUrl}/signup`, signupData);
  }

  logout(): void {
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    localStorage.removeItem('currentUser');
    this.router.navigate(['/']);
  }

  isAdmin(): boolean {
    const user = this.currentUser();
    return user ? user.roles.includes('ROLE_ADMIN') : false;
  }

  isUser(): boolean {
    const user = this.currentUser();
    return user ? user.roles.includes('ROLE_USER') : false;
  }

  // Helper method to get current user ID
  getCurrentUserId(): number | null {
    const user = this.currentUser();
    return user ? user.id : null;
  }
}