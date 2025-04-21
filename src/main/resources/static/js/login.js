// login.js
document.addEventListener('DOMContentLoaded', function () {
    const loginForm = document.getElementById('loginForm'); // Add id="loginForm" to your form in login.html
    const errorMessage = document.getElementById('errorMessage'); // Add id="errorMessage" div in login.html if needed
    const successMessage = document.getElementById('successMessage');
    const urlParams = new URLSearchParams(window.location.search);
    const isRegistered = urlParams.get('registered') === 'true';
    const API_BASE_URL = 'http://localhost:8080/api'; // Make sure this matches your backend

    // Show success message if redirected from registration
    if (isRegistered && successMessage) {
        successMessage.style.display = 'block';
        setTimeout(() => {
            if (successMessage) {
                successMessage.style.opacity = '0';
                setTimeout(() => { successMessage.style.display = 'none'; }, 500);
            }
        }, 3000);
    }

    if (loginForm) {
        loginForm.addEventListener('submit', async function (event) {
            event.preventDefault(); // Prevent default form submission

            if (errorMessage) errorMessage.style.display = 'none'; // Hide previous errors

            const usernameInput = loginForm.querySelector('input[name="username"]');
            const passwordInput = loginForm.querySelector('input[name="password"]');
            const submitButton = loginForm.querySelector('button[type="submit"]');

            const username = usernameInput.value.trim();
            const password = passwordInput.value.trim();

            if (!username || !password) {
                if (errorMessage) {
                    errorMessage.textContent = 'Please enter username and password.';
                    errorMessage.style.display = 'block';
                }
                return;
            }

            submitButton.disabled = true;
            submitButton.textContent = 'Logging in...';

            try {
                const response = await fetch(`${API_BASE_URL}/auth/login`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ username: username, password: password }),
                });

                if (response.ok) {
                    const data = await response.json(); // Expecting { "token": "..." }
                    if (data.token) {
                        // --- Store the token ---
                        localStorage.setItem('jwtToken', data.token); // Use localStorage
                        // sessionStorage.setItem('jwtToken', data.token); // Or sessionStorage

                        console.log('Login successful, token stored.');
                        // --- Redirect to the main chat page ---
                        window.location.href = '/index.html'; // Redirect to your main chat page
                    } else {
                        throw new Error("Token not found in login response");
                    }
                } else {
                    // Handle login errors (e.g., 401 Unauthorized)
                    let errorMsg = `Login failed: ${response.statusText}`;
                    try {
                        const errorData = await response.json();
                        errorMsg = errorData.message || errorData.error || errorMsg;
                    } catch(e) { /* Ignore if response body is not JSON */ }

                    if (errorMessage) {
                        errorMessage.textContent = errorMsg;
                        errorMessage.style.display = 'block';
                    }
                    console.error('Login failed:', errorMsg);
                }
            } catch (error) {
                console.error('Login request error:', error);
                if (errorMessage) {
                    errorMessage.textContent = 'An error occurred during login. Please try again.';
                    errorMessage.style.display = 'block';
                }
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = 'Login';
            }
        });
    } else {
        console.error("Login form not found");
    }
});