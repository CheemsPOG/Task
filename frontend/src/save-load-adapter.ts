import type {
	ChartData,
	ChartMetaInfo,
	ChartTemplate,
	ChartTemplateContent,
	EntityId,
	IExternalSaveLoadAdapter,
	LineToolsAndGroupsLoadRequestContext,
	LineToolsAndGroupsLoadRequestType,
	LineToolsAndGroupsState,
	LineToolState,
	StudyTemplateData,
	StudyTemplateMetaInfo,
} from 'charting_library';

const storageKeys = {
	charts: 'LocalStorageSaveLoadAdapter_charts',
	studyTemplates: 'LocalStorageSaveLoadAdapter_studyTemplates',
	drawingTemplates: 'LocalStorageSaveLoadAdapter_drawingTemplates',
	chartTemplates: 'LocalStorageSaveLoadAdapter_chartTemplates',
	drawings: 'LocalStorageSaveLoadAdapter_drawings',
};

interface StoredChart extends ChartData {
	id: string;
	timestamp: number;
}

interface DrawingTemplateRecord {
	name: string;
	content: string;
	toolName: string;
}

interface ChartTemplateRecord {
	name: string;
	content: ChartTemplateContent;
}

type DrawingStore = Record<string, Record<string, LineToolState>>;

export class LocalStorageSaveLoadAdapter implements IExternalSaveLoadAdapter {
	private _charts: StoredChart[];
	private _studyTemplates: StudyTemplateData[];
	private _drawingTemplates: DrawingTemplateRecord[];
	private _chartTemplates: ChartTemplateRecord[];
	private _drawings: DrawingStore;
	private _isDirty = false;

	constructor() {
		this._charts = this._getFromLocalStorage(storageKeys.charts) ?? [];
		this._studyTemplates =
			this._getFromLocalStorage(storageKeys.studyTemplates) ?? [];
		this._drawingTemplates =
			this._getFromLocalStorage(storageKeys.drawingTemplates) ?? [];
		this._chartTemplates =
			this._getFromLocalStorage(storageKeys.chartTemplates) ?? [];
		this._drawings = this._getFromLocalStorage(storageKeys.drawings) ?? {};
		setInterval(() => {
			if (this._isDirty) {
				this._saveAllToLocalStorage();
				this._isDirty = false;
			}
		}, 1000);
	}

	getAllCharts(): Promise<ChartMetaInfo[]> {
		return Promise.resolve(this._charts as unknown as ChartMetaInfo[]);
	}

	removeChart<T extends number | string>(id: T): Promise<void> {
		for (let i = 0; i < this._charts.length; ++i) {
			if (this._charts[i].id === String(id)) {
				this._charts.splice(i, 1);
				this._isDirty = true;
				return Promise.resolve();
			}
		}
		return Promise.reject(new Error('The chart does not exist'));
	}

	saveChart(chartData: ChartData): Promise<string> {
		if (!chartData.id) {
			chartData.id = this._generateUniqueChartId();
		} else {
			void this.removeChart(chartData.id);
		}
		const savedChartData: StoredChart = {
			...chartData,
			id: chartData.id,
			timestamp: Math.round(Date.now() / 1000),
		};
		this._charts.push(savedChartData);
		this._isDirty = true;
		return Promise.resolve(savedChartData.id);
	}

	getChartContent(id: number): Promise<string> {
		for (let i = 0; i < this._charts.length; ++i) {
			if (this._charts[i].id === String(id)) {
				return Promise.resolve(this._charts[i].content);
			}
		}
		return Promise.reject(new Error('The chart does not exist'));
	}

	removeStudyTemplate(studyTemplateData: StudyTemplateMetaInfo): Promise<void> {
		for (let i = 0; i < this._studyTemplates.length; ++i) {
			if (this._studyTemplates[i].name === studyTemplateData.name) {
				this._studyTemplates.splice(i, 1);
				this._isDirty = true;
				return Promise.resolve();
			}
		}
		return Promise.reject(new Error('The study template does not exist'));
	}

	getStudyTemplateContent(studyTemplateData: StudyTemplateMetaInfo): Promise<string> {
		for (let i = 0; i < this._studyTemplates.length; ++i) {
			if (this._studyTemplates[i].name === studyTemplateData.name) {
				return Promise.resolve(this._studyTemplates[i].content);
			}
		}
		return Promise.reject(new Error('The study template does not exist'));
	}

	saveStudyTemplate(studyTemplateData: StudyTemplateData): Promise<void> {
		for (let i = 0; i < this._studyTemplates.length; ++i) {
			if (this._studyTemplates[i].name === studyTemplateData.name) {
				this._studyTemplates.splice(i, 1);
				break;
			}
		}
		this._studyTemplates.push(studyTemplateData);
		this._isDirty = true;
		return Promise.resolve();
	}

	getAllStudyTemplates(): Promise<StudyTemplateMetaInfo[]> {
		return Promise.resolve(this._studyTemplates);
	}

	removeDrawingTemplate(toolName: string, templateName: string): Promise<void> {
		for (let i = 0; i < this._drawingTemplates.length; ++i) {
			if (
				this._drawingTemplates[i].name === templateName &&
				this._drawingTemplates[i].toolName === toolName
			) {
				this._drawingTemplates.splice(i, 1);
				this._isDirty = true;
				return Promise.resolve();
			}
		}
		return Promise.reject(new Error('The drawing template does not exist'));
	}

	loadDrawingTemplate(toolName: string, templateName: string): Promise<string> {
		for (let i = 0; i < this._drawingTemplates.length; ++i) {
			if (
				this._drawingTemplates[i].name === templateName &&
				this._drawingTemplates[i].toolName === toolName
			) {
				return Promise.resolve(this._drawingTemplates[i].content);
			}
		}
		return Promise.reject(new Error('The drawing template does not exist'));
	}

	saveDrawingTemplate(
		toolName: string,
		templateName: string,
		content: string
	): Promise<void> {
		for (let i = 0; i < this._drawingTemplates.length; ++i) {
			if (
				this._drawingTemplates[i].name === templateName &&
				this._drawingTemplates[i].toolName === toolName
			) {
				this._drawingTemplates.splice(i, 1);
				break;
			}
		}
		this._drawingTemplates.push({
			name: templateName,
			content: content,
			toolName: toolName,
		});
		this._isDirty = true;
		return Promise.resolve();
	}

	getDrawingTemplates(_toolName: string): Promise<string[]> {
		return Promise.resolve(
			this._drawingTemplates.map(template => template.name)
		);
	}

	async getAllChartTemplates(): Promise<string[]> {
		return this._chartTemplates.map(x => x.name);
	}

	async saveChartTemplate(
		templateName: string,
		content: ChartTemplateContent
	): Promise<void> {
		const theme = this._chartTemplates.find(x => x.name === templateName);
		if (theme) {
			theme.content = content;
		} else {
			this._chartTemplates.push({ name: templateName, content });
		}
		this._isDirty = true;
	}

	async removeChartTemplate(templateName: string): Promise<void> {
		this._chartTemplates = this._chartTemplates.filter(
			x => x.name !== templateName
		);
		this._isDirty = true;
	}

	async getChartTemplateContent(templateName: string): Promise<ChartTemplate> {
		const content = this._chartTemplates.find(
			x => x.name === templateName
		)?.content;
		return {
			content: structuredClone(content),
		};
	}

	async saveLineToolsAndGroups(
		layoutId: string | undefined,
		chartId: string | number,
		state: LineToolsAndGroupsState
	): Promise<void> {
		const drawings = state.sources;
		if (!drawings) return;
		const drawingKey = this._getDrawingKey(layoutId, chartId);
		if (!this._drawings[drawingKey]) {
			this._drawings[drawingKey] = {};
		}
		for (const [key, drawingState] of drawings) {
			if (drawingState === null) {
				delete this._drawings[drawingKey][key];
			} else {
				this._drawings[drawingKey][key] = drawingState;
			}
		}
		this._isDirty = true;
	}

	async loadLineToolsAndGroups(
		layoutId: string | undefined,
		chartId: string | number,
		_requestType: LineToolsAndGroupsLoadRequestType,
		_requestContext: LineToolsAndGroupsLoadRequestContext
	): Promise<Partial<LineToolsAndGroupsState> | null> {
		if (!layoutId) {
			return null;
		}
		const rawSources = this._drawings[this._getDrawingKey(layoutId, chartId)];
		if (!rawSources) return null;
		const sources = new Map<EntityId, LineToolState | null>();
		for (const [key, state] of Object.entries(rawSources)) {
			sources.set(key as EntityId, state);
		}
		return {
			sources,
		};
	}

	private _generateUniqueChartId(): string {
		const existingIds = this._charts.map(i => i.id);
		while (true) {
			const uid = Math.random().toString(16).slice(2);
			if (!existingIds.includes(uid)) {
				return uid;
			}
		}
	}

	private _getFromLocalStorage<T>(key: string): T | null {
		const dataFromStorage = window.localStorage.getItem(key);
		return JSON.parse(dataFromStorage || 'null') as T | null;
	}

	private _saveToLocalStorage(key: string, data: unknown): void {
		const dataString = JSON.stringify(data);
		window.localStorage.setItem(key, dataString);
	}

	private _saveAllToLocalStorage(): void {
		this._saveToLocalStorage(storageKeys.charts, this._charts);
		this._saveToLocalStorage(storageKeys.studyTemplates, this._studyTemplates);
		this._saveToLocalStorage(
			storageKeys.drawingTemplates,
			this._drawingTemplates
		);
		this._saveToLocalStorage(storageKeys.chartTemplates, this._chartTemplates);
		this._saveToLocalStorage(storageKeys.drawings, this._drawings);
	}

	private _getDrawingKey(
		layoutId: string | undefined,
		chartId: string | number
	): string {
		return `${layoutId}/${chartId}`;
	}
}
