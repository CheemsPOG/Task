import type { Overrides, ThemeName } from 'charting_library';

function getRequestedTheme(): ThemeName | null {
	const requestedTheme = new URLSearchParams(window.location.search).get('theme');
	return requestedTheme === 'dark' || requestedTheme === 'light'
		? requestedTheme
		: null;
}

function prefersDarkTheme(): boolean {
	return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
}

const customCSS = `
  #theme-toggle {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 12px;
  }

  .switcher {
    display: inline-block;
    position: relative;
    flex: 0 0 auto;
    width: 38px;
    height: 20px;
    vertical-align: middle;
    z-index: 0;
  }

  .switcher input {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
    z-index: 1;
    cursor: pointer;
  }

  .switcher .thumb-wrapper {
    display: block;
    border-radius: 20px;
    position: relative;
    width: 100%;
    height: 100%;
  }

  .switcher .track {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    border-radius: 20px;
    background-color: #a3a6af;
  }

  #theme-switch:checked + .thumb-wrapper .track {
    background-color: #2962ff;
  }

  .switcher .thumb {
    display: block;
    width: 14px;
    height: 14px;
    border-radius: 14px;
    transition: transform 250ms ease-out;
    transform: translate(3px, 3px);
    background: #fff;
  }

  .switcher input:checked + .thumb-wrapper .thumb {
    transform: translate(21px, 3px);
  }

  #fx-quote-price {
    display: flex;
    align-items: center;
    min-width: 5.5em;
    padding: 0 10px;
    font-size: 14px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    letter-spacing: 0.02em;
    pointer-events: none;
  }
`;

const cssBlob = new Blob([customCSS], { type: 'text/css' });

export const cssBlobUrl = URL.createObjectURL(cssBlob);
export const theme: ThemeName =
	getRequestedTheme() ?? (prefersDarkTheme() ? 'dark' : 'light');

export function getChartOverrides(currentTheme: ThemeName = theme): Overrides {
	if (currentTheme === 'dark') {
		return {
			'paneProperties.background': '#111827',
			'paneProperties.vertGridProperties.color': 'rgba(255, 255, 255, 0.08)',
			'paneProperties.horzGridProperties.color': 'rgba(255, 255, 255, 0.08)',
			'scalesProperties.textColor': '#ffffff',
			'mainSeriesProperties.showCountdown': true,
			'mainSeriesProperties.statusViewStyle.showExchange': false,
		};
	}

	return {
		'paneProperties.background': '#f8fafc',
		'paneProperties.vertGridProperties.color': 'rgba(15, 23, 42, 0.08)',
		'paneProperties.horzGridProperties.color': 'rgba(15, 23, 42, 0.08)',
		'scalesProperties.textColor': '#1f2933',
		'mainSeriesProperties.showCountdown': true,
		'mainSeriesProperties.statusViewStyle.showExchange': false,
	};
}
