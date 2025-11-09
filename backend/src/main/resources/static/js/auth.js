// Common authentication functions

function checkAuthentication() {
    const currentUser = localStorage.getItem('currentUser');
    if (currentUser) {
        const user = JSON.parse(currentUser);
        if (user.roles && user.roles.includes('ROLE_ADMIN')) {
            window.location.href = 'admin-dashboard.html';
        } else {
            window.location.href = 'user-dashboard.html';
        }
    }
}

function handleLogin(event) {
    event.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    fetch('/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password })
    })
        .then(response => response.json())
        .then(data => {
            if (data.message && data.message !== "Login successful") {
                // Show error message
                alert(data.message || 'Login failed');
                return;
            }

            if (data.username) {
                localStorage.setItem('currentUser', JSON.stringify(data));
                if (data.roles && data.roles.includes('ROLE_ADMIN')) {
                    window.location.href = 'admin-dashboard.html';
                } else {
                    window.location.href = 'user-dashboard.html';
                }
            } else {
                alert('Login failed. Please check your credentials.');
            }
        })
        .catch(error => {
            alert('Login failed. Please try again.');
            console.error('Error:', error);
        });
}

function handleSignup(event) {
    event.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    fetch('/api/auth/signup', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password, confirmPassword })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Account created successfully! Please login.');
                window.location.href = 'login.html';
            } else {
                alert(data.message || 'Signup failed. Please try again.');
            }
        })
        .catch(error => {
            alert('Signup failed. Please try again.');
            console.error('Error:', error);
        });
}

function logout() {
    localStorage.removeItem('currentUser');
    window.location.href = 'index.html';
}