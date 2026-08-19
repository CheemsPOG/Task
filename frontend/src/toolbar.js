import { getChartOverrides, theme as initialTheme } from './theme.js';

export function installThemeToolbar(widget) {
	widget.headerReady().then(() => {
		const themeToggleEl = widget.createButton({
			useTradingViewStyle: false,
			align: 'right',
		});

		themeToggleEl.id = 'theme-toggle';
		themeToggleEl.innerHTML = `<label for="theme-switch" id="theme-switch-label"></label>
      <div class="switcher">
        <input type="checkbox" id="theme-switch">
        <span class="thumb-wrapper">
          <span class="track"></span>
          <span class="thumb"></span>
        </span>
      </div>`;
		themeToggleEl.title = 'Toggle theme';

		const checkboxEl = themeToggleEl.querySelector('#theme-switch');
		const labelEl = themeToggleEl.querySelector('#theme-switch-label');

		function updateLabel() {
			labelEl.textContent = checkboxEl.checked ? 'Dark theme' : 'Light theme';
		}

		checkboxEl.checked =
			typeof widget.getTheme === 'function'
				? widget.getTheme() === 'dark'
				: initialTheme === 'dark';
		updateLabel();

		checkboxEl.addEventListener('change', async function onChange() {
			const themeToSet = this.checked ? 'dark' : 'light';
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
