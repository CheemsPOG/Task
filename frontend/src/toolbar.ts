import type { IChartingLibraryWidget, ThemeName } from 'charting_library';
import { getChartOverrides, theme as initialTheme } from './theme.ts';

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
				: initialTheme === 'dark';
		updateLabel();

		checkboxEl.addEventListener('change', async function onChange() {
			const themeToSet: ThemeName = this.checked ? 'dark' : 'light';
			this.disabled = true;
			try {
				await widget.changeTheme(themeToSet, { disableUndo: true });
				widget.applyOverrides(getChartOverrides(themeToSet));
			} finally {
				this.disabled = false;
				updateLabel();
			}
		});
	});
}
