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
import java.util.Set;
import java.util.StringJoiner;

@Service
@Slf4j
public class BrowserLoginService {

    @Value("${browser-login.target-url:https://example.com/login}")
    private String targetUrl;

    @Value("${browser-login.dashboard-url-contains:dashboard}")
    private String dashboardUrlContains;

    @Value("${browser-login.headless:true}")
    private boolean headless;

    @Value("${browser-login.timeout-seconds:15}")
    private int timeoutSeconds;

    // ── CSS Selectors (configurable via application.yml) ──
    @Value("${browser-login.selectors.pan-input:#pan}")
    private String panInputSelector;

    @Value("${browser-login.selectors.continue-button:#continue}")
    private String continueButtonSelector;

    @Value("${browser-login.selectors.password-input:#password}")
    private String passwordInputSelector;

    @Value("${browser-login.selectors.confirm-checkbox:#confirm}")
    private String confirmCheckboxSelector;

    @Value("${browser-login.selectors.continue-button-2:#continue}")
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
            return doLogin(pan, password);
        }
    }

    private BrowserLoginResponse doLogin(String pan, String password) {
        WebDriver driver = null;
        try {
            // ── Step 1: Initialize ChromeDriver ──
            log.info("Starting browser login for PAN: {}***", pan.substring(0, Math.min(4, pan.length())));
            driver = createDriver();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

            // ── Step 2: Open Login Page ──
            log.info("Navigating to login page: {}", targetUrl);
            driver.get(targetUrl);
            log.info("✓ Login page opened. Current URL: {}", driver.getCurrentUrl());

            // ── Step 3: Enter PAN ──
            log.info("Waiting for PAN input field: {}", panInputSelector);
            WebElement panInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(panInputSelector))
            );
            panInput.clear();
            panInput.sendKeys(pan);
            log.info("✓ PAN entered");

            // ── Step 4: Click Continue ──
            log.info("Clicking Continue button: {}", continueButtonSelector);
            WebElement continueBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector(continueButtonSelector))
            );
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

            // ── Step 7: Click Confirmation Checkbox ──
            log.info("Clicking confirmation checkbox: {}", confirmCheckboxSelector);
            WebElement checkbox = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector(confirmCheckboxSelector))
            );
            if (!checkbox.isSelected()) {
                checkbox.click();
            }
            log.info("✓ Confirmation checkbox checked");

            // ── Step 8: Enter Password ──
            log.info("Entering password...");
            WebElement passwordInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(passwordInputSelector))
            );
            passwordInput.clear();
            passwordInput.sendKeys(password);
            log.info("✓ Password entered");

            // ── Step 9: Click Continue (second time) ──
            log.info("Clicking Continue button (second): {}", continueButton2Selector);
            WebElement continueBtn2 = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector(continueButton2Selector))
            );
            continueBtn2.click();
            log.info("✓ Second Continue button clicked");

            // ── Step 10: Wait for Dashboard ──
            log.info("Waiting for dashboard (URL contains: '{}')", dashboardUrlContains);
            wait.until(ExpectedConditions.urlContains(dashboardUrlContains));
            log.info("✓ Dashboard loaded. Current URL: {}", driver.getCurrentUrl());

            // ── Step 11: Save Cookies ──
            String cookies = extractCookies(driver);
            log.info("✓ Cookies saved ({} characters)", cookies.length());

            // ── Step 12: Return Success ──
            log.info("✓ Browser login successful for PAN: {}***", pan.substring(0, Math.min(4, pan.length())));
            return new BrowserLoginResponse(cookies, "Login Successful", true);

        } catch (org.openqa.selenium.TimeoutException e) {
            String currentUrl = driver != null ? driver.getCurrentUrl() : "unknown";
            log.error("Browser login timed out. Current URL: {}. Error: {}", currentUrl, e.getMessage());
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
     * Create and configure a ChromeDriver instance.
     */
    private WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments(
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-extensions",
                "--disable-popup-blocking",
                "--disable-blink-features=AutomationControlled",
                "--window-size=1920,1080",
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        );

        // Avoid detection as automated browser
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

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
