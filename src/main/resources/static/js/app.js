document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('login-form');
    const userNameInput = document.getElementById('userName');
    const passwordInput = document.getElementById('password');
    const togglePasswordBtn = document.getElementById('toggle-password');
    const submitBtn = document.getElementById('submit-btn');
    const btnText = submitBtn.querySelector('.btn-text');
    const loader = submitBtn.querySelector('.loader');
    const feedbackMessage = document.getElementById('feedback-message');
    
    const otpForm = document.getElementById('otp-form');
    const otpInput = document.getElementById('otp');
    const verifyBtn = document.getElementById('verify-btn');
    const otpFeedbackMessage = document.getElementById('otp-feedback-message');
    const backToLoginBtn = document.getElementById('back-to-login');
    
    let currentPreCt = '';
    let currentCookie = '';

    // Generate or retrieve device ID
    let deviceId = localStorage.getItem('mca_device_id');
    if (!deviceId) {
        // Generate a random UUID
        deviceId = crypto.randomUUID ? crypto.randomUUID() : 'dev-' + Math.random().toString(36).substring(2, 15);
        localStorage.setItem('mca_device_id', deviceId);
    }

    // Toggle password visibility
    togglePasswordBtn.addEventListener('click', () => {
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        passwordInput.setAttribute('type', type);
        
        // Toggle icon
        if (type === 'text') {
            togglePasswordBtn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-eye-off"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>';
        } else {
            togglePasswordBtn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-eye"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>';
        }
    });

    function showFeedback(message, type, targetElement = feedbackMessage) {
        targetElement.textContent = message;
        targetElement.className = `feedback-message ${type}`;
        targetElement.style.display = 'block';
    }

    function hideFeedback(targetElement = feedbackMessage) {
        targetElement.style.display = 'none';
        targetElement.className = 'feedback-message';
    }

    function setLoading(isLoading, btn = submitBtn, text = btnText, load = loader) {
        if (isLoading) {
            btn.disabled = true;
            text.style.display = 'none';
            load.style.display = 'block';
        } else {
            btn.disabled = false;
            text.style.display = 'block';
            load.style.display = 'none';
        }
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideFeedback();
        setLoading(true);

        const payload = {
            userName: userNameInput.value.trim(),
            password: passwordInput.value,
            deviceId: deviceId,
            captcha: "",
            preCt: "",
            cookie: ""
        };

        try {
            const response = await fetch('/roclogin', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            const data = await response.json();

            if (response.ok) {
                if (data.message === 'Otp Required' || (data.data && data.data.message === 'Otp Required')) {
                    // Switch to OTP form
                    form.style.display = 'none';
                    otpForm.style.display = 'block';
                    currentCookie = data.data ? data.data.cookie : currentCookie; // Update cookie if provided
                    showFeedback('OTP sent to your email/mobile.', 'success', otpFeedbackMessage);
                } else {
                    // Success
                    showFeedback(data.message || 'Login successful!', 'success');
                    // window.location.href = '/dashboard';
                }
            } else {
                // Error from server
                showFeedback(data.message || 'Login failed. Please check your credentials.', 'error');
            }
        } catch (error) {
            console.error('Login error:', error);
            showFeedback('A network error occurred. Please try again later.', 'error');
        } finally {
            setLoading(false, submitBtn, btnText, loader);
        }
    });

    const resendOtpBtn = document.getElementById('resend-otp');
    let resendCooldown = 0;
    let resendTimer = null;

    function startResendCooldown() {
        resendCooldown = 30;
        resendOtpBtn.disabled = true;
        resendOtpBtn.style.opacity = '0.5';
        resendOtpBtn.style.cursor = 'not-allowed';
        resendOtpBtn.textContent = `Resend OTP (${resendCooldown}s)`;
        resendTimer = setInterval(() => {
            resendCooldown--;
            if (resendCooldown <= 0) {
                clearInterval(resendTimer);
                resendOtpBtn.disabled = false;
                resendOtpBtn.style.opacity = '1';
                resendOtpBtn.style.cursor = 'pointer';
                resendOtpBtn.textContent = 'Resend OTP';
            } else {
                resendOtpBtn.textContent = `Resend OTP (${resendCooldown}s)`;
            }
        }, 1000);
    }

    resendOtpBtn.addEventListener('click', async () => {
        if (resendCooldown > 0) return;

        hideFeedback(otpFeedbackMessage);
        resendOtpBtn.textContent = 'Sending...';
        resendOtpBtn.disabled = true;

        try {
            const response = await fetch('/resendotp', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: userNameInput.value.trim(),
                    cookie: currentCookie
                })
            });

            const data = await response.json();

            if (response.ok) {
                showFeedback(data.message || 'OTP resent successfully!', 'success', otpFeedbackMessage);
            } else {
                showFeedback(data.message || 'Failed to resend OTP.', 'error', otpFeedbackMessage);
            }
        } catch (error) {
            console.error('Resend OTP error:', error);
            showFeedback('A network error occurred while resending OTP.', 'error', otpFeedbackMessage);
        } finally {
            startResendCooldown();
        }
    });

    backToLoginBtn.addEventListener('click', () => {
        otpForm.style.display = 'none';
        form.style.display = 'block';
        hideFeedback();
        hideFeedback(otpFeedbackMessage);
        if (resendTimer) {
            clearInterval(resendTimer);
            resendCooldown = 0;
            resendOtpBtn.disabled = false;
            resendOtpBtn.style.opacity = '1';
            resendOtpBtn.style.cursor = 'pointer';
            resendOtpBtn.textContent = 'Resend OTP';
        }
    });

    otpForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideFeedback(otpFeedbackMessage);
        
        const verifyBtnText = verifyBtn.querySelector('.btn-text');
        const verifyLoader = verifyBtn.querySelector('.loader');
        setLoading(true, verifyBtn, verifyBtnText, verifyLoader);

        const payload = {
            email: userNameInput.value.trim(),
            password: passwordInput.value,
            deviceId: deviceId,
            otp: otpInput.value.trim(),
            cookie: currentCookie
        };

        try {
            const response = await fetch('/verifyotpp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            const data = await response.json();

            if (response.ok) {
                showFeedback('OTP Verified! Transitioning to MCA Home Page...', 'success', otpFeedbackMessage);
                // Attempt to close the tab entirely. If the browser blocks it, show a message.
                setTimeout(() => {
                    document.body.innerHTML = '<div style="display:flex; flex-direction:column; justify-content:center; align-items:center; height:100vh; color:white; font-family:sans-serif; text-align:center;"><h2>Successfully logged into MCA.</h2><p>Your session has opened in a new Chrome window.</p><p>You can safely close this tab.</p></div>';
                    window.close();
                }, 1000);
            } else {
                showFeedback(data.message || 'OTP Verification failed.', 'error', otpFeedbackMessage);
            }
        } catch (error) {
            console.error('OTP error:', error);
            showFeedback('A network error occurred during OTP verification.', 'error', otpFeedbackMessage);
        } finally {
            setLoading(false, verifyBtn, verifyBtnText, verifyLoader);
        }
    });
});
