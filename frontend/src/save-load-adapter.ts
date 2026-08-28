/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * TradingView IExternalSaveLoadAdapter backed by Java REST (docs 127–139).
 *
 * Widget Save/Load (header_saveload + study_templates +
 * chart_template_storage) calls this class. Layouts → /api/layouts.
 * Study templates → /api/indicator-templates. Chart style templates →
 * /api/chart-templates. JWT customer_no is applied
 * on the server. Drawings live inside layout content; drawing-template and
 * line-tool APIs have no Peach tables so those methods stay empty stubs.
 * Deleting the last layout only closes the Load dialog; the visible chart
 * is left as-is. getChartContent 404 returns cached/draft JSON (never `{}`
 * and never widget.save) so the Load dialog cannot deadlock.
 */

import type {
	ChartData,
	ChartMetaInfo,
	ChartTemplate,
	ChartTemplateContent,
	IExternalSaveLoadAdapter,
	LineToolsAndGroupsLoadRequestContext,
	LineToolsAndGroupsLoadRequestType,
	LineToolsAndGroupsState,
	ResolutionString,
	StudyTemplateData,
	StudyTemplateMetaInfo,
} from 'charting_library';
import { ApiHttpError, apiDelete, apiGet, apiPost, apiPut } from './api.ts';
import {
	clearLastLayoutId,
	getFactoryChartContent,
	loadChartDraft,
	loadChartPrefs,
	saveLastLayoutId,
} from './chartPrefs.ts';

export interface LayoutListItem {
	id: number;
	name: string;
	resolution: string;
	symbol: string;
	timestamp: number;
}

export interface SaveLoadAdapterHooks {
	onLastLayoutRemoved?: () => void;
}

interface LayoutIdResponse {
	id: number;
}

interface LayoutDetail {
	id: number;
	name: string;
	timestamp: number;
	content: string;
}

interface NamedTemplate {
	name: string;
}

interface NamedTemplateDetail {
	name: string;
	content: string;
}

function templatePath(base: string, name: string): string {
	return `${base}/${encodeURIComponent(name)}`;
}

function drawingTemplateUnsupported(): Promise<never> {
	return Promise.reject(new Error('Drawing templates are not stored on the server'));
}

function isNotFound(error: unknown): boolean {
	return error instanceof ApiHttpError && error.status === 404;
}

function cachedChartContent(lastContent: string | null): string {
	if (lastContent && lastContent !== '{}') {
		return lastContent;
	}
	const draft = loadChartDraft();
	if (draft) {
		return JSON.stringify(draft);
	}
	return getFactoryChartContent() ?? lastContent ?? '';
}

export class ServerSaveLoadAdapter implements IExternalSaveLoadAdapter {
	private lastChartContent: string | null = null;

	constructor(private readonly hooks: SaveLoadAdapterHooks = {}) {}

	async getAllCharts(): Promise<ChartMetaInfo[]> {
		const layouts = await apiGet<LayoutListItem[]>('/layouts');
		return layouts.map(layout => ({
			id: layout.id,
			name: layout.name,
			symbol: layout.symbol,
			resolution: layout.resolution as ResolutionString,
			timestamp: layout.timestamp,
		}));
	}

	async removeChart<T extends number | string>(id: T): Promise<void> {
		await apiDelete(`/layouts/${id}`);
		if (String(id) === loadChartPrefs().lastLayoutId) {
			clearLastLayoutId();
		}
		try {
			const remaining = await apiGet<LayoutListItem[]>('/layouts');
			if (remaining.length === 0) {
				window.setTimeout(() => this.hooks.onLastLayoutRemoved?.(), 0);
			}
		} catch {
			// Delete already succeeded; list refresh is best-effort.
		}
	}

	async saveChart(chartData: ChartData): Promise<string> {
		this.lastChartContent = chartData.content;
		const body = {
			name: chartData.name,
			content: chartData.content,
			symbol: chartData.symbol,
			resolution: chartData.resolution,
		};
		const saved = chartData.id
			? await apiPut<LayoutIdResponse>(`/layouts/${chartData.id}`, body)
			: await apiPost<LayoutIdResponse>('/layouts', body);
		saveLastLayoutId(String(saved.id));
		return String(saved.id);
	}

	async getChartContent(chartId: number): Promise<string> {
		try {
			const layout = await apiGet<LayoutDetail>(`/layouts/${chartId}`);
			this.lastChartContent = layout.content;
			saveLastLayoutId(String(chartId));
			return layout.content;
		} catch (error) {
			if (!isNotFound(error)) {
				throw error;
			}
			return cachedChartContent(this.lastChartContent);
		}
	}

	async getAllStudyTemplates(): Promise<StudyTemplateMetaInfo[]> {
		return apiGet<NamedTemplate[]>('/indicator-templates');
	}

	async saveStudyTemplate(studyTemplateData: StudyTemplateData): Promise<void> {
		await apiPost('/indicator-templates', {
			name: studyTemplateData.name,
			content: studyTemplateData.content,
		});
	}

	async getStudyTemplateContent(
		studyTemplateInfo: StudyTemplateMetaInfo
	): Promise<string> {
		const template = await apiGet<NamedTemplateDetail>(
			templatePath('/indicator-templates', studyTemplateInfo.name)
		);
		return template.content;
	}

	async removeStudyTemplate(studyTemplateInfo: StudyTemplateMetaInfo): Promise<void> {
		await apiDelete(templatePath('/indicator-templates', studyTemplateInfo.name));
	}

	async getAllChartTemplates(): Promise<string[]> {
		const templates = await apiGet<NamedTemplate[]>('/chart-templates');
		return templates.map(template => template.name);
	}

	async saveChartTemplate(
		templateName: string,
		theme: ChartTemplateContent
	): Promise<void> {
		await apiPost('/chart-templates', {
			name: templateName,
			content: JSON.stringify(theme),
		});
	}

	async getChartTemplateContent(templateName: string): Promise<ChartTemplate> {
		try {
			const template = await apiGet<NamedTemplateDetail>(
				templatePath('/chart-templates', templateName)
			);
			try {
				return { content: JSON.parse(template.content) as ChartTemplateContent };
			} catch {
				throw new Error('Chart template content is not valid JSON');
			}
		} catch (error) {
			// Save-as calls this first; 404 means the name is new. Returning
			// {} (no content) makes isThemeExist false so saveChartTemplate runs.
			if (isNotFound(error)) {
				return {};
			}
			throw error;
		}
	}

	async removeChartTemplate(templateName: string): Promise<void> {
		await apiDelete(templatePath('/chart-templates', templateName));
	}

	getDrawingTemplates(_toolName: string): Promise<string[]> {
		return Promise.resolve([]);
	}

	loadDrawingTemplate(_toolName: string, _templateName: string): Promise<string> {
		return drawingTemplateUnsupported();
	}

	removeDrawingTemplate(_toolName: string, _templateName: string): Promise<void> {
		return drawingTemplateUnsupported();
	}

	saveDrawingTemplate(
		_toolName: string,
		_templateName: string,
		_content: string
	): Promise<void> {
		return drawingTemplateUnsupported();
	}

	async saveLineToolsAndGroups(
		_layoutId: string | undefined,
		_chartId: string | number,
		_state: LineToolsAndGroupsState
	): Promise<void> {
		return;
	}

	async loadLineToolsAndGroups(
		_layoutId: string | undefined,
		_chartId: string | number,
		_requestType: LineToolsAndGroupsLoadRequestType,
		_requestContext: LineToolsAndGroupsLoadRequestContext
	): Promise<Partial<LineToolsAndGroupsState> | null> {
		return null;
	}
}
