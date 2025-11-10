// Cookie Consent Manager
class CookieConsentManager {
    constructor() {
        this.init();
    }

    async init() {
        // Sprawdź czy użytkownik już wyraził zgodę
        const status = await this.getConsentStatus();
        if (status.showBanner) {
            this.createBanner();
        }
    }

    async getConsentStatus() {
        try {
            const response = await fetch('/api/cookies/status');
            return await response.json();
        } catch (error) {
            console.error('Błąd podczas pobierania statusu cookies:', error);
            return { showBanner: true, hasConsent: false };
        }
    }

    createBanner() {
        const banner = document.createElement('div');
        banner.id = 'cookie-banner';
        banner.className = 'cookie-banner';
        banner.innerHTML = `
            <div class="container">
                <div class="cookie-banner-content">
                    <div class="cookie-banner-title">
                        <i class="bi bi-cookie"></i>
                        Używamy plików cookie
                    </div>
                    <div class="cookie-banner-text">
                        Ta strona używa plików cookie w celu zapewnienia najlepszego doświadczenia. 
                        Kontynuując korzystanie ze strony, wyrażasz zgodę na ich używanie.
                    </div>
                </div>
                <div class="cookie-banner-actions">
                    <button type="button" class="btn btn-settings" onclick="cookieConsent.showSettings()">
                        Ustawienia
                    </button>
                    <button type="button" class="btn btn-reject" onclick="cookieConsent.reject()">
                        Odrzuć
                    </button>
                    <button type="button" class="btn btn-accept" onclick="cookieConsent.accept()">
                        Zaakceptuj wszystkie
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(banner);
        
        // Pokaż banner z animacją
        setTimeout(() => {
            banner.classList.add('show');
        }, 100);
    }

    createSettingsModal() {
        const modal = document.createElement('div');
        modal.id = 'cookie-modal';
        modal.className = 'cookie-modal';
        modal.innerHTML = `
            <div class="cookie-modal-content">
                <div class="cookie-modal-header">
                    <div class="cookie-modal-title">Ustawienia plików cookie</div>
                    <div class="cookie-modal-subtitle">
                        Możesz zarządzać preferencjami dotyczącymi plików cookie. 
                        Niektóre z nich są niezbędne do działania strony.
                    </div>
                </div>
                
                <div class="cookie-category">
                    <div class="cookie-category-header">
                        <div class="cookie-category-title">Niezbędne</div>
                        <label class="cookie-toggle">
                            <input type="checkbox" checked disabled>
                            <span class="cookie-toggle-slider"></span>
                        </label>
                    </div>
                    <div class="cookie-category-description">
                        Te pliki cookie są niezbędne do działania strony i nie można ich wyłączyć. 
                        Obejmują uwierzytelnianie, bezpieczeństwo i podstawowe funkcje.
                    </div>
                </div>

                <div class="cookie-category">
                    <div class="cookie-category-header">
                        <div class="cookie-category-title">Funkcjonalne</div>
                        <label class="cookie-toggle">
                            <input type="checkbox" id="functional-cookies" checked>
                            <span class="cookie-toggle-slider"></span>
                        </label>
                    </div>
                    <div class="cookie-category-description">
                        Te pliki cookie pozwalają na zapamiętywanie Twoich preferencji.
                    </div>
                </div>

                <div class="cookie-category">
                    <div class="cookie-category-header">
                        <div class="cookie-category-title">Analityczne</div>
                        <label class="cookie-toggle">
                            <input type="checkbox" id="analytics-cookies">
                            <span class="cookie-toggle-slider"></span>
                        </label>
                    </div>
                    <div class="cookie-category-description">
                        Te pliki cookie pomagają nam zrozumieć, jak korzystasz ze strony, 
                        aby móc ją ulepszać.
                    </div>
                </div>

                <div class="cookie-modal-actions">
                    <button type="button" class="btn btn-secondary" onclick="cookieConsent.closeSettings()">
                        Anuluj
                    </button>
                    <button type="button" class="btn btn-primary" onclick="cookieConsent.saveSettings()">
                        Zapisz ustawienia
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        
        // Pokaż modal z animacją
        setTimeout(() => {
            modal.classList.add('show');
        }, 10);

        // Zamknij modal przy kliknięciu na tło
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                this.closeSettings();
            }
        });
    }

    showSettings() {
        this.createSettingsModal();
    }

    closeSettings() {
        const modal = document.getElementById('cookie-modal');
        if (modal) {
            modal.classList.remove('show');
            setTimeout(() => {
                modal.remove();
            }, 300);
        }
    }

    async accept() {
        await this.setConsent(true, 'all');
        this.hideBanner();
        this.showNotification('Zgoda na pliki cookie została udzielona', 'success');
    }

    async reject() {
        await this.setConsent(false, 'essential');
        this.hideBanner();
        this.showNotification('Zgoda na pliki cookie została odrzucona', 'info');
    }

    async saveSettings() {
        const functional = document.getElementById('functional-cookies')?.checked || false;
        const analytics = document.getElementById('analytics-cookies')?.checked || false;
        
        let preferences = 'essential';
        if (functional && analytics) {
            preferences = 'all';
        } else if (functional) {
            preferences = 'functional';
        }

        await this.setConsent(true, preferences);
        this.closeSettings();
        this.hideBanner();
        this.showNotification('Ustawienia cookies zostały zapisane', 'success');
    }

    async setConsent(accepted, preferences) {
        try {
            const response = await fetch('/api/cookies/consent', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `accepted=${accepted}&preferences=${preferences}`
            });
            
            if (!response.ok) {
                throw new Error('Błąd podczas zapisywania zgody');
            }
            
            return await response.json();
        } catch (error) {
            console.error('Błąd podczas zapisywania zgody:', error);
            this.showNotification('Wystąpił błąd podczas zapisywania ustawień', 'error');
        }
    }

    hideBanner() {
        const banner = document.getElementById('cookie-banner');
        if (banner) {
            banner.classList.remove('show');
            setTimeout(() => {
                banner.remove();
            }, 300);
        }
    }

    showNotification(message, type = 'info') {
        // Sprawdź czy istnieje kontener na powiadomienia Bootstrap
        const alertClass = type === 'success' ? 'alert-success' : 
                          type === 'error' ? 'alert-danger' : 'alert-info';
        
        const notification = document.createElement('div');
        notification.className = `alert ${alertClass} alert-dismissible fade show position-fixed`;
        notification.style.cssText = 'top: 20px; right: 20px; z-index: 10001; min-width: 300px;';
        notification.innerHTML = `
            ${message}
            <button type="button" class="btn-close" aria-label="Zamknij"></button>
        `;

        document.body.appendChild(notification);

        // Auto hide po 5 sekundach
        setTimeout(() => {
            notification.classList.remove('show');
            setTimeout(() => {
                notification.remove();
            }, 150);
        }, 5000);

        // Zamknij po kliknięciu
        notification.querySelector('.btn-close').addEventListener('click', () => {
            notification.classList.remove('show');
            setTimeout(() => {
                notification.remove();
            }, 150);
        });
    }
}

// Inicjalizacja po załadowaniu strony
let cookieConsent;
document.addEventListener('DOMContentLoaded', () => {
    cookieConsent = new CookieConsentManager();
});