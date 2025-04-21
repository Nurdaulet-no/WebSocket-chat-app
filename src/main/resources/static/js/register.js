document.addEventListener('DOMContentLoaded', function() {
    const registerForm = document.getElementById('registerForm');
    const errorMessage = document.getElementById('errorMessage');
    const API_BASE_URL = 'http://localhost:8080/api'; // Match your backend

    if (registerForm) {
        registerForm.addEventListener('submit', async function (event) {
            event.preventDefault(); // Prevent default form submission

            errorMessage.style.display = 'none'; // Hide previous errors

            // Get form inputs
            const usernameInput = registerForm.querySelector('input[name="username"]');
            const firstNameInput = registerForm.querySelector('input[name="firstName"]');
            const emailInput = registerForm.querySelector('input[name="email"]');
            const passwordInput = registerForm.querySelector('input[name="password"]');
            const confirmPasswordInput = registerForm.querySelector('input[name="confirmPassword"]');
            const submitButton = registerForm.querySelector('button[type="submit"]');

            const password = passwordInput.value;
            const confirmPassword = confirmPasswordInput.value;

            // Basic validation
            if (password !== confirmPassword) {
                errorMessage.textContent = 'Passwords do not match!';
                errorMessage.style.display = 'block';
                return;
            }
            if (password.length < 8 || password.length > 25) {
                errorMessage.textContent = 'Password must be between 8 and 25 characters long.';
                errorMessage.style.display = 'block';
                return;
            }

            const userData = {
                username: usernameInput.value.trim(),
                userFirstName: firstNameInput.value.trim(),
                email: emailInput.value.trim(),
                password: password // Send the plain password
            };

            submitButton.disabled = true;
            submitButton.textContent = 'Registering...';

            try {
                const response = await fetch(`${API_BASE_URL}/auth/register`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(userData),
                });

                if (response.ok) { // Check for 200 OK or 201 Created
                    console.log('Registration successful!');
                    // Redirect to login page with success indicator
                    window.location.href = '/login.html?registered=true';
                } else {
                    // Handle registration errors (e.g., 409 Conflict)
                    let errorMsg = `Registration failed: ${response.statusText}`;
                    try {
                        const errorData = await response.json(); // Try to get error details from backend DTO
                        errorMsg = errorData.message || errorData.error || errorMsg;
                    } catch(e) { /* Ignore if response body is not JSON */ }

                    errorMessage.textContent = errorMsg;
                    errorMessage.style.display = 'block';
                    console.error('Registration failed:', errorMsg);
                }

            } catch (error) {
                console.error('Registration request error:', error);
                errorMessage.textContent = 'An error occurred during registration. Please try again.';
                errorMessage.style.display = 'block';
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = 'Register';
            }
        });
    } else {
        console.error("Register form not found");
    }
});