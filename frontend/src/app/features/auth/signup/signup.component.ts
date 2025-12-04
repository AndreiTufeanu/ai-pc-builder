import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { SignupRequest } from '../../../core/models/user.model';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent {
  signupData: SignupRequest = {
    username: '',
    password: '',
    confirmPassword: ''
  };
  
  isLoading = false;
  errorMessage = '';

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  onSignup(): void {
    if (!this.signupData.username || !this.signupData.password || !this.signupData.confirmPassword) {
      this.errorMessage = 'Please fill in all fields';
      return;
    }

    if (this.signupData.username.length < 3) {
      this.errorMessage = 'Username must be at least 3 characters long';
      return;
    }

    if (this.signupData.password.length < 3) {
      this.errorMessage = 'Password must be at least 3 characters long';
      return;
    }

    if (this.signupData.password !== this.signupData.confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.signup(this.signupData).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.success) {
          this.router.navigate(['/login'], { 
            queryParams: { message: 'Account created successfully! Please login.' }
          });
        } else {
          this.errorMessage = response.message;
        }
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Signup failed. Please try again.';
        console.error('Signup error:', error);
      }
    });
  }

  navigateToLogin(): void {
    this.router.navigate(['/login']);
  }

  navigateToHome(): void {
    this.router.navigate(['/']);
  }
}