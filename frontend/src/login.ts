/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

import { login } from './auth.ts';

export type LoginOverlayCallbacks = {
	onLoggedIn: () => void;
};

/**
 * Wires the login overlay form (Login / Demo).
 *
 * @param callbacks after successful login
 */
export function installLoginOverlay(callbacks: LoginOverlayCallbacks): void {
	const overlay = document.getElementById('login-overlay');
	const form = document.getElementById('login-form') as HTMLFormElement | null;
	const usernameInput = document.getElementById('login-username') as HTMLInputElement | null;
	const passwordInput = document.getElementById('login-password') as HTMLInputElement | null;
	const errorEl = document.getElementById('login-error');
	const demoButton = document.getElementById('login-demo');
	const submitButton = document.getElementById('login-submit') as HTMLButtonElement | null;

	if (!overlay || !form || !usernameInput || !passwordInput || !errorEl || !demoButton) {
		throw new Error('Login overlay markup is missing from index.html');
	}

	function setError(message: string): void {
		errorEl!.textContent = message;
		errorEl!.hidden = !message;
	}

	demoButton.addEventListener('click', () => {
		usernameInput.value = 'demo';
		passwordInput.value = 'demo';
		setError('');
	});

	form.addEventListener('submit', async (event) => {
		event.preventDefault();
		setError('');
		if (submitButton) {
			submitButton.disabled = true;
		}
		try {
			await login(usernameInput.value.trim(), passwordInput.value);
			hideLoginOverlay();
			callbacks.onLoggedIn();
		} catch (error) {
			const message = error instanceof Error ? error.message : 'Login failed';
			setError(message);
		} finally {
			if (submitButton) {
				submitButton.disabled = false;
			}
		}
	});
}

/**
 * Shows the login overlay and clears any previous error.
 */
export function showLoginOverlay(): void {
	const overlay = document.getElementById('login-overlay');
	const errorEl = document.getElementById('login-error');
	if (errorEl) {
		errorEl.textContent = '';
		errorEl.hidden = true;
	}
	overlay?.classList.add('is-visible');
	overlay?.removeAttribute('aria-hidden');
	const usernameInput = document.getElementById('login-username') as HTMLInputElement | null;
	usernameInput?.focus();
}

/**
 * Hides the login overlay.
 */
export function hideLoginOverlay(): void {
	const overlay = document.getElementById('login-overlay');
	overlay?.classList.remove('is-visible');
	overlay?.setAttribute('aria-hidden', 'true');
}
