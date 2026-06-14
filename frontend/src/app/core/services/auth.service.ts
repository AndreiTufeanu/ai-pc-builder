import { Injectable, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { User, LoginRequest, LoginResponse, SignupRequest, SignupResponse } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = `${environment.apiUrl}/api/auth`;
  
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
              id: response.userId,
              username: response.username,
              roles: response.roles
            };
            
            this.currentUser.set(user);
            this.isAuthenticated.set(true);
            localStorage.setItem('currentUser', JSON.stringify(user));
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

  getCurrentUserId(): number | null {
    const user = this.currentUser();
    return user ? user.id : null;
  }
}