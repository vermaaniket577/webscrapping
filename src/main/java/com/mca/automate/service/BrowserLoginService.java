package com.mca.automate.service;

import com.mca.automate.dto.BrowserLoginResponse;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

@Service
@Slf4j
public class BrowserLoginService {

    @Value("${browser-login.target-url:https://eportal.incometax.gov.in/iec/foservices/#/login}")
    private String targetUrl;

    @Value("${browser-login.dashboard-url-contains:dashboard}")
    private String dashboardUrlContains;

    @Value("${browser-login.headless:false}")
    private boolean headless;

    @Value("${browser-login.timeout-seconds:30}")
    private int timeoutSeconds;

    // ── CSS Selectors (configurable via application.yml) ──
    @Value("${browser-login.selectors.pan-input:#panAdhaarUserId}")
    private String panInputSelector;

    @Value("${browser-login.selectors.continue-button://button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'continue')]}")
    private String continueButtonSelector;

    @Value("${browser-login.selectors.password-input:#loginPasswordField}")
    private String passwordInputSelector;

    @Value("${browser-login.selectors.confirm-checkbox:#passwordCheckBox-input}")
    private String confirmCheckboxSelector;

    @Value("${browser-login.selectors.continue-button-2://button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'continue')]}")
    private String continueButton2Selector;

    @Value("${browser-login.selectors.password-page-url-contains:password}")
    private String passwordPageUrlContains;

    // ── Per-PAN locking to prevent concurrent login races ──
    private static final int LOCK_COUNT = 32;
    private static final Object[] LOCKS = createLocks();

    @PostConstruct
    public void init() {
        log.info("Setting up ChromeDriver via WebDriverManager...");
        WebDriverManager.chromedriver().setup();
        log.info("✓ ChromeDriver setup complete");
    }

    /**
     * Execute the full browser-based login flow.
     * Synchronized per PAN to prevent concurrent login attempts for the same user.
     */
    public BrowserLoginResponse login(String pan, String password) {
        synchronized (lockFor(pan)) {
            return doLogin(pan, password, targetUrl);
        }
    }

    private BrowserLoginResponse doLogin(String pan, String password, String loginUrl) {
        WebDriver driver = null;
        try {
            // ── Step 1: Initialize ChromeDriver ──
            log.info("Starting browser login for PAN: {}***", pan.substring(0, Math.min(4, pan.length())));
            driver = createDriver();
            
            // Akamai Anti-Bot evasion via CDP
            if (driver instanceof org.openqa.selenium.chrome.ChromeDriver) {
                ((org.openqa.selenium.chrome.ChromeDriver) driver).executeCdpCommand(
                    "Page.addScriptToEvaluateOnNewDocument",
                    java.util.Map.of("source", 
                        "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});" +
                        "window.chrome = { runtime: {} };" +
                        "Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});" +
                        "Object.defineProperty(navigator, 'languages', {get: () => ['en-US', 'en']});"
                    )
                );
            }

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));

            // ── Step 2: Open Login Page ──
            log.info("Navigating to login page: {}", loginUrl);
            driver.get(loginUrl);
            log.info("✓ Login page opened. Current URL: {}", driver.getCurrentUrl());

            // ── Step 3: Enter PAN ──
            log.info("Waiting for PAN input field: {}", panInputSelector);
            WebElement panInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(getBy(panInputSelector))
            );
            scrollToElement(driver, panInput);
            panInput.clear();
            // Type slowly to mimic human behavior
            for (char c : pan.toCharArray()) {
                panInput.sendKeys(String.valueOf(c));
                Thread.sleep(50 + (long)(Math.random() * 100));
            }
            log.info("✓ PAN entered");

            // ── Step 4: Click Continue ──
            Thread.sleep(500);
            log.info("Clicking Continue button: {}", continueButtonSelector);
            WebElement continueBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(getBy(continueButtonSelector))
            );
            scrollToElement(driver, continueBtn);
            continueBtn.click();
            log.info("✓ Continue button clicked");

            // ── Step 5: Wait until Password page loads ──
            log.info("Waiting for password page (URL contains: '{}')", passwordPageUrlContains);
            wait.until(ExpectedConditions.urlContains(passwordPageUrlContains));
            log.info("✓ Password page loaded. Current URL: {}", driver.getCurrentUrl());

            // ── Step 6: Verify URL ──
            String currentUrl = driver.getCurrentUrl();
            if (!currentUrl.contains(passwordPageUrlContains)) {
                log.error("URL verification failed. Expected URL containing '{}', got: {}", passwordPageUrlContains, currentUrl);
                return new BrowserLoginResponse("", "URL verification failed - not on password page", false);
            }
            log.info("✓ URL verified: {}", currentUrl);

            // Wait for password page to fully render
            Thread.sleep(1500);

            // ── Step 7: Click Confirmation Checkbox ──
            log.info("Clicking confirmation checkbox: {}", confirmCheckboxSelector);
            WebElement checkbox = wait.until(
                    ExpectedConditions.presenceOfElementLocated(getBy(confirmCheckboxSelector))
            );
            scrollToElement(driver, checkbox);
            if (!checkbox.isSelected()) {
                try {
                    Thread.sleep(500);
                    // Try to use Actions to simulate a real human mouse click on the label
                    String id = checkbox.getAttribute("id");
                    if (id != null && !id.isEmpty()) {
                        WebElement label = driver.findElement(By.cssSelector("label[for='" + id + "']"));
                        new org.openqa.selenium.interactions.Actions(driver).moveToElement(label).click().perform();
                    } else {
                        new org.openqa.selenium.interactions.Actions(driver).moveToElement(checkbox).click().perform();
                    }
                    Thread.sleep(500);
                    // Fallback to JS if still not checked
                    if (!checkbox.isSelected()) {
                        log.info("Actions click failed to check box, falling back to JS forced check");
                        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                            "arguments[0].checked = true;" +
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", 
                            checkbox
                        );
                    }
                } catch (Exception e) {
                    log.warn("Failed to check confirmation checkbox: {}", e.getMessage());
                }
            }
            log.info("✓ Confirmation checkbox checked");

            // ── Step 8: Enter Password ──
            Thread.sleep(500);
            log.info("Entering password...");
            WebElement passwordInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(getBy(passwordInputSelector))
            );
            passwordInput.clear();
            // Type password slowly to mimic human behavior
            for (char c : password.toCharArray()) {
                passwordInput.sendKeys(String.valueOf(c));
                Thread.sleep(30 + (long)(Math.random() * 70));
            }
            
            // Force Angular change detection
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));", 
                passwordInput
            );
            log.info("✓ Password entered and Angular events dispatched");

            // Verify password field actually has content before continuing
            String passwordValue = passwordInput.getAttribute("value");
            if (passwordValue == null || passwordValue.isEmpty()) {
                log.error("Password field is empty after entering password! Retrying with JS...");
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "var el = arguments[0];" +
                    "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                    "nativeInputValueSetter.call(el, arguments[1]);" +
                    "el.dispatchEvent(new Event('input', { bubbles: true }));" +
                    "el.dispatchEvent(new Event('change', { bubbles: true }));",
                    passwordInput, password
                );
                Thread.sleep(300);
            }

            // ── Step 9: Click Continue (second time) ──
            Thread.sleep(500);
            log.info("Clicking Continue button (second): {}", continueButton2Selector);
            WebElement continueBtn2 = wait.until(
                    ExpectedConditions.elementToBeClickable(getBy(continueButton2Selector))
            );
            scrollToElement(driver, continueBtn2);
            try {
                continueBtn2.click();
            } catch (Exception e) {
                log.info("Standard click on Continue intercepted, using Actions click");
                try {
                    new org.openqa.selenium.interactions.Actions(driver).moveToElement(continueBtn2).click().perform();
                } catch (Exception ex) {
                    log.info("Actions click failed, using Javascript click");
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", continueBtn2);
                }
            }
            log.info("✓ Second Continue button clicked");

            // ── Step 10: Check for login errors before waiting for dashboard ──
            // Wait a moment for the API response to come back
            Thread.sleep(3000);

            // Check for error toast messages (the portal uses ngx-toastr or similar)
            String errorMessage = detectLoginError(driver);
            if (errorMessage != null) {
                log.error("Login error detected: {}", errorMessage);
                return new BrowserLoginResponse("", "Login failed - " + errorMessage, false);
            }

            // Check if the form was reset (password field cleared = server rejected credentials)
            try {
                WebElement pwdField = driver.findElement(getBy(passwordInputSelector));
                String pwdValue = pwdField.getAttribute("value");
                boolean pwdEmpty = (pwdValue == null || pwdValue.isEmpty());
                String pwdClasses = pwdField.getAttribute("class");
                boolean isPristine = (pwdClasses != null && pwdClasses.contains("ng-pristine"));
                
                if (pwdEmpty && isPristine && driver.getCurrentUrl().contains(passwordPageUrlContains)) {
                    log.error("Form was reset after submission - credentials rejected by server. " +
                              "Password field is empty and pristine. Current URL: {}", driver.getCurrentUrl());
                    return new BrowserLoginResponse("", 
                        "Login failed - credentials rejected. The portal reset the form (invalid password or account issue). " +
                        "Please verify your PAN and password are correct.", false);
                }
            } catch (org.openqa.selenium.NoSuchElementException ignored) {
                // Password field not found - page may have navigated away (good sign)
                log.debug("Password field not found - page may have navigated successfully");
            }

            // ── Step 11: Wait for Dashboard ──
            log.info("Waiting for dashboard (URL contains: '{}'). Current URL: {}", 
                     dashboardUrlContains, driver.getCurrentUrl());
            
            // Use a longer wait for dashboard since the login API + redirect takes time
            WebDriverWait dashboardWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            dashboardWait.until(ExpectedConditions.urlContains(dashboardUrlContains));
            log.info("✓ Dashboard loaded. Current URL: {}", driver.getCurrentUrl());

            // ── Step 12: Save Cookies ──
            String cookies = extractCookies(driver);
            log.info("✓ Cookies saved ({} characters)", cookies.length());

            // ── Step 13: Return Success ──
            log.info("✓ Browser login successful for PAN: {}***", pan.substring(0, Math.min(4, pan.length())));
            return new BrowserLoginResponse(cookies, "Login Successful", true);

        } catch (org.openqa.selenium.TimeoutException e) {
            String errUrl = driver != null ? driver.getCurrentUrl() : "unknown";
            log.error("Browser login timed out. Current URL: {}. Error: {}", errUrl, e.getMessage());
            
            // Try to capture any error message that appeared
            if (driver != null) {
                String errorMsg = detectLoginError(driver);
                if (errorMsg != null) {
                    log.error("Error message found on page: {}", errorMsg);
                    return new BrowserLoginResponse("", "Login failed - " + errorMsg, false);
                }
                
                // Log the page title for debugging
                try {
                    log.error("Page title at timeout: {}", driver.getTitle());
                } catch (Exception ignored) {}
            }
            
            return new BrowserLoginResponse("", "Login timed out - " + e.getMessage(), false);

        } catch (Exception e) {
            log.error("Browser login failed with exception: {}", e.getMessage(), e);
            return new BrowserLoginResponse("", "Login failed - " + e.getMessage(), false);

        } finally {
            // Always close the browser
            if (driver != null) {
                try {
                    driver.quit();
                    log.info("Browser closed");
                } catch (Exception e) {
                    log.warn("Failed to close browser: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Detect login error messages on the page (toast notifications, inline errors, modal popups).
     * Returns the error message if found, null otherwise.
     */
    private String detectLoginError(WebDriver driver) {
        try {
            // 1. Check for toast/snackbar notifications (common Angular pattern)
            String[] toastSelectors = {
                ".toast-message", ".ngx-toastr", ".toast-error",
                "snack-bar-container", "mat-snack-bar-container",
                ".cdk-overlay-container .mat-snack-bar-container",
                ".p-toast-message", ".p-toast-detail"
            };
            for (String selector : toastSelectors) {
                List<WebElement> toasts = driver.findElements(By.cssSelector(selector));
                for (WebElement toast : toasts) {
                    if (toast.isDisplayed()) {
                        String text = toast.getText().trim();
                        if (!text.isEmpty()) {
                            return text;
                        }
                    }
                }
            }

            // 2. Check for inline validation errors
            String[] errorSelectors = {
                "mat-error", ".error-message", ".err-msg", ".errorMessage",
                "[role='alert']", ".alert-danger", ".alert-error",
                ".invalid-feedback"
            };
            for (String selector : errorSelectors) {
                List<WebElement> errors = driver.findElements(By.cssSelector(selector));
                for (WebElement error : errors) {
                    if (error.isDisplayed()) {
                        String text = error.getText().trim();
                        if (!text.isEmpty()) {
                            return text;
                        }
                    }
                }
            }

            // 3. Check for modal popups (attempts warning, account locked, etc.)
            String[] modalSelectors = {
                "#attemptsWarningPopup", "#maxAttemptsPopup",
                ".modal.show", ".modal.in",
                "mat-dialog-container", "[role='dialog']"
            };
            for (String selector : modalSelectors) {
                List<WebElement> modals = driver.findElements(By.cssSelector(selector));
                for (WebElement modal : modals) {
                    String ariaHidden = modal.getAttribute("aria-hidden");
                    boolean isVisible = modal.isDisplayed() || 
                                       (ariaHidden != null && "false".equals(ariaHidden)) ||
                                       modal.getAttribute("class").contains("show");
                    if (isVisible) {
                        String text = modal.getText().trim();
                        if (!text.isEmpty()) {
                            return "Portal message: " + text;
                        }
                    }
                }
            }

            // 4. Use JavaScript to search for any visible error text
            String jsErrorText = (String) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);" +
                "var node;" +
                "while (node = walker.nextNode()) {" +
                "  var parent = node.parentElement;" +
                "  if (parent && parent.offsetWidth > 0 && parent.offsetHeight > 0) {" +
                "    var text = node.textContent.trim().toLowerCase();" +
                "    if (text.includes('invalid') || text.includes('incorrect') || " +
                "        text.includes('wrong password') || text.includes('account locked') || " +
                "        text.includes('too many attempts') || text.includes('login failed') || " +
                "        text.includes('authentication failed')) {" +
                "      return node.textContent.trim();" +
                "    }" +
                "  }" +
                "}" +
                "return null;"
            );
            if (jsErrorText != null && !jsErrorText.isEmpty()) {
                return jsErrorText;
            }

        } catch (Exception e) {
            log.debug("Error while detecting login errors: {}", e.getMessage());
        }
        return null;
    }

    private By getBy(String selector) {
        if (selector.startsWith("//") || selector.startsWith("(//")) {
            return By.xpath(selector);
        }
        return By.cssSelector(selector);
    }

    private void scrollToElement(WebDriver driver, WebElement element) {
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
            Thread.sleep(200); // Give it a moment to scroll
        } catch (Exception e) {
            log.debug("Failed to scroll to element: {}", e.getMessage());
        }
    }

    /**
     * Create and configure a ChromeDriver instance.
     */
    private WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1920,1080");
        
        // Anti-Bot Evasion for Akamai / WAF (Income Tax portal requires this)
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", java.util.Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        // Match the actual Chrome version to avoid user-agent fingerprint mismatch
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36");

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments(
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-extensions",
                "--disable-popup-blocking"
        );

        return new ChromeDriver(options);
    }

    /**
     * Extract all cookies from the browser session as a semicolon-separated string.
     */
    private String extractCookies(WebDriver driver) {
        Set<Cookie> seleniumCookies = driver.manage().getCookies();
        StringJoiner joiner = new StringJoiner("; ");
        for (Cookie cookie : seleniumCookies) {
            joiner.add(cookie.getName() + "=" + cookie.getValue());
            log.debug("  Cookie: {}={}", cookie.getName(),
                    cookie.getValue().length() > 20
                            ? cookie.getValue().substring(0, 20) + "..."
                            : cookie.getValue());
        }
        return joiner.toString();
    }

    public BrowserLoginResponse loginWithPanAndUrl(String url, String pan, String password) {
        synchronized (lockFor(pan)) {
            return doLogin(pan, password, url);
        }
    }

    // ── Locking infrastructure (same pattern as McaLoginService) ──

    private static Object[] createLocks() {
        Object[] locks = new Object[LOCK_COUNT];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    private Object lockFor(String pan) {
        String key = pan == null ? "" : pan.trim().toUpperCase();
        return LOCKS[Math.floorMod(key.hashCode(), LOCKS.length)];
    }
}
