/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Extra TradingView header buttons that are not part of the datafeed.
 *
 * - Theme switch: widget.changeTheme + pane overrides from theme.ts
 * - Logout: calls the callback from main.ts (POST /api/auth/logout + reload)
 *
 * FX quote ticker / BID-ASK-MID dropdown live in fx/quoteToolbar.ts, not here.
 */

import type { IChartingLibraryWidget, ThemeName } from 'charting_library';
import { saveTheme } from './chartPrefs.ts';
import { getChartOverrides, resolveTheme } from './theme.ts';

export function installThemeToolbar(widget: IChartingLibraryWidget): void {
	widget.headerReady().then(() => {
		const themeToggleEl = widget.createButton({
			useTradingViewStyle: false,
			align: 'right',
		});
		if (!themeToggleEl) {
			return;
		}

		themeToggleEl.dataset.internalAllowKeyboardNavigation = 'true';
		themeToggleEl.id = 'theme-toggle';
		themeToggleEl.innerHTML = `<label for="theme-switch" id="theme-switch-label"></label>
      <div class="switcher">
        <input type="checkbox" id="theme-switch" tabindex="-1">
        <span class="thumb-wrapper">
          <span class="track"></span>
          <span class="thumb"></span>
        </span>
      </div>`;
		themeToggleEl.title = 'Toggle theme';

		const checkboxEl = themeToggleEl.querySelector(
			'#theme-switch'
		) as HTMLInputElement;
		const labelEl = themeToggleEl.querySelector(
			'#theme-switch-label'
		) as HTMLLabelElement;

		function updateLabel(): void {
			labelEl.textContent = checkboxEl.checked ? 'Dark theme' : 'Light theme';
		}

		checkboxEl.checked =
			typeof widget.getTheme === 'function'
				? widget.getTheme() === 'dark'
				: resolveTheme() === 'dark';
		updateLabel();

		checkboxEl.addEventListener('change', async function onChange() {
			const themeToSet: ThemeName = this.checked ? 'dark' : 'light';
			this.disabled = true;
			try {
				await widget.changeTheme(themeToSet, { disableUndo: true });
				widget.applyOverrides(getChartOverrides(themeToSet));
				saveTheme(themeToSet);
			} finally {
				this.disabled = false;
				updateLabel();
			}
		});
	});
}

/**
 * Adds a Logout control to the chart header.
 *
 * @param widget chart widget
 * @param onLogout callback when Logout is clicked
 */
export function installLogoutButton(
	widget: IChartingLibraryWidget,
	onLogout: () => void
): void {
	widget.headerReady().then(() => {
		const buttonEl = widget.createButton({
			useTradingViewStyle: false,
			align: 'right',
		});
		if (!buttonEl) {
			return;
		}

		buttonEl.id = 'logout-button';
		buttonEl.textContent = 'Logout';
		buttonEl.title = 'Log out';
		buttonEl.style.cursor = 'pointer';
		buttonEl.style.padding = '0 10px';
		buttonEl.addEventListener('click', () => {
			onLogout();
		});
	});
}
